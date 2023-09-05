package ar.edu.itba.pod.grpc.servants;

import ar.edu.itba.pod.grpc.persistance.ParkRepository;
import ar.edu.itba.pod.grpc.requests.BookingRequestsServiceGrpc;
import ar.edu.itba.pod.grpc.requests.RidesRequestModel;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookingRequestsServant extends BookingRequestsServiceGrpc.BookingRequestsServiceImplBase {

    private final static Logger logger = LoggerFactory.getLogger(BookingRequestsServant.class);

    private final ParkRepository repository = ParkRepository.getRepository();

    @Override
    public void getAttractionsRequest(Empty request,
                                      StreamObserver<RidesRequestModel> responseObserver){

    }


}
