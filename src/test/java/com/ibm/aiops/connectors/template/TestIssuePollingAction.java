package com.ibm.aiops.connectors.template;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ibm.aiops.connectors.template.integrations.GithubIntegration;

public class TestIssuePollingAction {
    // In case of failure, try: export JAVA_HOME=$(/usr/libexec/java_home)
    // to prevent unsupported version error

    @Test
    @DisplayName("Test calling GitHub")
    void managerDropsDuplicateConfigurationEvents() throws Exception {
        HttpClientUtil httpClient = mock(HttpClientUtil.class);

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("hello", "world");
        headers.put("bye", "computer");

        HttpResponse<String> httpResponse = new HttpResponseTester(200, "{\"hello\":\"world\"}", headers);

        // HttpHeaders headers = new HttpHeaders();
        // headers.add

        TicketConnector ticketConnector = new TicketConnector();

        when(httpClient.getByURL("http://example.com")).thenReturn(CompletableFuture.completedFuture(httpResponse));

        GithubIntegration gh = new GithubIntegration(httpClient, ticketConnector);
        HttpResponse<String> response = gh.getIssues("http://example.com");

        Assertions.assertEquals("{\"hello\":\"world\"}", response.body());

        System.out.println(response.headers());
    }
}
