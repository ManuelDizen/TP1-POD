package ar.edu.itba.pod.grpc.models;

import ar.edu.itba.pod.grpc.requests.PassType;

import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static ar.edu.itba.pod.grpc.requests.PassType.THREE;
import static ar.edu.itba.pod.grpc.utils.LockUtils.*;

public class AttractionPass {
    private final UUID visitor;
    private final PassType type;
    private final int day;
    private int remaining;

    private final ReadWriteLock remainingLock = new ReentrantReadWriteLock(fairness4locks);

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

    public boolean rideConsumption() {
        if(type == THREE) {
            lockWrite(remainingLock);
            if(remaining > 0) {
                this.remaining--;
                unlockWrite(remainingLock);
                return true;
            }
            unlockWrite(remainingLock);
            return false;
        }
        return true;
    }

    public void cancelConsumption() {
        lockWrite(remainingLock);
        this.remaining++;
        unlockWrite(remainingLock);
    }

    public int getRemaining() {
        int rem;
        lockRead(remainingLock);
        rem = remaining;
        unlockRead(remainingLock);
        return rem;
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
