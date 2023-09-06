package ar.edu.itba.pod.grpc.client;

import ar.edu.itba.pod.grpc.requests.BookingRequestsServiceGrpc;
import ar.edu.itba.pod.grpc.requests.RidesRequestModel;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
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
        switch(action) {
            case "attractions":
                getAllAttractions(req);
                break;
            case "availability":
                break;
            case "book":
                break;
            case "confirm":
                break;
            case "cancel":
                break;
        }
    }

    public static void getAllAttractions(BookingRequestsServiceGrpc.BookingRequestsServiceBlockingStub req) {
        //TODO: Manejar el stream respuesta

        List<RidesRequestModel> attractionList = new ArrayList<>();
        req.getAttractionsRequest(Empty.getDefaultInstance()).forEachRemaining(attractionList::add);

    }

}
