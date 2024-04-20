package org.example.bootstrapper.Service.Network;

import lombok.Getter;
import org.example.bootstrapper.Model.Node;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Getter
@Service
public class NodesClusterService {
    private final List<Node> nodes;
    public static final int CLUSTER_SIZE = 4;

    public NodesClusterService() {
        nodes = new ArrayList<>();
    }

    public void addNode(Node node) {
        nodes.add(node);
    }
}