package com.inventory.stockmgmt.chatbot.controller;

import com.inventory.stockmgmt.chatbot.model.ChatRequest;
import com.inventory.stockmgmt.chatbot.model.ChatResponse;
import com.inventory.stockmgmt.chatbot.model.ChatUser;
import com.inventory.stockmgmt.chatbot.model.Intent;
import com.inventory.stockmgmt.chatbot.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * JSON API used by the chat screen.
 *   POST /chatbot/api/message   {"message": "low stock items"}
 *   GET  /chatbot/api/welcome
 * Both require a logged-in user (covered by the app's existing "anyRequest().authenticated()" rule),
 * and the POST requires the normal CSRF token like every other form in the app.
 */
@RestController
@RequestMapping("/chatbot/api")
public class ChatbotApiController {

    private final ChatbotService chatbotService;

    public ChatbotApiController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public ChatResponse message(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        return chatbotService.reply(request.message(), ChatUser.from(authentication));
    }

    @GetMapping("/welcome")
    public ChatResponse welcome(Authentication authentication) {
        return chatbotService.reply("hello", ChatUser.from(authentication));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ChatResponse> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getDefaultMessage())
                .orElse("Invalid message.");
        return ResponseEntity.badRequest().body(ChatResponse.text(Intent.UNKNOWN, msg, List.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ChatResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ChatResponse.text(Intent.UNKNOWN, "I couldn't read that message.", List.of()));
    }
}
