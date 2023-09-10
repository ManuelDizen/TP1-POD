package ar.edu.itba.pod.grpc.servants;

import ar.edu.itba.pod.grpc.models.Reservation;
import ar.edu.itba.pod.grpc.persistance.ParkRepository;
import ar.edu.itba.pod.grpc.requests.QueryCapacityModel;
import ar.edu.itba.pod.grpc.requests.QueryConfirmedModel;
import ar.edu.itba.pod.grpc.requests.QueryRequestModel;
import ar.edu.itba.pod.grpc.requests.QueryRequestsServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryRequestsServant extends QueryRequestsServiceGrpc.QueryRequestsServiceImplBase {
    private final static Logger logger = LoggerFactory.getLogger(QueryRequestsServant.class);
    private final ParkRepository repository = ParkRepository.getRepository();

    @Override
    public void getCapacityRequest(QueryRequestModel request, StreamObserver<QueryCapacityModel> responseObserver) {
        int day = request.getDay();
        if(day < 1 || day > 365) {
            responseObserver.onError(Status.INTERNAL.withDescription("Invalid Day").asRuntimeException());
        }
        List<QueryCapacityModel> capacityList = repository.getPendingReservationsByDay(day);
        capacityList.forEach(item -> {responseObserver.onNext(item);});
        responseObserver.onCompleted();
    }

    @Override
    public void getConfirmedRequest(QueryRequestModel request, StreamObserver<QueryConfirmedModel> responseObserver) {
        int day = request.getDay();
        if(day < 1 || day > 365) {
            responseObserver.onError(Status.INTERNAL.withDescription("Invalid Day").asRuntimeException());
        }
        List<QueryConfirmedModel> confirmedList = repository.getConfirmedReservationsByDay(day);
        confirmedList.forEach(item -> {responseObserver.onNext(item);});
        responseObserver.onCompleted();
    }
}
