package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostData {
    private static final Logger logger = LoggerFactory.getLogger(HostData.class);

    private String host;
    private int port;

    public HostData(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public HostData(String unparsed){
        HostData d = ParsingUtils.parseIpPort(unparsed);
        if(d == null){
            logger.error("Error parsing host data. Now exiting");
            return;
        }
        this.host = d.host;
        this.port = d.port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
