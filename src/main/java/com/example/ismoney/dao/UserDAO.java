package com.example.ismoney.dao;

import com.example.ismoney.model.User;

public interface UserDAO {

    User findByEmail(String email);
    User findByUsername(String username);
    boolean save(User user);
    boolean isEmailExists(String email);
    boolean isUsernameExists(String username);
    User authenticateUser(String emailOrUsername, String password);
    String hashPassword(String plainPassword);
    boolean updateLastLogin(int userId);
}