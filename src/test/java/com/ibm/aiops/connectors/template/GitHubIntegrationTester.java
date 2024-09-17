package com.ibm.aiops.connectors.template;

import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.aiops.connectors.template.integrations.GithubIntegration;

public class GitHubIntegrationTester extends GithubIntegration {

    int createIssueCount = 0;
    int updateIssueCount = 0;

    public HttpResponse<String> handleTriggerResponse;
    public int handleTriggerErrorCode;

    public GitHubIntegrationTester(HttpClientUtil httpClient, TicketConnector connector) {
        super(httpClient, connector);
        // TODO Auto-generated constructor stub
    }

    public ObjectNode createIssue(ObjectNode requestNode, String jsonata) {
        if (createIssueCount == 0) {
            createIssueCount++;
            return super.createIssue(requestNode, jsonata);
        } else {
            // To prevent a stack overflow, once create issue is called once, just return nulls
            return null;
        }
    }

    public ObjectNode updateIssue(ObjectNode requestNode, String jsonata, String issueNumber, String state) {
        if (updateIssueCount == 0) {
            updateIssueCount++;
            return super.updateIssue(requestNode, jsonata, issueNumber, state);
        } else {
            // To prevent a stack overflow, once create issue is called once, just return nulls
            return null;
        }
    }

    public void handleTrigger(HttpResponse<String> response, int errorCode) {
        handleTriggerErrorCode = errorCode;
        handleTriggerResponse = response;
    }

    public HttpResponse<String> getHandleTriggerResponse() {
        return handleTriggerResponse;
    }

    public int getHandleTriggerErrorCode() {
        return handleTriggerErrorCode;
    }
}