package io.github.aikobn26.teamprogressviz.feature.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.aikobn26.teamprogressviz.feature.user.entity.User;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGithubId(Long githubId);

    Optional<User> findByGithubIdAndDeletedAtIsNull(Long githubId);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);
}
