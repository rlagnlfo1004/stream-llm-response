package com.mailsangja.streamingdemo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:streamGenerateContent";

    private static final String PROMPT = """
            한국어 속담 하나를 선택하여 마크다운 형식으로 소개해줘.
            다음 형식을 정확히 따라줘:

            ## [속담]

            **뜻:** [속담의 의미를 한 문장으로]

            **설명:** [속담에 대한 간단한 설명 2~3문장]
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiService(RestClient.Builder builder, ObjectMapper objectMapper) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
    }

    public void streamProverb(SseEmitter emitter) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", PROMPT)))
                )
        );

        try {
            restClient.post()
                    .uri(GEMINI_URL + "?alt=sse&key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .exchange((req, res) -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(res.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data: ")) continue;

                                String json = line.substring(6).trim();
                                if ("[DONE]".equals(json)) break;

                                try {
                                    JsonNode textNode = objectMapper.readTree(json)
                                            .path("candidates").path(0)
                                            .path("content").path("parts").path(0)
                                            .path("text");
                                    if (!textNode.isMissingNode()) {
                                        emitter.send(SseEmitter.event()
                                                .name("message")
                                                .data(textNode.asText()));
                                    }
                                } catch (Exception ignored) {
                                    // skip malformed chunk
                                }
                            }
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                            return null;
                        }

                        try {
                            emitter.send(SseEmitter.event().name("done").data(""));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                        return null;
                    });
        } catch (Exception e) {
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
        }
    }
}
