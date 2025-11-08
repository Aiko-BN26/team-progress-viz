package io.github.aikobn26.teamprogressviz.feature.user.service;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.aikobn26.teamprogressviz.feature.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.feature.organization.repository.ActivityDailyRepository;
import io.github.aikobn26.teamprogressviz.feature.organization.repository.DailyStatusRepository;
import io.github.aikobn26.teamprogressviz.feature.organization.repository.UserOrganizationRepository;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final DailyStatusRepository dailyStatusRepository;
    private final ActivityDailyRepository activityDailyRepository;

    public User ensureUserExists(AuthenticatedUser authenticatedUser) {
        return ensureUserExists(authenticatedUser, null);
    }

    public User ensureUserExists(AuthenticatedUser authenticatedUser, Consumer<User> onCreated) {
        if (authenticatedUser == null) {
            throw new IllegalArgumentException("authenticatedUser must not be null");
        }
        var githubId = authenticatedUser.id();
        if (githubId == null) {
            throw new IllegalArgumentException("GitHub id must not be null");
        }

        Optional<User> existing = userRepository.findByGithubId(githubId);
        if (existing.isPresent()) {
            return updateIfChanged(existing.get(), authenticatedUser);
        }

        var user = User.builder()
                .githubId(githubId)
                .login(authenticatedUser.login())
                .name(authenticatedUser.name())
                .avatarUrl(authenticatedUser.avatarUrl())
                .deletedAt(null)
                .build();
        User saved = userRepository.save(user);
        if (onCreated != null) {
            onCreated.accept(saved);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<User> findActiveById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findByIdAndDeletedAtIsNull(userId);
    }

    public User upsertGitHubUser(Long githubId, String login, String name, String avatarUrl) {
        if (githubId == null) {
            throw new IllegalArgumentException("githubId must not be null");
        }
        if (!StringUtils.hasText(login)) {
            throw new IllegalArgumentException("login must not be blank");
        }

        return userRepository.findByGithubId(githubId)
                .map(existing -> updateIfChanged(existing, login, name, avatarUrl))
                .orElseGet(() -> userRepository.save(User.builder()
                        .githubId(githubId)
                        .login(login)
                        .name(name)
                        .avatarUrl(avatarUrl)
                        .deletedAt(null)
                        .build()));
    }

    private User updateIfChanged(User user, AuthenticatedUser payload) {
        return updateIfChanged(user, payload.login(), payload.name(), payload.avatarUrl());
    }

    private User updateIfChanged(User user, String login, String name, String avatarUrl) {
        boolean dirty = false;

        if (user.isDeleted()) {
            user.setDeletedAt(null);
            dirty = true;
        }
        if (!Objects.equals(user.getLogin(), login)) {
            user.setLogin(login);
            dirty = true;
        }
        if (!Objects.equals(user.getName(), name)) {
            user.setName(name);
            dirty = true;
        }
        if (!Objects.equals(user.getAvatarUrl(), avatarUrl)) {
            user.setAvatarUrl(avatarUrl);
            dirty = true;
        }
        if (dirty) {
            user.setUpdatedAt(OffsetDateTime.now());
        }
        return dirty ? userRepository.save(user) : user;
    }

    public void deleteUser(User user) {
        if (user == null || user.getId() == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        user.setDeletedAt(now);
        userRepository.save(user);

        userOrganizationRepository.findByUserIdAndDeletedAtIsNull(user.getId()).forEach(membership -> {
            membership.setDeletedAt(now);
            userOrganizationRepository.save(membership);
        });

        dailyStatusRepository.findByUserIdAndDeletedAtIsNull(user.getId()).forEach(status -> {
            status.setDeletedAt(now);
            dailyStatusRepository.save(status);
        });

        activityDailyRepository.findByUserIdAndDeletedAtIsNull(user.getId()).forEach(activity -> {
            activity.setDeletedAt(now);
            activityDailyRepository.save(activity);
        });
    }
}
