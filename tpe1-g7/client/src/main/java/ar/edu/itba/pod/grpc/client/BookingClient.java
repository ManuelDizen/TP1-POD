package ar.edu.itba.pod.grpc.client;

import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConnectionUtils;
import utils.ParsingUtils;
import utils.PropertyNames;

public class BookingClient {

    private static Logger logger = LoggerFactory.getLogger(BookingClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("AdminClient starting...");

        ManagedChannel channel = ConnectionUtils.createChannel();
        String action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow();
        //TODO: NullPointerDereference
    }
}
