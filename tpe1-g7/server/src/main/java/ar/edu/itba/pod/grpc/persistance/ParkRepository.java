package ar.edu.itba.pod.grpc.persistance;

import ar.edu.itba.pod.grpc.requests.PassType;

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

    public List<Attraction> getAttractions() {
        return attractions;
    }

    public boolean addReservation(Reservation reservation) {

        List<Reservation> reservationsForAttraction = reservations.get(reservation.getAttractionName());

        if(reservationsForAttraction.contains(reservation)) {
            return false;
        }

        reservationsForAttraction.add(reservation);
        reservations.put(reservation.getAttractionName(), reservationsForAttraction);

        //TODO: Bajar la capacidad del slot en 1
        return true;

    }

    public boolean visitorCanVisit(UUID id, int day, LocalTime slot) {

        Optional<AttractionPass> pass = passes.stream().filter(a -> a.getVisitor().equals(id) && a.getDay() == day).findFirst();

        if(pass.isEmpty())
            return false;

        PassType type = pass.get().getType();

        switch (type) {
            case THREE:
                if(pass.get().getRemaining() <= 0)
                    return false;
                break;
            case HALF_DAY:
                if(slot.isAfter(LocalTime.of(14, 0)))
                    return false;
                break;
        }

        return true;

    }

    public int getRemainingCapacity(String name, int day, LocalTime slot) {
        //TODO!!
        return 0;
    }



}
