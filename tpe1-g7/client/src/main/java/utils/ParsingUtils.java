package utils;

import ar.edu.itba.pod.grpc.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ParsingUtils {

    private static Logger logger = LoggerFactory.getLogger(Client.class);

    public static Optional<String> getSystemProperty(String name){
        final String prop = System.getProperty(name);
        if(prop == null){
            logger.error("Error parsing property of name " + name + ". Now exiting");
            return Optional.empty();
        }
        return Optional.of(prop);
    }

    public static HostData parseIpPort(String input) {
        if(input == null){
            logger.error("Error parsing data. Input is null. Exiting.");
            return null;
        }
        String[] parts = input.split(":");
        if (parts.length == 2) {
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);
            return new HostData(ip, port);
        }
        return null;
    }


}
