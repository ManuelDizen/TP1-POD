package utils;

import ar.edu.itba.pod.grpc.requests.PassType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParsingUtils {

    private static final Logger logger = LoggerFactory.getLogger(ParsingUtils.class);

    public static Optional<String> getSystemProperty(String name){
        final String prop = System.getProperty(name);
        if(prop == null){
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

    public static List<String[]> parseCsv(String path){
        FileReader file;
        try{
            file = new FileReader(path);
        }
        catch(FileNotFoundException e){
            throw new RuntimeException(e.getMessage());
        }

        CSVParser parser;
        try {
            parser = new CSVParser(file, CSVFormat.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        boolean skipFirstLine = true;
        List<String[]> lines = new ArrayList<>();

        for (CSVRecord record : parser) {
            if (skipFirstLine) {
                skipFirstLine = false;
                continue;
            }
            String[] tokenizedArray = record.get(0).split(";");
            lines.add(tokenizedArray);
        }
        return lines;
    }

    public static PassType getFromString(String name) {
        System.out.println("Entro a getFromString con: " + "-"+name+"-\n");
        return switch (name) {
            case "UNLIMITED" -> PassType.UNLIMITED;
            case "HALFDAY" -> PassType.HALF_DAY;
            case "THREE" -> PassType.THREE;
            default -> null;
        };
    }

}
