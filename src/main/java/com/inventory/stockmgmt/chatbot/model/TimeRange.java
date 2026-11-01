package com.inventory.stockmgmt.chatbot.model;

import java.time.LocalDateTime;

/** A half-open time window [from, to) plus a human-friendly label such as "this month". */
public record TimeRange(String label, LocalDateTime from, LocalDateTime to) {
}
