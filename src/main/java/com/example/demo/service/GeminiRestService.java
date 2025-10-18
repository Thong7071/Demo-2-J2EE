package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiRestService {
    private final RestTemplate http = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();
    private final String apiKey;
    private final String chatUrl;
    private final String embedUrl;

    public GeminiRestService(@Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.chat.url}") String chatUrl,
            @Value("${gemini.embed.url}") String embedUrl) {
        this.apiKey = apiKey;
        this.chatUrl = chatUrl;
        this.embedUrl = embedUrl;

        System.out.println("Gemini key length = " + (apiKey == null ? 0 : apiKey.length()));
        System.out.println("Gemini chatUrl    = " + chatUrl);
        System.out.println("Gemini embedUrl   = " + embedUrl);
    }

    public String chat(String prompt) throws Exception {
        String body = "{\n  \"contents\": [{ \"parts\": [{ \"text\": " + om.writeValueAsString(prompt) + " }] }]\n}";
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        String url = chatUrl + "?key=" + apiKey;
        ResponseEntity<String> resp = http.exchange(url, HttpMethod.POST, new HttpEntity<>(body, h), String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Gemini chat failed: " + resp.getStatusCode() + " - " + resp.getBody());
        }
        JsonNode root = om.readTree(resp.getBody());
        JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
        return textNode.isMissingNode() ? "" : textNode.asText();
    }

    public float[] embed(String text) throws Exception {
        String body = "{\n  \"content\": { \"parts\": [{ \"text\": " + om.writeValueAsString(text) + " }] }\n}";
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        String url = embedUrl + "?key=" + apiKey;
        ResponseEntity<String> resp = http.exchange(url, HttpMethod.POST, new HttpEntity<>(body, h), String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Gemini embed failed: " + resp.getStatusCode() + " - " + resp.getBody());
        }
        JsonNode root = om.readTree(resp.getBody());
        JsonNode values = root.at("/embedding/values");
        if (values.isMissingNode())
            values = root.at("/data/0/embedding");
        List<Float> list = new ArrayList<>();
        for (JsonNode v : values)
            list.add((float) v.asDouble());
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++)
            arr[i] = list.get(i);
        return arr;
    }
}
