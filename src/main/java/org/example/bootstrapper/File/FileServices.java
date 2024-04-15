package org.example.bootstrapper.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.Enum.Role;
import org.example.bootstrapper.model.Admin;
import org.example.bootstrapper.model.User;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Log4j2
@Service
public final class FileServices {
    private static final String USERS_FILE_PATH ="src/main/resources/dbData/users.json";
    private static final String ADMINS_FILE_PATH ="src/main/resources/dbData/admins.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    private FileServices(){
    }

    public static void saveUser(ObjectNode userJson) {
        Role role = Role.valueOf(userJson.get("role").asText());
        String path = (role == Role.ADMIN) ? ADMINS_FILE_PATH : USERS_FILE_PATH;
        ArrayNode jsonArray = getExistingUsers(path);
        jsonArray.add(userJson);
        createDirectoriesIfNotExist();
        writeJsonArrayFile(new File(path), jsonArray);
    }

    private static ArrayNode getExistingUsers(String path) {
        if (isFileExists(path)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                return (ArrayNode) mapper.readTree(reader);
            } catch (IOException e) {
                log.error("Error reading the file: " + e.getMessage());
            }
        }
        return mapper.createArrayNode();
    }

    private static void createDirectoriesIfNotExist() {
        try {
            Files.createDirectories(Path.of(USERS_FILE_PATH).getParent());
        } catch (IOException e) {
            log.error("Error creating directories: " + e.getMessage());
        }
    }

    public static void deleteUser(String username) {
        ArrayNode jsonArray = readJsonArrayFile(getUsersFile());
        if (jsonArray == null) return;

        jsonArray.remove(indexOf(jsonArray, username));
        writeJsonArrayFile(getUsersFile(), jsonArray);
    }

    private static int indexOf(ArrayNode arrayNode, String username) {
        for (int i = 0; i < arrayNode.size(); i++) {
            if (arrayNode.get(i).get("username").asText().equals(username)) {
                return i;
            }
        }
        return -1;
    }

    public static Optional<User> getAdminByUsername(String username) {
        ArrayNode jsonArray = getExistingUsers(ADMINS_FILE_PATH);
        for (int i = 0; i < jsonArray.size(); i++) {
            ObjectNode userObject = (ObjectNode) jsonArray.get(i);
            String adminUsername = userObject.get("username").asText();
            if (adminUsername != null && adminUsername.equals(username)) {
                User admin = new User(adminUsername,userObject.get("password").asText(),Role.ADMIN);
                return Optional.of(admin);
            }
        }
        return Optional.empty();
    }

    public static boolean isFileExists(String filePath){
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }

    public static ArrayNode readJsonArrayFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            if (file.length() == 0) {
                return mapper.createArrayNode();
            }
            return (ArrayNode) mapper.readTree(reader);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error while reading JSON file in bootstrapping: " + e.getMessage());
            return null;
        }
    }

    public static void writeJsonArrayFile(File file, ArrayNode jsonArray) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(mapper.writeValueAsString(jsonArray));
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error while writing JSON file: " + e.getMessage());
        }
    }

    public static File getUsersFile(){
        return new File(USERS_FILE_PATH);
    }

    public static String adminJsonFilePath(){
        return "src/main/resources/static/admin.json";
    }
}