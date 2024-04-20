package org.example.bootstrapper.Loadbalancer;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.File.FileService;
import org.example.bootstrapper.Model.Node;
import org.example.bootstrapper.Service.Network.NodesClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Log4j2
@Service
public class LoadBalancer {

    private final ConcurrentHashMap<Node, CopyOnWriteArrayList<String>> nodeUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Node> userNodeMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Node> nodesQueue = new ConcurrentLinkedQueue<>();
    private final FileService fileService;
    private final NodesClusterService nodesClusterService;

    @Autowired
    public LoadBalancer(FileService fileService, NodesClusterService nodesClusterService) {
        this.fileService = fileService;
        this.nodesClusterService = nodesClusterService;
    }
    public void init(){
        nodesQueue.addAll(nodesClusterService.getNodes());
        log.info("LoadBalancer initialized with {} nodes", nodesQueue.size());
    }

    public void balanceExistingUsers() {
        log.info("Balancing existing users between the nodes...");
        ArrayNode usersArray;
        File usersFile = fileService.getUsersFile();
        if (!usersFile.exists()) {
            log.warn("Users file not found. Skipping user balancing.");
            return;
        }
        try {
            usersArray = fileService.readJsonArrayFile(fileService.getUsersFile());
        } catch (Exception e) {
            log.error("Error reading user data from file", e);
            return;
        }

        if (usersArray == null) {
            log.error("User data is null. Skipping user balancing.");
            return;
        }

        for (Object obj : usersArray) {
            ObjectNode userJson = (ObjectNode) obj;
            String username = String.valueOf(userJson.get("username"));
            this.assignUserToNextNode(username);
        }
        log.info("Finished balancing {} users", usersArray.size());
    }

    public Node assignUserToNextNode(String username) {
        Node node = getNextNode();
        nodeUsers.computeIfAbsent(node, k -> new CopyOnWriteArrayList<>()).add(username);
        userNodeMap.put(username, node);
        log.info("Assigned user {} to node {}", username, node.getNodeId());
        return node;
    }


    public Node getNextNode() {
        if (nodesQueue.isEmpty()) {
            log.error("No nodes available for balancing");
            throw new IllegalStateException("No nodes available for balancing");
        }
        Node nextNode = nodesQueue.poll();
        nodesQueue.add(nextNode);
        log.info("Next node for balancing is {}", nextNode.getNodeId());
        return nextNode;
    }

    public Node getUserNode(String username) {
        Node node = userNodeMap.get(username);
        if (node == null) {
            log.warn("No node found for user {}", username);
        } else {
            log.info("Node {} is user {}'s node", node.getNodeId(), username);
        }
        return node;
    }

    public void deleteUserFromNode(String username) {
        Node node = userNodeMap.get(username);
        if (node == null) {
            log.error("No node found for user {}", username);
            return;
        }
        CopyOnWriteArrayList<String> users = nodeUsers.get(node);
        if (users == null) {
            log.error("No users found for node {}", node.getNodeId());
            return;
        }
        users.remove(username);
        userNodeMap.remove(username);
        log.info("User {} deleted from node {}", username, node.getNodeId());
    }
}