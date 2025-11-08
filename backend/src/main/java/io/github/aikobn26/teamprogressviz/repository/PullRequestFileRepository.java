package io.github.aikobn26.teamprogressviz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.aikobn26.teamprogressviz.entity.PullRequestFile;

@Repository
public interface PullRequestFileRepository extends JpaRepository<PullRequestFile, Long> {

    List<PullRequestFile> findByPullRequestIdAndDeletedAtIsNullOrderByPathAsc(Long pullRequestId);
}
