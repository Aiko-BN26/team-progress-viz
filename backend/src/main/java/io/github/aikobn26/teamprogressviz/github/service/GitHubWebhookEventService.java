package io.github.aikobn26.teamprogressviz.github.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.aikobn26.teamprogressviz.entity.WebhookEvent;
import io.github.aikobn26.teamprogressviz.exception.ValidationException;
import io.github.aikobn26.teamprogressviz.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GitHubWebhookEventService {

    private final WebhookEventRepository webhookEventRepository;

    public WebhookEvent recordEvent(String eventType,
                                    String deliveryId,
                                    String signature,
                                    String payload) {
        if (!StringUtils.hasText(payload)) {
            throw new ValidationException("Invalid payload or signature");
        }
        WebhookEvent event = WebhookEvent.builder()
                .eventType(eventType)
                .deliveryId(deliveryId)
                .signature(signature)
                .payload(payload)
                .status("pending")
                .build();
        return webhookEventRepository.save(event);
    }
}
