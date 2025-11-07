package io.github.aikobn26.teamprogressviz.service.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import io.github.aikobn26.teamprogressviz.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.entity.User;
import io.github.aikobn26.teamprogressviz.repository.UserRepository;

@DataJpaTest
@Import(UserService.class)
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void ensureUserExists_createsNewUserWhenMissing() {
        var authUser = new AuthenticatedUser(100L, "octocat", "Octo Cat", "https://avatar");

        User user = userService.ensureUserExists(authUser);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getGithubId()).isEqualTo(100L);
        assertThat(user.getLogin()).isEqualTo("octocat");
        assertThat(userRepository.findByGithubId(100L)).isPresent();
    }

    @Test
    void ensureUserExists_updatesExistingUser() {
        var initial = User.builder()
                .githubId(200L)
                .login("old-login")
                .name("Old Name")
                .avatarUrl("https://old")
                .build();
        userRepository.save(initial);

        var updatedPayload = new AuthenticatedUser(200L, "new-login", "New Name", "https://new");

        User user = userService.ensureUserExists(updatedPayload);

        assertThat(user.getLogin()).isEqualTo("new-login");
        assertThat(user.getName()).isEqualTo("New Name");
        assertThat(user.getAvatarUrl()).isEqualTo("https://new");
    }

    @Test
    void ensureUserExists_reactivatesSoftDeletedUser() {
        var deleted = User.builder()
                .githubId(300L)
                .login("deleted")
                .name("Deleted User")
                .avatarUrl("https://avatar")
                .deletedAt(java.time.OffsetDateTime.now())
                .build();
        userRepository.save(deleted);

        var payload = new AuthenticatedUser(300L, "revived", "Revived", "https://new");

        User user = userService.ensureUserExists(payload);

        assertThat(user.getDeletedAt()).isNull();
        assertThat(user.getLogin()).isEqualTo("revived");
    }
}
