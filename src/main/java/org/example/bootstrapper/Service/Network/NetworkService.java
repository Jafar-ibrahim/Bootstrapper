package org.example.bootstrapper.Service.Network;

import lombok.extern.log4j.Log4j2;
import org.example.bootstrapper.Loadbalancer.LoadBalancer;
import org.example.bootstrapper.Model.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public class NetworkService {

    private final NodesClusterService nodesClusterService;
    private final LoadBalancer loadBalancer;

    @Autowired
    public NetworkService(NodesClusterService nodesClusterService, LoadBalancer loadBalancer) {
        this.nodesClusterService = nodesClusterService;
        this.loadBalancer = loadBalancer;
    }


    public void init() {
        createNetwork();
        checkClusterStatus();
        loadBalancer.init();
        loadBalancer.balanceExistingUsers();
    }

    private void createNetwork() {
        for (int i = 1; i <= NodesClusterService.CLUSTER_SIZE; i++) {
            Node node = new Node();
            node.setNodeId(i);
            node.setNodeIP("192.168.1.10" + i);
            nodesClusterService.addNode(node);
        }
        setNodesInfo();
    }

    private void setNodesInfo() {
        for (Node node : nodesClusterService.getNodes()) {
            int maxAttempts = 5;
            int attempt = 0;
            while (attempt < maxAttempts) {
                try {
                    String url = "http://" + node.getNodeIP() + ":9000/api/node/info";
                    HttpEntity<MultiValueMap<String, String>> entity = getMapHttpEntity(node);

                    RestTemplate restTemplate = new RestTemplate();
                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                    while (response.getStatusCode() != HttpStatus.OK) {
                        log.info("Failed to set worker name for node: " + node.getNodeIP() + ". HTTP Status: " + response.getStatusCode());
                        log.info("Retrying...");
                        response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                    }
                    log.info("Successfully set worker name for node: " + node.getNodeIP());
                    node.setActive(true);
                    break; // break the loop if the request is successful
                } catch (ResourceAccessException e) {
                    if (++attempt == maxAttempts) throw e; // if max attempts reached, throw the exception
                    log.info("Connection refused while setting up worker name for node: " + node.getNodeIP() + ". Retrying attempt " + attempt);
                    try {
                        Thread.sleep(2000); // wait for 2 seconds before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    log.info("Error setting up worker name for node: " + node.getNodeIP());
                    e.printStackTrace();
                }
            }
        }
    }

    private static HttpEntity<MultiValueMap<String, String>> getMapHttpEntity(Node node) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("nodeId", String.valueOf(node.getNodeId()));
        parameters.add("nodeIP", node.getNodeIP());
        parameters.add("isActive", "true");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        return new HttpEntity<>(parameters, headers);
    }

    private void checkClusterStatus() {
        nodesClusterService.getNodes().forEach(node -> log.info("Node " + node.getNodeId() + " is " + (node.isActive() ? "active." : "not active.")));
    }
}