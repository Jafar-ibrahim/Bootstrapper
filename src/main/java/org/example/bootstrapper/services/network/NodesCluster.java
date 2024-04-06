package org.example.bootstrapper.services.network;

import lombok.Getter;
import org.example.bootstrapper.model.Node;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class NodesCluster {
    private final List<Node> nodes;
    public static final int CLUSTER_SIZE = 4;

    public NodesCluster() {
        nodes = new ArrayList<>();
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

}