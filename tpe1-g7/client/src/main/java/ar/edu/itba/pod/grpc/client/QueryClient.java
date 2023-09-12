package ar.edu.itba.pod.grpc.client;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class QueryClient {
    private static final Logger logger = LoggerFactory.getLogger(AdminClient.class);
    //TODO: checkear que los parametros de entrada sean correctos
    public static void main(String[] args) throws InterruptedException {
        logger.info("QueryClient starting...");

        String action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow(() -> new RuntimeException("Error parsing parameter"));
        //TODO: NullPointerDereference
        String serverAddress = ParsingUtils.getSystemProperty(PropertyNames.SERVER_ADDRESS).orElseThrow(() -> new RuntimeException("Error parsing parameter"));
        String outPath = ParsingUtils.getSystemProperty(PropertyNames.OUT_PATH).orElseThrow(() -> new RuntimeException("Error parsing parameter"));

        ManagedChannel channel = ConnectionUtils.createNewChannel(serverAddress);

        QueryRequestsServiceGrpc.QueryRequestsServiceBlockingStub req =
                QueryRequestsServiceGrpc.newBlockingStub(channel);

        switch(action) {
            case "capacity":
                int dayC = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow(() -> new RuntimeException("Error parsing parameter")));
                QueryRequestModel modelC = QueryRequestModel.newBuilder().setDay(dayC).build();
                List<QueryCapacityModel> capacityList = new ArrayList<>();
                req.getCapacityRequest(modelC).forEachRemaining(capacityList::add);
                writeCapacityOutput(capacityList, outPath);
                break;
            case "confirmed":
                int day = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow(() -> new RuntimeException("Error parsing parameter")));
                QueryRequestModel model = QueryRequestModel.newBuilder().setDay(day).build();
                List<QueryConfirmedModel> confirmedList = new ArrayList<>();
                req.getConfirmedRequest(model).forEachRemaining(confirmedList::add);
                writeConfirmedOutput(confirmedList, outPath);
                break;
        }
        channel.shutdownNow().awaitTermination(10, TimeUnit.SECONDS);
    }

    private static void writeOnFile(StringBuilder output, String outPath) {
        try {
            File outFile = new File(outPath);
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
            output.append(capacity.getSlot());
            output.append(" | ");
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
