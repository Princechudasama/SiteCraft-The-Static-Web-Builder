package com.sitecraft.backend.controller;

import com.sitecraft.backend.service.GenerateService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/generate")
public class GenerateController {

    private final GenerateService generateService;

    public GenerateController(GenerateService generateService){
        this.generateService = generateService;
    }

    @PostMapping("/new")
    public Map<String, Object> newConversation(@RequestBody Map<String, String> request) {
        UUID conversationId = UUID.randomUUID();

        String response = generateService.generate(
                request.get("query"),
                conversationId
        );

        return Map.of(
                "conversationId", conversationId,
                "response", response
        );
    }

    @PostMapping("/{conversationId}/message")
    public Map<String, Object> sendMessage(
            @PathVariable UUID conversationId,
            @RequestBody Map<String, String> request) {

        String response = generateService.generate(
                request.get("query"),
                conversationId
        );

        return Map.of(
                "conversationId", conversationId,
                "response", response
        );
    }

}
