package com.inventory.stockmgmt.chatbot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Serves the chat screen at /chatbot (any logged-in user). */
@Controller
@RequestMapping("/chatbot")
public class ChatbotPageController {

    @GetMapping
    public String chatPage() {
        return "chatbot/chat";
    }
}
