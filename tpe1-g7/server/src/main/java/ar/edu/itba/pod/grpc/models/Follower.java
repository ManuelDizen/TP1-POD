package ar.edu.itba.pod.grpc.models;

import ar.edu.itba.pod.grpc.requests.NotifAttrReplyModel;
import io.grpc.stub.StreamObserver;

public class Follower {
    private final String visitorId;
    private final StreamObserver<NotifAttrReplyModel> obs;

    public Follower(String visitorId, StreamObserver<NotifAttrReplyModel> obs) {
        this.visitorId = visitorId;
        this.obs = obs;
    }

    public String getVisitorId() {
        return visitorId;
    }

    public StreamObserver<NotifAttrReplyModel> getObs() {
        return obs;
    }
}
