package ar.edu.itba.pod.grpc.client;

import ar.edu.itba.pod.grpc.client.queryModels.NotifQueryParamsModel;
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

import java.security.InvalidParameterException;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;

import static utils.ConnectionUtils.shutdownChannel;

public class NotifClient {
    private static final Logger logger = LoggerFactory.getLogger(NotifClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("NotifyClient starting...");

        ManagedChannel channel = ConnectionUtils.createChannel();

        String action = null;
        try {
            action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow();
        }
        catch(NoSuchElementException e){
            System.out.println("Action requested is invalid. Please check action is one of the following options:\n[follow|unfollow]");
            shutdownChannel(channel);
        }

        NotifQueryParamsModel params;
        try{
            params = new NotifQueryParamsModel();
        }
        catch(InvalidParameterException e){
            System.out.println("Invalid parameters. Please try again.");
            shutdownChannel(channel);
            return;
        }

        NotifAttrRequestModel model = buildModel(params.getVisitorId(), params.getAttraction(), params.getDay());

        switch (action) {
            case "follow" -> {
                NotifRequestsServiceGrpc.NotifRequestsServiceStub stub =
                        NotifRequestsServiceGrpc.newStub(channel);
                CountDownLatch latch = new CountDownLatch(1); //On/Off Latch (doc)
                StreamObserver<NotifAttrReplyModel> obs = getNotifObserver(latch);
                stub.followAttrRequest(model, obs);
                latch.await();
            }
            case "unfollow" -> {
                NotifRequestsServiceGrpc.NotifRequestsServiceBlockingStub
                        blockingStub = NotifRequestsServiceGrpc.newBlockingStub(channel);
                try{
                    NotifAttrReplyModel reply = blockingStub.unfollowAttrRequest(model);
                    System.out.println(reply.getMessage());
                }
                catch(RuntimeException e){
                    System.out.println(e.getMessage());
                }
            }
            default -> System.out.println("Action not recognized. Please try again.");
        }

        shutdownChannel(channel);
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
                System.out.println(throwable.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };
    }
}
