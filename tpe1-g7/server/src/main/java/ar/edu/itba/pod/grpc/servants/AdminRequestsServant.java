package ar.edu.itba.pod.grpc.servants;

import ar.edu.itba.pod.grpc.models.Attraction;
import ar.edu.itba.pod.grpc.models.AttractionPass;
import ar.edu.itba.pod.grpc.persistance.ParkRepository;
import ar.edu.itba.pod.grpc.models.ReturnValues;
import ar.edu.itba.pod.grpc.requests.*;
import com.google.protobuf.Int32Value;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;

public class AdminRequestsServant extends AdminRequestsServiceGrpc.AdminRequestsServiceImplBase{

    private final static Logger logger = LoggerFactory.getLogger(AdminRequestsServant.class);
    private final ParkRepository repository = ParkRepository.getRepository();
    @Override
    public void addSlotsRequest(SlotsRequestModel request,
                                StreamObserver<SlotsReplyModel> responseObserver){
        int day = request.getDay();
        int capacity = request.getCapacity();
        String name = request.getRide();

        if(day < 1 || day > 365){
            returnOnError("Invalid day: " + day + ".", responseObserver);
            return;
        }
        if(capacity <= 0){
            returnOnError("Capacity cannot be 0 or negative.", responseObserver);
            return;
        }
        SlotsReplyModel reply = null;
        try {
            reply = repository.addSlots(name, day, capacity);
            if(reply != null){
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
            else{
                returnOnError("Unknown error", responseObserver);

            }
        }
        catch(RuntimeException e){
            returnOnError(e.getMessage(), responseObserver);

        }
    }

    @Override
    public void addTicketsRequest(TicketsRequestModel request,
                                  StreamObserver<Int32Value> responseObserver){
        int day = request.getDay();
        UUID id = UUID.fromString(request.getId());
        PassType type = request.getType();
        if(day < 1 || day > 365){
            returnOnError("Day is invalid.", responseObserver);
            return;
        }
        try{
            repository.addPass(new AttractionPass(id, type, day));
            responseObserver.onNext(Int32Value.of(ReturnValues.SUCCESSFUL_PETITION.ordinal()));
            responseObserver.onCompleted();
        }
        catch(RuntimeException e){
            returnOnError(e.getMessage(), responseObserver);
        }
    }

    @Override
    public void addRidesRequest(RidesRequestModel request,
                                StreamObserver<Int32Value> responseObserver){
        String name = request.getName();
        LocalTime opening = LocalTime.parse(request.getOpening());
        LocalTime closing = LocalTime.parse(request.getClosing());
        int minsPerSlot = request.getMinsPerSlot();

        if(opening.isAfter(closing)){
            returnOnError("Error with times.",responseObserver);
            return;
        }
        if(minsPerSlot <= 0 || Duration.between(opening, closing).toMinutes() < minsPerSlot){
            returnOnError("Invalid minutes per slot.", responseObserver);
            return;
        }

        try{
            Attraction att = repository.addRide(new Attraction(name, opening, closing, minsPerSlot));
            if(att != null){
                responseObserver.onNext(Int32Value.of(ReturnValues.SUCCESSFUL_PETITION.ordinal()));
                responseObserver.onCompleted();
            }
            else{
                returnOnError("Unknown error.", responseObserver);
            }
        }
        catch(RuntimeException e){
            returnOnError(e.getMessage(), responseObserver);
        }
    }

    private <T> void returnOnError(String errMsg, StreamObserver<T> observer){
        logger.error(errMsg);
        observer.onError(Status.INTERNAL.withDescription(errMsg).asRuntimeException());
    }

}
