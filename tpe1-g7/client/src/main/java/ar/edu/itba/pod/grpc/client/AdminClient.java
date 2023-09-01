package ar.edu.itba.pod.grpc.client;

import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ConnectionUtils;
import utils.ParsingUtils;
import utils.PropertyNames;

import java.util.concurrent.TimeUnit;

public class AdminClient {
    private static Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("AdminClient starting...");

        ManagedChannel channel = ConnectionUtils.createChannel();
        String action = ParsingUtils.getSystemProperty(PropertyNames.ACTION).orElseThrow();
        //TODO: NullPointerDereference
        switch (action){
            case "rides":
                logger.debug("rides...");
                break;
            case "tickets":
                logger.debug("tickets...");
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

                break;
            default:
                logger.error("Action requested is invalid. Please check action is one of the following options:\n[rides|tickets|slots]");
        }
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);


        /*
        PingRequest request = PingRequest.newBuilder().setName("Hola!!!").build();
        HealthServiceGrpc.HealthServiceBlockingStub blocking = HealthServiceGrpc.newBlockingStub(channel);
        PingResponse response = blocking.ping(request);
        System.out.println(response.getMessage());
         */

    }
}
