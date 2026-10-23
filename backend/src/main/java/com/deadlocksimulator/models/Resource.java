package com.deadlocksimulator.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-instance resource: at most one holder, plus a FIFO queue of waiting processes.
 */
public class Resource {

    private String id;
    private String holder;
    private List<String> waitQueue = new ArrayList<>();

    public Resource() {
    }

    public Resource(String id) {
        this.id = id;
    }

    @JsonIgnore
    public boolean isFree() {
        return holder == null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHolder() { return holder; }
    public void setHolder(String holder) { this.holder = holder; }
    public List<String> getWaitQueue() { return waitQueue; }
    public void setWaitQueue(List<String> waitQueue) { this.waitQueue = new ArrayList<>(waitQueue); }
}
