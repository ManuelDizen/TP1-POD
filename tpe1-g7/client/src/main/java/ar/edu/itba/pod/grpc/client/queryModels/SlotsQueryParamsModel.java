package ar.edu.itba.pod.grpc.client.queryModels;

import utils.ParsingUtils;
import utils.PropertyNames;

import java.security.InvalidParameterException;
import java.util.NoSuchElementException;

public class SlotsQueryParamsModel {
    private final int day;
    private final String ride;
    private final int capacity;
    public SlotsQueryParamsModel() {
        try {
            this.day = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.DAY).orElseThrow());
            this.ride = ParsingUtils.getSystemProperty(PropertyNames.RIDE).orElseThrow();
            this.capacity = Integer.parseInt(ParsingUtils.getSystemProperty(PropertyNames.CAPACITY).orElseThrow());
        }
        catch(NoSuchElementException | NumberFormatException e){
            throw new InvalidParameterException("Invalid parameters. Now exiting");
        }
    }

    public int getDay() {
        return day;
    }

    public String getRide() {
        return ride;
    }

    public int getCapacity() {
        return capacity;
    }
}
