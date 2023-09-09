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
import java.util.Map;
import java.util.UUID;

import static ar.edu.itba.pod.grpc.models.ReservationStatus.CONFIRMED;
import static ar.edu.itba.pod.grpc.models.ReservationStatus.PENDING;

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

    public void checkAvailability(AvailabilityRequestModel request, StreamObserver<AvailabilityResponseModel> responseObserver) {

        String name = request.getAttraction();
        List<String> slots = request.getSlotsList();
        int day = request.getDay();


        if(day < 1 || day > 365){
            responseObserver.onError(Status.INTERNAL.withDescription("Day is invalid").asRuntimeException());
        }

        Attraction att = repository.getAttractionByName(name);
        if(att == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Ride not found").asRuntimeException());
        }
        Map<Integer, Integer> capacities = att.getCapacities();

        //busco la capacidad para ese día de esa atracción
        int capacity = 0;
        if(capacities != null)
            capacity = capacities.get(day);

        for(String s : slots) {

            if(repository.isValidSlot(name, LocalTime.parse(s)))
                responseObserver.onError(Status.INTERNAL.withDescription("Slot is invalid").asRuntimeException());

            LocalTime slot = att.getSlot(LocalTime.parse(s));
            int pending = repository.getReservations(name, day, slot, PENDING);
            int confirmed = repository.getReservations(name, day, slot, CONFIRMED);

            AvailabilityResponseModel response = AvailabilityResponseModel.newBuilder().setAttraction(name)
                    .setSlot(String.valueOf(slot))
                    .setCapacity(capacity)
                    .setPending(pending)
                    .setConfirmed(confirmed).build();

            responseObserver.onNext(response);
        }

    }

    @Override
    public void bookingRequest(BookRequestModel request, StreamObserver<ReservationState> responseObserver) {

        System.out.println("estoy en book");

        int day = request.getDay();
        UUID id = UUID.fromString(request.getId());
        String attraction = request.getName();
        LocalTime time = LocalTime.parse(request.getTime());

        LocalTime slot = checkBookingParameters(attraction, day, time, id, responseObserver);

        if(!repository.attractionHasCapacityAlready(attraction, day)) {
            System.out.println("no hay capacidad");
            if(repository.addReservation(new Reservation(attraction, day, id, slot, PENDING))) {
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

    @Override
    public void confirmBooking(BookRequestModel request, StreamObserver<ReservationState> responseObserver) {

        int day = request.getDay();
        UUID id = UUID.fromString(request.getId());
        String attraction = request.getName();
        LocalTime time = LocalTime.parse(request.getTime());

        LocalTime slot = checkBookingParameters(attraction, day, time, id, responseObserver);

        if(!repository.attractionHasCapacityAlready(attraction, day)) {
            bookOnError("Ride not available", "Internal", responseObserver);
        }

        int amount = repository.confirmReservation(attraction, day, slot, id);

        if(amount <= 0)
            bookOnError("Reservation not found", "Not found", responseObserver);

        responseObserver.onNext(ReservationState.newBuilder().setStatus(ResStatus.CONFIRMED).setAmount(amount).build());
        responseObserver.onCompleted();

    }

    @Override
    public void cancelBooking(BookRequestModel request, StreamObserver<ReservationState> responseObserver) {

        int day = request.getDay();
        UUID id = UUID.fromString(request.getId());
        String attraction = request.getName();
        LocalTime time = LocalTime.parse(request.getTime());

        LocalTime slot = checkBookingParameters(attraction, day, time, id, responseObserver);

        int amount = repository.cancelReservation(attraction, day, slot, id);

        if(amount <= 0)
            bookOnError("Reservation not found", "Not found", responseObserver);

        responseObserver.onNext(ReservationState.newBuilder().setStatus(ResStatus.CANCELLED).setAmount(amount).build());
        responseObserver.onCompleted();

    }

    private LocalTime checkBookingParameters(String name, int day, LocalTime time, UUID id, StreamObserver<ReservationState> responseObserver) {

        if(!repository.attractionExists(name)) {
            bookOnError("Ride not found", "Not found", responseObserver);
        }

        if(day < 1 || day > 365){
            bookOnError("Day is invalid", "Internal", responseObserver);
        }

        if(!repository.isValidSlot(name, time))
            bookOnError("Slot is invalid", "Internal", responseObserver);

        LocalTime slot = repository.getAttractionByName(name).getSlot(time);

        if(!repository.visitorHasPass(id, day) || !repository.visitorCanVisit(id, day, slot)) {
            bookOnError("Invalid pass", "Permission denied", responseObserver);
        }

        return slot;
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
