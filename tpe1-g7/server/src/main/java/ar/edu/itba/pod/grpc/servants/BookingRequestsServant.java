package ar.edu.itba.pod.grpc.servants;

import ar.edu.itba.pod.grpc.models.Attraction;
import ar.edu.itba.pod.grpc.persistance.ParkRepository;
import ar.edu.itba.pod.grpc.models.Reservation;
import ar.edu.itba.pod.grpc.models.ReservationStatus;
import ar.edu.itba.pod.grpc.requests.*;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
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
                    .setOpening(String.valueOf(att.getOpening()))
                    .setClosing(String.valueOf(att.getClosing()))
                    .setMinsPerSlot(att.getMinsPerSlot())
                    .build();
            responseObserver.onNext(ride);
        }
        responseObserver.onCompleted();

    }

    @Override
    public void bookingRequest(BookRequestModel request, StreamObserver<ReservationState> responseObserver) {

        System.out.println("estoy en book");

        int day = request.getDay();
        UUID id = UUID.fromString(request.getId());
        String attraction = request.getName();
        LocalTime slot = repository.getAttractionByName(attraction).getSlot(LocalTime.parse(request.getTime()));
        System.out.println("slot: " + slot);

        if(day < 1 || day > 365){
            bookOnError("Day is invalid", "Internal", responseObserver);
        }

        if(!repository.attractionExists(attraction)) {
            bookOnError("Ride not found", "Not found", responseObserver);
        }

        if(!repository.visitorHasPass(id, day) || !repository.visitorCanVisit(id, day, slot)) {
            bookOnError("Invalid pass", "Permission denied", responseObserver);
        }

        if(!repository.attractionHasCapacityAlready(attraction, day)) {
            System.out.println("no hay capacidad");
            if(repository.addReservation(new Reservation(attraction, day, id, slot, ReservationStatus.PENDING))) {
                responseObserver.onNext(ReservationState.newBuilder().setStatus(ResStatus.PENDING).build());
                responseObserver.onCompleted();
            } else
                bookOnError("Unknown error", "Internal", responseObserver);
        } else {
            int capacity = repository.getRemainingCapacity(attraction, day, slot);

            if(capacity <= 0) {
                bookOnError("Slot is full", "Permission denied", responseObserver);
            }

            if(repository.addReservation(new Reservation(attraction, day, id, slot, ReservationStatus.CONFIRMED))) {
                responseObserver.onNext(ReservationState.newBuilder().setStatus(ResStatus.CONFIRMED).build());
                responseObserver.onCompleted();
            } else {
                bookOnError("Unknown error", "Internal", responseObserver);
            }
        }

    }



    private void bookOnError(String errMsg, String status, StreamObserver<ReservationState> responseObserver){
        logger.error(errMsg);
        switch (status) {
            case "Permission denied":
                responseObserver.onError(Status.PERMISSION_DENIED.withDescription(errMsg).asRuntimeException());
                break;
            case "Not found":
                responseObserver.onError(Status.NOT_FOUND.withDescription(errMsg).asRuntimeException());
                break;
            default:
                responseObserver.onError(Status.INTERNAL.withDescription(errMsg).asRuntimeException());
                break;
        }
    }







}
