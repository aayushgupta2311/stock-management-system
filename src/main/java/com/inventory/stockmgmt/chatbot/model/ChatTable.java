package com.inventory.stockmgmt.chatbot.model;

import java.util.List;

/** Optional tabular payload attached to a reply (headers + rows of plain text). */
public record ChatTable(List<String> headers, List<List<String>> rows) {
}
