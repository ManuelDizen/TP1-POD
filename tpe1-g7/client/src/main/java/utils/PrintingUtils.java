package utils;

import ar.edu.itba.pod.grpc.requests.*;

import java.util.List;

public class PrintingUtils {

    public static void printSlotsReply(SlotsReplyModel model, int capacity, String ride, int day){
        System.out.println("Loaded capacity of " + capacity + " for " + ride + " on day " + day + ".");
        if(model.getConfirmed() != 0)
            System.out.println(model.getConfirmed() + " confirmed without changes.\n");
        if(model.getRelocated() != 0)
            System.out.println(model.getRelocated() + " bookings relocated.\n");
        if(model.getCancelled() != 0)
            System.out.println(model.getCancelled() + " bookings cancelled.");
    }

    public static void printAttractions(List<RidesRequestModel> attractions) {

        System.out.println("Attractions: ");

        for(RidesRequestModel r : attractions) {
            System.out.println(r.getName() + " : " + r.getOpening() + " - " + r.getClosing());
        }

    }

    public static void printAvailability(List<AvailabilityResponse> availability) {

        System.out.println("Slot  | Capacity | Pending | Confirmed | Attraction");

        for(AvailabilityResponse a : availability) {
            System.out.println(a.getSlot() + " |    " +(a.getCapacity() != 0 ? a.getCapacity() : "X") + "    |    " + a.getPending() + "    |    " + a.getConfirmed() + "    | " + a.getAttraction());
        }

    }

    public static void printRidesReply(int expected, int actual){
        if(actual != expected){
            System.out.println("Cannot add " + (expected-actual) + " attractions.");
        }
        if(actual != 0){
            System.out.println(actual + " attractions added.");
        }
    }

    public static void printTicketsReply(int expected, int actual){
        if(actual != expected){
            System.out.println("Cannot add " + (expected-actual) + " passes.");
        }
        if(actual != 0){
            System.out.println(actual + " passes added.");
        }
    }
}
