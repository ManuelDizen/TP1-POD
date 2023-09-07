package ar.edu.itba.pod.grpc.models;

import ar.edu.itba.pod.grpc.requests.NotifAttrReplyModel;
import io.grpc.stub.StreamObserver;

import java.util.Objects;
import java.util.UUID;

public class Follower {
    private final UUID visitorId;
    private final int day;
    private final StreamObserver<NotifAttrReplyModel> obs;

    public Follower(UUID visitorId, int day, StreamObserver<NotifAttrReplyModel> obs) {
        this.visitorId = visitorId;
        this.day = day;
        this.obs = obs;
    }

    public UUID getVisitorId() {
        return visitorId;
    }

    public StreamObserver<NotifAttrReplyModel> getObs() {
        return obs;
    }

    public int getDay() {
        return day;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Follower follower = (Follower) o;
        return day == follower.day && Objects.equals(visitorId, follower.visitorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visitorId, day);
    }

    public void sendMessage(String msg){
        obs.onNext(NotifAttrReplyModel.newBuilder().setMessage(msg).build());
    }
}
