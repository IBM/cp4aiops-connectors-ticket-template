
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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
// import java.nio.charset.StandardCharsets;
// import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class HttpClientUtil {
    private HttpClient httpClient;
    private HttpRequest request;
    private String auth;
    private String url;

    public HttpClientUtil(String url, String username, String password) {
        httpClient = HttpClient.newHttpClient();
        this.url = url.replaceAll("/$", "");
        this.auth = password; // Assuming this is already bearer token.

        // Uncomment the below if the password needs to be formatted to Bearer token.

        // String apiKey = username + ":" + password;
        // byte[] eb = Base64.getEncoder().encode(apiKey.getBytes(StandardCharsets.UTF_8));
        // this.auth = new String(eb, StandardCharsets.UTF_8);
    }

    public CompletableFuture<HttpResponse<String>> post(String data) throws IOException, InterruptedException {
        request = HttpRequest.newBuilder(URI.create(url)).header("accept", "application/json")
                .header("Authorization", "Bearer " + auth).POST(HttpRequest.BodyPublishers.ofString(data)).build();
        return send(request);
    }

    public CompletableFuture<HttpResponse<String>> patch(String path, String data)
            throws IOException, InterruptedException {
        request = HttpRequest.newBuilder(URI.create(url + path)).header("accept", "application/json")
                .header("Authorization", "Bearer " + auth).method("PATCH", HttpRequest.BodyPublishers.ofString(data))
                .build();
        return send(request);
    }

    // Assisted by watsonx Code Assistant
    /**
     * Send a GET request to the specified path and return the response body as a String.
     *
     * @param path
     *            The path to send the request to
     *
     * @return The response body as a String
     *
     * @throws IOException
     *             if an I/O error occurs
     * @throws InterruptedException
     *             if the request is interrupted
     */
    public CompletableFuture<HttpResponse<String>> get(String path) throws IOException, InterruptedException {
        request = HttpRequest.newBuilder(URI.create(path)).header("accept", "application/json")
                .header("Authorization", "Bearer " + auth).GET().build();
        return send(request);
    }

    private CompletableFuture<HttpResponse<String>> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
