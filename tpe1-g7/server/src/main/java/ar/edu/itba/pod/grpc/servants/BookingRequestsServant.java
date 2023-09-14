package ar.edu.itba.pod.grpc.servants;

import ar.edu.itba.pod.grpc.models.Attraction;
import ar.edu.itba.pod.grpc.persistance.ParkRepository;
import ar.edu.itba.pod.grpc.models.Reservation;
import ar.edu.itba.pod.grpc.requests.*;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;

public class BookingRequestsServant extends BookingRequestsServiceGrpc.BookingRequestsServiceImplBase {

    private final static Logger logger = LoggerFactory.getLogger(BookingRequestsServant.class);

    private final ParkRepository repository = ParkRepository.getRepository();

    @Override
    public void getAttractionsRequest(Empty request,
                                      StreamObserver<RidesResponseModel> responseObserver){

        List<Attraction> attractions = repository.getAttractions();

        List<RidesRequestModel> rides = new ArrayList<>();


        for(Attraction att : attractions) {
            RidesRequestModel ride = RidesRequestModel.newBuilder().setName(att.getName())
                    .setOpening(String.valueOf(att.getOpening()))
                    .setClosing(String.valueOf(att.getClosing()))
                    .setMinsPerSlot(att.getMinsPerSlot())
                    .build();
            rides.add(ride);
        }

        RidesResponseModel response = RidesResponseModel.newBuilder().addAllRides(rides).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    public void checkAvailability(AvailabilityRequestModel request, StreamObserver<AvailabilityResponseModel> responseObserver) {

        int day = request.getDay();
        String attraction = request.getAttraction();

        if(checkAvailabilityParameters(attraction, day, responseObserver)){
            if(!checkSlots(attraction, request.getSlotsList())) {
                responseObserver.onError(Status.INTERNAL.withDescription("Invalid slot").asRuntimeException());
            } else {
                List<LocalTime> slots = repository.getSlotsInterval(attraction, request.getSlotsList());
                List<AvailabilityResponse> responseList = repository.getAvailability(attraction, day, slots);
                responseObserver.onNext(AvailabilityResponseModel.newBuilder().addAllAvailability(responseList).build());
                responseObserver.onCompleted();
            }
        }

    }

    public void checkAvailabilityAllAttractions(AvailabilityRequestModel request, StreamObserver<AvailabilityResponseModel> responseObserver) {

        List<AvailabilityResponse> responseList = new ArrayList<>();

        List<Attraction> attractions = repository.getAttractions();
        int day = request.getDay();
        if(day < 1 || day > 365){
            responseObserver.onError(Status.INTERNAL.withDescription("Day is invalid").asRuntimeException());
        } else {
            for(Attraction att : attractions) {
                if(!checkSlots(att.getName(), request.getSlotsList())) {
                    responseObserver.onError(Status.INTERNAL.withDescription("Invalid slot").asRuntimeException());
                } else {
                    List<LocalTime> slots = repository.getSlotsInterval(att.getName(), request.getSlotsList());
                    responseList.addAll(repository.getAvailability(att.getName(), day, slots));
                }
            }
                responseList.sort((p1, p2) -> {
                    int c = p1.getSlot().compareTo(p2.getSlot());
                    if (c == 0) {
                        return (p1.getAttraction().compareTo(p2.getAttraction()));
                    } else {
                        return c;
                    }
                });

                responseObserver.onNext(AvailabilityResponseModel.newBuilder().addAllAvailability(responseList).build());
                responseObserver.onCompleted();
                }
    }


    private boolean checkSlots(String att, List<String> slots) {
        for(String slot : slots) {
            if(!repository.isValidSlot(att, LocalTime.parse(slot)))
                return false;
        }
        return true;
    }

    @Override
    public void bookingRequest(BookRequestModel request, StreamObserver<ReservationState> responseObserver) {

        int day = request.getDay();
        UUID id = UUID.fromString(request.getId());
        String attraction = request.getName();
        LocalTime slot = LocalTime.parse(request.getTime());

        if(checkBookingParameters(attraction, day, slot, id, responseObserver))
            {

            try {
                ResStatus status = repository.addReservation(new Reservation(attraction, day, id, slot, null));
                responseObserver.onNext(ReservationState.newBuilder().setStatus(status)
                        .setAttraction(attraction).setDay(day).setSlot(String.valueOf(slot)).build());
                responseObserver.onCompleted();
            } catch (RuntimeException e) {
                bookOnError(e.getMessage(), "Permission denied", responseObserver);
            }

        }
    }

    @Override
    public void confirmBooking(BookRequestModel request, StreamObserver<ReservationState> responseObserver) {

        int day = request.getDay();
        UUID id = UUID.fromString(request.getId());
        String attraction = request.getName();
        LocalTime slot = LocalTime.parse(request.getTime());

        if(checkBookingParameters(attraction, day, slot, id, responseObserver)) {

            try {
                repository.confirmReservation(attraction, day, slot, id);
                responseObserver.onNext(ReservationState.newBuilder().setStatus(ResStatus.CONFIRMED).
                        setAttraction(attraction).setDay(day).setSlot(String.valueOf(slot)).build());
                responseObserver.onCompleted();
            } catch (RuntimeException e) {
                bookOnError(e.getMessage(), "Permission denied", responseObserver);
            }

        }

    }

    @Override
    public void cancelBooking(BookRequestModel request, StreamObserver<ReservationState> responseObserver) {

        int day = request.getDay();
        UUID id = UUID.fromString(request.getId());
        String attraction = request.getName();

        LocalTime slot = LocalTime.parse(request.getTime());

        if(checkBookingParameters(attraction, day, slot, id, responseObserver)) {

            try {
                repository.cancelReservation(attraction, day, slot, id);
                responseObserver.onNext(ReservationState.newBuilder().setStatus(ResStatus.CANCELLED).
                        setAttraction(attraction).setDay(day).setSlot(String.valueOf(slot)).build());
                responseObserver.onCompleted();
            } catch (RuntimeException e) {
                bookOnError(e.getMessage(), "Permission denied", responseObserver);
            }

        }

    }

    private boolean checkBookingParameters(String name, int day, LocalTime time, UUID id, StreamObserver<ReservationState> responseObserver) {

        if(!repository.attractionExists(name)) {
            bookOnError("Ride not found", "Not found", responseObserver);
            return false;
        }

        if(day < 1 || day > 365){
            bookOnError("Day is invalid", "Internal", responseObserver);
            return false;
        }

        if(!repository.isValidSlot(name, time)) {
            bookOnError("Slot is invalid", "Internal", responseObserver);
            return false;
        }

        return true;
    }

    private boolean checkAvailabilityParameters(String name, int day, StreamObserver<AvailabilityResponseModel> responseObserver) {

        if(!repository.attractionExists(name)) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Ride not found").asRuntimeException());
            return false;
        }

        if(day < 1 || day > 365){
            responseObserver.onError(Status.INTERNAL.withDescription("Day is invalid").asRuntimeException());
            return false;
        }

        return true;
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
            case "Already exists":
                responseObserver.onError(Status.ALREADY_EXISTS.withDescription(errMsg).asRuntimeException());
                break;
            default:
                responseObserver.onError(Status.INTERNAL.withDescription(errMsg).asRuntimeException());
                break;
        }
    }







}
