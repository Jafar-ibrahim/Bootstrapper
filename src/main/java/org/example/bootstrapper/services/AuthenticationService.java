package org.example.bootstrapper.services;

import org.example.bootstrapper.File.FileServices;
import org.example.bootstrapper.model.Admin;
import org.example.bootstrapper.model.User;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;

@Service
public class AuthenticationService {

    public boolean isAdmin(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("username or password is null");
        }
        Optional<Admin> adminCredentialsOpt = FileServices.getAdminByUsername(username);
        if (adminCredentialsOpt.isEmpty()) {
            return false;
        }
        Admin adminCredentials = adminCredentialsOpt.get();
        String fileUsername = adminCredentials.getUsername();
        String hashedPassword = PasswordHashing.hashPassword(password);  //to compare hashed passwords
        String storedPassword = adminCredentials.getPassword();
        return fileUsername.equals(username) && storedPassword.equals(hashedPassword);
    }

    public boolean adminExists(String username) {
        return FileServices.getAdminByUsername(username).isPresent();
    }

    public boolean isCustomerExists(User user) {
        if (user == null || user.getUsername() == null) {
            return false;
        }
        File jsonFile = FileServices.getUsersFile();
        if (jsonFile.exists()) {
            JSONArray jsonArray = FileServices.readJsonArrayFile(jsonFile);
            if (jsonArray != null) {
                for (Object obj : jsonArray) {
                    JSONObject userObject = (JSONObject) obj;
                    String username = (String) userObject.get("username");
                    if (username != null && username.equals(user.getUsername())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
