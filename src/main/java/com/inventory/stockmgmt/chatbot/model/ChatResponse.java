package com.inventory.stockmgmt.chatbot.model;

import java.util.List;

/**
 * Reply sent back to the browser.
 *
 * @param reply       the text to show
 * @param intent      which intent the backend resolved (handy for debugging / analytics)
 * @param table       optional table (null when the reply is text only)
 * @param suggestions follow-up questions the UI can show as clickable chips
 */
public record ChatResponse(String reply, String intent, ChatTable table, List<String> suggestions) {

    public static ChatResponse text(Intent intent, String reply, List<String> suggestions) {
        return new ChatResponse(reply, intent.name(), null, suggestions);
    }

    public static ChatResponse withTable(Intent intent, String reply, ChatTable table, List<String> suggestions) {
        return new ChatResponse(reply, intent.name(), table, suggestions);
    }
}
