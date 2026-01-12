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
import java.util.concurrent.ExecutionException;
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

public class CustomIntegration extends Integration {

    private static final Logger logger = Logger.getLogger(IntegrationManager.class.getName());

    private HttpClientUtil httpClient;
    private TicketConnector connector;
    int MAX_RETRIES = 32;

    public CustomIntegration(HttpClientUtil httpClient, TicketConnector connector) {
        super("customconnector");
        this.httpClient = httpClient;
        this.connector = connector;
    }

    @Override
    public ObjectNode createIssue(ObjectNode requestNode, String jsonata) {
        logger.log(Level.INFO, "Create issue called");
        ObjectNode responseJson = JsonNodeFactory.instance.objectNode();

        responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
        // If error, the following JSON can be sent
        // responseJson.set("status", JsonNodeFactory.instance.textNode("error"));

        return responseJson;
    }

    @Override
    public ObjectNode updateIssue(ObjectNode requestNode, String jsonata, String issueNumber, String state) {
        logger.log(Level.INFO, "Update issue called");
        ObjectNode responseJson = JsonNodeFactory.instance.objectNode();

        responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
        // If error, the following JSON can be sent
        // responseJson.set("status", JsonNodeFactory.instance.textNode("error"));

        return responseJson;
    }

    @Override
    public HttpResponse<String> getIssues(String url) throws ConnectorActionException {
        int responseCode = 200;
        HttpResponse<String> result = null;
        // retrying logic

        System.out.println("getIssues");

        try {
            CompletableFuture<HttpResponse<String>> res = this.httpClient.getByURL(url);

            HttpResponse<String> response;  
            response = res.get();
            result = response;
            responseCode = response.statusCode();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("RESULT : " + result);
        return result;
    }
    
    @Override
    public void verifyIntegration() {
        if (isVerified()) {
            return;
        }
        int retryCount = 2;
        while (retryCount < MAX_RETRIES) {
            try {
                // Add a URL path here for verifying the integration
                String url = "";
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

    @Override
    public String getComments(String commentsURL, String queryParam) throws ConnectorActionException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getComments'");
    }
}