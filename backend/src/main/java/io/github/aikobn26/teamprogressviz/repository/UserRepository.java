package io.github.aikobn26.teamprogressviz.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.aikobn26.teamprogressviz.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGithubId(Long githubId);

    Optional<User> findByGithubIdAndDeletedAtIsNull(Long githubId);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);
}
