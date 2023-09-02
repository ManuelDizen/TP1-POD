package ar.edu.itba.pod.grpc.servants;

import ar.edu.itba.pod.grpc.requests.AdminRequestsServiceGrpc;
import ar.edu.itba.pod.grpc.requests.PassType;
import ar.edu.itba.pod.grpc.requests.SlotsRequestModel;
import ar.edu.itba.pod.grpc.requests.TicketsRequestModel;
import com.google.protobuf.Int32Value;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.UUID;

public class AdminRequestsServant extends AdminRequestsServiceGrpc.AdminRequestsServiceImplBase{

    @Override
    public void addSlotsRequest(SlotsRequestModel request,
                                StreamObserver<Int32Value> responseObserver){
        System.out.println("Hola llegue a addSlotsRequest!!!\n");
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
    @Override
    public void addTicketsRequest(TicketsRequestModel request,
                                  StreamObserver<Int32Value> responseObserver){
        int day = request.getDay();
        UUID id = UUID.fromString(request.getId());
        PassType type = request.getType();

        //TODO: Validations and persistance
        System.out.println("Llegue a addTicketsRequest.");

        responseObserver.onNext(Int32Value.of(1));
        responseObserver.onCompleted();


    }

}
