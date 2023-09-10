package utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ConnectionUtils {
    public static ManagedChannel createChannel(){
        String hostData = ParsingUtils.getSystemProperty(PropertyNames.SERVER_ADDRESS).orElseThrow();
        return ManagedChannelBuilder.forTarget(hostData)
                .usePlaintext()
                .build();
    }

    public static ManagedChannel createNewChannel(String serverAddress){
        return ManagedChannelBuilder.forTarget(serverAddress)
                .usePlaintext()
                .build();
    }
}
