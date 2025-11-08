package io.github.aikobn26.teamprogressviz.feature.organization.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nimbusds.openid.connect.sdk.assurance.evidences.Organization;


@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByGithubId(Long githubId);

    Optional<Organization> findByGithubIdAndDeletedAtIsNull(Long githubId);

    Optional<Organization> findByLoginIgnoreCase(String login);

    Optional<Organization> findByLoginIgnoreCaseAndDeletedAtIsNull(String login);

    Optional<Organization> findByIdAndDeletedAtIsNull(Long id);
}
