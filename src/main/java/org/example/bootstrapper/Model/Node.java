package org.example.bootstrapper.Model;


import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class Node {
    private int nodeId;
    private String nodeIP;
    private boolean isActive;

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}