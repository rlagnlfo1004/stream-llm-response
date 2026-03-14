package com.mailsangja.streamingdemo.controller;

import com.mailsangja.streamingdemo.service.GeminiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/proverb")
public class ProverbController {

    private final GeminiService geminiService;

    public ProverbController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProverb() {
        SseEmitter emitter = new SseEmitter(60_000L);
        CompletableFuture.runAsync(() -> geminiService.streamProverb(emitter));
        return emitter;
    }
}
