package com.mkisten.vacancybackend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long telegramId;
    private LocalDateTime expiresAt;
}