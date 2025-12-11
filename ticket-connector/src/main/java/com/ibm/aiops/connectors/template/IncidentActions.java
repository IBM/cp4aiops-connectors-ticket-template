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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.aiops.connectors.template.integrations.Integration;
import com.ibm.aiops.connectors.template.model.Configuration;
import com.ibm.aiops.connectors.template.model.IssueModel;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionDataDeserializationException;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionRequest;
import com.ibm.cp4waiops.connectors.sdk.actions.ConnectorActionException;

import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.Counter;

public class IncidentActions implements Runnable {
    ConnectorAction action;

    static final Logger logger = Logger.getLogger(IncidentActions.class.getName());
    static String ACTION_GITHUB_RESPONSE = "cp4waiops-cartridge.itsmincidentresponse";// "cp4waiops-cartridge.githubcreateresponse";

    public IncidentActions(ConnectorAction action) {
        this.action = action;
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

    private void createIncident(ConnectorAction action) {

        Counter actionCounter = action.getActionCounter();
        Counter actionSuccessCounter = action.getActionSuccessCounter();
        Counter actionErrorCounter = action.getActionErrorCounter();

        Configuration config = action.getConfiguration();
        TicketConnector connector = action.getConnector();
        ActionRequest request = action.getActionRequest();

        // Map incoming data bytes to a JSON structure
        ObjectNode requestContent = null;
        try {
            requestContent = request.dataAs(ObjectNode.class);
        } catch (ActionDataDeserializationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        try {
            actionCounter.increment();
            Integration integration = connector.getCurrentIntegration();
            ObjectNode responseJSON = integration.createIssue(requestContent, config.getMappingsGithub());
            logger.log(Level.INFO, "Notify Creation Completable Future integration Response", responseJSON);
            if (responseJSON.get("status").asText().equals("success")) {
                // trigger kafka topic to push it to insights.
                logger.log(Level.INFO, "Triggering Kafka topic");
                ObjectMapper objectMapper = new ObjectMapper();
                String responseBody = responseJSON.get("data").asText();
                JsonNode data = objectMapper.readTree(responseBody);
                logger.log(Level.INFO, "Triggering Kafka topic data", data);
                String permalink = data.get("html_url").asText();
                logger.log(Level.INFO, "Triggering Kafka topic", data.get("html_url").asText());
                String response = IssueModel.getResponse(data.get("id").asText(), true,
                        "Created incident with id =  " + data.get("number").asText(), data.get("number").asText(),
                        connector.getConnectorID(), IssueModel.getStoryId(request.getData()), "Successful", permalink);
                CloudEvent ce = connector.createEvent(0, "com.ibm.sdlc.github.issue.create.response", response,
                        new URI(permalink));
                connector.emitCloudEvent(ACTION_GITHUB_RESPONSE, connector.getPartition(), ce);
                actionSuccessCounter.increment();
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

    private void updateIncident(ConnectorAction action) {

        Counter actionCounter = action.getActionCounter();
        Counter actionSuccessCounter = action.getActionSuccessCounter();
        Counter actionErrorCounter = action.getActionErrorCounter();

        Configuration config = action.getConfiguration();
        TicketConnector connector = action.getConnector();
        ActionRequest request = action.getActionRequest();

        ObjectNode requestContent = null;
        try {
            requestContent = request.dataAs(ObjectNode.class);
        } catch (ActionDataDeserializationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        try {
            actionCounter.increment();
            Integration integration = connector.getCurrentIntegration();
            String issueNum = IssueModel.getIssueId(request.getData(), config.getOwner(), config.getRepo());
            if (issueNum != null) {
                ObjectNode responseJSON = integration.updateIssue(requestContent, config.getMappingsGithub(), issueNum,
                        null);
                logger.log(Level.INFO, "Notify Updating Completable Future integration Response", responseJSON);
                if (responseJSON.get("status").asText().equals("success")) {
                    actionSuccessCounter.increment();
                } else {
                    actionErrorCounter.increment();
                }
            } else {
                logger.log(Level.INFO, "Didn't find issueNum to update to GitHub", issueNum);
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

    private void closeIncident(ConnectorAction action) {

        Counter actionCounter = action.getActionCounter();
        Counter actionSuccessCounter = action.getActionSuccessCounter();
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
            actionCounter.increment();
            Integration integration = connector.getCurrentIntegration();
            String issueNum = IssueModel.getIssueId(request.getData(), config.getOwner(), config.getRepo());
            if (issueNum != null) {
                ObjectNode responseJSON = integration.updateIssue(requestContent, config.getMappingsGithub(), issueNum,
                        "close");
                logger.log(Level.INFO, "Notify Updating Completable Future integration Response", responseJSON);
                if (responseJSON.get("status").asText().equals("success")) {
                    actionSuccessCounter.increment();
                } else {
                    actionErrorCounter.increment();
                }
            } else {
                logger.log(Level.INFO, "Didn't find issueNum to update to GitHub", issueNum);
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
}