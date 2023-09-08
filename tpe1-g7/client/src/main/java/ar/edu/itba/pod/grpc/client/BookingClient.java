package ar.edu.itba.pod.grpc.client;

import ar.edu.itba.pod.grpc.requests.BookRequestModel;
import ar.edu.itba.pod.grpc.requests.BookingRequestsServiceGrpc;
import ar.edu.itba.pod.grpc.requests.ReservationState;
import ar.edu.itba.pod.grpc.requests.RidesRequestModel;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConnectionUtils;
import utils.ParsingUtils;
import utils.PropertyNames;

import java.util.ArrayList;
import java.util.List;

public class BookingClient {

    private static Logger logger = LoggerFactory.getLogger(BookingClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("AdminClient starting...");

        ManagedChannel channel = ConnectionUtils.createChannel();

        String action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow();
        //TODO: NullPointerDereference

        BookingRequestsServiceGrpc.BookingRequestsServiceBlockingStub req =
                BookingRequestsServiceGrpc.newBlockingStub(channel);

        BookRequestModel model = bookModel();
        ReservationState response;
        switch(action) {
            case "attractions":
                getAllAttractions(req);
                break;
            case "availability":
                break;
            case "book":
                response = req.bookingRequest(model);
                System.out.println("God! " + response.getAmount());
                break;
            case "confirm":
                response = req.confirmBooking(model);
                System.out.println("God! " + response.getAmount());
                break;
            case "cancel":
                response = req.cancelBooking(model);
                System.out.println("God! " + response.getAmount());
                break;
            default:
                System.out.println("Invalid action. Please try again.");
                break;
        }
    }

    private static void getAllAttractions(BookingRequestsServiceGrpc.BookingRequestsServiceBlockingStub req) {
        //TODO: Manejar el stream respuesta

        List<RidesRequestModel> attractionList = new ArrayList<>();
        req.getAttractionsRequest(Empty.getDefaultInstance()).forEachRemaining(attractionList::add);

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

    private static void confirm(BookingRequestsServiceGrpc.BookingRequestsServiceBlockingStub req) {
        String attraction = ParsingUtils.getSystemProperty(PropertyNames.RIDE).orElseThrow();
        int day = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow());
        String time = ParsingUtils.getSystemProperty(PropertyNames.SLOT).orElseThrow();
        String visitorId = ParsingUtils.getSystemProperty(PropertyNames.VISITOR).orElseThrow();

        BookRequestModel model = BookRequestModel.newBuilder().setName(attraction)
                .setDay(day)
                .setTime(time)
                .setId(visitorId)
                .build();
        ReservationState response = req.bookingRequest(model);
        System.out.println("God! " + response);
    }

    private static void cancel(BookingRequestsServiceGrpc.BookingRequestsServiceBlockingStub req) {
        String attraction = ParsingUtils.getSystemProperty(PropertyNames.RIDE).orElseThrow();
        int day = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow());
        String time = ParsingUtils.getSystemProperty(PropertyNames.SLOT).orElseThrow();
        String visitorId = ParsingUtils.getSystemProperty(PropertyNames.VISITOR).orElseThrow();

        BookRequestModel model = BookRequestModel.newBuilder().setName(attraction)
                .setDay(day)
                .setTime(time)
                .setId(visitorId)
                .build();
        ReservationState response = req.bookingRequest(model);
        System.out.println("God! " + response);
    }


}
