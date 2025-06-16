package com.example.ismoney.model.user;

import com.example.ismoney.dao.UserDAO;

public class GoogleUser extends User {

    private String googleId;

    public GoogleUser(String email, String googleId) {
        super(email, null);
        this.googleId = googleId;
    }

    @Override
    public boolean signUp() {
        UserDAO dao = new UserDAO();
        return dao.insertGoogleUser(this.getEmail(), this.googleId);
    }

    @Override
    public boolean login() {
        UserDAO dao = new UserDAO();
        return dao.checkGoogleCredentials(this.getEmail(), this.googleId);
    }
}
