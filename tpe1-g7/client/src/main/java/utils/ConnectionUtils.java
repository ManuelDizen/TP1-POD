package utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class ConnectionUtils {
    public static ManagedChannel createChannel(){
        String hostData = ParsingUtils.getSystemProperty(PropertyNames.SERVER_ADDRESS).orElseThrow();
        System.out.println("Host data desde createChannel: " + hostData + "\n");
        return ManagedChannelBuilder.forTarget(hostData)
                .usePlaintext()
                .build();
    }

    public static ManagedChannel createNewChannel(String serverAddress){
        System.out.println("Host data desde createNewChannel: " + serverAddress + "\n");
        return ManagedChannelBuilder.forTarget(serverAddress)
                .usePlaintext()
                .build();
    }

    public static void shutdownChannel(ManagedChannel channel) throws InterruptedException {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
}
