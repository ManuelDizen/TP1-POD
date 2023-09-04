package ar.edu.itba.pod.grpc.persistance;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class Attraction {
    private final String name;
    private final LocalTime opening;
    private final LocalTime closing;
    private final int minsPerSlot;
    private final Map<Integer, Integer> capacities = new HashMap<>();

    public Attraction(String name, LocalTime opening, LocalTime closing, int minsPerSlot) {
        this.name = name;
        this.opening = opening;
        this.closing = closing;
        this.minsPerSlot = minsPerSlot;
    }

    public String getName() {
        return name;
    }

    public LocalTime getOpening() {
        return opening;
    }

    public LocalTime getClosing() {
        return closing;
    }

    public int getMinsPerSlot() {
        return minsPerSlot;
    }

    public Map<Integer, Integer> getCapacities() {
        return capacities;
    }
}
