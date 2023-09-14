package ar.edu.itba.pod.grpc.client.queryModels;

import utils.ParsingUtils;
import utils.PropertyNames;

import java.security.InvalidParameterException;
import java.util.NoSuchElementException;

public class QueryParamsModel {
    private final int day;
    private final String outPath;


    public QueryParamsModel() {
        try{
            day = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow());
            outPath = ParsingUtils.getSystemProperty(PropertyNames.OUT_PATH).orElseThrow();
        }
        catch(NoSuchElementException | NumberFormatException f) {
            throw new InvalidParameterException("Invalid parameters. Now exiting");
        }
    }

    public int getDay() {
        return day;
    }

    public String getOutPath() {
        return outPath;
    }

}
