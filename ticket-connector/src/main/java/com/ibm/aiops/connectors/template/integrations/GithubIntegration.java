/***********************************************************************
 *
 *      IBM Confidential
 *
 *      (C) Copyright IBM Corp. 2024
 *
 *      5737-M96
 *
 **********************************************************************/
package com.ibm.aiops.connectors.template.integrations;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.aiops.connectors.template.ConnectorConstants;
import com.ibm.aiops.connectors.template.HttpClientUtil;
import com.ibm.aiops.connectors.template.TicketConnector;
import com.ibm.aiops.connectors.template.Utils;
import com.ibm.aiops.connectors.template.helpers.JsonParsing;
import com.ibm.cp4waiops.connectors.sdk.actions.ConnectorActionException;

public class GithubIntegration extends Integration {

    private static final Logger logger = Logger.getLogger(IntegrationManager.class.getName());

    private HttpClientUtil httpClient;
    private TicketConnector connector;
    int MAX_RETRIES = 32;

    public GithubIntegration(HttpClientUtil httpClient, TicketConnector connector) {
        super("github");
        this.httpClient = httpClient;
        this.connector = connector;
    }

    @Override
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
                if (field.getKey().contains("labels")) {
                    ArrayNode labels = (ArrayNode) field.getValue();
                    logger.log(Level.INFO, "Creating GitHub issue with labels", labels);
                    requestBodyJson.set("labels", labels);
                } else if (field.getKey().contains("assignees")) {
                    ArrayNode assignees = (ArrayNode) field.getValue();
                    logger.log(Level.INFO, "Creating GitHub issue with assignees", assignees);
                    requestBodyJson.set("assignees", assignees);
                } else {
                    if (field.getValue().isArray() || field.getValue().isObject()) {
                        requestBodyJson.set(field.getKey(), field.getValue());
                    } else if (field.getValue().isInt()) {
                        requestBodyJson.put(field.getKey(), field.getValue().asInt());
                    } else if (field.getValue().isBoolean()) {
                        requestBodyJson.put(field.getKey(), field.getValue().asBoolean());
                    } else {
                        requestBodyJson.put(field.getKey(), field.getValue().asText());
                    }
                }
            }

            // retrying logic
            int retryCount = 2;
            while (retryCount < MAX_RETRIES) {

                try {

                    String requestBody = new ObjectMapper().writeValueAsString(requestBodyJson);
                    logger.log(Level.INFO, "Creating GitHub issue with requestBody", requestBody);

                    CompletableFuture<HttpResponse<String>> res = this.httpClient.post(ConnectorConstants.ISSUE_GITHUB,
                            requestBody);
                    HttpResponse<String> response = res.get();
                    if (response.statusCode() == 201) {
                        logger.log(Level.INFO, "Issue created successfully");
                        responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
                        responseJson.set("data", JsonNodeFactory.instance.textNode(response.body()));
                        break;
                    } else if (response.statusCode() == 401) {
                        logger.log(Level.INFO,
                                "Request was unsuccessful - Authentication credentials are invalid. Please check the configuration.");
                        this.verified = false;
                        break;
                    } else if (response.statusCode() == 403 || response.statusCode() == 429) {
                        logger.log(Level.WARNING, "Forbidden | Unauthenticated Error", response);
                        handleTrigger(response, response.statusCode());
                        retryCount = retryCount * 2;
                    } else {
                        logger.log(Level.INFO, "Failed to create issue" + ". Status code: " + response.statusCode());
                        logger.log(Level.INFO, "Response body: " + response.body());
                        responseJson.set("status", JsonNodeFactory.instance.textNode("error"));
                        break;
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error occurred while creating github issue", ex);
                    break;
                }
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

    @Override
    public ObjectNode updateIssue(ObjectNode requestNode, String jsonata, String issueNumber, String state) {
        ObjectNode responseJson = JsonNodeFactory.instance.objectNode();

        try {
            // We are adding comments before closing the ticket.
            if (state != null && state.equals("close")) {
                addComment("Closing the issue as the associated incident has been closed in AIOps.", issueNumber);
            }

            String parsedJSON = JsonParsing.jsonataMap(requestNode.toString(), jsonata);
            logger.log(Level.INFO, "parsed json", parsedJSON);

            JsonNode parsedContent = new ObjectMapper().readTree(parsedJSON);
            logger.log(Level.INFO, "parsed content", parsedContent);

            ObjectNode requestBodyJson = JsonNodeFactory.instance.objectNode();

            if (state != null && state.equals("close")) {
                requestBodyJson.put("state", "close");
            }

            Iterator<Map.Entry<String, JsonNode>> fields = parsedContent.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (field.getKey().contains("labels")) {
                    ArrayNode labels = (ArrayNode) field.getValue();
                    JsonNode issueRes = getIssueById(issueNumber);
                    logger.log(Level.INFO, "Creating GitHub issue with issueRes ", issueRes);
                    if (issueRes != null) {
                        ArrayNode labelsFromGit = (ArrayNode) issueRes.get("labels");
                        ArrayNode mergedLabels = mergeLabels(labelsFromGit, labels);
                        logger.log(Level.INFO, "Creating GitHub issue with issueRes ", mergedLabels);
                        requestBodyJson.set("labels", mergedLabels);
                    }
                } else if (field.getKey().contains("assignees")) {
                    ArrayNode assignees = (ArrayNode) field.getValue();
                    logger.log(Level.INFO, "Creating GitHub issue with assignees", assignees);
                    requestBodyJson.set("assignees", assignees);
                } else {
                    if (field.getValue().isArray() || field.getValue().isObject()) {
                        requestBodyJson.set(field.getKey(), field.getValue());
                    } else if (field.getValue().isInt()) {
                        requestBodyJson.put(field.getKey(), field.getValue().asInt());
                    } else if (field.getValue().isBoolean()) {
                        requestBodyJson.put(field.getKey(), field.getValue().asBoolean());
                    } else {
                        requestBodyJson.put(field.getKey(), field.getValue().asText());
                    }
                }
            }

            logger.log(Level.INFO, "parsed content", parsedContent);

            String requestBody = new ObjectMapper().writeValueAsString(requestBodyJson);
            logger.log(Level.INFO, "Updating GitHub issue with requestBody", requestBody);

            String url = String.format("%s/%s", ConnectorConstants.ISSUE_GITHUB, issueNumber);

            // retrying logic
            int retryCount = 2;
            while (retryCount < MAX_RETRIES) {
                try {
                    CompletableFuture<HttpResponse<String>> res = this.httpClient.patch(url, requestBody);
                    HttpResponse<String> response = res.get();

                    if (response.statusCode() == 200) {
                        if (state != null && state.equals("close")) {
                            logger.log(Level.INFO, "Issue closed successfully");
                        } else {
                            logger.log(Level.INFO, "Issue updated successfully");
                        }
                        responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
                        responseJson.set("data", JsonNodeFactory.instance.textNode(response.body()));
                        break;
                    } else if (response.statusCode() == 401) {
                        logger.log(Level.INFO,
                                "Request was unsuccessful - Authentication credentials are invalid. Please check the configuration.");
                        this.verified = false;
                        break;
                    } else if (response.statusCode() == 403 || response.statusCode() == 429) {
                        logger.log(Level.WARNING, "Forbidden | Unauthenticated Error");
                        handleTrigger(response, response.statusCode());
                        retryCount = retryCount * 2;
                    } else {
                        logger.log(Level.INFO, "Failed to create issue" + ". Status code: " + response.statusCode());
                        logger.log(Level.INFO, "Response body: " + response.body());
                        responseJson.set("status", JsonNodeFactory.instance.textNode("error"));
                        break;
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error occurred while creating github issue", ex);
                    break;
                }
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

    public ArrayNode mergeLabels(ArrayNode existingLabels, ArrayNode fetchedLabels) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode mergedLabels = objectMapper.createArrayNode();

        // Add all existing labels to the merged list,
        // excluding those that match the pattern
        for (JsonNode existingLabel : existingLabels) {
            if (existingLabel.has("name")) {
                if (!existingLabel.get("name").asText().startsWith("priority:")) {
                    mergedLabels.add(existingLabel.get("name"));
                }
            }
        }

        // Check each fetched label to see if it exists in the existing labels
        for (JsonNode fetchedLabel : fetchedLabels) {
            mergedLabels.add(fetchedLabel.asText());
        }
        logger.log(Level.INFO, "mergedLabels", mergedLabels);
        return mergedLabels;
    }

    @Override
    public HttpResponse<String> getIssues(String url) throws ConnectorActionException {
        int responseCode = 200;
        HttpResponse<String> result = null;
        // retrying logic
        int retryCount = 2;
        while (retryCount < MAX_RETRIES) {
            try {

                CompletableFuture<HttpResponse<String>> res = this.httpClient.getByURL(url);

                HttpResponse<String> response = res.get();

                responseCode = response.statusCode();
                if (responseCode == 200) {
                    result = response;
                    break;
                } else if (response.statusCode() == 401) {
                    logger.log(Level.INFO,
                            "Request was unsuccessful - Authentication credentials are invalid. Please check the configuration.");
                    this.verified = false;
                    break;
                } else if (response.statusCode() == 403 || response.statusCode() == 429) {
                    logger.log(Level.WARNING, "Forbidden | Unauthenticated Error");
                    handleTrigger(response, response.statusCode());
                    retryCount = retryCount * 2;
                    Thread.sleep(1000);
                    if (retryCount >= MAX_RETRIES) {
                        logger.log(Level.WARNING, "Maximum retries reached for getting issues");
                        result = response;
                        break;
                    }
                } else {
                    logger.log(Level.INFO, "Failed to retrieve issues. Status code: " + responseCode);
                    logger.log(Level.INFO, "Response body: " + response.body());
                    result = response;
                    break;
                }
            } catch (InterruptedException e) {
                logger.log(Level.INFO, "Interrupt caused by a valid stop, polling stopped");
                break;
            } catch (Exception e) {
                throw new ConnectorActionException("Failed to query issues with error: " + e.getMessage(),
                        responseCode);
            }
        }
        return result;
    }

    public String getComments(String commentsURL, String queryParam) throws ConnectorActionException {
        int responseCode = 200;
        String resultBody = null;
        // retrying logic
        int retryCount = 2;
        while (retryCount < MAX_RETRIES) {
            try {
                String url = commentsURL + queryParam;

                logger.log(Level.INFO, "Get comments query: " + url);

                CompletableFuture<HttpResponse<String>> res = this.httpClient.getByURL(url);
                HttpResponse<String> response = res.get();

                responseCode = response.statusCode();
                if (responseCode == 200) {
                    String body = response.body();
                    return body;
                } else if (response.statusCode() == 401) {
                    logger.log(Level.INFO,
                            "Request was unsuccessful - Authentication credentials are invalid. Please check the configuration.");
                    this.verified = false;
                    break;
                } else if (response.statusCode() == 403 || response.statusCode() == 429) {
                    logger.log(Level.WARNING, "Forbidden | Unauthenticated Error");
                    handleTrigger(response, response.statusCode());
                    retryCount = retryCount * 2;
                    Thread.sleep(1000);
                    if (retryCount >= MAX_RETRIES) {
                        logger.log(Level.WARNING, "Maximum retries reached for getting comments");
                        resultBody = response.body();
                        break;
                    }
                } else {
                    logger.log(Level.INFO, "Failed to retrieve comments. Status code: " + responseCode);
                    logger.log(Level.INFO, "Response body: " + response.body());
                    return null;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception occurred when retrieving comments. Error: ", e);
                return null;
            }
        }
        return resultBody;
    }

    public ObjectNode addComment(String comment, String issueNumber) {
        ObjectNode responseJson = JsonNodeFactory.instance.objectNode();

        try {
            ObjectNode requestBodyJson = JsonNodeFactory.instance.objectNode();
            requestBodyJson.put("body", comment);

            String requestBody = new ObjectMapper().writeValueAsString(requestBodyJson);
            logger.log(Level.INFO, "Updating GitHub issue with comments", requestBody);

            String url = String.format("%s/%s/%s", ConnectorConstants.ISSUE_GITHUB, issueNumber,
                    ConnectorConstants.COMMENTS_GITHUB);

            // retrying logic
            int retryCount = 2;
            while (retryCount < MAX_RETRIES) {
                try {
                    CompletableFuture<HttpResponse<String>> res = this.httpClient.post(url, requestBody);
                    HttpResponse<String> response = res.get();

                    if (response.statusCode() == 201) {
                        logger.log(Level.INFO, "Added comment to the issue successfully");
                        responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
                        responseJson.set("data", JsonNodeFactory.instance.textNode(response.body()));
                        break;
                    } else if (response.statusCode() == 401) {
                        logger.log(Level.INFO,
                                "Request was unsuccessful - Authentication credentials are invalid. Please check the configuration.");
                        this.verified = false;
                        break;
                    } else if (response.statusCode() == 403 || response.statusCode() == 429) {
                        logger.log(Level.WARNING, "Forbidden | Unauthenticated Error");
                        handleTrigger(response, response.statusCode());
                        retryCount = retryCount * 2;
                    } else {
                        logger.log(Level.INFO,
                                "Failed to add comment to the issue" + ". Status code: " + response.statusCode());
                        logger.log(Level.INFO, "Response body: " + response.body());
                        responseJson.set("status", JsonNodeFactory.instance.textNode("error"));
                        break;
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Error occurred while adding comment to github issue", ex);
                    break;
                }
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

    public JsonNode getIssueById(String issueNumber) {
        // retrying logic
        JsonNode issueResultRes = null;
        int retryCount = 2;
        while (retryCount < MAX_RETRIES) {
            try {

                String url = String.format("%s/%s", ConnectorConstants.ISSUE_GITHUB, issueNumber);
                CompletableFuture<HttpResponse<String>> res = this.httpClient.get(url);
                HttpResponse<String> response = res.get();

                if (response.statusCode() == 200) {
                    String body = response.body();
                    JsonNode issueResponse = new ObjectMapper().readTree(body);
                    logger.log(Level.INFO, "Successfully retrieved all issues:");
                    logger.log(Level.INFO, "Successfully retrieved issue #", issueResponse);
                    issueResultRes = issueResponse;
                    break;
                } else if (response.statusCode() == 401) {
                    logger.log(Level.INFO,
                            "Request was unsuccessful - Authentication credentials are invalid. Please check the configuration.");
                    this.verified = false;
                    break;
                } else if (response.statusCode() == 403 || response.statusCode() == 429) {
                    logger.log(Level.WARNING, "Forbidden | Unauthenticated Error");
                    handleTrigger(response, response.statusCode());
                    retryCount = retryCount * 2;
                    Thread.sleep(1000);
                    if (retryCount >= MAX_RETRIES) {
                        logger.log(Level.WARNING, "Maximum retries reached for getting comments");
                        break;
                    }
                } else {
                    logger.log(Level.INFO,
                            "Failed to retrieve issue #" + issueNumber + ". Status code: " + response.statusCode());
                    logger.log(Level.INFO, "Response body: " + response.body());
                    return null;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception occurred when retrieving issue by id. Error: ", e);
                return null;
            }
        }
        return issueResultRes;
    }

    @Override
    public void verifyIntegration() {
        if (isVerified()) {
            return;
        }
        int retryCount = 2;
        while (retryCount < MAX_RETRIES) {
            try {
                String url = String.format("%s%s", ConnectorConstants.ISSUE_GITHUB, "?page=1&per_page=1");
                CompletableFuture<HttpResponse<String>> res = this.httpClient.get(url);
                HttpResponse<String> response = res.get();

                if (response.statusCode() == 200) {
                    logger.log(Level.INFO, "Connection test successful. Authenticated user details:");
                    this.verified = true;
                    break;
                } else if (response.statusCode() == 401) {
                    logger.log(Level.INFO,
                            "Request was unsuccessful - Authentication credentials are invalid. Please check the configuration.");
                    this.verified = false;
                    break;
                } else if (response.statusCode() == 403 || response.statusCode() == 429) {
                    this.verified = false;
                    logger.log(Level.SEVERE, "Forbidden | Unauthenticated Error");
                    retryCount = retryCount * 2;
                    Thread.sleep(1000);
                    if (retryCount >= MAX_RETRIES) {
                        logger.log(Level.WARNING, "Maximum retries reached for getting comments");
                        break;
                    }
                } else {
                    this.verified = false;
                    logger.log(Level.INFO, "Connection test failed. Status code: " + response.statusCode());
                    break;
                }
            } catch (Exception e) {
                this.verified = false;
                logger.log(Level.SEVERE, "Connection test failed. Status code: ", e);
                break;
            }
        }
    }

    protected void handleTrigger(HttpResponse<String> response, int errorCode) {
        String rateLimitRemaining = response.headers().firstValue("X-RateLimit-Remaining").orElse(null); // UTC timezone

        if (rateLimitRemaining != null && rateLimitRemaining.equals("0")) {
            long currentTimeStamp = Instant.now().getEpochSecond();
            logger.log(Level.FINE, "Current Time Stamp", currentTimeStamp);
            String resetTime = response.headers().firstValue("X-RateLimit-Reset").orElse(null);
            String message = "";
            int triggerType = response.statusCode();
            if (resetTime != null) {
                long sleepDuration = Utils.getTimeDifference(resetTime, currentTimeStamp) + 1;
                if (sleepDuration > 0) {
                    logger.log(Level.FINE, "Time difference for github rate limit", sleepDuration);
                    message = "You reached your limit to GitHub API calls, GitHub connector is sleeping until "
                            + Utils.getReadableDateFromEpoch(resetTime);
                    logger.log(Level.WARNING, message);
                    this.connector.triggerAlerts(triggerType, message, sleepDuration);
                }
            } else {
                logger.log(Level.SEVERE, "Couldn't get rate limit from GitHub calls.", response);
            }
        } else {
            if (response.statusCode() == 403) {
                logger.log(Level.SEVERE,
                        "User doesn't have permission to access GitHub APIs. Please check if personal token have the right permissions.",
                        response);
            } else if (response.statusCode() == 429) {
                logger.log(Level.SEVERE, "Too many requests to GitHub API", response);
            } else {
                logger.log(Level.SEVERE, "Error occured when calling GitHub API", response);
            }
        }

    }
}