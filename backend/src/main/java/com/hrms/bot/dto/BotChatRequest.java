package com.hrms.bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BotChatRequest {
    @NotBlank
    @Size(max = 500)
    private String message;
}
