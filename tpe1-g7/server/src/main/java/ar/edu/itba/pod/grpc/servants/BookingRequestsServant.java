package ar.edu.itba.pod.grpc.servants;

import ar.edu.itba.pod.grpc.models.Attraction;
import ar.edu.itba.pod.grpc.persistance.ParkRepository;
import ar.edu.itba.pod.grpc.models.Reservation;
import ar.edu.itba.pod.grpc.models.ReservationStatus;
import ar.edu.itba.pod.grpc.requests.BookRequestModel;
import ar.edu.itba.pod.grpc.requests.BookingRequestsServiceGrpc;
import ar.edu.itba.pod.grpc.requests.RidesRequestModel;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class BookingRequestsServant extends BookingRequestsServiceGrpc.BookingRequestsServiceImplBase {

    private final static Logger logger = LoggerFactory.getLogger(BookingRequestsServant.class);

    private final ParkRepository repository = ParkRepository.getRepository();

    @Override
    public void getAttractionsRequest(Empty request,
                                      StreamObserver<RidesRequestModel> responseObserver){

        //busco las attractions del repository, y las devuelvo

        List<Attraction> attractions = repository.getAttractions();

        for(Attraction att : attractions) {
            RidesRequestModel ride = RidesRequestModel.newBuilder().setName(att.getName())
                    .setOpening(att.getOpening())
                    .setClosing(att.getClosing())
                    .setMinsPerSlot(att.getMinsPerSlot())
                    .build();
            responseObserver.onNext(ride);
        }
        responseObserver.onCompleted();

    }

    /*@Override
    public void BookingRequest(BookRequestModel request, StreamObserver<Int32Value> responseObserver) {

        int day = request.getDay();
        UUID id = UUID.fromString(request.getId());
        String attraction = request.getName();
        LocalTime slot = LocalTime.parse(request.getTime());

        repository.addReservation(new Reservation(attraction, day, id, slot, ReservationStatus.PENDING));

    }*/




}
