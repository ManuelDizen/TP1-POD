package ar.edu.itba.pod.grpc.models;

import ar.edu.itba.pod.grpc.requests.PassType;

import java.util.UUID;

public class AttractionPass {
    private final UUID visitor;
    private final PassType type;
    private final int day;

    private int remaining;

    public AttractionPass(UUID visitor, PassType type, int day) {
        this.visitor = visitor;
        this.type = type;
        this.day = day;
        this.setRemaining();
    }

    public void setRemaining() {
        switch (type) {
            case UNLIMITED, HALF_DAY:
                remaining = -1;
                break;
            case THREE:
                remaining = 3;
                break;
        }
    }

    public void rideConsumption() {
        this.remaining--;
    }

    public int getRemaining() {
        return remaining;
    }

    public UUID getVisitor() {
        return visitor;
    }

    public PassType getType() {
        return type;
    }

    public int getDay() {
        return day;
    }

}
