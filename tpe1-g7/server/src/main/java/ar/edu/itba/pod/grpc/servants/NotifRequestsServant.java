package ar.edu.itba.pod.grpc.servants;

import ar.edu.itba.pod.grpc.models.Follower;
import ar.edu.itba.pod.grpc.persistance.ParkRepository;
import ar.edu.itba.pod.grpc.requests.NotifAttrReplyModel;
import ar.edu.itba.pod.grpc.requests.NotifAttrRequestModel;
import ar.edu.itba.pod.grpc.requests.NotifRequestsServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

public class NotifRequestsServant extends NotifRequestsServiceGrpc.NotifRequestsServiceImplBase {
    private final static Logger logger = LoggerFactory.getLogger(NotifRequestsServant.class);

    private final ParkRepository repository = ParkRepository.getRepository();

    @Override
    public void followAttrRequest(NotifAttrRequestModel request,
                                  StreamObserver<NotifAttrReplyModel> responseObserver){
        int day = request.getDay();
        UUID visitorId = UUID.fromString(request.getVisitorId());
        String name = request.getName();

        repository.getAttractions().stream()
                .filter(a -> Objects.equals(a.getName(), name)) // ¿Existe atracción?
                .findFirst().ifPresentOrElse(
                        a -> {
                            if (repository.visitorHasPass(visitorId, day)) {
                                if(
                                        repository.getAttractionByName(name).getFollowers().stream()
                                                .anyMatch(f -> f.getDay() == day && f.getVisitorId() == visitorId)
                                ){
                                    String msg = "User " + visitorId + " is already following attraction " + name + " for day " + day + ".";
                                    logger.error(msg);
                                    responseObserver.onError(Status.ALREADY_EXISTS.withDescription(msg).asRuntimeException());
                                }
                                else{
                                    a.addFollower(new Follower(visitorId, day, responseObserver));
                                }

                            } else {
                                String msg = "User " + visitorId + " does not have a pass for day " + day + ".";
                                logger.error(msg);
                                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(msg).asRuntimeException());
                            }
                        },
                        () -> {
                            String msg = "Attraction " + name + " does not exist.";
                            logger.error(msg);
                            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(msg).asRuntimeException());
                        }
                );


    }

    @Override
    public void unfollowAttrRequest(NotifAttrRequestModel request,
                                    StreamObserver<NotifAttrReplyModel> responseObserver){
        int day = request.getDay();
        UUID visitorId = UUID.fromString(request.getVisitorId());
        String name = request.getName();

        repository.getAttractions().stream()
                .filter(a -> Objects.equals(a.getName(), name)) // ¿Existe atracción?
                .findFirst().ifPresentOrElse(
                        a -> {
                            if(!repository.isValidDay(day)){
                                String msg = "Day " + day + " is invalid.";
                                logger.error(msg);
                                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(msg).asRuntimeException());
                                return;
                            }
                            if(!repository.visitorHasPass(visitorId, day)){
                                String msg = "Day " + day + " is invalid.";
                                logger.error(msg);
                                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(msg).asRuntimeException());
                                return;
                            }
                            if(!a.isVisitorSubscribedForDay(visitorId, day)){
                                String msg = "Visitor " + visitorId + " is not subscribed for day " + day + ".";
                                logger.error(msg);
                                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(msg).asRuntimeException());
                                return;
                            }
                            a.unsubscribeFollower(visitorId, day);
                            NotifAttrReplyModel replyModel = NotifAttrReplyModel.newBuilder().setMessage("Visitor " +
                                            visitorId + " succesfully unfollowed attraction" + name + " for day " + day + ".")
                                    .build();
                            responseObserver.onNext(replyModel);
                            responseObserver.onCompleted();
                        },
                        () -> {
                            String msg = "Attraction " + name + " does not exist.";
                            logger.error(msg);
                            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(msg).asRuntimeException());
                        }
                );
    }

}
