package com.inventory.stockmgmt.chatbot.model;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Who is asking. Used so the bot respects the same role rules as the rest of the app:
 * cashiers only see stock/sales info, managers add purchases & money figures, admins see everything.
 */
public record ChatUser(String username, boolean admin, boolean manager) {

    public static ChatUser from(Authentication auth) {
        if (auth == null) {
            return new ChatUser("guest", false, false);
        }
        boolean admin = false;
        boolean manager = false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) admin = true;
            if ("ROLE_MANAGER".equals(a.getAuthority())) manager = true;
        }
        return new ChatUser(auth.getName(), admin, manager);
    }

    /** Purchases, cost prices, inventory value and profit: Admin or Manager only. */
    public boolean canViewFinancials() {
        return admin || manager;
    }
}
