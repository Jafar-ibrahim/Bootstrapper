package org.example.bootstrapper.Service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.bootstrapper.File.FileService;
import org.example.bootstrapper.Model.User;
import org.example.bootstrapper.Util.PasswordHashing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;

@Service
public class AuthenticationService {

    private final FileService fileService;

    @Autowired
    public AuthenticationService(FileService fileService) {
        this.fileService = fileService;
    }

    public boolean authenticateAdmin(String username, String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("username or password is null");
        }
        if (username.equals("admin") && password.equals("admin")) {
            return true;
        }
        Optional<User> adminOptional = fileService.getAdminByUsername(username);
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
        return fileService.getAdminByUsername(username).isPresent();
    }

    public boolean userExists(String username) {
        if (username == null) {
            return false;
        }
        File jsonFile = fileService.getUsersFile();
        if (jsonFile.exists()) {
            ArrayNode jsonArray = fileService.readJsonArrayFile(jsonFile);
            if (jsonArray != null) {
                for (Object obj : jsonArray) {
                    ObjectNode userObject = (ObjectNode) obj;
                    String otherUserName = String.valueOf(userObject.get("username"));
                    if (username.equals(otherUserName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
