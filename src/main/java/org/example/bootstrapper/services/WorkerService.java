package org.example.bootstrapper.services;

import org.example.bootstrapper.loadbalancer.LoadBalancer;
import org.example.bootstrapper.model.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkerService {

    private final LoadBalancer loadBalancer;

    @Autowired
    public WorkerService(LoadBalancer loadBalancer){
        this.loadBalancer = loadBalancer;
    }

    public String getWorkerForUser(String username){
        Node node = loadBalancer.getUserNode(username);
        if (node != null) {
            return String.valueOf(node.getNodeId());
        } else {
            return "Customer not found on any worker";
        }
    }
}
