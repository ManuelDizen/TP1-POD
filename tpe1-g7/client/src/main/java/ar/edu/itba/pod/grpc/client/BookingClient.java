package ar.edu.itba.pod.grpc.client;

import ar.edu.itba.pod.grpc.client.queryModels.BookQueryParamsModel;
import ar.edu.itba.pod.grpc.client.queryModels.SlotsQueryParamsModel;
import ar.edu.itba.pod.grpc.requests.*;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConnectionUtils;
import utils.ParsingUtils;
import utils.PrintingUtils;
import utils.PropertyNames;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static utils.ConnectionUtils.shutdownChannel;
import static utils.PrintingUtils.printBookingReply;

public class BookingClient {

    private static final Logger logger = LoggerFactory.getLogger(BookingClient.class);


    public static void main(String[] args) throws InterruptedException {
        logger.info("BookingClient starting...");

        ManagedChannel channel = ConnectionUtils.createChannel();

        String action = null;
        try {
            action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow();
        }
        catch(NoSuchElementException e){
            System.out.println("Action requested is invalid. Please check action is one of the following options:\n[rides|tickets|slots]");
            shutdownChannel(channel);
            return;
        }

        BookingRequestsServiceGrpc.BookingRequestsServiceBlockingStub req =
                BookingRequestsServiceGrpc.newBlockingStub(channel);

        BookRequestModel model;

        ReservationState response;
        switch (action) {
            case "attractions" -> getAllAttractions(req);
            case "availability" -> checkAvailability(req);
            case "book" -> {
                model = bookModel();
                response = req.bookingRequest(model);
                printBookingReply(response);
            }
            case "confirm" -> {
                model = bookModel();
                response = req.confirmBooking(model);
                printBookingReply(response);
            }
            case "cancel" -> {
                model = bookModel();
                response = req.cancelBooking(model);
                printBookingReply(response);
            }
            default -> System.out.println("Invalid action. Please try again.");
        }
        shutdownChannel(channel);
    }

    private static void getAllAttractions(BookingRequestsServiceGrpc.BookingRequestsServiceBlockingStub req) {
        List<RidesRequestModel> attractionList = req.getAttractionsRequest(Empty.getDefaultInstance()).getRidesList();
        PrintingUtils.printAttractions(attractionList);
    }

    private static void checkAvailability(BookingRequestsServiceGrpc.BookingRequestsServiceBlockingStub req) {

        int day = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow());
        List<String> slots = new ArrayList<>(){};
        Optional<String> slot = ParsingUtils.getSystemProperty(PropertyNames.SLOT);
        if(slot.isEmpty()) {
            String slotFrom = ParsingUtils.getSystemProperty(PropertyNames.SLOT_FROM).orElseThrow();
            String slotTo = ParsingUtils.getSystemProperty(PropertyNames.SLOT_TO).orElseThrow();
            slots.add(slotFrom);
            slots.add(slotTo);
        } else {
            slots.add(slot.get());
        }
        Optional<String> attraction = ParsingUtils.getSystemProperty(PropertyNames.RIDE);
        AvailabilityResponseModel response;
        if(attraction.isEmpty()) {
            response = req.checkAvailabilityAllAttractions(AvailabilityRequestModel.newBuilder()
                   .setDay(day).addAllSlots(slots).build());
        } else {
            response = req.checkAvailability(AvailabilityRequestModel.newBuilder()
                    .setAttraction(attraction.get()).setDay(day).addAllSlots(slots).build());
        }
        PrintingUtils.printAvailability(response.getAvailabilityList());

    }

    private static BookRequestModel bookModel() {
        BookQueryParamsModel queryModel;
        try {
            queryModel = new BookQueryParamsModel();
        } catch (InvalidParameterException e) {
            System.out.println(e.getMessage());
            return null;
        }

        return BookRequestModel.newBuilder().setName(queryModel.getAttraction())
                .setDay(queryModel.getDay())
                .setTime(queryModel.getTime())
                .setId(queryModel.getVisitorId())
                .build();
    }

}
