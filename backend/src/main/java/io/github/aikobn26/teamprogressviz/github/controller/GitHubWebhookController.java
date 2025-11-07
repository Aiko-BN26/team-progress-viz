package io.github.aikobn26.teamprogressviz.github.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.github.service.GitHubWebhookEventService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class GitHubWebhookController {

    private final GitHubWebhookEventService webhookEventService;

    @PostMapping("/github")
    public ResponseEntity<Map<String, String>> handleWebhook(@RequestHeader(name = "X-GitHub-Event", required = false) String eventType,
                                                             @RequestHeader(name = "X-GitHub-Delivery", required = false) String deliveryId,
                                                             @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
                                                             @RequestBody(required = false) String payload) {
        webhookEventService.recordEvent(eventType, deliveryId, signature, payload);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "queued"));
    }
}
