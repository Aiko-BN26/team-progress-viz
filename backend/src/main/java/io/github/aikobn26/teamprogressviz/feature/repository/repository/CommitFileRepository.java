package io.github.aikobn26.teamprogressviz.feature.repository.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.aikobn26.teamprogressviz.feature.repository.entity.CommitFile;


@Repository
public interface CommitFileRepository extends JpaRepository<CommitFile, Long> {

    List<CommitFile> findByCommitIdAndDeletedAtIsNullOrderByPathAsc(Long commitId);

    boolean existsByCommitIdAndDeletedAtIsNull(Long commitId);
}
