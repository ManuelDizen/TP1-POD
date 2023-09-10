package ar.edu.itba.pod.grpc.client;

import ar.edu.itba.pod.grpc.requests.*;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConnectionUtils;
import utils.ParsingUtils;
import utils.PrintingUtils;
import utils.PropertyNames;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookingClient {

    private static final Logger logger = LoggerFactory.getLogger(BookingClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("BookingClient starting...");

        ManagedChannel channel = ConnectionUtils.createChannel();

        String action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow();
        //TODO: NullPointerDereference

        BookingRequestsServiceGrpc.BookingRequestsServiceBlockingStub req =
                BookingRequestsServiceGrpc.newBlockingStub(channel);

        BookRequestModel model;

        ReservationState response;
        switch(action) {
            case "attractions":
                getAllAttractions(req);
                break;
            case "availability":
                checkAvailability(req);
                break;
            case "book":
                model = bookModel();
                response = req.bookingRequest(model);
                break;
            case "confirm":
                model = bookModel();
                response = req.confirmBooking(model);
                break;
            case "cancel":
                model = bookModel();
                response = req.cancelBooking(model);
                break;
            default:
                System.out.println("Invalid action. Please try again.");
                break;
        }
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
            // Nota al lector: La consigna en el testeo usa "-Dattraction=..." en vez de "ride". Asumimos que fue
            // un error de redacción.

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
        String attraction = ParsingUtils.getSystemProperty(PropertyNames.RIDE).orElseThrow();
        int day = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow());
        String time = ParsingUtils.getSystemProperty(PropertyNames.SLOT).orElseThrow();
        String visitorId = ParsingUtils.getSystemProperty(PropertyNames.VISITOR).orElseThrow();

        return BookRequestModel.newBuilder().setName(attraction)
                .setDay(day)
                .setTime(time)
                .setId(visitorId)
                .build();
    }




}
