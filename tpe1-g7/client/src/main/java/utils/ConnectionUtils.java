package utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class ConnectionUtils {
    public static ManagedChannel createChannel(){
        String hostData = ParsingUtils.getSystemProperty(PropertyNames.SERVER_ADDRESS).orElseThrow(() -> new RuntimeException("Invalid server address"));
        return ManagedChannelBuilder.forTarget(hostData)
                .usePlaintext()
                .build();
    }

    public static void shutdownChannel(ManagedChannel channel) throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
}
