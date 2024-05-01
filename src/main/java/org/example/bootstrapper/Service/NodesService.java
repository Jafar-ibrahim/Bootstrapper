package org.example.bootstrapper.Service;

import org.example.bootstrapper.LoadBalancer.LoadBalancer;
import org.example.bootstrapper.Model.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NodesService {

    private final LoadBalancer loadBalancer;

    @Autowired
    public NodesService(LoadBalancer loadBalancer){
        this.loadBalancer = loadBalancer;
    }

    public String getWorkerIdForUser(String username){
        Node node = loadBalancer.getUserNode(username);
        if (node != null) {
            return String.valueOf(node.getNodeId());
        } else {
            return "User not found on any worker";
        }
    }
}
