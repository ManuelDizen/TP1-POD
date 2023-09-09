package utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ConnectionUtils {
    public static ManagedChannel createChannel(){
        String hostData = ParsingUtils.getSystemProperty(PropertyNames.SERVER_ADDRESS).orElseThrow();
        System.out.println("Host data desde createChannel: " + hostData + "\n");
        return ManagedChannelBuilder.forTarget(hostData)
                .usePlaintext()
                .build();
    }

    public static ManagedChannel createNewChannel(String serverAddress){
        String hostData = serverAddress;
        System.out.println("Host data desde createNewChannel: " + hostData + "\n");
        return ManagedChannelBuilder.forTarget(hostData)
                .usePlaintext()
                .build();
    }
}
