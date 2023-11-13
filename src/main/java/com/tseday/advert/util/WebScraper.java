package com.tseday.advert.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WebScraper {


    public static String extract() {

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.facebook.com/121697844326871/posts/174874532342535"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String html = response.body();

            Document doc = Jsoup.parse(html);
            Elements links = doc.select("meta");

            links.stream().map(l ->  l.attr("abs:href"))
                    .forEach(System.out::println);

            return "success";
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
