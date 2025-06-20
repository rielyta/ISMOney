package com.example.ismoney.util;

import com.example.ismoney.model.user.User;
import java.time.LocalDateTime;
import java.util.logging.Logger;

public class SessionManager {
    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());
    private static SessionManager instance;
    private User currentUser;
    private LocalDateTime loginTime;
    private boolean rememberMe;
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
        logger.info("User session started for: " + user.getUsername());
    }

    public User getCurrentUser() {
        if (currentUser != null && !isSessionExpired()) {
            return currentUser;
        }
        return null;
    }

    public void logout() {
        if (currentUser != null) {
            logger.info("User session ended for: " + currentUser.getUsername());
        }
        currentUser = null;
        loginTime = null;
        rememberMe = false;
    }

    public boolean isSessionExpired() {
        if (loginTime == null) return true;
        if (rememberMe) return false;

        return loginTime.plusMinutes(SESSION_TIMEOUT_MINUTES).isBefore(LocalDateTime.now());
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}