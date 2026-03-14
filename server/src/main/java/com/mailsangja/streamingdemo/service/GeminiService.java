package com.mailsangja.streamingdemo.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
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

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-04-17:streamGenerateContent";

    private static final String PROMPT = """
            한국어 속담 하나를 선택하여 마크다운 형식으로 소개해줘.
            다음 형식을 정확히 따라줘:

            ## [속담]

            **뜻:** [속담의 의미를 한 문장으로]

            **설명:** [속담에 대한 간단한 설명 2~3문장]
            """;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

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
                        HttpStatusCode status = res.getStatusCode();
                        log.info("Gemini response status: {}", status);

                        if (status.isError()) {
                            String errorBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            log.error("Gemini API error [{}]: {}", status, errorBody);
                            sendErrorAndComplete(emitter, "Gemini API 오류: " + status);
                            return null;
                        }

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(res.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                log.debug("SSE line: {}", line);
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
                                } catch (Exception e) {
                                    log.warn("Failed to parse chunk: {}", line, e);
                                }
                            }
                        } catch (IOException e) {
                            log.error("Stream read error", e);
                            emitter.completeWithError(e);
                            return null;
                        }

                        emitter.send(SseEmitter.event().name("done").data(""));
                        emitter.complete();
                        return null;
                    });
        } catch (Exception e) {
            log.error("Gemini request failed", e);
            sendErrorAndComplete(emitter, e.getMessage());
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
            emitter.complete();
        } catch (Exception ignored) {
        }
    }
}
