package ar.edu.itba.pod.grpc.persistance;

import ar.edu.itba.pod.grpc.requests.PassType;
import org.checkerframework.checker.units.qual.A;

import java.time.LocalTime;
import java.util.*;

public class ParkRepository {
    private final List<Attraction> attractions = new ArrayList<>();
    private final List<AttractionPass> passes = new ArrayList<>();
    private final Map<String, List<Reservation>> reservations = new HashMap<>();
    private static ParkRepository repository;

    public static ParkRepository getRepository(){
        if(repository == null){
            repository = new ParkRepository();
        }
        return repository;
    }

    public boolean attractionExists(String name){
        return attractions.stream().anyMatch(a -> name.equals(a.getName()));
    }

    public boolean attractionHasCapacityAlready(String name, int day){
        if(attractionExists(name)){
            Optional<Attraction> att = attractions.stream().filter(a -> name.equals(a.getName())).findFirst();
            return att.map(attraction -> attraction.getCapacities().containsKey(day)).orElse(false);
        }
        return false;
    }

    //Con att, dia y capacidad, genera la capcaidad para todos los slots de ese día. Hace falta validar
    // las reservas en espera, y confirmarlas/cancelarlas/reubicarlas.
    public synchronized boolean addSlots(String name, int day, int capacity){
        Attraction att = getAttractionByName(name);
        att.getCapacities().put(day, capacity); //Validations have been done so that attraction does not have capacity yet that day
        // updateReservations(name, day, capacity); //TODO!!!
        return true;
    }

    public synchronized Attraction addRide(Attraction att){
        attractions.add(att);
        return att;
    }

    public synchronized boolean addPass(AttractionPass pass){
        passes.add(pass);
        return true;
    }

    private Attraction getAttractionByName(String name){
        Optional<Attraction> att = attractions.stream().filter(a -> name.equals(a.getName())).findFirst();
        return att.orElse(null);

    }

    public boolean visitorHasPass(UUID id, int day){
        return passes.stream().anyMatch(a -> a.getVisitor().equals(id) && a.getDay()==day);
    }

}
