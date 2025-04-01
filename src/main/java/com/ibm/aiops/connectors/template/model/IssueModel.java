/***********************************************************************
 *
 *      IBM Confidential
 *
 *      (C) Copyright IBM Corp. 2024
 *
 *      5737-M96
 *
 **********************************************************************/

package com.ibm.aiops.connectors.template.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.aiops.connectors.template.ConnectorConstants;
import com.ibm.aiops.connectors.template.TicketConnector;

// This model works as a reference point to map the incident id with outbound systems incident id.
// For example if a GitHub Issue is created for AIOps Incident id, by using this model AIOps will map both ids. This will help in updating the incident back to Github.
public class IssueModel {
    static final Logger logger = Logger.getLogger(TicketConnector.class.getName());
    public static String ACTION_TYPE_INCIDENT_CREATION = "invokeAction";

    public static String ISSUE_CREATION_SYS_ID = "sys_id";
    public static String ISSUE_CREATION_SUCCESS = "success";
    public static String ISSUE_CREATION_AIOPS = "aiops";
    public static String ISSUE_CREATION_MESSAGE = "message";
    public static String ISSUE_CREATION_ALERTS = "alerts";
    public static String ISSUE_CREATION_NUMBER = "number";
    public static String ISSUE_CREATION_EVENTSYSID = "eventSysIds";
    public static String ISSUE_CREATION_EVENTS = "events";
    public static String ISSUE_CREATION_STATUS = "status";
    public static String ISSUE_CREATION = "issue";
    public static String ISSUE_CREATION_RESULT = "result";
    public static String ISSUE_CREATION_PERMALINK = "permalink";
    public static String ISSUE_CREATION_TICKETRESPONSE = "ticket_response";
    public static String ISSUE_CREATION_STORY_ID = "story_id";
    public static String ISSUE_CREATION_CONNECTION_ID = "connection_id";

    /**
     * Creates a JSON response that follows the schema
     *
     * @param id
     *            the unique identifier for the ITSM
     * @param success
     *            true if successful, false otherwise
     * @param message
     *            a message about the successful or unsucessful creation of the issue. Diagnostic information can be
     *            shown here for debugging purposes (no sensitive data should be put here)
     * @param issueNumber
     *            the issue number. Some ITSM systems have another identifer. This can be the same as the unique
     *            identifier for id
     * @param connectionId
     *            the connection ID of your integration
     * @param storyId
     *            the story identifier in AIOps
     * @param status
     *            Can be "Success" or "Failure"
     * @param html_url
     *            the URL to be added to the AIOps issue to map the story in AIOps to the issue in the ITSM
     *
     * @return the JSON string respones that needs to be sent back to AIOps. The bot orchestrator will take this
     *         response and map the AIOPs story to this issue
     */
    public static String getResponse(String id, boolean success, String message, String issueNumber,
            String connectionId, String storyId, String status, String html_url) {
        JSONObject json = new JSONObject();
        JSONObject bodyStringJSONObject = null;

        JSONObject resultJSON = new JSONObject();
        JSONObject aiopsJSON = new JSONObject();

        aiopsJSON.put(ISSUE_CREATION_SYS_ID, id);
        aiopsJSON.put(ISSUE_CREATION_SUCCESS, success);
        resultJSON.put(ISSUE_CREATION_AIOPS, aiopsJSON);
        resultJSON.put(ISSUE_CREATION_MESSAGE, message);

        JSONObject alertJSON = new JSONObject();
        alertJSON.put(ISSUE_CREATION_SUCCESS, success);
        resultJSON.put(ISSUE_CREATION_ALERTS, alertJSON);

        JSONObject issueJSON = new JSONObject();
        issueJSON.put(ISSUE_CREATION_SYS_ID, id);
        issueJSON.put(ISSUE_CREATION_SUCCESS, success);
        issueJSON.put(ISSUE_CREATION_NUMBER, issueNumber);

        JSONObject eventJSON = new JSONObject();
        eventJSON.put(ISSUE_CREATION_SUCCESS, success);
        eventJSON.put(ISSUE_CREATION_EVENTSYSID, new String[] {});

        resultJSON.put(ISSUE_CREATION_EVENTS, eventJSON);
        resultJSON.put(ISSUE_CREATION_STATUS, status);
        resultJSON.put(ISSUE_CREATION, issueJSON);

        bodyStringJSONObject = new JSONObject();
        bodyStringJSONObject.put(ISSUE_CREATION_RESULT, resultJSON);

        json.put(ISSUE_CREATION_PERMALINK, html_url);
        json.put(ISSUE_CREATION_STORY_ID, storyId);
        json.put(ISSUE_CREATION_CONNECTION_ID, connectionId);

        if (bodyStringJSONObject != null) {
            json.put(ISSUE_CREATION_TICKETRESPONSE, bodyStringJSONObject);
        }
        /*
         * Example json body: { "connection_id":"1b1aeb97-775c-46a6-9767-ce76838433d4", "story_id":"storyID2",
         * "ticket_response":{ "result":{ "message":"Successfully created issue with number 1347", "issue":{
         * "id":"1347", "number":"1347" }, "status":"success" } }, "html_url":
         * "https://github.ibm.com/octocat/Hello-World/issues/1347" }
         */
        return json.toString();
    }

    /**
     * A helper method for getting the story ID from the issue creation CloudEvent
     *
     * @param event
     *            the CloudEvent in JSON format
     *
     * @return
     */
    public static String getStoryId(byte[] data) {
        // A helper method to get the proper k
        try {
            String jsonString = new String(data);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode nestedval = jsonNode.get("incident");
            return nestedval.get("id").asText();
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    /**
     * A helper method for getting the story ID from the issue creation CloudEvent
     *
     * @param event
     *            the CloudEvent in JSON format
     *
     * @return
     */
    
     // Todo: Modify this function if required
    public static String getIssueId(byte[] data) {
        // A helper method to get the proper k
        try {
            String jsonString = new String(data);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode nestedval = jsonNode.get("incident");
            JsonNode insights = nestedval.get("insights");
            for (JsonNode insight : insights) {
                // if ()) {
                if (insight.has("type")) {
                    if ("aiops.ibm.com/insight-type/itsm/metadata".equals(insight.get("type").asText())) {
                        if (insight.has("details")) {
                            JsonNode details = insight.get("details");
                            if (details.has("name")
                                    && details.get("name").asText().equals(ConnectorConstants.TICKET_TYPE)
                                    && details.has("permalink")) {
                                return details.get("ticket_num").asText();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }
}
