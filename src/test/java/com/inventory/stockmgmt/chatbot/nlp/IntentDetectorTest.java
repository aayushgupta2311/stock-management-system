package com.inventory.stockmgmt.chatbot.nlp;

import com.inventory.stockmgmt.chatbot.model.Intent;
import com.inventory.stockmgmt.chatbot.model.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pure unit tests (no Spring context, no database needed). Run with: mvn test */
class IntentDetectorTest {

    private static Intent detect(String message) {
        return IntentDetector.detect(IntentDetector.normalize(message));
    }

    @Test
    void understandsGreetingsAndHelp() {
        assertEquals(Intent.GREETING, detect("Hello!"));
        assertEquals(Intent.UNKNOWN, detect("high stock items"));   // "hi" must not match inside "high"
        assertEquals(Intent.THANKS, detect("thanks a lot"));
        assertEquals(Intent.HELP, detect("what can you do?"));
    }

    @Test
    void understandsStockQuestions() {
        assertEquals(Intent.LOW_STOCK, detect("Which items are low on stock?"));
        assertEquals(Intent.LOW_STOCK, detect("kaunse products kam stock mein hai"));
        assertEquals(Intent.OUT_OF_STOCK, detect("what is out of stock"));
        assertEquals(Intent.REORDER, detect("what should I reorder?"));
        assertEquals(Intent.PRODUCT_COUNT, detect("how many products do we have"));
        assertEquals(Intent.INVENTORY_VALUE, detect("what is my inventory value"));
    }

    @Test
    void understandsSalesAndPurchases() {
        assertEquals(Intent.SALES_SUMMARY, detect("sales today"));
        assertEquals(Intent.SALES_SUMMARY, detect("aaj ki bikri kitni hui"));
        assertEquals(Intent.SALES_SUMMARY, detect("how much revenue this month"));
        assertEquals(Intent.PURCHASE_SUMMARY, detect("how much did we spend last month"));
        assertEquals(Intent.RECENT_SALES, detect("recent sales"));        // beats plain "sales"
        assertEquals(Intent.RECENT_PURCHASES, detect("show latest purchases"));
        assertEquals(Intent.TOP_SELLING, detect("top selling products"));
        assertEquals(Intent.PROFIT, detect("profit this month"));
    }

    @Test
    void unknownTextFallsThrough() {
        assertEquals(Intent.UNKNOWN, detect("tell me a joke"));
        assertEquals(Intent.UNKNOWN, detect(""));
    }

    @Test
    void parsesTimeRanges() {
        LocalDate today = LocalDate.of(2026, 11, 1); // a Sunday
        assertEquals("today", range("sales today", today).label());
        assertEquals("yesterday", range("yesterday sales", today).label());
        assertEquals("this week", range("sales this week", today).label());
        assertEquals(LocalDate.of(2026, 10, 26).atStartOfDay(), range("sales this week", today).from()); // Monday
        assertEquals("last month", range("profit last month", today).label());
        assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(), range("profit last month", today).from());
        assertEquals("all time", range("total sales", today).label());
    }

    private static TimeRange range(String message, LocalDate today) {
        return TimeRangeParser.parse(IntentDetector.normalize(message), today);
    }
}
