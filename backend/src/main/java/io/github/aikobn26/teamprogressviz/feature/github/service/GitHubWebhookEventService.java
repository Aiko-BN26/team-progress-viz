package io.github.aikobn26.teamprogressviz.feature.github.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.aikobn26.teamprogressviz.feature.github.entity.WebhookEvent;
import io.github.aikobn26.teamprogressviz.feature.github.repository.WebhookEventRepository;
import io.github.aikobn26.teamprogressviz.shared.exception.ValidationException;
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
