package io.github.aikobn26.teamprogressviz.feature.organization.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.aikobn26.teamprogressviz.feature.organization.entity.UserOrganization;


@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, Long> {

    List<UserOrganization> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<UserOrganization> findByUserIdAndOrganizationId(Long userId, Long organizationId);

    Optional<UserOrganization> findByUserIdAndOrganizationIdAndDeletedAtIsNull(Long userId, Long organizationId);

    List<UserOrganization> findByOrganizationIdAndDeletedAtIsNull(Long organizationId);

    boolean existsByOrganizationIdAndDeletedAtIsNull(Long organizationId);
}
