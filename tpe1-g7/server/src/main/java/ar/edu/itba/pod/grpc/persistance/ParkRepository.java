package ar.edu.itba.pod.grpc.persistance;

import ar.edu.itba.pod.grpc.requests.PassType;
import ar.edu.itba.pod.grpc.models.Attraction;
import ar.edu.itba.pod.grpc.models.AttractionPass;
import ar.edu.itba.pod.grpc.models.Reservation;
import ar.edu.itba.pod.grpc.models.ReservationStatus;
import ar.edu.itba.pod.grpc.requests.SlotsReplyModel;

import java.time.LocalTime;
import java.util.*;

import static ar.edu.itba.pod.grpc.models.ReservationStatus.CONFIRMED;
import static ar.edu.itba.pod.grpc.models.ReservationStatus.PENDING;

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
            return att.map(attraction -> attraction.getSpaceAvailable().containsKey(day)).orElse(false);
        }
        return false;
    }

    //Con att, dia y capacidad, genera la capcaidad para todos los slots de ese día. Hace falta validar
    // las reservas en espera, y confirmarlas/cancelarlas/reubicarlas.
    public synchronized SlotsReplyModel addSlots(String name, int day, int capacity){
        Attraction att = getAttractionByName(name);
        att.getCapacities().put(day, capacity); //Validations have been done so that attraction does not have capacity yet that day
        att.initializeSlots(day, capacity);
        updateReservations(name, day, capacity);
        return updateReservations(name, day, capacity);
    }

    public synchronized Attraction addRide(Attraction att){
        attractions.add(att);
        reservations.put(att.getName(), new ArrayList<>());
        return att;
    }

    public synchronized boolean addPass(AttractionPass pass){
        passes.add(pass);
        return true;
    }

    public Attraction getAttractionByName(String name){
        Optional<Attraction> att = attractions.stream().filter(a -> name.equals(a.getName())).findFirst();
        return att.orElse(null);

    }

    public boolean visitorHasPass(UUID id, int day){
        return isValidDay(day) &&
                passes.stream().anyMatch(a -> a.getVisitor().equals(id) && a.getDay()==day);
    }

    public List<Attraction> getAttractions() {
        return attractions;
    }

    private void manageNotifications(Reservation reservation){
        Attraction a = getAttractionByName(reservation.getAttractionName());
        if(a.checkToNotify(reservation))
            a.notifyReservation(reservation.getDay(), reservation.getVisitorId(), reservation);
    }

    private void manageNotifications(Reservation reservation, String message){
        Attraction a = getAttractionByName(reservation.getAttractionName());
        if(a.checkToNotify(reservation))
            a.notifyReservation(reservation.getDay(), reservation.getVisitorId(),
                    reservation, message);
    }

    public boolean addReservation(Reservation reservation) {

        if(reservation.getStatus() == PENDING) {
            List<Reservation> pendingReservations = reservations.get(reservation.getAttractionName());
            if(pendingReservations.contains(reservation)) {
                return false;
            }
            pendingReservations.add(reservation);
            reservations.put(reservation.getAttractionName(), pendingReservations);
            manageNotifications(reservation);

        } else if(reservation.getStatus() == CONFIRMED) {
            //me busco el mapa Map<Horario, Capacidad> de la atracción para ese día
            Map<LocalTime, Integer> slots = repository.getAttractionByName(reservation.getAttractionName()).getSpaceAvailable().get(reservation.getDay());
            Integer capacity = slots.get(reservation.getSlot());
            if(capacity < 0) {
                return false;
            }
            capacity--;
            slots.put(reservation.getSlot(), capacity);
            manageNotifications(reservation);
        }

        return true;

    }

    private SlotsReplyModel updateReservations(String name, int day, int capacity){
        int confirmed = 0, cancelled = 0, relocated = 0;

        //Method called when an attraction receives a capacity so that it confirms/denies/relocates reservations
        List<Reservation> attReservs = new ArrayList<>(reservations.get(name).stream()
                .filter(r -> r.getDay() == day && r.getStatus() == PENDING)
                .toList());
        attReservs.sort((new Comparator<Reservation>() {
            @Override
            public int compare(Reservation o1, Reservation o2) {
                return o1.getCreatedAt().compareTo(o2.getCreatedAt());
            }
        }));

        //Tengo las reservas del día que todavía estan pendientes (es decir, las otras que estaban para ese dia
        // estan confirmadas o canceladas).
        // Ahora, tengo que confirmar las primeras N que llegaron
        Map<LocalTime, Integer> capacities = getAttractionByName(name).getSpaceAvailable().get(day);
        for(Reservation r : attReservs){
            boolean updated = false;
            LocalTime prevSlot = r.getSlot();
            int vacants = capacities.get(r.getSlot());
            if(vacants > 0){
                // Tengo lugar para la reserva pedida. Confirmo, y bajo capacidad en mapa
                r.setStatus(ReservationStatus.CONFIRMED);
                capacities.put(r.getSlot(), capacities.get(r.getSlot()) - 1);
                confirmed++;
            }
            else{
                // Si no hay lugar, tengo que buscar el primer slot disponible posterior a este para confirmarla
                // Si no encuentro slot, la cancelo
                LocalTime firstAvailable = null;
                TreeSet<LocalTime> keySet = new TreeSet<>(capacities.keySet());
                List<LocalTime> orderedKeys = new ArrayList<>(keySet).stream()
                        .filter(a -> a.isAfter(r.getSlot())) // No testeo el == porque con vacants lo chequee
                        .toList();
                for(LocalTime t : orderedKeys){ //TODO: Debería hacerlo pero corroborar que itere en orden
                    if(capacities.get(t) > 0){
                        firstAvailable = t;
                        break;
                    }
                }
                if(firstAvailable != null){
                    //TODO IMPORTANTE: Si pase es de medio dia, y la reserva se mueve para después, SE CANCELA
                    r.setStatus(PENDING);
                    r.setSlot(firstAvailable);
                    capacities.put(firstAvailable, capacities.get(firstAvailable)-1);
                    relocated++;
                    updated = true;

                }
                else{
                    r.setStatus(ReservationStatus.CANCELLED); // No hay espacio en ningún slot, se cancela
                    cancelled++;
                }
            }
            if(updated){
                manageNotifications(r, "The reservation for " + r.getAttractionName() + " at "
                + prevSlot.toString() + " on the day " + day + " was moved to " + r.getSlot().toString()
                + " and is " + r.getStatus().toString());
            }
            else{
                manageNotifications(r);
            }
        }
        return SlotsReplyModel.newBuilder()
                .setCancelled(cancelled)
                .setConfirmed(confirmed)
                .setRelocated(relocated)
                .build();
    }

    public boolean visitorCanVisit(UUID id, int day, LocalTime slot) {

        Optional<AttractionPass> pass = passes.stream().filter(a -> a.getVisitor().equals(id) && a.getDay() == day)
                .findFirst();

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

    public boolean isValidDay(int day){
        return day > 0 && day <= 365;
    }

}
