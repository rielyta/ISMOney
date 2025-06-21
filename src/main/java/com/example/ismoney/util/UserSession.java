package com.example.ismoney.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class UserSession {
    private static UserSession instance;
    private Integer currentUserId;
    private String currentUsername;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    private static final int SESSION_TIMEOUT_HOURS = 24; // Session timeout 24 jam

    private UserSession() {
        // Private constructor untuk singleton
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public static void setUserSession(Integer userId, String username) {
        UserSession session = getInstance();
        session.currentUserId = userId;
        session.currentUsername = username;
        session.loginTime = LocalDateTime.now();
        session.lastActivity = LocalDateTime.now();

        System.out.println("User session created for: " + username + " (ID: " + userId + ")");
    }

    public static Integer getCurrentUserId() {
        UserSession session = getInstance();
        if (session.isSessionValid()) {
            session.updateLastActivity();
            return session.currentUserId;
        }
        return null;
    }

    public static String getCurrentUsername() {
        UserSession session = getInstance();
        if (session.isSessionValid()) {
            session.updateLastActivity();
            return session.currentUsername;
        }
        return null;
    }

    public static boolean isSessionValid() {
        UserSession session = getInstance();
        return session.isValid();
    }

    public static void clearSession() {
        UserSession session = getInstance();
        String username = session.currentUsername;
        Integer userId = session.currentUserId;

        session.currentUserId = null;
        session.currentUsername = null;
        session.loginTime = null;
        session.lastActivity = null;

        System.out.println("Session cleared for user: " + username + " (ID: " + userId + ")");
    }

    public static boolean isLoggedIn() {
        return getCurrentUserId() != null;
    }

    public static LocalDateTime getLoginTime() {
        UserSession session = getInstance();
        if (session.isSessionValid()) {
            return session.loginTime;
        }
        return null;
    }

    public static LocalDateTime getLastActivity() {
        UserSession session = getInstance();
        if (session.isSessionValid()) {
            return session.lastActivity;
        }
        return null;
    }

    public static long getSessionDurationMinutes() {
        UserSession session = getInstance();
        if (session.isSessionValid() && session.loginTime != null) {
            return ChronoUnit.MINUTES.between(session.loginTime, LocalDateTime.now());
        }
        return 0;
    }

    public static void extendSession() {
        UserSession session = getInstance();
        if (session.currentUserId != null) {
            session.updateLastActivity();
        }
    }

    private boolean isValid() {
        if (currentUserId == null || lastActivity == null) {
            return false;
        }

        long hoursSinceLastActivity = ChronoUnit.HOURS.between(lastActivity, LocalDateTime.now());
        if (hoursSinceLastActivity > SESSION_TIMEOUT_HOURS) {
            System.out.println("Session expired for user ID: " + currentUserId +
                    " (inactive for " + hoursSinceLastActivity + " hours)");
            return false;
        }

        return true;
    }

    private void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public static void printSessionInfo() {
        UserSession session = getInstance();
        if (session.isSessionValid()) {
            System.out.println("=== Session Info ===");
            System.out.println("User ID: " + session.currentUserId);
            System.out.println("Username: " + session.currentUsername);
            System.out.println("Login Time: " + session.loginTime);
            System.out.println("Last Activity: " + session.lastActivity);
            System.out.println("Session Duration: " + getSessionDurationMinutes() + " minutes");
            System.out.println("==================");
        } else {
            System.out.println("No active session");
        }
    }

    public static void forceTimeout() {
        UserSession session = getInstance();
        if (session.currentUserId != null) {
            session.lastActivity = LocalDateTime.now().minusHours(SESSION_TIMEOUT_HOURS + 1);
            System.out.println("Session forced to timeout for user ID: " + session.currentUserId);
        }
    }
}