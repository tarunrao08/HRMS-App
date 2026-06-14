package com.hrms.bot.service;

import com.hrms.bot.dto.BotChatResponse;
import org.springframework.security.core.Authentication;

public interface BotService {
    BotChatResponse chat(String message, Authentication authentication);
}
