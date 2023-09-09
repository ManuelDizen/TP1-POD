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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryRequestsServant extends QueryRequestsServiceGrpc.QueryRequestsServiceImplBase {
    private final static Logger logger = LoggerFactory.getLogger(QueryRequestsServant.class);
    private final ParkRepository repository = ParkRepository.getRepository();

    //TODO: where to sort??
    @Override
    public void getCapacityRequest(QueryRequestModel request, StreamObserver<QueryCapacityModel> responseObserver) {
        int day = request.getDay();
        if(day < 1 || day > 365) {
            responseObserver.onError(Status.INTERNAL.withDescription("Invalid Day").asRuntimeException());
        }
        List<Reservation> reservationsInDay = repository.getPendingReservationsByDay(day);
        for (Reservation reservation : reservationsInDay) {
            Map<LocalTime, Long> acc = reservationsInDay.stream().collect(
                    Collectors.groupingBy(Reservation::getSlot, Collectors.counting())
            );
            //TODO: slots con mismas capacities, me quedo con el mas temprano??
            LocalTime maxSlot = acc.entrySet().stream().max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse(null);
            QueryCapacityModel capacityModel = QueryCapacityModel.newBuilder()
                    .setSlot(maxSlot.toString())
                    .setCapacity(acc.get(maxSlot).intValue())
                    .setAttraction(reservation.getAttractionName())
                    .build();
            responseObserver.onNext(capacityModel);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getConfirmedRequest(QueryRequestModel request, StreamObserver<QueryConfirmedModel> responseObserver) {
        int day = request.getDay();
        if(day < 1 || day > 365) {
            responseObserver.onError(Status.INTERNAL.withDescription("Invalid Day").asRuntimeException());
        }
        List<Reservation> reservationsInDay = repository.getConfirmedReservationsByDay(day);
        for (Reservation reservation : reservationsInDay) {
            QueryConfirmedModel capacityModel = QueryConfirmedModel.newBuilder()
                    .setSlot(reservation.getSlot().toString())
                    .setVisitor(reservation.getVisitorId().toString())
                    .setAttraction(reservation.getAttractionName())
                    .build();
            responseObserver.onNext(capacityModel);
        }
        responseObserver.onCompleted();
    }
}
