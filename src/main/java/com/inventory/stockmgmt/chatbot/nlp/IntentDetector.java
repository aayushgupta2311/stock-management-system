package com.inventory.stockmgmt.chatbot.nlp;

import com.inventory.stockmgmt.chatbot.model.Intent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Rule-based intent detection (no external AI service needed).
 *
 * Every intent has a list of keywords/phrases (English + Roman-script Hindi). Each phrase that
 * appears in the message adds to that intent's score; longer phrases score higher so that
 * "recent sales" beats plain "sales". The intent with the highest score wins.
 * Pure Java (no Spring) so it is easy to unit test.
 */
public final class IntentDetector {

    private record Keyword(Pattern pattern, int weight) {
    }

    private static final Map<Intent, List<Keyword>> KEYWORDS = new EnumMap<>(Intent.class);

    static {
        register(Intent.GREETING, "hi", "hii", "hello", "hey", "namaste", "good morning", "good afternoon", "good evening");
        register(Intent.THANKS, "thanks", "thank you", "thx", "shukriya", "dhanyavad");
        register(Intent.HELP, "help", "what can you do", "commands", "options", "menu", "features", "madad", "kya kar sakte");

        register(Intent.LOW_STOCK, "low stock", "low on stock", "low in stock", "running low", "running out",
                "low inventory", "shortage", "critical stock", "critical items", "kam stock", "stock kam", "kam hai");
        register(Intent.OUT_OF_STOCK, "out of stock", "stock out", "stockout", "sold out", "zero stock", "no stock",
                "khatam", "stock khatam");
        register(Intent.REORDER, "reorder", "restock", "what to order", "what should i order", "kitna order", "order karna");

        register(Intent.PRODUCT_COUNT, "how many products", "how many items", "how many skus", "total products",
                "total items", "total skus", "number of products", "product count", "kitne products", "kitne product", "kitne items");
        register(Intent.INVENTORY_VALUE, "inventory value", "stock value", "value of stock", "value of inventory",
                "total value", "inventory worth", "worth", "asset", "kitni value");

        register(Intent.SALES_SUMMARY, "sales", "sale", "revenue", "sold", "income", "bikri", "becha");
        register(Intent.PURCHASE_SUMMARY, "purchases", "purchase", "spend", "spent", "spending", "expense", "expenses",
                "bought", "procurement", "kharid", "kharcha");
        register(Intent.RECENT_SALES, "recent sales", "latest sales", "last sales", "recent sale", "latest sale",
                "last sale", "recent invoices", "last invoices", "recent invoice");
        register(Intent.RECENT_PURCHASES, "recent purchases", "latest purchases", "last purchases", "recent purchase",
                "latest purchase", "last purchase", "recent purchase orders");
        register(Intent.TOP_SELLING, "top selling", "best selling", "best seller", "bestseller", "top seller",
                "most sold", "most popular", "popular", "top products", "top product", "sabse zyada", "sabse jyada");
        register(Intent.PROFIT, "profit", "margin", "earnings", "munafa", "fayda");

        register(Intent.CATEGORIES, "categories", "category", "categorie");
        register(Intent.SUPPLIERS, "suppliers", "supplier", "vendors", "vendor");

        register(Intent.PRODUCT_LOOKUP, "price", "rate", "stock of", "kitna", "kitne", "available",
                "availability", "in stock", "quantity", "qty", "cost of", "details", "info", "find", "search", "check", "hai kya");
    }

    private IntentDetector() {
    }

    private static void register(Intent intent, String... phrases) {
        List<Keyword> list = new ArrayList<>();
        for (String phrase : phrases) {
            int words = phrase.split(" ").length;
            int weight = words * 10 + phrase.length();
            // Short words must match exactly ("hi" must not match "high"); longer ones may carry a suffix ("sale" -> "sales").
            String suffix = phrase.length() >= 5 ? "\\w*" : "\\b";
            list.add(new Keyword(Pattern.compile("\\b" + Pattern.quote(phrase) + suffix), weight));
        }
        KEYWORDS.put(intent, list);
    }

    /** Lower-cases, strips punctuation (keeps letters, digits and '-') and collapses spaces. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\-\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** @param normalized text already passed through {@link #normalize(String)} */
    public static Intent detect(String normalized) {
        Intent best = Intent.UNKNOWN;
        int bestScore = 0;
        for (Map.Entry<Intent, List<Keyword>> entry : KEYWORDS.entrySet()) {
            int score = 0;
            for (Keyword k : entry.getValue()) {
                if (k.pattern().matcher(normalized).find()) {
                    score += k.weight();
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }
        return best;
    }
}
