package com.inventory.stockmgmt.chatbot.model;

/**
 * Everything the chatbot knows how to answer.
 * Declaration order matters: when two intents score the same, the earlier one wins.
 */
public enum Intent {
    GREETING,
    THANKS,
    HELP,
    LOW_STOCK,
    OUT_OF_STOCK,
    REORDER,
    PRODUCT_COUNT,
    INVENTORY_VALUE,
    SALES_SUMMARY,
    PURCHASE_SUMMARY,
    RECENT_SALES,
    RECENT_PURCHASES,
    TOP_SELLING,
    PROFIT,
    CATEGORIES,
    SUPPLIERS,
    PRODUCT_LOOKUP,
    UNKNOWN
}
