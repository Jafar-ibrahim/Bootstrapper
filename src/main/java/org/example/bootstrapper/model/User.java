package org.example.bootstrapper.model;

import org.example.bootstrapper.Enum.Role;
import org.example.bootstrapper.services.PasswordHashing;
import org.json.simple.JSONObject;



public class User {
    private final String username;
    private String password;
    private final Role role;

    public User(String username, String password, Role role) {
        this.username = username;
        setPassword(password);
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = PasswordHashing.hashPassword(password);
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJson() {
        JSONObject userJson = new JSONObject();
        userJson.put("username", username);
        userJson.put("password", password);
        userJson.put("role", role.toString());
        return userJson;
    }
}