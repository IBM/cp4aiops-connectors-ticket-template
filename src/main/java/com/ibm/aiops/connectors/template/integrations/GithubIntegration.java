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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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

            if (parsedContent.has("title")) {
                requestBodyJson.put("title", parsedContent.get("title").asText());
            }
            if (parsedContent.has("body")) {
                requestBodyJson.put("body", parsedContent.get("body").asText());
            }
            if (parsedContent.has("labels")) {
                ArrayNode labels = (ArrayNode) parsedContent.get("labels");
                logger.log(Level.INFO, "Creating GitHub issue with lab", labels);
                requestBodyJson.set("labels", labels);
            }
            if (parsedContent.has("assignees")) {
                ArrayNode assignees = (ArrayNode) parsedContent.get("labels");
                logger.log(Level.INFO, "Creating GitHub issue with assignees", assignees);
                requestBodyJson.set("assignees", assignees);
            }

            String requestBody = new ObjectMapper().writeValueAsString(requestBodyJson);
            logger.log(Level.INFO, "Creating GitHub issue with requestBody", requestBody);

            CompletableFuture<HttpResponse<String>> res = this.httpClient.post(ConnectorConstants.ISSUE_GITHUB,
                    requestBody);
            HttpResponse<String> response = res.get();
            if (response.statusCode() == 201) {
                logger.log(Level.INFO, "Issue created successfully");
                responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
                responseJson.set("data", JsonNodeFactory.instance.textNode(response.body()));
            } else if (response.statusCode() == 403 || response.statusCode() == 401) {
                logger.log(Level.WARNING, "Forbidden | Unauthenticated Error", response);
                handleTrigger(response, response.statusCode());
                // retrying create after sleep.
                createIssue(requestNode, jsonata);
            } else {
                logger.log(Level.INFO, "Failed to create issue" + ". Status code: " + response.statusCode());
                logger.log(Level.INFO, "Response body: " + response.body());
                responseJson.set("status", JsonNodeFactory.instance.textNode("error"));
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
            String parsedJSON = JsonParsing.jsonataMap(requestNode.toString(), jsonata);
            logger.log(Level.INFO, "parsed json", parsedJSON);

            JsonNode parsedContent = new ObjectMapper().readTree(parsedJSON);
            logger.log(Level.INFO, "parsed content", parsedContent);

            ObjectNode requestBodyJson = JsonNodeFactory.instance.objectNode();

            if (parsedContent.has("title")) {
                requestBodyJson.put("title", parsedContent.get("title").asText());
            }
            if (parsedContent.has("body")) {
                requestBodyJson.put("body", parsedContent.get("body").asText());
            }

            if (parsedContent.has("labels")) {
                ArrayNode labels = (ArrayNode) parsedContent.get("labels");
                JsonNode issueRes = getIssueById(issueNumber);
                logger.log(Level.INFO, "Creating GitHub issue with issueRes ", issueRes);
                if (issueRes != null) {
                    ArrayNode labelsFromGit = (ArrayNode) issueRes.get("labels");
                    ArrayNode mergedLabels = mergeLabels(labelsFromGit, labels);
                    logger.log(Level.INFO, "Creating GitHub issue with issueRes ", mergedLabels);
                    requestBodyJson.set("labels", mergedLabels);
                }
            }
            if (parsedContent.has("assignees")) {
                ArrayNode assignees = (ArrayNode) parsedContent.get("labels");
                logger.log(Level.INFO, "Creating GitHub issue with assignees", assignees);
                requestBodyJson.set("assignees", assignees);
            }

            if (state != null && state.equals("close")) {
                requestBodyJson.put("state", "close");
            }

            logger.log(Level.INFO, "parsed content", parsedContent);

            String requestBody = new ObjectMapper().writeValueAsString(requestBodyJson);
            logger.log(Level.INFO, "Updating GitHub issue with requestBody", requestBody);

            String url = String.format("%s/%s", ConnectorConstants.ISSUE_GITHUB, issueNumber);

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
            } else if (response.statusCode() == 403 || response.statusCode() == 401) {
                logger.log(Level.WARNING, "Forbidden | Unauthenticated Error");
                handleTrigger(response, response.statusCode());
                // retrying update after sleep.
                updateIssue(requestNode, jsonata, issueNumber, state);
            } else {
                logger.log(Level.INFO, "Failed to create issue" + ". Status code: " + response.statusCode());
                logger.log(Level.INFO, "Response body: " + response.body());
                responseJson.set("status", JsonNodeFactory.instance.textNode("error"));
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
        try {

            CompletableFuture<HttpResponse<String>> res = this.httpClient.getByURL(url);
            HttpResponse<String> response = res.get();

            responseCode = response.statusCode();
            if (responseCode == 200) {
                return response;
            } else if (response.statusCode() == 403 || response.statusCode() == 401) {
                logger.log(Level.WARNING, "Forbidden | Unauthenticated Error");
                handleTrigger(response, response.statusCode());
                // retrying after sleep.
                return getIssues(url);
            } else {
                logger.log(Level.INFO, "Failed to retrieve issues. Status code: " + responseCode);
                logger.log(Level.INFO, "Response body: " + response.body());
                return response;
            }
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Interrupt caused by a valid stop, polling stopped");
            return null;
        } catch (Exception e) {
            throw new ConnectorActionException("Failed to query issues with error: " + e.getMessage(), responseCode);
        }
    }

    public String getComments(String commentsURL, String queryParam) throws ConnectorActionException {
        int responseCode = 200;
        try {
            String url = commentsURL + queryParam;

            logger.log(Level.INFO, "Get comments query: " + url);

            CompletableFuture<HttpResponse<String>> res = this.httpClient.getByURL(url);
            HttpResponse<String> response = res.get();

            responseCode = response.statusCode();
            if (responseCode == 200) {
                String body = response.body();
                return body;
            } else if (response.statusCode() == 403 || response.statusCode() == 401) {
                logger.log(Level.WARNING, "Forbidden | Unauthenticated Error");
                handleTrigger(response, response.statusCode());
                // retrying after sleep.
                return getComments(commentsURL, queryParam);
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

    public JsonNode getIssueById(String issueNumber) {
        try {

            String url = String.format("%s/%s", ConnectorConstants.ISSUE_GITHUB, issueNumber);
            CompletableFuture<HttpResponse<String>> res = this.httpClient.get(url);
            HttpResponse<String> response = res.get();

            if (response.statusCode() == 200) {
                String body = response.body();
                JsonNode issueResponse = new ObjectMapper().readTree(body);
                logger.log(Level.INFO, "Successfully retrieved all issues:");
                logger.log(Level.INFO, "Successfully retrieved issue #", issueResponse);
                return issueResponse;
            } else if (response.statusCode() == 403 || response.statusCode() == 401) {
                logger.log(Level.WARNING, "Forbidden | Unauthenticated Error");
                handleTrigger(response, response.statusCode());
                // retrying after sleep.
                return getIssueById(issueNumber);
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

    @Override
    public void verifyIntegration() {
        if (isVerified()) {
            return;
        }
        try {
            String url = String.format("%s%s", ConnectorConstants.ISSUE_GITHUB, "?page=1&per_page=1");
            CompletableFuture<HttpResponse<String>> res = this.httpClient.get(url);
            HttpResponse<String> response = res.get();

            if (response.statusCode() == 200) {
                logger.log(Level.INFO, "Connection test successful. Authenticated user details:");
                this.verified = true;
            } else if (response.statusCode() == 403 || response.statusCode() == 401) {
                logger.log(Level.WARNING, "Forbidden | Unauthenticated Error");
                handleTrigger(response, response.statusCode());
                // retrying after sleep.
                verifyIntegration();
            } else {
                logger.log(Level.INFO, "Connection test failed. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Connection test failed. Status code: ", e);
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
            logger.log(Level.SEVERE, "Error occured when calling GitHub API", response);
        }

    }
}
