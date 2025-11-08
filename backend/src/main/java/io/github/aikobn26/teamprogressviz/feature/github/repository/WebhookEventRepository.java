package io.github.aikobn26.teamprogressviz.feature.github.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.aikobn26.teamprogressviz.feature.github.entity.WebhookEvent;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
}
