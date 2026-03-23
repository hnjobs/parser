package com.emilburzo.hnjobs.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpClient {

    private static final java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

    public static String getUrl(String url) throws IOException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }
}
