package com.example.ismoney.model.user;

import com.example.ismoney.dao.UserDAO;
import com.example.ismoney.database.DatabaseConfig;

public class User {

    private String email;
    private String password;

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Provide getters to allow subclasses to view
    public String getEmail() {
        return email;
    }
    public String getPassword() { return password; }

    public boolean signUp() {
        UserDAO dao = new UserDAO();
        return dao.insertUser(this.email, this.password);
    }

    // Declare login() here, even if it's not implemented in User
    public boolean login() {
        // Default implementation (Username/Pass)
        UserDAO dao = new UserDAO();
        return dao.checkCredentials(this.email, this.password);
    }

    public boolean checkCredentials() {
        DatabaseConfig db = DatabaseConfig.getInstance();
        return db.checkCredentials(this.email, this.password);
    }
}
