package ar.edu.itba.pod.grpc.client;

import ar.edu.itba.pod.grpc.requests.*;
import com.google.protobuf.Int32Value;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConnectionUtils;
import utils.ParsingUtils;
import utils.PrintingUtils;
import utils.PropertyNames;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AdminClient {
    private static final Logger logger = LoggerFactory.getLogger(AdminClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("AdminClient starting...");

        ManagedChannel channel = ConnectionUtils.createChannel();
        String action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow();
        //TODO: NullPointerDereference

        AdminRequestsServiceGrpc.AdminRequestsServiceBlockingStub req =
                AdminRequestsServiceGrpc.newBlockingStub(channel);
        List<String[]> entries;
        int added = 0;
        switch (action){
            case "rides":
                logger.debug("rides...");
                entries = ParsingUtils.parseCsv(ParsingUtils.getSystemProperty(PropertyNames.IN_PATH).orElseThrow());
                for(String[] entry : entries){
                    String name = entry[0];
                    String opening = entry[1];
                    String closing = entry[2];
                    int minsPerSlot = Integer.parseInt(entry[3]);
                    RidesRequestModel model = RidesRequestModel.newBuilder()
                            .setName(name)
                            .setOpening(opening)
                            .setClosing(closing)
                            .setMinsPerSlot(minsPerSlot)
                            .build();
                    Int32Value response = req.addRidesRequest(model);
                    added += response.getValue();
                }
                PrintingUtils.printRidesReply(entries.size(), added);
                break;
            case "tickets":
                logger.debug("tickets...");
                entries = ParsingUtils.parseCsv(ParsingUtils.getSystemProperty(PropertyNames.IN_PATH).orElseThrow());
                for(String[] entry : entries){
                    String id = entry[0];
                    PassType type = ParsingUtils.getFromString(entry[1]);
                    if(type == null) {
                        throw new RuntimeException("Error in type parameter. Please check csv file.\n");
                    }
                    int day = Integer.parseInt(entry[2]);
                    TicketsRequestModel model = TicketsRequestModel.newBuilder()
                            .setDay(day)
                            .setType(type)
                            .setId(id)
                            .build();
                    Int32Value response = req.addTicketsRequest(model);
                    added += response.getValue();
                }
                PrintingUtils.printTicketsReply(entries.size(), added);
                break;
            case "slots":
                /*TODO: Esto me lo anoto para acordarme después. Falla si:
                    - Nombre no existe
                    - Dia menor a 1 y mayor a 365
                    - Capacidad negativa o ya existe capacidad para ese dia y nombre
                 */
                logger.debug("slots...");
                int day = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow());
                String ride = ParsingUtils.getSystemProperty(PropertyNames.RIDE).orElseThrow();
                int capacity = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.CAPACITY).orElseThrow());
                SlotsRequestModel model = SlotsRequestModel.newBuilder()
                        .setDay(day)
                        .setCapacity(capacity)
                        .setRide(ride)
                        .build();
                SlotsReplyModel response = req.addSlotsRequest(model);
                PrintingUtils.printSlotsReply(response, capacity, ride, day);
                //TODO: Discuss if client side validation or server side is necessary
                break;
            default:
                logger.error("Action requested is invalid. Please check action is one of the following options:\n[rides|tickets|slots]");
        }

        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);

    }
}
