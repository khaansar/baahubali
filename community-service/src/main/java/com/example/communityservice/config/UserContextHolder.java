package com.example.communityservice.config;

public class UserContextHolder {
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE = new ThreadLocal<>();

    public static void setUserId(String userId) { USER_ID.set(userId); }
    public static String getUserId() { return USER_ID.get(); }
    
    public static void setUserRole(String role) { USER_ROLE.set(role); }
    public static String getUserRole() { return USER_ROLE.get(); }

    public static void clear() {
        USER_ID.remove();
        USER_ROLE.remove();
    }
}