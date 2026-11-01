package com.inventory.stockmgmt.chatbot.service;

import com.inventory.stockmgmt.chatbot.model.ChatResponse;
import com.inventory.stockmgmt.chatbot.model.ChatTable;
import com.inventory.stockmgmt.chatbot.model.ChatUser;
import com.inventory.stockmgmt.chatbot.model.Intent;
import com.inventory.stockmgmt.chatbot.model.TimeRange;
import com.inventory.stockmgmt.chatbot.nlp.IntentDetector;
import com.inventory.stockmgmt.chatbot.nlp.TimeRangeParser;
import com.inventory.stockmgmt.chatbot.repository.ChatbotProductRepository;
import com.inventory.stockmgmt.chatbot.repository.ChatbotPurchaseRepository;
import com.inventory.stockmgmt.chatbot.repository.ChatbotSaleRepository;
import com.inventory.stockmgmt.entity.Category;
import com.inventory.stockmgmt.entity.Product;
import com.inventory.stockmgmt.entity.PurchaseOrder;
import com.inventory.stockmgmt.entity.SaleOrder;
import com.inventory.stockmgmt.entity.Supplier;
import com.inventory.stockmgmt.repository.CategoryRepository;
import com.inventory.stockmgmt.repository.SupplierRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatbotService {

    private static final int MAX_ROWS = 10;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final Pattern SKU_PATTERN = Pattern.compile("\\b[a-z]{2,6}-\\d{2,}\\b");

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "and", "for", "are", "you", "can", "what", "how", "much", "many", "show", "tell", "give",
            "about", "please", "stock", "price", "cost", "rate", "details", "detail", "info", "information",
            "check", "find", "search", "available", "availability", "quantity", "product", "products", "item",
            "items", "have", "has", "own", "our", "this", "that", "with", "from", "any", "all", "kitna",
            "kitne", "hai", "hain", "kya", "ka", "ki", "ke", "mein", "me", "batao", "dikhao", "bata", "sku",
            "does", "did", "there", "is", "of", "in", "on", "to", "do", "we", "my", "me"));

    private final ChatbotProductRepository productRepo;
    private final ChatbotSaleRepository saleRepo;
    private final ChatbotPurchaseRepository purchaseRepo;
    private final CategoryRepository categoryRepo;
    private final SupplierRepository supplierRepo;

    public ChatbotService(ChatbotProductRepository productRepo,
                          ChatbotSaleRepository saleRepo,
                          ChatbotPurchaseRepository purchaseRepo,
                          CategoryRepository categoryRepo,
                          SupplierRepository supplierRepo) {
        this.productRepo = productRepo;
        this.saleRepo = saleRepo;
        this.purchaseRepo = purchaseRepo;
        this.categoryRepo = categoryRepo;
        this.supplierRepo = supplierRepo;
    }

    @Transactional(readOnly = true)
    public ChatResponse reply(String rawMessage, ChatUser user) {
        String text = IntentDetector.normalize(rawMessage);
        if (text == null || text.isBlank()) {
            return unknown(user);
        }

        Intent intent = IntentDetector.detect(text);

        if (intent == Intent.UNKNOWN || intent == Intent.PRODUCT_LOOKUP) {
            List<Product> matches = findProducts(text);
            if (!matches.isEmpty()) {
                return productResponse(matches, user);
            }
            Category mentioned = findMentionedCategory(text);
            if (mentioned != null) {
                return categoryProducts(mentioned, user);
            }
            if (intent == Intent.PRODUCT_LOOKUP) {
                return ChatResponse.text(Intent.PRODUCT_LOOKUP,
                        "I couldn't find a product matching that. Try the product name or its SKU, for example \"price of EL-9081\".",
                        defaultSuggestions(user));
            }
            return unknown(user);
        }

        switch (intent) {
            case GREETING:
                return greeting(user);
            case THANKS:
                return ChatResponse.text(intent, "You're welcome! Ask me anything else about your stock.", defaultSuggestions(user));
            case HELP:
                return help(user);
            case LOW_STOCK:
                return lowStock(user);
            case OUT_OF_STOCK:
                return outOfStock(user);
            case REORDER:
                return reorder(user);
            case PRODUCT_COUNT:
                return productCount(user);
            case INVENTORY_VALUE:
                return inventoryValue(user);
            case SALES_SUMMARY:
                return salesSummary(text, user);
            case PURCHASE_SUMMARY:
                return purchaseSummary(text, user);
            case RECENT_SALES:
                return recentSales(user);
            case RECENT_PURCHASES:
                return recentPurchases(user);
            case TOP_SELLING:
                return topSelling(text, user);
            case PROFIT:
                return profit(text, user);
            case CATEGORIES:
                return categories(text, user);
            case SUPPLIERS:
                return suppliers(user);
            default:
                return unknown(user);
        }
    }

    private ChatResponse greeting(ChatUser user) {
        long low = productRepo.countAtOrBelowReorderLevel();
        String alert = low > 0
                ? "Heads up: " + low + " product(s) are at or below their reorder level."
                : "All products are above their reorder levels.";
        return ChatResponse.text(Intent.GREETING,
                "Hello " + user.username() + "! I'm StockBot, your inventory assistant.\n" + alert
                        + "\nAsk me about stock, sales or purchases, or type \"help\" to see everything I can do.",
                defaultSuggestions(user));
    }

    private ChatResponse help(ChatUser user) {
        StringBuilder sb = new StringBuilder();
        sb.append("Here's what I can do:\n");
        sb.append("• Stock levels – \"low stock items\", \"out of stock\", \"how many products\"\n");
        sb.append("• Product info – \"price of EL-9081\", \"stock of barcode scanner\"\n");
        sb.append("• Sales – \"sales today\", \"revenue this month\", \"recent sales\", \"top selling products\"\n");
        sb.append("• Categories – \"list categories\", \"products in electronics\"\n");
        if (user.canViewFinancials()) {
            sb.append("• Purchases – \"purchases this week\", \"recent purchases\", \"what should I reorder\"\n");
            sb.append("• Money – \"inventory value\", \"profit this month\"\n");
        }
        if (user.admin()) {
            sb.append("• Admin – \"show suppliers\"\n");
        }
        sb.append("You can also ask in simple Hinglish, e.g. \"aaj ki bikri\" or \"kam stock wale products\".\n\n");
        sb.append("--------------------\n");
        sb.append("Developer: Aayush Gupta");
        return ChatResponse.text(Intent.HELP, sb.toString(), defaultSuggestions(user));
    }

    private ChatResponse unknown(ChatUser user) {
        return ChatResponse.text(Intent.UNKNOWN,
                "Sorry, I didn't understand that. I can answer questions about stock levels, products, sales and more. "
                        + "Try one of the suggestions below or type \"help\".",
                defaultSuggestions(user));
    }

    private ChatResponse denied(Intent intent, String what, String who, ChatUser user) {
        return ChatResponse.text(intent,
                "Sorry, " + what + " can only be viewed by " + who + " accounts.",
                defaultSuggestions(user));
    }

    private ChatResponse lowStock(ChatUser user) {
        List<Product> low = productRepo.findAtOrBelowReorderLevel();
        if (low.isEmpty()) {
            return ChatResponse.text(Intent.LOW_STOCK,
                    "Good news – no product is at or below its reorder level.", defaultSuggestions(user));
        }
        List<List<String>> rows = new ArrayList<>();
        for (Product p : limit(low)) {
            rows.add(List.of(p.getSku(), p.getName(), qty(p) + unitSuffix(p), String.valueOf(reorder(p)),
                    qty(p) <= 0 ? "Out of stock" : "Low"));
        }
        String msg = low.size() + " product(s) are at or below their reorder level" + shownNote(low.size()) + ".";
        return ChatResponse.withTable(Intent.LOW_STOCK, msg,
                new ChatTable(List.of("SKU", "Product", "In stock", "Reorder level", "Status"), rows),
                user.canViewFinancials() ? List.of("What should I reorder?", "Out of stock items") : defaultSuggestions(user));
    }

    private ChatResponse outOfStock(ChatUser user) {
        List<Product> out = productRepo.findByQuantityInStockLessThanEqualOrderByNameAsc(0);
        if (out.isEmpty()) {
            return ChatResponse.text(Intent.OUT_OF_STOCK, "Nothing is out of stock right now.", defaultSuggestions(user));
        }
        List<List<String>> rows = new ArrayList<>();
        for (Product p : limit(out)) {
            rows.add(List.of(p.getSku(), p.getName(), categoryName(p)));
        }
        return ChatResponse.withTable(Intent.OUT_OF_STOCK,
                out.size() + " product(s) are out of stock" + shownNote(out.size()) + ".",
                new ChatTable(List.of("SKU", "Product", "Category"), rows), defaultSuggestions(user));
    }

    private ChatResponse reorder(ChatUser user) {
        if (!user.canViewFinancials()) {
            return denied(Intent.REORDER, "reorder planning", "Admin or Manager", user);
        }
        List<Product> low = productRepo.findAtOrBelowReorderLevel();
        if (low.isEmpty()) {
            return ChatResponse.text(Intent.REORDER, "Nothing needs reordering – every product is above its reorder level.",
                    defaultSuggestions(user));
        }
        BigDecimal totalCost = BigDecimal.ZERO;
        List<List<String>> rows = new ArrayList<>();
        int shown = 0;
        for (Product p : low) {
            int suggested = suggestedOrderQty(p);
            BigDecimal lineCost = nz(p.getCostPrice()).multiply(BigDecimal.valueOf(suggested));
            totalCost = totalCost.add(lineCost);
            if (shown++ < MAX_ROWS) {
                Supplier s = p.getSupplier();
                rows.add(List.of(p.getSku(), p.getName(), qty(p) + unitSuffix(p), String.valueOf(suggested),
                        s != null ? s.getCompanyName() : "— no supplier —", money(lineCost)));
            }
        }
        String msg = "Suggested restock for " + low.size() + " product(s)" + shownNote(low.size())
                + ". Quantities bring each product up to 2× its reorder level. Estimated total cost: " + money(totalCost) + ".";
        return ChatResponse.withTable(Intent.REORDER, msg,
                new ChatTable(List.of("SKU", "Product", "In stock", "Order qty", "Supplier", "Est. cost"), rows),
                defaultSuggestions(user));
    }

    private ChatResponse productCount(ChatUser user) {
        long total = productRepo.count();
        long low = productRepo.countAtOrBelowReorderLevel();
        long out = productRepo.countByQuantityInStockLessThanEqual(0);
        long units = nz(productRepo.totalUnits());
        long categories = categoryRepo.count();
        String msg = "You have " + total + " product(s) across " + categories + " categor" + (categories == 1 ? "y" : "ies")
                + ", with " + units + " units in stock in total.\n"
                + "• At or below reorder level: " + low + "\n"
                + "• Out of stock: " + out;
        return ChatResponse.text(Intent.PRODUCT_COUNT, msg, defaultSuggestions(user));
    }

    private ChatResponse inventoryValue(ChatUser user) {
        if (!user.canViewFinancials()) {
            return denied(Intent.INVENTORY_VALUE, "inventory valuation", "Admin or Manager", user);
        }
        BigDecimal cost = nz(productRepo.totalCostValue());
        BigDecimal retail = nz(productRepo.totalRetailValue());
        String msg = "Inventory valuation:\n"
                + "• At cost price: " + money(cost) + "\n"
                + "• At selling price: " + money(retail) + "\n"
                + "• Potential gross margin: " + money(retail.subtract(cost)) + "\n"
                + "(" + nz(productRepo.totalUnits()) + " units in stock)";
        return ChatResponse.text(Intent.INVENTORY_VALUE, msg, defaultSuggestions(user));
    }

    private ChatResponse salesSummary(String text, ChatUser user) {
        TimeRange r = TimeRangeParser.parse(text, LocalDate.now());
        long orders = saleRepo.countBetween(r.from(), r.to());
        if (orders == 0) {
            return ChatResponse.text(Intent.SALES_SUMMARY, "No sales recorded for " + r.label() + ".", defaultSuggestions(user));
        }
        BigDecimal revenue = nz(saleRepo.revenueBetween(r.from(), r.to()));
        long units = nz(saleRepo.unitsBetween(r.from(), r.to()));
        BigDecimal avg = revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
        String msg = "Sales (" + r.label() + "):\n"
                + "• Orders: " + orders + "\n"
                + "• Units sold: " + units + "\n"
                + "• Revenue: " + money(revenue) + "\n"
                + "• Average order value: " + money(avg);
        return ChatResponse.text(Intent.SALES_SUMMARY, msg, List.of("Top selling products", "Recent sales", "Sales this month"));
    }

    private ChatResponse purchaseSummary(String text, ChatUser user) {
        if (!user.canViewFinancials()) {
            return denied(Intent.PURCHASE_SUMMARY, "purchase information", "Admin or Manager", user);
        }
        TimeRange r = TimeRangeParser.parse(text, LocalDate.now());
        long orders = purchaseRepo.countBetween(r.from(), r.to());
        if (orders == 0) {
            return ChatResponse.text(Intent.PURCHASE_SUMMARY, "No purchases recorded for " + r.label() + ".", defaultSuggestions(user));
        }
        String msg = "Purchases (" + r.label() + "):\n"
                + "• Purchase orders: " + orders + "\n"
                + "• Units bought: " + nz(purchaseRepo.unitsBetween(r.from(), r.to())) + "\n"
                + "• Total spend: " + money(nz(purchaseRepo.spendBetween(r.from(), r.to())));
        return ChatResponse.text(Intent.PURCHASE_SUMMARY, msg, List.of("Recent purchases", "Purchases this month"));
    }

    private ChatResponse recentSales(ChatUser user) {
        List<SaleOrder> sales = saleRepo.findTop10ByOrderBySaleDateDesc();
        if (sales.isEmpty()) {
            return ChatResponse.text(Intent.RECENT_SALES, "No sales have been recorded yet.", defaultSuggestions(user));
        }
        List<List<String>> rows = new ArrayList<>();
        for (SaleOrder s : sales) {
            rows.add(List.of(s.getInvoiceNumber(), s.getCustomerName(), s.getProduct().getName(),
                    String.valueOf(s.getQuantity()), money(s.getTotalAmount()), formatDate(s.getSaleDate())));
        }
        return ChatResponse.withTable(Intent.RECENT_SALES, "Your " + sales.size() + " most recent sale(s):",
                new ChatTable(List.of("Invoice", "Customer", "Product", "Qty", "Total", "Date"), rows),
                List.of("Sales today", "Top selling products"));
    }

    private ChatResponse recentPurchases(ChatUser user) {
        if (!user.canViewFinancials()) {
            return denied(Intent.RECENT_PURCHASES, "purchase information", "Admin or Manager", user);
        }
        List<PurchaseOrder> orders = purchaseRepo.findTop10ByOrderByOrderDateDesc();
        if (orders.isEmpty()) {
            return ChatResponse.text(Intent.RECENT_PURCHASES, "No purchases have been recorded yet.", defaultSuggestions(user));
        }
        List<List<String>> rows = new ArrayList<>();
        for (PurchaseOrder o : orders) {
            rows.add(List.of(o.getPoNumber(), o.getProduct().getName(),
                    o.getSupplier() != null ? o.getSupplier().getCompanyName() : "—",
                    String.valueOf(o.getQuantity()), money(o.getTotalAmount()), formatDate(o.getOrderDate())));
        }
        return ChatResponse.withTable(Intent.RECENT_PURCHASES, "Your " + orders.size() + " most recent purchase order(s):",
                new ChatTable(List.of("PO", "Product", "Supplier", "Qty", "Total", "Date"), rows),
                List.of("Purchases this month", "What should I reorder?"));
    }

    private ChatResponse topSelling(String text, ChatUser user) {
        TimeRange r = TimeRangeParser.parse(text, LocalDate.now());
        List<Object[]> top = saleRepo.topSelling(r.from(), r.to(), PageRequest.of(0, 5));
        if (top.isEmpty()) {
            return ChatResponse.text(Intent.TOP_SELLING, "No sales recorded for " + r.label() + ".", defaultSuggestions(user));
        }
        List<List<String>> rows = new ArrayList<>();
        int rank = 1;
        for (Object[] row : top) {
            rows.add(List.of(String.valueOf(rank++), String.valueOf(row[0]),
                    String.valueOf(((Number) row[1]).longValue()), money(toBigDecimal(row[2]))));
        }
        return ChatResponse.withTable(Intent.TOP_SELLING, "Top selling products (" + r.label() + "):",
                new ChatTable(List.of("#", "Product", "Units sold", "Revenue"), rows),
                List.of("Sales today", "Top selling products this month"));
    }

    private ChatResponse profit(String text, ChatUser user) {
        if (!user.canViewFinancials()) {
            return denied(Intent.PROFIT, "profit figures", "Admin or Manager", user);
        }
        TimeRange r = TimeRangeParser.parse(text, LocalDate.now());
        if (saleRepo.countBetween(r.from(), r.to()) == 0) {
            return ChatResponse.text(Intent.PROFIT, "No sales recorded for " + r.label() + ", so there is no profit to report.",
                    defaultSuggestions(user));
        }
        BigDecimal revenue = nz(saleRepo.revenueBetween(r.from(), r.to()));
        BigDecimal profit = nz(saleRepo.estimatedProfitBetween(r.from(), r.to()));
        String margin = revenue.signum() > 0
                ? profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP) + "%"
                : "n/a";
        String msg = "Profit (" + r.label() + "):\n"
                + "• Revenue: " + money(revenue) + "\n"
                + "• Estimated gross profit: " + money(profit) + "\n"
                + "• Margin: " + margin + "\n"
                + "(Estimate: uses each product's current cost price.)";
        return ChatResponse.text(Intent.PROFIT, msg, List.of("Sales this month", "Inventory value"));
    }

    private ChatResponse categories(String text, ChatUser user) {
        List<Category> all = categoryRepo.findAll();
        if (all.isEmpty()) {
            return ChatResponse.text(Intent.CATEGORIES, "No categories have been created yet.", defaultSuggestions(user));
        }
        Category mentioned = findMentionedCategory(text);
        if (mentioned != null) {
            return categoryProducts(mentioned, user);
        }
        List<List<String>> rows = new ArrayList<>();
        for (Category c : all) {
            rows.add(List.of(c.getName(), c.getDescription() == null ? "—" : c.getDescription(),
                    String.valueOf(productRepo.countByCategoryId(c.getId()))));
        }
        return ChatResponse.withTable(Intent.CATEGORIES, "You have " + all.size() + " categor" + (all.size() == 1 ? "y" : "ies") + ":",
                new ChatTable(List.of("Category", "Description", "Products"), rows), defaultSuggestions(user));
    }

    private Category findMentionedCategory(String text) {
        for (Category c : categoryRepo.findAll()) {
            String name = IntentDetector.normalize(c.getName());
            if (!name.isBlank() && Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(text).find()) {
                return c;
            }
        }
        return null;
    }

    private ChatResponse categoryProducts(Category c, ChatUser user) {
        List<Product> products = productRepo.findByCategoryIdOrderByNameAsc(c.getId());
        if (products.isEmpty()) {
            return ChatResponse.text(Intent.CATEGORIES, "There are no products in " + c.getName() + " yet.", defaultSuggestions(user));
        }
        List<List<String>> rows = new ArrayList<>();
        for (Product p : limit(products)) {
            rows.add(List.of(p.getSku(), p.getName(), qty(p) + unitSuffix(p), money(p.getSellingPrice())));
        }
        return ChatResponse.withTable(Intent.CATEGORIES,
                products.size() + " product(s) in " + c.getName() + shownNote(products.size()) + ":",
                new ChatTable(List.of("SKU", "Product", "In stock", "Price"), rows), defaultSuggestions(user));
    }

    private ChatResponse suppliers(ChatUser user) {
        if (!user.admin()) {
            return denied(Intent.SUPPLIERS, "supplier details", "Admin", user);
        }
        List<Supplier> all = supplierRepo.findAll();
        if (all.isEmpty()) {
            return ChatResponse.text(Intent.SUPPLIERS, "No suppliers have been added yet.", defaultSuggestions(user));
        }
        List<List<String>> rows = new ArrayList<>();
        for (Supplier s : limit(all)) {
            rows.add(List.of(s.getCompanyName(), orDash(s.getContactPerson()), orDash(s.getPhone()), orDash(s.getEmail())));
        }
        return ChatResponse.withTable(Intent.SUPPLIERS, all.size() + " supplier(s)" + shownNote(all.size()) + ":",
                new ChatTable(List.of("Company", "Contact", "Phone", "Email"), rows), defaultSuggestions(user));
    }

    private ChatResponse productResponse(List<Product> matches, ChatUser user) {
        if (matches.size() == 1) {
            Product p = matches.get(0);
            StringBuilder sb = new StringBuilder();
            sb.append(p.getName()).append(" (").append(p.getSku()).append(")\n");
            sb.append("• In stock: ").append(qty(p)).append(unitSuffix(p)).append(" – ").append(stockStatus(p)).append('\n');
            sb.append("• Selling price: ").append(money(p.getSellingPrice())).append('\n');
            if (user.canViewFinancials()) {
                sb.append("• Cost price: ").append(money(p.getCostPrice())).append('\n');
                if (p.getSupplier() != null) {
                    sb.append("• Supplier: ").append(p.getSupplier().getCompanyName()).append('\n');
                }
            }
            sb.append("• Category: ").append(categoryName(p));
            return ChatResponse.text(Intent.PRODUCT_LOOKUP, sb.toString(), defaultSuggestions(user));
        }
        List<List<String>> rows = new ArrayList<>();
        for (Product p : matches) {
            rows.add(List.of(p.getSku(), p.getName(), qty(p) + unitSuffix(p), money(p.getSellingPrice())));
        }
        return ChatResponse.withTable(Intent.PRODUCT_LOOKUP, "I found " + matches.size() + " products that could match. Ask again with the SKU for full details:",
                new ChatTable(List.of("SKU", "Product", "In stock", "Price"), rows), defaultSuggestions(user));
    }

    private List<Product> findProducts(String text) {
        Matcher m = SKU_PATTERN.matcher(text);
        while (m.find()) {
            Optional<Product> bySku = productRepo.findBySkuIgnoreCase(m.group());
            if (bySku.isPresent()) {
                return List.of(bySku.get());
            }
        }

        List<String> tokens = Arrays.stream(text.split(" "))
                .filter(t -> t.length() >= 3 && !STOP_WORDS.contains(t))
                .distinct()
                .limit(6)
                .toList();
        if (tokens.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> byId = new LinkedHashMap<>();
        Map<Long, Integer> score = new HashMap<>();
        for (String token : tokens) {
            for (Product p : productRepo.search(token)) {
                byId.putIfAbsent(p.getId(), p);
                score.merge(p.getId(), 1, Integer::sum);
            }
        }
        if (byId.isEmpty()) {
            return List.of();
        }
        int best = Collections.max(score.values());
        return byId.values().stream().filter(p -> score.get(p.getId()) == best).limit(5).toList();
    }

    private static List<String> defaultSuggestions(ChatUser user) {
        List<String> s = new ArrayList<>(List.of("Low stock items", "Out of stock items", "Sales today", "Top selling products"));
        if (user.canViewFinancials()) {
            s.add("Inventory value");
            s.add("Profit this month");
        }
        return s;
    }

    private static <T> List<T> limit(List<T> list) {
        return list.size() > MAX_ROWS ? list.subList(0, MAX_ROWS) : list;
    }

    private static String shownNote(int total) {
        return total > MAX_ROWS ? " (showing the first " + MAX_ROWS + ")" : "";
    }

    private static int qty(Product p) {
        return p.getQuantityInStock() == null ? 0 : p.getQuantityInStock();
    }

    private static int reorder(Product p) {
        return p.getReorderLevel() == null ? 0 : p.getReorderLevel();
    }

    private static int suggestedOrderQty(Product p) {
        int target = Math.max(reorder(p) * 2, 1);
        return Math.max(target - qty(p), 1);
    }

    private static String unitSuffix(Product p) {
        return p.getUnit() == null || p.getUnit().isBlank() ? "" : " " + p.getUnit();
    }

    private static String stockStatus(Product p) {
        if (qty(p) <= 0) return "OUT OF STOCK";
        if (qty(p) <= reorder(p)) return "LOW (reorder level " + reorder(p) + ")";
        return "OK (reorder level " + reorder(p) + ")";
    }

    private static String categoryName(Product p) {
        return p.getCategory() == null ? "—" : p.getCategory().getName();
    }

    private static String orDash(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String money(BigDecimal value) {
        return String.format(Locale.US, "$%,.2f", nz(value));
    }

    private static String formatDate(LocalDateTime dt) {
        return dt == null ? "—" : DATE_FMT.format(dt);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}