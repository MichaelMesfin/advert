package com.tseday.advert.exception.httpclient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class Client {

    public static String JSONBodyAsMap(URI uri, Map<String, String> requestParams) throws IOException, InterruptedException {
        UncheckedObjectMapper objectMapper = new UncheckedObjectMapper();

        String[] params = requestParams.entrySet().stream().map(e -> List.of(e.getKey(), (e.getValue())))
                .flatMap(List::stream)
                .toArray(String[]::new);


        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .headers(params)
                .build();

        HttpResponse<String> send = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

       return send.body();


    }

    static class UncheckedObjectMapper extends com.fasterxml.jackson.databind.ObjectMapper {
        /**
         * Parses the given JSON string into a Map.
         */
        Map<String, String> readValue(String content) {
            try {
                return this.readValue(content, new TypeReference<>() {
                });
            } catch (IOException ioe) {
                throw new CompletionException(ioe);
            }
        }

        public CompletableFuture<Void> postJson(URI uri, Map<String, String> map) throws IOException {


            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(map);


            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();


            return HttpClient.newHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::statusCode)
                    .thenAccept(System.out::println);
        }


    }
}
