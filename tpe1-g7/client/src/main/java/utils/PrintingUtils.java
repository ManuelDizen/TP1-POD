package utils;

import ar.edu.itba.pod.grpc.requests.SlotsReplyModel;

public class PrintingUtils {

    public static void printSlotsReply(SlotsReplyModel model){
        if(model.getConfirmed() != 0)
            System.out.println(model.getConfirmed() + " confirmed without changes.\n");
        if(model.getRelocated() != 0)
            System.out.println(model.getRelocated() + " bookings relocated.\n");
        if(model.getCancelled() != 0)
            System.out.println(model.getCancelled() + " bookings cancelled.");
    }
}
