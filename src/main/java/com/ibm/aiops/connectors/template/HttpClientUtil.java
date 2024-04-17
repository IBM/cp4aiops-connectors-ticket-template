
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
import java.util.concurrent.CompletableFuture;

public class HttpClientUtil {
    private HttpClient httpClient;
    private HttpRequest request;
    private String auth;
    private String url;

    public HttpClientUtil(String url, String owner, String repo, String token) {
        httpClient = HttpClient.newHttpClient();
        String truncatedurl = url.replaceAll("/$", "");
        this.url = String.format("%s/repos/%s/%s", truncatedurl, owner, repo);
        this.auth = Utils.decode(token);
    }

    public CompletableFuture<HttpResponse<String>> post(String path, String data)
            throws IOException, InterruptedException {
        request = HttpRequest.newBuilder(URI.create(url + path)).header("accept", "application/json")
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

    public CompletableFuture<HttpResponse<String>> get(String path) throws IOException, InterruptedException {
        request = HttpRequest.newBuilder(URI.create(url + path)).header("accept", "application/json")
                .header("Authorization", "Bearer " + auth).GET().build();
        return send(request);
    }

    public CompletableFuture<HttpResponse<String>> getByURL(String path) throws IOException, InterruptedException {
        request = HttpRequest.newBuilder(URI.create(path)).header("accept", "application/json")
                .header("Authorization", "Bearer " + auth).GET().build();
        return send(request);
    }

    private CompletableFuture<HttpResponse<String>> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
