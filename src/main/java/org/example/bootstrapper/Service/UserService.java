package org.example.bootstrapper.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.Enum.Role;
import org.example.bootstrapper.File.FileService;
import org.example.bootstrapper.Loadbalancer.LoadBalancer;
import org.example.bootstrapper.Model.User;
import org.example.bootstrapper.Model.Node;
import org.example.bootstrapper.Model.UserDTO;
import org.example.bootstrapper.Service.Network.NodesClusterService;
import org.example.bootstrapper.Util.PasswordHashing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Log4j2
@Service
public class UserService {

    private final LoadBalancer loadBalancer;
    private final FileService fileService;

    @Autowired
    public UserService(LoadBalancer loadBalancer, FileService fileService) {
        this.loadBalancer = loadBalancer;
        this.fileService = fileService;
    }

    public void addUser(User user,String adminUsername,String adminPassword) {
        Node node = loadBalancer.assignUserToNextNode(user.getUsername());

        user.setPassword(PasswordHashing.hashPassword(user.getPassword()));
        fileService.saveUser(user.toJson());

        log.info("User added to bootstrapper successfully.");
        String url = "http://" + node.getNodeIP() + ":9000/api/users/"+user.getUsername();

        HttpHeaders headers = new HttpHeaders();
        headers.add("password", user.getPassword());
        headers.add("role", "USER");
        headers.add("adminUsername", adminUsername);
        headers.add("adminPassword", adminPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        log.info("Adding user with username " + user.getUsername());
        //adding the users into the database worker nodes
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
                log.info("User added successfully to node " + node.getNodeId());
            } else {
                log.error("Failed to add user to node " + node.getNodeId() + ". Response: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Exception occurred while adding user to node " + node.getNodeId(), e);
        }
    }

    public void deleteUser(String username,String adminUsername,String adminPassword) {

        String url = "http://" + loadBalancer.getUserNode(username).getNodeIP() + ":9000/api/users/"+username;

        // delete from bootstrapper and load balancer
        fileService.deleteUser(username);
        loadBalancer.balanceExistingUsers();
        loadBalancer.deleteUserFromNode(username);


        HttpHeaders headers = new HttpHeaders();
        headers.add("adminUsername", adminUsername);
        headers.add("adminPassword", adminPassword);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        //delete from nodes
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        log.info("Response from Database: " + response.getBody());
    }

    public void addAdmin(User admin , String authUsername, String authPassword) {
        String username = admin.getUsername();
        admin.setPassword(PasswordHashing.hashPassword(admin.getPassword()));
        fileService.saveUser(admin.toJson());
        log.info("Admin with username ("+username+") added to bootstrapper successfully.");


        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("password", admin.getPassword());
        headers.add("role", "ADMIN");
        headers.add("adminUsername", authUsername);
        headers.add("adminPassword", authPassword);

        log.info("Adding admin to all nodes");
        for(int i = 1; i <= NodesClusterService.CLUSTER_SIZE; i++) {
            Node node = loadBalancer.assignUserToNextNode(username);
            String url = "http://" + node.getNodeIP() + ":9000/api/users/"+username;

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
                if (response.getStatusCode() == HttpStatus.CREATED) {
                    log.info("Admin added successfully to node " + node.getNodeId());
                } else {
                    log.error("Failed to add admin to node " + node.getNodeId() + ". Response: " + response.getBody());
                }
            } catch (Exception e) {
                log.error("Exception occurred while adding admin to node " + node.getNodeId(), e);
            }
        }
    }

    public List<UserDTO> getAllUsers() {
        ArrayNode usersArray = fileService.getAllNormalUsers();

        List<UserDTO> users = new ArrayList<>();
        for (JsonNode user : usersArray) {
            UserDTO userDTO = new UserDTO();
            userDTO.setUsername(user.get("username").asText());
            userDTO.setRole(Role.valueOf(user.get("role").asText()));
            users.add(userDTO);
        }
        return users;
    }

    public void createInitialAdminIfNotExists() {
        User admin = new User("admin", "admin", Role.ADMIN);
        if (fileService.getAdminByUsername(admin.getUsername()).isEmpty()){
            addAdmin(admin, "admin", "admin");
            log.info("Initial admin created successfully.");
        }else {
            log.info("Initial admin already exists.");
        }
    }
}