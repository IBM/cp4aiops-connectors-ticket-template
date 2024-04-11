package com.ibm.aiops.connectors.template;

import javax.net.ssl.SSLSession;

import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

public class HttpResponseTester implements HttpResponse<String> {

    private int code = 200;
    private String message = "";
    Map<String, String> headers = null;

    public HttpResponseTester(int code, String message, Map<String, String> headers) {
        this.code = code;
        this.message = message;
        this.headers = headers;
    }

    @Override
    public int statusCode() {
        return this.code;
    }

    @Override
    public HttpRequest request() {
        return null;
    }

    @Override
    public Optional<HttpResponse<String>> previousResponse() {
        return Optional.empty();
    }

    @Override
    public HttpHeaders headers() {
        // Only way to build headers is to create this
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create("http://exampleURL.com/aFile"));

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            requestBuilder.header(key, value);
        }

        HttpRequest request = requestBuilder.build();

        return request.headers();
    }

    @Override
    public String body() {
        return message;
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return Optional.empty();
    }

    @Override
    public URI uri() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'uri'");
    }

    @Override
    public Version version() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'version'");
    }
}