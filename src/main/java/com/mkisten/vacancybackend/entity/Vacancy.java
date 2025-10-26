package com.mkisten.vacancybackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vacancies")
@Getter
@Setter
public class Vacancy {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "user_telegram_id", nullable = false)
    private Long userTelegramId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "employer", length = 255)
    private String employer;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "schedule", length = 50)
    private String schedule;

    @Column(name = "salary", length = 100)
    private String salary;

    @Column(name = "url", length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VacancyStatus status = VacancyStatus.NEW;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "loaded_at")
    private LocalDateTime loadedAt;

    // Конструкторы
    public Vacancy() {}

    public Vacancy(String id, Long userTelegramId, String title) {
        this.id = id;
        this.userTelegramId = userTelegramId;
        this.title = title;
        this.loadedAt = LocalDateTime.now();
    }
}
