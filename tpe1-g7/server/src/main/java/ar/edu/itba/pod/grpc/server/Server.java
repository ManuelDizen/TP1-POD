package ar.edu.itba.pod.grpc.server;

import ar.edu.itba.pod.grpc.servants.AdminRequestsServant;
import ar.edu.itba.pod.grpc.servants.BookingRequestsServant;
import ar.edu.itba.pod.grpc.servants.NotifRequestsServant;
import ar.edu.itba.pod.grpc.servants.QueryRequestsServant;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        logger.info(" Server Starting ...");

        int port = 50051;
        io.grpc.Server server = ServerBuilder.forPort(port)
                .addService(new AdminRequestsServant())
                .addService(new BookingRequestsServant())
                .addService(new NotifRequestsServant())
                .addService(new QueryRequestsServant())
                .build();
        server.start();
        logger.info("Server started, listening on " + port);
        server.awaitTermination();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server since JVM is shutting down");
            server.shutdown();
            logger.info("Server shut down");
        }));
    }}
