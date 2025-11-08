package io.github.aikobn26.teamprogressviz.feature.organization.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.aikobn26.teamprogressviz.feature.organization.entity.DailyStatus;


@Repository
public interface DailyStatusRepository extends JpaRepository<DailyStatus, Long> {

    Optional<DailyStatus> findByIdAndOrganizationIdAndDeletedAtIsNull(Long id, Long organizationId);

    Optional<DailyStatus> findByOrganizationIdAndUserIdAndDateAndDeletedAtIsNull(Long organizationId,
                                                                                 Long userId,
                                                                                 LocalDate date);

    List<DailyStatus> findByOrganizationIdAndDateAndDeletedAtIsNull(Long organizationId, LocalDate date);

    List<DailyStatus> findByOrganizationIdAndUserIdAndDateLessThanEqualAndDeletedAtIsNullOrderByDateDesc(
            Long organizationId,
            Long userId,
            LocalDate date);

    long countByOrganizationIdAndDateAndDeletedAtIsNull(Long organizationId, LocalDate date);

    List<DailyStatus> findByOrganizationIdAndUserIdAndDateBetweenAndDeletedAtIsNullOrderByDateDesc(
            Long organizationId,
            Long userId,
            LocalDate startDate,
            LocalDate endDate);

        List<DailyStatus> findByUserIdAndDeletedAtIsNull(Long userId);
}
