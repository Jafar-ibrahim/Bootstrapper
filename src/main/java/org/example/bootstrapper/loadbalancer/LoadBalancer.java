package org.example.bootstrapper.loadbalancer;

import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.File.FileServices;
import org.example.bootstrapper.model.Node;
import org.example.bootstrapper.services.network.NodesCluster;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Log4j2
@Service
public class LoadBalancer {

    private final ConcurrentHashMap<Node, CopyOnWriteArrayList<String>> nodeUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Node> userNodeMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Node> nodesQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    public LoadBalancer(NodesCluster nodesCluster){
        nodesQueue.addAll(nodesCluster.getNodes());
        log.info("LoadBalancer initialized with {} nodes", nodesCluster.getNodes().size());
    }

    public void balanceExistingUsers() {
        log.info("Balancing existing users between the nodes...");
        JSONArray usersArray;
        try {
            usersArray = FileServices.readJsonArrayFile(FileServices.getUsersFile());
        } catch (Exception e) {
            log.error("Error reading user data from file", e);
            return;
        }

        if (usersArray == null) {
            log.error("User data is null. Skipping user balancing.");
            return;
        }

        for (Object obj : usersArray) {
            JSONObject userJson = (JSONObject) obj;
            String username = (String) userJson.get("username");
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
            log.info("Node {} found for user {}", node.getNodeId(), username);
        }
        return node;
    }
}