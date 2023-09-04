package ar.edu.itba.pod.grpc.persistance;

import ar.edu.itba.pod.grpc.requests.PassType;

import java.util.UUID;

public class AttractionPass {
    private final UUID visitor;
    private final PassType type;
    private final int day;

    public AttractionPass(UUID visitor, PassType type, int day) {
        this.visitor = visitor;
        this.type = type;
        this.day = day;
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
