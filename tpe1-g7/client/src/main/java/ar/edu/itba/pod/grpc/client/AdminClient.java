package ar.edu.itba.pod.grpc.client;

import ar.edu.itba.pod.grpc.client.queryModels.SlotsQueryParamsModel;
import ar.edu.itba.pod.grpc.requests.*;
import com.google.protobuf.Int32Value;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConnectionUtils;
import utils.ParsingUtils;
import utils.PrintingUtils;
import utils.PropertyNames;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static utils.ConnectionUtils.shutdownChannel;

public class AdminClient {
    private static final Logger logger = LoggerFactory.getLogger(AdminClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("AdminClient starting...");

        ManagedChannel channel = ConnectionUtils.createChannel();

        String action;
        try {
            action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow();
        }
        catch(NoSuchElementException e){
            System.out.println("Action requested is invalid. Please check action is one of the following options:\n[rides|tickets|slots]");
            shutdownChannel(channel);
            return;
        }

        AdminRequestsServiceGrpc.AdminRequestsServiceBlockingStub req =
                AdminRequestsServiceGrpc.newBlockingStub(channel);

        List<String[]> entries;
        int added = 0;
        Optional<String> path;

        switch (action) {
            case "rides" -> {
                logger.debug("rides...");
                path = ParsingUtils.getSystemProperty(PropertyNames.IN_PATH);
                if (path.isEmpty()) {
                    System.out.println("Path does not exist. Now exiting.");
                    break;
                }
                entries = ParsingUtils.parseCsv(path.get());
                for (String[] entry : entries) {
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
                    try{
                        Int32Value response = req.addRidesRequest(model);
                        added += response.getValue();
                        System.out.println(name + " ride added.");
                    }
                    catch(RuntimeException e){
                        System.out.println("Cannot add ride " + name + ": " + e.getMessage());
                    }
                }
                PrintingUtils.printRidesReply(entries.size(), added);
            }
            case "tickets" -> {
                logger.debug("tickets...");
                path = ParsingUtils.getSystemProperty(PropertyNames.IN_PATH);
                if (path.isEmpty()) {
                    System.out.println("Path does not exist. Now exiting.");
                    break;
                }
                entries = ParsingUtils.parseCsv(path.get());
                for (String[] entry : entries) {
                    PassType type = ParsingUtils.getPassNameFromString(entry[1]);
                    if (type == null) {
                        System.out.println("Pass type is invalid. Please try again.\n");
                        break;
                    }
                    String id = entry[0];
                    int day = Integer.parseInt(entry[2]);
                    TicketsRequestModel model = TicketsRequestModel.newBuilder()
                            .setDay(day)
                            .setType(type)
                            .setId(id)
                            .build();
                    try {
                        Int32Value response = req.addTicketsRequest(model);
                        added += response.getValue();
                        System.out.println("Pass for visitor " + id + " for day " + entry[2] + " added.");
                    }
                    catch(RuntimeException e){
                        System.out.println("Could not add pass for visitor " + id + " on day " + entry[2] + ".");
                    }
                }
                PrintingUtils.printTicketsReply(entries.size(), added);
            }
            case "slots" -> {
                logger.debug("slots...");
                SlotsQueryParamsModel queryModel;
                try {
                    queryModel = new SlotsQueryParamsModel();
                } catch (InvalidParameterException e) {
                    System.out.println(e.getMessage());
                    break;
                }
                SlotsRequestModel model = SlotsRequestModel.newBuilder()
                        .setDay(queryModel.getDay())
                        .setCapacity(queryModel.getCapacity())
                        .setRide(queryModel.getRide())
                        .build();
                try{
                    SlotsReplyModel response = req.addSlotsRequest(model);
                    PrintingUtils.printSlotsReply(response, queryModel.getCapacity(),
                            queryModel.getRide(), queryModel.getDay());
                }
                catch(RuntimeException e){
                    System.out.println(e.getMessage());
                }
            }
            default ->
                //Note: Should never reach this point
                    logger.error("Action requested is invalid. Please check action is one of the following options:\n[rides|tickets|slots]");
        }
        shutdownChannel(channel);
    }
}
