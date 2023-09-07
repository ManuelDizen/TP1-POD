package ar.edu.itba.pod.grpc.servants;

import ar.edu.itba.pod.grpc.requests.NotifAttrReplyModel;
import ar.edu.itba.pod.grpc.requests.NotifAttrRequestModel;
import ar.edu.itba.pod.grpc.requests.NotifRequestsServiceGrpc;
import io.grpc.stub.StreamObserver;

public class NotifRequestsServant extends NotifRequestsServiceGrpc.NotifRequestsServiceImplBase {
    @Override
    public void followAttrRequest(NotifAttrRequestModel request,
                                  StreamObserver<NotifAttrReplyModel> responseObserver){

    }

    @Override
    public void unfollowAttrRequest(NotifAttrRequestModel request,
                                    StreamObserver<NotifAttrReplyModel> responseObserver){

    }

}
