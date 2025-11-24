package com.mkisten.vacancybackend.repository;

import com.mkisten.vacancybackend.entity.Vacancy;
import com.mkisten.vacancybackend.entity.VacancyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface VacancyRepository extends JpaRepository<Vacancy, String> {

    List<Vacancy> findByUserTelegramIdOrderByPublishedAtDesc(Long telegramId);

    List<Vacancy> findByUserTelegramIdAndStatusOrderByPublishedAtDesc(
            Long telegramId, VacancyStatus status);

    boolean existsByIdAndUserTelegramId(String id, Long userTelegramId);

    @Query("SELECT v.id FROM Vacancy v WHERE v.userTelegramId = :telegramId")
    Set<String> findVacancyIdsByUser(@Param("telegramId") Long telegramId);

    @Modifying
    @Query("UPDATE Vacancy v SET v.status = :status " +
            "WHERE v.userTelegramId = :telegramId AND v.id = :vacancyId")
    void updateStatus(@Param("telegramId") Long telegramId,
                      @Param("vacancyId") String vacancyId,
                      @Param("status") VacancyStatus status);

    @Modifying
    @Query("UPDATE Vacancy v SET v.status = :status " +
            "WHERE v.userTelegramId = :telegramId AND v.id IN :vacancyIds")
    void updateMultipleStatus(@Param("telegramId") Long telegramId,
                              @Param("vacancyIds") List<String> vacancyIds,
                              @Param("status") VacancyStatus status);

    @Modifying
    @Query("DELETE FROM Vacancy v WHERE v.userTelegramId = :telegramId AND v.id = :vacancyId")
    void deleteByUserAndId(@Param("telegramId") Long telegramId,
                           @Param("vacancyId") String vacancyId);

    @Query("SELECT COUNT(v) FROM Vacancy v WHERE v.userTelegramId = :telegramId AND v.status = 'NEW'")
    Long countNewVacancies(@Param("telegramId") Long telegramId);

    List<Vacancy> findBySentToTelegramFalse();
    List<Vacancy> findAllByTelegramIdAndSentToTelegramFalse(Long telegramId);

    // Для фильтрации уже отправленных
    List<Vacancy> findByUserTelegramIdAndSentToTelegramFalse(Long telegramId);

    // Для пакетной смены статуса "отправлено"
    @Modifying
    @Query("UPDATE Vacancy v SET v.sentToTelegram = true WHERE v.userTelegramId = :telegramId AND v.id IN :ids")
    int markAsSentToTelegram(@Param("telegramId") Long telegramId, @Param("ids") List<String> ids);
}
