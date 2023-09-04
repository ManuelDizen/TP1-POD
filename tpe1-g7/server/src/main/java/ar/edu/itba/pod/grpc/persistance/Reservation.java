package ar.edu.itba.pod.grpc.persistance;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class Reservation {
    private final String attractionName;
    private final int day;
    private final UUID visitorId;
    private LocalTime slot;
    private ReservationStatus status;
    private final LocalDateTime createdAt;


    public Reservation(String attractionName, int day, UUID visitorId, LocalTime slot, ReservationStatus status) {
        this.attractionName = attractionName;
        this.day = day;
        this.visitorId = visitorId;
        this.slot = slot;
        this.status = status;
        this.createdAt = LocalDateTime.now();
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
        return slot;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setSlot(LocalTime slot) {
        this.slot = slot;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}
