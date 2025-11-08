package io.github.aikobn26.teamprogressviz.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.aikobn26.teamprogressviz.entity.ActivityDaily;

@Repository
public interface ActivityDailyRepository extends JpaRepository<ActivityDaily, Long> {

    Optional<ActivityDaily> findByOrganizationIdAndUserIdAndDateAndDeletedAtIsNull(Long organizationId,
                                                                                   Long userId,
                                                                                   LocalDate date);

    List<ActivityDaily> findByOrganizationIdAndDateBetweenAndDeletedAtIsNull(Long organizationId,
                                                                             LocalDate startDate,
                                                                             LocalDate endDate);

    List<ActivityDaily> findByOrganizationIdAndUserIdAndDateBetweenAndDeletedAtIsNullOrderByDateAsc(Long organizationId,
                                                                                                    Long userId,
                                                                                                    LocalDate startDate,
                                                                                                    LocalDate endDate);

    List<ActivityDaily> findByUserIdAndDeletedAtIsNull(Long userId);
}
