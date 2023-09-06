package ar.edu.itba.pod.grpc.servants;

import ar.edu.itba.pod.grpc.models.Attraction;
import ar.edu.itba.pod.grpc.persistance.ParkRepository;
import ar.edu.itba.pod.grpc.models.Reservation;
import ar.edu.itba.pod.grpc.models.ReservationStatus;
import ar.edu.itba.pod.grpc.requests.*;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class BookingRequestsServant extends BookingRequestsServiceGrpc.BookingRequestsServiceImplBase {

    private final static Logger logger = LoggerFactory.getLogger(BookingRequestsServant.class);

    private final ParkRepository repository = ParkRepository.getRepository();

    @Override
    public void getAttractionsRequest(Empty request,
                                      StreamObserver<RidesRequestModel> responseObserver){

        //busco las attractions del repository, y las devuelvo

        List<Attraction> attractions = repository.getAttractions();

        for(Attraction att : attractions) {
            RidesRequestModel ride = RidesRequestModel.newBuilder().setName(att.getName())
                    .setOpening(att.getOpening())
                    .setClosing(att.getClosing())
                    .setMinsPerSlot(att.getMinsPerSlot())
                    .build();
            responseObserver.onNext(ride);
        }
        responseObserver.onCompleted();

    }

    @Override
    public void bookingRequest(BookRequestModel request, StreamObserver<ReservationState> responseObserver) {
        String errMsg;
        int day = request.getDay();
        UUID id = UUID.fromString(request.getId());
        String attraction = request.getName();
        LocalTime slot = LocalTime.parse(request.getTime());

        if(day < 1 || day > 365){
            errMsg = "Day is invalid";
            logger.error(errMsg);
            responseObserver.onError(Status.INTERNAL.withDescription(errMsg).asRuntimeException());
        }

        if(!repository.attractionExists(attraction)) {
            errMsg = "Ride not found";
            logger.error(errMsg);
            responseObserver.onError(Status.NOT_FOUND.withDescription(errMsg).asRuntimeException());
        }

        if(!repository.visitorHasPass(id, day) || !repository.visitorCanVisit(id, day, slot)) {
            errMsg = "Invalid pass";
            logger.error(errMsg);
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription(errMsg).asRuntimeException());
        }

        if(!repository.attractionHasCapacityAlready(attraction, day)) {
            if(repository.addReservation(new Reservation(attraction, day, id, slot, ReservationStatus.PENDING))) {
                responseObserver.onNext(ReservationState.newBuilder().setStatus(ResStatus.PENDING).build());
                responseObserver.onCompleted();
            }
            errMsg = "Unknown error";
            logger.error(errMsg);
            responseObserver.onError(Status.INTERNAL.withDescription(errMsg).asRuntimeException());
        }

        int capacity = repository.getRemainingCapacity(attraction, day, slot);

        if(capacity <= 0) {
            errMsg = "Slot is full";
            logger.error(errMsg);
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription(errMsg).asRuntimeException());
        }

        if(repository.addReservation(new Reservation(attraction, day, id, slot, ReservationStatus.CONFIRMED))) {
            responseObserver.onNext(ReservationState.newBuilder().setStatus(ResStatus.CONFIRMED).build());
            responseObserver.onCompleted();
        }

        errMsg = "Unknown error";
        logger.error(errMsg);
        responseObserver.onError(Status.INTERNAL.withDescription(errMsg).asRuntimeException());


    }







}
