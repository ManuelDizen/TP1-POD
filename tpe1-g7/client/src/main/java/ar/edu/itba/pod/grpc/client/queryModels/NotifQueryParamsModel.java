package ar.edu.itba.pod.grpc.client.queryModels;

import utils.ParsingUtils;
import utils.PropertyNames;

import java.security.InvalidParameterException;
import java.util.NoSuchElementException;

public class NotifQueryParamsModel {
    private final String attraction;
    private final int day;
    private final String visitorId;

    public NotifQueryParamsModel() {
        try{
            attraction = ParsingUtils.getSystemProperty(PropertyNames.RIDE).orElseThrow();
            day = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow());
            visitorId = ParsingUtils.getSystemProperty(PropertyNames.VISITOR).orElseThrow();
        }
        catch(NoSuchElementException | NumberFormatException f){
            throw new InvalidParameterException("Invalid parameters. Now exiting");
        }
    }

    public String getAttraction() {
        return attraction;
    }

    public int getDay() {
        return day;
    }

    public String getVisitorId() {
        return visitorId;
    }
}
