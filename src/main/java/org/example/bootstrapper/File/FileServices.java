package org.example.bootstrapper.File;

import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.Enum.Role;
import org.example.bootstrapper.model.Admin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
public final class FileServices {
    private static final String USERS_FILE_PATH ="src/main/resources/dbData/users.json";
    private static final String ADMINS_FILE_PATH ="src/main/resources/dbData/admins.json";
    private FileServices(){

    }


    @SuppressWarnings("unchecked")
    public static void saveUser(JSONObject userJson) {
        Role role = Role.valueOf((String) userJson.get("role"));
        String path = (role == Role.ADMIN) ? ADMINS_FILE_PATH : USERS_FILE_PATH;
        JSONArray jsonArray = getExistingUsers(path);
        jsonArray.add(userJson);
        createDirectoriesIfNotExist();
        writeJsonArrayFile(new File(path), jsonArray);
    }

    private static JSONArray getExistingUsers(String path) {
        if (isFileExists(path)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                return (JSONArray) new JSONParser().parse(reader);
            } catch (IOException | ParseException e) {
                log.error("Error reading the file: " + e.getMessage());
            }
        }
        return new JSONArray();
    }

    private static void createDirectoriesIfNotExist() {
        try {
            Files.createDirectories(Path.of(USERS_FILE_PATH).getParent());
        } catch (IOException e) {
            log.error("Error creating directories: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static void deleteUser(String username) {
        JSONArray jsonArray = readJsonArrayFile(getUsersFile());
        if (jsonArray == null) return;

        JSONArray updatedArray = (JSONArray) jsonArray.stream()
                .filter(obj -> !username.equals(((JSONObject) obj).get("username")))
                .collect(Collectors.toCollection(JSONArray::new));

        writeJsonArrayFile(getUsersFile(), updatedArray);
    }

    public static Optional<Admin> getAdminByUsername(String username) {
        JSONArray jsonArray = getExistingUsers(ADMINS_FILE_PATH);
        for (Object obj : jsonArray) {
            JSONObject userObject = (JSONObject) obj;
            String adminUsername = (String) userObject.get("username");
            if (adminUsername != null && adminUsername.equals(username)) {
                Admin admin = new Admin();
                admin.setUsername(adminUsername);
                admin.setPassword((String) userObject.get("password"));
                return Optional.of(admin);
            }
        }
        return Optional.empty();
    }

    public static void writeJsonObjectFile(File file, JSONObject jsonObject) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(jsonObject.toJSONString());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Error writing to the file: " + e.getMessage());
        }
    }


    public static boolean isFileExists(String filePath){
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }

    public static JSONArray readJsonArrayFile(File file) {
        JSONParser parser = new JSONParser();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            if (file.length() == 0) {
                return new JSONArray();
            }
            Object obj = parser.parse(reader);
            return (JSONArray) obj;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            log.error("Error while reading JSON file in bootstrapping: " + e.getMessage());
            return null;
        }
    }

    public static void writeJsonArrayFile(File file, JSONArray jsonArray) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(jsonArray.toJSONString());
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