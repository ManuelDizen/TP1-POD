package ar.edu.itba.pod.grpc.servants;

import ar.edu.itba.pod.grpc.requests.AdminRequestsServiceGrpc;
import ar.edu.itba.pod.grpc.requests.SlotsRequestModel;
import com.google.protobuf.Int32Value;
import io.grpc.stub.StreamObserver;

public class AdminRequestsServant extends AdminRequestsServiceGrpc.AdminRequestsServiceImplBase{

    @Override
    public void addSlotsRequest(SlotsRequestModel request,
                                StreamObserver<Int32Value> responseObserver){
        // System.out.println("Hola llegue a addSlotsRequest!!!\n");
        int day = request.getDay();
        int capacity = request.getCapacity();
        String name = request.getRide();

        if(day < 1 || day > 365){
            //TODO El resto de las validaciones
            System.out.println("Malisimo abz\n");
        }

        responseObserver.onNext(Int32Value.of(1));
        responseObserver.onCompleted();

        // addSlot(day, capacity, name);



    }

}
