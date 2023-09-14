package ar.edu.itba.pod.grpc.models;

import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static ar.edu.itba.pod.grpc.utils.LockUtils.*;

public class Reservation {
    private final String attractionName;
    private final int day;
    private final UUID visitorId;
    private LocalTime slot;
    private ReservationStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime confirmedAt = null;

    private boolean reserved;
    private final ReadWriteLock statusLock = new ReentrantReadWriteLock(fairness4locks);
    private final ReadWriteLock slotLock = new ReentrantReadWriteLock(fairness4locks);

    public Reservation(String attractionName, int day, UUID visitorId, LocalTime slot, ReservationStatus status) {
        this.attractionName = attractionName;
        this.day = day;
        this.visitorId = visitorId;
        this.slot = slot;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.reserved = false;
    }

    public String getAttractionName() {
        return attractionName;
    }

    public int getDay() {
        return day;
    }

    public UUID getVisitorId() {
        return visitorId;
    }

    public LocalTime getSlot() {
        lockRead(slotLock);
        LocalTime s =  slot;
        unlockRead(slotLock);
        return s;
    }

    public ReservationStatus getStatus() {
        lockRead(statusLock);
        ReservationStatus s =  status;
        unlockRead(statusLock);
        return s;
    }

    public void setSlot(LocalTime slot) {
        lockWrite(slotLock);
        this.slot = slot;
        unlockWrite(slotLock);
    }

    public void setStatus(ReservationStatus status) {
        lockWrite(statusLock);
        this.status = status;
        unlockWrite(statusLock);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return day == that.day && attractionName.equals(that.attractionName) && visitorId.equals(that.visitorId) && slot.equals(that.slot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attractionName, day, visitorId, slot);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public boolean isReserved() {
        return reserved;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}
