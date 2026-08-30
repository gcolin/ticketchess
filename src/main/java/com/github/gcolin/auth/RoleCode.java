package com.github.gcolin.auth;

import java.util.Set;

public enum RoleCode {
    ADMIN,
    TRESORIER,
    ARBITRE,
    EVENT_ADMIN;

    public static boolean satisfies(Set<RoleCode> granted, RoleCode required) {
        if (required == null || granted == null || granted.isEmpty()) {
            return false;
        }
        if (granted.contains(ADMIN)) {
            return true;
        }
        if (required == ARBITRE && granted.contains(EVENT_ADMIN)) {
            return true;
        }
        return granted.contains(required);
    }
}
