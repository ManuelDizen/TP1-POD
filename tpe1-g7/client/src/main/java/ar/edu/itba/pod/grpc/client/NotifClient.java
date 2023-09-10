package ar.edu.itba.pod.grpc.client;

import ar.edu.itba.pod.grpc.requests.NotifAttrReplyModel;
import ar.edu.itba.pod.grpc.requests.NotifAttrRequestModel;
import ar.edu.itba.pod.grpc.requests.NotifRequestsServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConnectionUtils;
import utils.ParsingUtils;
import utils.PropertyNames;

import java.util.concurrent.CountDownLatch;

public class NotifClient {
    private static final Logger logger = LoggerFactory.getLogger(NotifClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("NotifyClient starting...");

        ManagedChannel channel = ConnectionUtils.createChannel();

        String action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow();
        String attraction = ParsingUtils.getSystemProperty(PropertyNames.RIDE).orElseThrow();
        int day = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow());
        String visitorId = ParsingUtils.getSystemProperty(PropertyNames.VISITOR).orElseThrow();

        NotifAttrRequestModel model;

        switch(action){
            case "follow":
                NotifRequestsServiceGrpc.NotifRequestsServiceStub stub =
                        NotifRequestsServiceGrpc.newStub(channel);
                model = buildModel(visitorId, attraction, day);
                CountDownLatch latch = new CountDownLatch(1); //On/Off Latch (doc)
                StreamObserver<NotifAttrReplyModel> obs = getNotifObserver(latch);
                stub.followAttrRequest(model, obs);
                latch.await();
                break;
            case "unfollow":
                model = buildModel(visitorId, attraction, day);
                NotifRequestsServiceGrpc.NotifRequestsServiceBlockingStub
                        blockingStub = NotifRequestsServiceGrpc.newBlockingStub(channel);
                NotifAttrReplyModel reply = blockingStub.unfollowAttrRequest(model);
                System.out.println(reply.getMessage());
                break;
            default:
                System.out.println("Action not recognized. Please try again.");
                break;
        }

    }

    private static NotifAttrRequestModel buildModel(String visitorId, String name, int day){
        return NotifAttrRequestModel.newBuilder()
                .setVisitorId(visitorId)
                .setName(name)
                .setDay(day)
                .build();
    }

    public static StreamObserver<NotifAttrReplyModel> getNotifObserver(CountDownLatch latch){
        return new StreamObserver<>() {
            @Override
            public void onNext(NotifAttrReplyModel notifAttrReplyModel) {
                System.out.println(notifAttrReplyModel.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println();
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };
    }
}
