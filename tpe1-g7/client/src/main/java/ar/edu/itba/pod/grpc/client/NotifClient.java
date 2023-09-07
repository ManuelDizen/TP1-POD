package ar.edu.itba.pod.grpc.client;

import ar.edu.itba.pod.grpc.requests.NotifAttrReplyModel;
import ar.edu.itba.pod.grpc.requests.NotifAttrRequestModel;
import ar.edu.itba.pod.grpc.requests.NotifRequestsServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConnectionUtils;
import utils.ParsingUtils;
import utils.PropertyNames;

import java.util.concurrent.CountDownLatch;

public class NotifClient {
    private static Logger logger = LoggerFactory.getLogger(NotifClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("AdminClient starting...");

        ManagedChannel channel = ConnectionUtils.createChannel();

        String action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow();
        NotifRequestsServiceGrpc.NotifRequestsServiceStub stub =
                NotifRequestsServiceGrpc.newStub(channel);
        String attraction = ParsingUtils.getSystemProperty(PropertyNames.RIDE).orElseThrow();
        int day = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow());
        String visitorId = ParsingUtils.getSystemProperty(PropertyNames.VISITOR).orElseThrow();

        switch(action){
            case "follow":
                NotifAttrRequestModel model = NotifAttrRequestModel.newBuilder()
                        .setDay(day)
                        .setName(attraction)
                        .setVisitorId(visitorId)
                        .build();
                CountDownLatch latch = new CountDownLatch(1); //On/Off Latch (doc)
                StreamObserver<NotifAttrReplyModel> obs = getNotifObserver(latch);
                stub.followAttrRequest(model, obs);
                latch.await();
                break;
            case "unfollow":
                System.out.println("4 de septiembre, la llamada que llegaría");
                break;
            default:
                System.out.println("Action not recognized. Please try again.");
                break;
        }

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
