package org.example.bootstrapper.services.network;

import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.loadbalancer.LoadBalancer;
import org.example.bootstrapper.model.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
public class NetworkService {

    private final NodesCluster nodesCluster;
    private final LoadBalancer loadBalancer;

    @Autowired
    public NetworkService(NodesCluster nodesCluster, LoadBalancer loadBalancer){
        this.nodesCluster = nodesCluster;
        this.loadBalancer = loadBalancer;
    }


    public void run() {
        createNetwork();
        checkClusterStatus();
        loadBalancer.balanceExistingUsers();
    }

    private void createNetwork() {
        for (int i = 1; i <= NodesCluster.CLUSTER_SIZE; i++) {
            Node node = new Node();
            node.setNodeId(i);
            node.setNodeIP("192.168.1.10" + i);
            nodesCluster.addNode(node);
        }
        setNodesInfo();
    }

    private void setNodesInfo() {
        for (Node node : nodesCluster.getNodes()) {
            try {
                String url = "http://" + node.getNodeIP() + ":9000/api/node/info";
                HttpEntity<Map<String, String>> entity = getMapHttpEntity(node);

                RestTemplate restTemplate = new RestTemplate();
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info("Successfully set worker name for node: " + node.getNodeIP());
                    node.setActive(true);
                } else {
                    log.info("Failed to set worker name for node: " + node.getNodeIP() + ". HTTP Status: " + response.getStatusCode());
                }
            } catch (Exception e) {
                log.info("Error setting up worker name for node: " + node.getNodeIP());
                e.printStackTrace();
            }
        }
    }

    private static HttpEntity<Map<String, String>> getMapHttpEntity(Node node) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("nodeId", String.valueOf(node.getNodeId()));
        parameters.put("nodeIP", node.getNodeIP());
        parameters.put("isActive", "true");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        return new HttpEntity<>(parameters, headers);
    }

    private void checkClusterStatus() {
        nodesCluster.getNodes().forEach(node -> log.info("Node " + node.getNodeId() + " is " + (node.isActive() ? "active." : "not active.")));
    }
}