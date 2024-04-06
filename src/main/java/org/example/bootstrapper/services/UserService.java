package org.example.bootstrapper.services;

import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.File.FileServices;
import org.example.bootstrapper.loadbalancer.LoadBalancer;
import org.example.bootstrapper.model.Admin;
import org.example.bootstrapper.model.User;
import org.example.bootstrapper.model.Node;
import org.example.bootstrapper.services.network.NodesCluster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
@Log4j2
@Service
public class UserService {

    private final LoadBalancer loadBalancer;

    @Autowired
    public UserService(LoadBalancer loadBalancer){
        this.loadBalancer = loadBalancer;
    }

    public void addUser(User user) {
        Node node = loadBalancer.assignUserToNextNode(user.getUsername());

        FileServices.saveUser(user.toJson());
        String url = "http://" + node.getNodeIP() + ":9000/api/add/customer";

        Map<String, String> params = new HashMap<>();
        params.put("username", user.getUsername());
        params.put("password", user.getPassword());
        params.put("adminUsername", "admin");
        params.put("adminPassword", "admin@12345");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();
        log.info("Adding user with username " + user.getUsername());
        //adding the users into the database worker nodes
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        log.info("Response from database: " + response.getBody());
    }

    public void deleteUser(String username,String adminUsername,String adminPassword) {
        // delete from bootstrapper
        FileServices.deleteUser(username);
        loadBalancer.balanceExistingUsers();
        String url = "http://" + loadBalancer.getUserNode(username).getNodeIP() + ":9000/api/delete/customer";

        // Create a map for the parameters
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("adminUsername",adminUsername );
        params.put("adminPassword", adminPassword);

        // Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();
        //delete from nodes
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        log.info("Response from Database: " + response.getBody());
    }

    public void addAdmin(User admin) {
        FileServices.saveUser(admin.toJson());
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> params = new HashMap<>();
        params.put("username", admin.getUsername());
        params.put("password", admin.getPassword());

        for(int i = 1; i <= NodesCluster.CLUSTER_SIZE; i++) {
            Node node = loadBalancer.assignUserToNextNode(admin.getUsername());
            String url = "http://" + node.getNodeIP() + ":9000/api/add/admin";

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(params, headers);

            try {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info("Admin added successfully to node " + node.getNodeId());
                } else {
                    log.error("Failed to add admin to node " + node.getNodeId() + ". Response: " + response.getBody());
                }
            } catch (Exception e) {
                log.error("Exception occurred while adding admin to node " + node.getNodeId(), e);
            }
        }
    }
}