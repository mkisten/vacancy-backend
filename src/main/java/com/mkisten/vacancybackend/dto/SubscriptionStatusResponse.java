package com.mkisten.vacancybackend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SubscriptionStatusResponse {
    private Long telegramId;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private LocalDate subscriptionEndDate;
    private String subscriptionPlan;
    private boolean active;
    private long daysRemaining;
    private boolean trialUsed;
    private String role;
}
