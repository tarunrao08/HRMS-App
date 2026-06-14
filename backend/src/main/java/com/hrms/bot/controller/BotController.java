package com.hrms.bot.controller;

import com.hrms.bot.dto.BotChatRequest;
import com.hrms.bot.dto.BotChatResponse;
import com.hrms.bot.service.BotService;
import com.hrms.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotController {

    private final BotService botService;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BotChatResponse>> chat(
            @Valid @RequestBody BotChatRequest request,
            Authentication authentication) {
        BotChatResponse response = botService.chat(request.getMessage(), authentication);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
