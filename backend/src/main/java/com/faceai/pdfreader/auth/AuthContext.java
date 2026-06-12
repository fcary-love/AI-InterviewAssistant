package com.faceai.pdfreader.auth;

public final class AuthContext {

    private static final ThreadLocal<AuthUser> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthUser user) {
        CURRENT_USER.set(user);
    }

    public static AuthUser currentUser() {
        AuthUser user = CURRENT_USER.get();
        if (user == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return user;
    }

    public static Long currentUserId() {
        return currentUser().id();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
