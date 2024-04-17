package org.example.bootstrapper.services;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.example.bootstrapper.File.FileService;
import org.example.bootstrapper.model.User;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;

@Service
public class AuthenticationService {

    public boolean authenticateAdmin(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("username or password is null");
        }
        Optional<User> adminOptional = FileService.getAdminByUsername(username);
        if (adminOptional.isEmpty()) {
            return false;
        }
        User admin = adminOptional.get();
        String fileUsername = admin.getUsername();
        String hashedPassword = PasswordHashing.hashPassword(password);
        String storedPassword = admin.getPassword();
        return fileUsername.equals(username) && storedPassword.equals(hashedPassword);
    }

    public boolean adminExists(String username) {
        return FileService.getAdminByUsername(username).isPresent();
    }

    public boolean userExists(User user) {
        if (user == null || user.getUsername() == null) {
            return false;
        }
        File jsonFile = FileService.getUsersFile();
        if (jsonFile.exists()) {
            ArrayNode jsonArray = FileService.readJsonArrayFile(jsonFile);
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
