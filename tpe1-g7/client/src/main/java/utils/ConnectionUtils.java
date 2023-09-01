package utils;

import ar.edu.itba.pod.grpc.client.Client;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ConnectionUtils {
    public static ManagedChannel createChannel(){
        String hostData = ParsingUtils.getSystemProperty(PropertyNames.SERVER_ADDRESS).orElseThrow();
        return ManagedChannelBuilder.forTarget(hostData)
                .usePlaintext()
                .build();
    }
}
