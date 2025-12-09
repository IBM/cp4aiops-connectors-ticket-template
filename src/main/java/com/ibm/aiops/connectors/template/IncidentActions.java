/***********************************************************************
 *
 *      IBM Confidential
 *
 *      (C) Copyright IBM Corp. 2024
 *
 *      5737-M96
 *
 **********************************************************************/

package com.ibm.aiops.connectors.template;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.aiops.connectors.template.model.Configuration;
import com.ibm.aiops.connectors.template.model.IssueModel;
import com.ibm.cp4waiops.connectors.sdk.JsonParsing;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionDataDeserializationException;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionRequest;
import com.ibm.cp4waiops.connectors.sdk.actions.ConnectorActionException;

import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.Counter;

public class IncidentActions implements Runnable {
    ConnectorAction action;

    static final Logger logger = Logger.getLogger(IncidentActions.class.getName());
    static String ACTION_TicketingSystem_RESPONSE = "cp4waiops-cartridge.itsmincidentresponse";// "cp4waiops-cartridge.Ticketing
                                                                                               // Systemcreateresponse";

    HttpClientUtil httpClientUtil;

    public IncidentActions(ConnectorAction action) {
        this.action = action;
        this.httpClientUtil = new HttpClientUtil(action.configuration.getUrl(), action.configuration.getUsername(),
                action.configuration.getPassword());
    }

    public void run() {
        logger.log(Level.INFO, "Run Incident Actions");
        if (action.actionType.equals(ConnectorConstants.ISSUE_CREATE)) {
            createIncident(action);
        } else if (action.actionType.equals(ConnectorConstants.ISSUE_UPDATE)) {
            updateIncident(action);
        } else if (action.actionType.equals(ConnectorConstants.ISSUE_CLOSE)) {
            closeIncident(action);
        }

    }

    /**
     * Creates an issue in the ticketing system
     *
     * @param action
     *            the ConnectorAction object
     */
    // Todo: make modifications if needed
    private void createIncident(ConnectorAction action) {

        Counter actionCounter = action.getActionCounter();
        Counter actionErrorCounter = action.getActionErrorCounter();

        Configuration config = action.getConfiguration();
        TicketConnector connector = action.getConnector();
        ActionRequest request = action.getActionRequest();

        // Map incoming data bytes to a JSON structure
        ObjectNode requestContent = null;
        try {
            requestContent = request.dataAs(ObjectNode.class);
        } catch (ActionDataDeserializationException e) {
            actionErrorCounter.increment();
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        try {
            ObjectNode responseJSON = createIssue(requestContent, config.getMappings());
            logger.log(Level.INFO, "Notify Creation Completable Future integration Response", responseJSON);
            if (responseJSON.get("status").asText().equals("success")) {
                // trigger kafka topic to push it to insights.
                logger.log(Level.INFO, "Triggering Kafka topic");
                ObjectMapper objectMapper = new ObjectMapper();
                // Todo: This this the response from your system. Change the id and number to fit with the system you
                // are using.
                // For example GitHub have {id:"someid", html_url:"permalink to issue", number:"issue number"} as the
                // response. We use these to link between AIOps incident id and Github number.

                String responseBody = responseJSON.get("data").asText();
                JsonNode data = objectMapper.readTree(responseBody);
                logger.log(Level.INFO, "Triggering Kafka topic data", data);
                // Todo: you can change html_url to point to your systems permalink
                String permalink = data.get("html_url").asText();
                logger.log(Level.INFO, "Triggering Kafka topic", data.get("html_url").asText());

                // Todo: Change the id and number to fit with the system you are using.
                // IssueModel.getResponse is used to set the mappings.
                String response = IssueModel.getResponse(data.get("id").asText(), true,
                        "Created incident with id =  " + data.get("number").asText(), data.get("number").asText(),
                        connector.getConnectorID(), IssueModel.getStoryId(request.getData()), "Successful", permalink);

                // Todo: Change the TicketingSystem to the name of your system
                CloudEvent ce = connector.createEvent(0, "com.ibm.sdlc.TicketingSystem.issue.create.response", response,
                        new URI(permalink));
                connector.emitCloudEvent(ACTION_TicketingSystem_RESPONSE, connector.getPartition(), ce);
                /**
                 * This saves as something as below { "name": ConnectorConstants.TICKET_TYPE, "permalink": "the one you
                 * set with html_url", "ticket_num": "number", "id":"id" }
                 */
                // When you are referecing to get the id you can use these fields. Check and modify
                // IssueModel.getIssueId in IssueModel.java file. We use this in updateIncident method.

                actionCounter.increment();
            } else {
                actionErrorCounter.increment();
            }
        } catch (ConnectorActionException ex) {
            actionErrorCounter.increment();
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (Exception e) {
            actionErrorCounter.increment();
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Updates an issue in the ticketing system
     *
     * @param action
     *            the ConnectorAction object
     */
    // Todo: make modifications if needed
    private void updateIncident(ConnectorAction action) {

        Counter actionCounter = action.getActionCounter();
        Counter actionErrorCounter = action.getActionErrorCounter();

        Configuration config = action.getConfiguration();
        ActionRequest request = action.getActionRequest();

        // Map incoming data bytes to a JSON structure
        ObjectNode requestContent = null;
        try {
            requestContent = request.dataAs(ObjectNode.class);
        } catch (ActionDataDeserializationException e) {
            actionErrorCounter.increment();
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        try {
            // Todo: IssueModel.getIssueId gets you the number/id of the system that you mapped AIOps incident id with.
            // Change it accordingly as required.
            String issueNum = IssueModel.getIssueId(request.getData());
            if (issueNum != null) {
                ObjectNode responseJSON = updateIssue(requestContent, config.getMappings(), issueNum);
                logger.log(Level.INFO, "Notify Updating Completable Future integration Response", responseJSON);
                if (responseJSON.get("status").asText().equals("success")) {
                    actionCounter.increment();
                } else {
                    actionErrorCounter.increment();
                }
            } else {
                logger.log(Level.INFO, "Didn't find issueNum to update to Ticketing System", issueNum);
            }
        } catch (ConnectorActionException ex) {
            actionErrorCounter.increment();
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (Exception e) {
            actionErrorCounter.increment();
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    /**
     * Closes an incident in the ticketing system by updating the issue status to "Closed".
     *
     * @param action
     *            the ConnectorAction object
     */
    // Todo: make modifications if needed
    private void closeIncident(ConnectorAction action) {

        Counter actionCounter = action.getActionCounter();
        Counter actionErrorCounter = action.getActionErrorCounter();

        Configuration config = action.getConfiguration();
        ActionRequest request = action.getActionRequest();

        // Map incoming data bytes to a JSON structure
        ObjectNode requestContent = null;
        try {
            requestContent = request.dataAs(ObjectNode.class);
        } catch (ActionDataDeserializationException e) {
            actionErrorCounter.increment();
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        try {
            String issueNum = IssueModel.getIssueId(request.getData());
            if (issueNum != null) {
                ObjectNode responseJSON = closeIssue(requestContent, config.getMappings(), issueNum);
                logger.log(Level.INFO, "Notify Updating Completable Future integration Response", responseJSON);
                if (responseJSON.get("status").asText().equals("success")) {
                    actionCounter.increment();
                } else {
                    actionErrorCounter.increment();
                }
            } else {
                logger.log(Level.INFO, "Didn't find issueNum to update to Ticketing System", issueNum);
            }
        } catch (ConnectorActionException ex) {
            actionErrorCounter.increment();
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (Exception e) {
            actionErrorCounter.increment();
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    // Todo: make modifications if needed
    public ObjectNode createIssue(ObjectNode requestNode, String jsonata) {
        ObjectNode responseJson = JsonNodeFactory.instance.objectNode();

        try {
            String parsedJSON = JsonParsing.jsonataMap(requestNode.toString(), jsonata);
            logger.log(Level.INFO, "parsed json", parsedJSON);

            JsonNode parsedContent = new ObjectMapper().readTree(parsedJSON);
            logger.log(Level.INFO, "parsed content", parsedContent);

            ObjectNode requestBodyJson = JsonNodeFactory.instance.objectNode();

            Iterator<Map.Entry<String, JsonNode>> fields = parsedContent.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                requestBodyJson.put(field.getKey().toString(), field.getValue().toString());
            }

            // title and body are the keys you provided in mappings. Format them if needed
            // if (parsedContent.has("title")) {
            // requestBodyJson.put("title", parsedContent.get("title").asText());
            // }
            // if (parsedContent.has("body")) {
            // requestBodyJson.put("body", parsedContent.get("body").asText());
            // }

            try {

                String requestBody = new ObjectMapper().writeValueAsString(requestBodyJson);
                logger.log(Level.INFO, "Creating Ticketing System issue with requestBody", requestBody);

                // Todo: change the path as required by your ticketing system.
                CompletableFuture<HttpResponse<String>> res = this.httpClientUtil.post(requestBody);
                HttpResponse<String> response = res.get();
                if (response.statusCode() == 201) {
                    logger.log(Level.INFO, "Issue created successfully");
                    responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
                    responseJson.set("data", JsonNodeFactory.instance.textNode(response.body()));

                } else {
                    logger.log(Level.INFO, "Failed to create issue" + ". Status code: " + response.statusCode());
                    logger.log(Level.INFO, "Response body: " + response.body());
                    responseJson.set("status", JsonNodeFactory.instance.textNode("error"));

                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error occurred while creating Ticketing System issue", ex);

            }
        } catch (JsonProcessingException e) {
            // Set proper error status
            responseJson.set("status", JsonNodeFactory.instance.textNode("error"));
            logger.log(Level.SEVERE, "Error occurred while parsing in create issue", e); // Their fault: invalid JSON
                                                                                         // payload
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error occurred", ex);
        }
        return responseJson;
    }

    // Todo: make modifications if needed
    public ObjectNode updateIssue(ObjectNode requestNode, String jsonata, String issueNumber) {
        ObjectNode responseJson = JsonNodeFactory.instance.objectNode();

        try {
            String parsedJSON = JsonParsing.jsonataMap(requestNode.toString(), jsonata);
            logger.log(Level.INFO, "parsed json", parsedJSON);

            JsonNode parsedContent = new ObjectMapper().readTree(parsedJSON);
            logger.log(Level.INFO, "parsed content", parsedContent);

            ObjectNode requestBodyJson = JsonNodeFactory.instance.objectNode();

            Iterator<Map.Entry<String, JsonNode>> fields = parsedContent.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                requestBodyJson.put(field.getKey().toString(), field.getValue().toString());
            }

            logger.log(Level.INFO, "parsed content", parsedContent);

            String requestBody = new ObjectMapper().writeValueAsString(requestBodyJson);
            logger.log(Level.INFO, "Updating Ticketing System issue with requestBody", requestBody);

            String path = String.format("/%s", issueNumber);

            try {
                CompletableFuture<HttpResponse<String>> res = this.httpClientUtil.patch(path, requestBody);
                HttpResponse<String> response = res.get();

                if (response.statusCode() == 200) {
                    logger.log(Level.INFO, "Issue updated successfully");
                    responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
                    responseJson.set("data", JsonNodeFactory.instance.textNode(response.body()));

                } else {
                    logger.log(Level.INFO, "Failed to create issue" + ". Status code: " + response.statusCode());
                    logger.log(Level.INFO, "Response body: " + response.body());
                    responseJson.set("status", JsonNodeFactory.instance.textNode("error"));

                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error occurred while creating Ticketing System issue", ex);

            }
        } catch (JsonProcessingException e) {
            // Set proper error status
            responseJson.set("status", JsonNodeFactory.instance.textNode("error"));
            throw new ConnectorActionException(e, 400); // Their fault: invalid JSON payload
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error occurred", ex);
        }
        return responseJson;
    }

    // Todo: make modifications if needed
    public ObjectNode closeIssue(ObjectNode requestNode, String jsonata, String issueNumber) {
        ObjectNode responseJson = JsonNodeFactory.instance.objectNode();

        try {
            String parsedJSON = JsonParsing.jsonataMap(requestNode.toString(), jsonata);
            logger.log(Level.INFO, "parsed json", parsedJSON);

            JsonNode parsedContent = new ObjectMapper().readTree(parsedJSON);
            logger.log(Level.INFO, "parsed content", parsedContent);

            ObjectNode requestBodyJson = JsonNodeFactory.instance.objectNode();
            requestBodyJson.put("state", "close");

            logger.log(Level.INFO, "parsed content", parsedContent);

            String requestBody = new ObjectMapper().writeValueAsString(requestBodyJson);
            logger.log(Level.INFO, "Updating Ticketing System issue with requestBody", requestBody);

            String path = String.format("/%s", issueNumber);

            try {
                CompletableFuture<HttpResponse<String>> res = this.httpClientUtil.patch(path, requestBody);
                HttpResponse<String> response = res.get();

                if (response.statusCode() == 200) {
                    logger.log(Level.INFO, "Issue closed successfully");
                    responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
                    responseJson.set("data", JsonNodeFactory.instance.textNode(response.body()));
                } else {
                    logger.log(Level.INFO, "Failed to create issue" + ". Status code: " + response.statusCode());
                    logger.log(Level.INFO, "Response body: " + response.body());
                    responseJson.set("status", JsonNodeFactory.instance.textNode("error"));
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error occurred while creating Ticketing System issue", ex);
            }
        } catch (JsonProcessingException e) {
            // Set proper error status
            responseJson.set("status", JsonNodeFactory.instance.textNode("error"));
            throw new ConnectorActionException(e, 400); // Their fault: invalid JSON payload
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error occurred", ex);
        }
        return responseJson;
    }
}