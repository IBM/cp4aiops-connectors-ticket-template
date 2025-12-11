package com.ibm.aiops.connectors.template;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.aiops.connectors.template.integrations.GithubIntegration;

import io.cloudevents.CloudEvent;

public class TestGithubIntegration {

    public final String KEY_RESPONSE_STATUS = "status";
    public final String KEY_RESPONSE_DATA = "data";
    public final String KEY_RESPONSE_STATE = "state";
    public final String VALUE_SUCCESS = "success";
    public final String VALUE_ERROR = "error";

    public final String DEFAULT_JSONATA_DATA = "{}";

    @Test
    @DisplayName("Test calling GitHub Create Issue with 201")
    void testGitHubCreateIssue201() throws Exception {
        HttpClientUtil httpClient = mock(HttpClientUtil.class);
        TicketConnector base = mock(TicketConnector.class);
        Mockito.when(base.getConnectorID()).thenReturn("connectionID");
        doNothing().when(base).emitCloudEvent(isA(String.class), isA(String.class), isA(CloudEvent.class));

        HashMap<String, String> headers = new HashMap<String, String>();

        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode root = mapper.createObjectNode();
        root.put("title", "myTitle");
        root.put("body", "myBody");
        // root.put("labels", "myLabels");
        ArrayNode assigneeArray = mapper.createArrayNode();
        assigneeArray.add("myAssignee01");
        assigneeArray.add("myAssignee02");

        ArrayNode labelArray = mapper.createArrayNode();
        labelArray.add("myAssignee01");
        labelArray.add("myAssignee02");
        root.putArray("assignees").addAll(assigneeArray);
        root.putArray("labels").addAll(labelArray);

        String expectedResponse = root.toPrettyString();

        // Testing response code of 201
        HttpResponse<String> httpResponse = new HttpResponseTester(201, expectedResponse, headers);

        when(httpClient.getByURL(Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(httpResponse));
        when(httpClient.post(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        GithubIntegration ghi = new GithubIntegration(httpClient, base);

        ObjectNode createIssueResults = ghi.createIssue(root, DEFAULT_JSONATA_DATA);

        Assertions.assertEquals(VALUE_SUCCESS, createIssueResults.get(KEY_RESPONSE_STATUS).asText());
        Assertions.assertEquals(expectedResponse, createIssueResults.get(KEY_RESPONSE_DATA).asText());
    }

    @Test
    @DisplayName("Test calling GitHub Create Issue with 403, 429, and 404")
    void testGitHubCreateIssueInvalidCodes() throws Exception {
        HttpClientUtil httpClient = mock(HttpClientUtil.class);
        TicketConnector base = mock(TicketConnector.class);

        Mockito.when(base.getConnectorID()).thenReturn("connectionID");
        doNothing().when(base).emitCloudEvent(isA(String.class), isA(String.class), isA(CloudEvent.class));

        HashMap<String, String> headers = new HashMap<String, String>();
        String expectedResponse = "{\"hello\":\"world\"}";

        ObjectNode createIssueResults;

        // Testing response code of 403
        HttpResponse<String> httpResponse = new HttpResponseTester(403, expectedResponse, headers);

        when(httpClient.getByURL(Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(httpResponse));
        when(httpClient.post(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        GitHubIntegrationTester ghi = new GitHubIntegrationTester(httpClient, base);

        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode root = mapper.createObjectNode();

        createIssueResults = ghi.createIssue(root, DEFAULT_JSONATA_DATA);

        // If status is 429, ensure code is returned as well as the handle trigger response is called with the response
        Assertions.assertEquals(403, ghi.getHandleTriggerErrorCode());
        Assertions.assertEquals(httpResponse, ghi.getHandleTriggerResponse());

        // Testing response code of 429
        httpResponse = new HttpResponseTester(429, expectedResponse, headers);
        when(httpClient.getByURL(Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(httpResponse));
        when(httpClient.post(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        ghi = new GitHubIntegrationTester(httpClient, base);
        createIssueResults = ghi.createIssue(root, DEFAULT_JSONATA_DATA);

        // If status is 429, ensure code is returned as well as the handle trigger response is called with the response
        Assertions.assertEquals(429, ghi.getHandleTriggerErrorCode());
        Assertions.assertEquals(httpResponse, ghi.getHandleTriggerResponse());

        // Testing response code of 404
        httpResponse = new HttpResponseTester(409, expectedResponse, headers);
        when(httpClient.getByURL(Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(httpResponse));
        when(httpClient.post(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        ghi = new GitHubIntegrationTester(httpClient, base);
        createIssueResults = ghi.createIssue(root, DEFAULT_JSONATA_DATA);

        Assertions.assertEquals(VALUE_ERROR, createIssueResults.get(KEY_RESPONSE_STATUS).asText());
        Assertions.assertEquals(null, createIssueResults.get(KEY_RESPONSE_DATA));
        Assertions.assertEquals(httpResponse.body(), expectedResponse);
    }

    @Test
    @DisplayName("Test calling GitHub Update with 200")
    void testGitHubCreateUpdate200() throws Exception {
        HttpClientUtil httpClient = mock(HttpClientUtil.class);
        TicketConnector base = mock(TicketConnector.class);
        Mockito.when(base.getConnectorID()).thenReturn("connectionID");
        doNothing().when(base).emitCloudEvent(isA(String.class), isA(String.class), isA(CloudEvent.class));

        HashMap<String, String> headers = new HashMap<String, String>();

        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode root = mapper.createObjectNode();
        root.put("title", "myTitle");
        root.put("body", "myBody");
        root.put("number", 1234);
        root.put("state", "open");
        // root.put("labels", "myLabels");
        ArrayNode assigneeArray = mapper.createArrayNode();
        assigneeArray.add("myAssignee01");
        assigneeArray.add("myAssignee02");

        ArrayNode labelArray = mapper.createArrayNode();
        labelArray.add("myAssignee01");
        labelArray.add("myAssignee02");
        root.putArray("assignees").addAll(assigneeArray);
        root.putArray("labels").addAll(labelArray);

        String expectedResponse = root.toPrettyString();

        // Testing response code of 200
        HttpResponse<String> httpResponse = new HttpResponseTester(200, expectedResponse, headers);

        when(httpClient.get(Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(httpResponse));
        when(httpClient.patch(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        GithubIntegration ghi = new GithubIntegration(httpClient, base);
        String jsonNataData = "{}";

        // 1. Test with open state
        ObjectNode responseJson = ghi.updateIssue(root, jsonNataData, "1234", "open");

        Assertions.assertEquals("success", responseJson.get(KEY_RESPONSE_STATUS).asText());
        Assertions.assertEquals(expectedResponse, responseJson.get(KEY_RESPONSE_DATA).asText());
        Assertions.assertEquals(null, responseJson.get(KEY_RESPONSE_STATE));

        root.put("state", "close");
        root.put("title", "myTitle1");
        String updatedResponse = root.toPrettyString();
        HttpResponse<String> httpResponse2 = new HttpResponseTester(200, updatedResponse, headers);
        when(httpClient.patch(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(CompletableFuture.completedFuture(httpResponse2));

        // 2. Test with close state
        responseJson = ghi.updateIssue(root, jsonNataData, "1234", "close");
        Assertions.assertEquals("success", responseJson.get(KEY_RESPONSE_STATUS).asText());
        Assertions.assertEquals(updatedResponse, responseJson.get(KEY_RESPONSE_DATA).asText());

        // Retry test with closed status
    }

    @Test
    @DisplayName("Test calling GitHub Create Issue with 403, 429, and 404")
    void testGitHubUpdateIssueInvalidCodes() throws Exception {
        HttpClientUtil httpClient = mock(HttpClientUtil.class);
        TicketConnector base = mock(TicketConnector.class);

        Mockito.when(base.getConnectorID()).thenReturn("connectionID");
        doNothing().when(base).emitCloudEvent(isA(String.class), isA(String.class), isA(CloudEvent.class));

        HashMap<String, String> headers = new HashMap<String, String>();
        String expectedResponse = "{\"hello\":\"world\"}";

        ObjectNode updateIssueResults;

        // Testing response code of 403
        HttpResponse<String> httpResponse = new HttpResponseTester(403, expectedResponse, headers);

        when(httpClient.get(Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(httpResponse));
        when(httpClient.patch(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        GitHubIntegrationTester ghi = new GitHubIntegrationTester(httpClient, base);

        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode root = mapper.createObjectNode();

        String jsonNataData = "{}";
        updateIssueResults = ghi.updateIssue(root, jsonNataData, "1234", "open");

        // If status is 429, ensure code is returned as well as the handle trigger response is called with the response
        Assertions.assertEquals(403, ghi.getHandleTriggerErrorCode());
        Assertions.assertEquals(httpResponse, ghi.getHandleTriggerResponse());

        // Testing response code of 429
        httpResponse = new HttpResponseTester(429, expectedResponse, headers);
        when(httpClient.get(Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(httpResponse));
        when(httpClient.patch(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        ghi = new GitHubIntegrationTester(httpClient, base);
        updateIssueResults = ghi.updateIssue(root, jsonNataData, "1234", "open");

        // If status is 429, ensure code is returned as well as the handle trigger response is called with the response
        Assertions.assertEquals(429, ghi.getHandleTriggerErrorCode());
        Assertions.assertEquals(httpResponse, ghi.getHandleTriggerResponse());

        // Testing response code of 404
        httpResponse = new HttpResponseTester(409, expectedResponse, headers);
        when(httpClient.get(Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(httpResponse));
        when(httpClient.patch(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(CompletableFuture.completedFuture(httpResponse));

        ghi = new GitHubIntegrationTester(httpClient, base);
        updateIssueResults = ghi.updateIssue(root, jsonNataData, "1234", "open");

        Assertions.assertEquals(VALUE_ERROR, updateIssueResults.get(KEY_RESPONSE_STATUS).asText());
        Assertions.assertEquals(null, updateIssueResults.get(KEY_RESPONSE_DATA));
        Assertions.assertEquals(httpResponse.body(), expectedResponse);
    }

}