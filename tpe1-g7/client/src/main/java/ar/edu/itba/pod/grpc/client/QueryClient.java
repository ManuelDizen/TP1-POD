package ar.edu.itba.pod.grpc.client;

import ar.edu.itba.pod.grpc.client.queryModels.QueryParamsModel;
import ar.edu.itba.pod.grpc.requests.*;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConnectionUtils;
import utils.ParsingUtils;
import utils.PropertyNames;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import static utils.ConnectionUtils.shutdownChannel;

public class QueryClient {
    private static final Logger logger = LoggerFactory.getLogger(AdminClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("QueryClient starting...");

        ManagedChannel channel = ConnectionUtils.createChannel();

        String action = null;
        try {
            action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow();
        }
        catch(NoSuchElementException e){
            System.out.println("Action requested is invalid. Please check action is one of the following options:\n[capacity|confirmed]");
            shutdownChannel(channel);
            return;
        }

        QueryParamsModel params;
        try{
            params = new QueryParamsModel();
        }
        catch(InvalidParameterException e){
            System.out.println("Invalid parameters. Please try again.");
            shutdownChannel(channel);
            return;
        }

        QueryRequestsServiceGrpc.QueryRequestsServiceBlockingStub req =
                QueryRequestsServiceGrpc.newBlockingStub(channel);

        switch(action) {
            case "capacity":
                logger.info("QueryClient capacity...");
                QueryRequestModel modelC = QueryRequestModel.newBuilder().setDay(params.getDay()).build();
                List<QueryCapacityModel> capacityList = new ArrayList<>();
                try {
                    req.getCapacityRequest(modelC).forEachRemaining(capacityList::add);
                    if (!capacityList.isEmpty()) {
                        writeCapacityOutput(capacityList, params.getOutPath());
                    } else {
                        System.out.println("There are no Attractions");
                    }
                } catch(RuntimeException e){
                    System.out.println("Error doing query: " + e.getMessage());
                }
                break;
            case "confirmed":
                logger.info("QueryClient confirmed...");
                QueryRequestModel model = QueryRequestModel.newBuilder().setDay(params.getDay()).build();
                List<QueryConfirmedModel> confirmedList = new ArrayList<>();
                try {
                    req.getConfirmedRequest(model).forEachRemaining(confirmedList::add);
                    if (!confirmedList.isEmpty()) {
                        writeConfirmedOutput(confirmedList, params.getOutPath());
                    } else {
                        System.out.println("There are no confirmed reservations");
                    }
                } catch(RuntimeException e){
                    System.out.println("Error doing query: " + e.getMessage());
                }
                break;
        }
        shutdownChannel(channel);
    }

    private static void writeOnFile(StringBuilder output, String outPath) {
        File outFile = new File(outPath);
        try {
            if (!outFile.exists()) {
                outFile.createNewFile();
            }
            FileWriter fw = new FileWriter(outFile);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(output.toString());
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while creating the output file.");
        }
    }

    private static void writeCapacityOutput(List<QueryCapacityModel> capacityList, String outPath) {
        StringBuilder output = new StringBuilder();
        output.append("Slot ").append(" | ").append("Capacity").append(" | ").append("Attraction\n");
        capacityList.forEach((capacity) -> {
            int len = String.valueOf(capacity.getCapacity()).length();
            output.append(capacity.getSlot());
            output.append(" | ");
            output.append(" ".repeat("Capacity".length()-len));
            output.append(capacity.getCapacity());
            output.append(" | ");
            output.append(capacity.getAttraction());
            output.append("\n");
        });
        writeOnFile(output, outPath);
    }

    private static void writeConfirmedOutput(List<QueryConfirmedModel> confirmedList, String outPath) {
        StringBuilder output = new StringBuilder();
        output.append("Slot ").append(" | ").append("Visitor                             ").append(" | ").append("Attraction\n");
        confirmedList.forEach((confirmed) -> {
            output.append(confirmed.getSlot());
            output.append(" | ");
            output.append(confirmed.getVisitor());
            output.append(" | ");
            output.append(confirmed.getAttraction());
            output.append("\n");
        });
        writeOnFile(output, outPath);
    }

}
