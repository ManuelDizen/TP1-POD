package ar.edu.itba.pod.grpc.models;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class Attraction {
    private final String name;
    private final LocalTime opening;
    private final LocalTime closing;
    private final int minsPerSlot;
    private final Map<Integer, Map<LocalTime, Integer>> spaceAvailable = new HashMap<>();
    private final Map<Integer, Integer> capacities = new HashMap<>();

    public Attraction(String name, LocalTime opening, LocalTime closing, int minsPerSlot) {
        this.name = name;
        this.opening = opening;
        this.closing = closing;
        this.minsPerSlot = minsPerSlot;
    }

    public String getName() {
        return name;
    }

    public String getOpening() {
        return String.valueOf(opening);
    }

    public String getClosing() {
        return String.valueOf(closing);
    }

    public int getMinsPerSlot() {
        return minsPerSlot;
    }

    public Map<Integer, Map<LocalTime, Integer>> getSpaceAvailable() {
        return spaceAvailable;
    }

    public Map<Integer, Integer> getCapacities() {
        return capacities;
    }

    public void initializeSlots(int day, int capacity){
        getSpaceAvailable().put(day, new HashMap<>());
        int i = 0;
        // La cuenta es: Si opening + (mins*i) es menor a closing pongo. Va a haber un caso borde
        // donde la cuenta puede dar mas, que es el último. Cuando creamos atracción, ya chequeamos que
        // entre al menos un slot, y de acuerdo a la consigna, si entra un slot y queda tiempo extra que es
        // menro a un slot, se hace igual.
        while((opening.plusMinutes((long) minsPerSlot * i)).isBefore(closing)){
            getSpaceAvailable().get(day).put(opening.plusMinutes((long) minsPerSlot * i), capacity);
            i++;
        }
    }
}
