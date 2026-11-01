package com.inventory.stockmgmt.chatbot.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Incoming message from the browser: {"message": "low stock items"} */
public record ChatRequest(
        @NotBlank(message = "Please type a message.")
        @Size(max = 500, message = "Message is too long (max 500 characters).")
        String message) {
}
