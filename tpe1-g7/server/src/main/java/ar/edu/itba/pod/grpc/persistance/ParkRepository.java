package ar.edu.itba.pod.grpc.persistance;

import ar.edu.itba.pod.grpc.requests.PassType;
import ar.edu.itba.pod.grpc.models.Attraction;
import ar.edu.itba.pod.grpc.models.AttractionPass;
import ar.edu.itba.pod.grpc.models.Reservation;
import ar.edu.itba.pod.grpc.models.ReservationStatus;
import ar.edu.itba.pod.grpc.requests.QueryCapacityModel;
import ar.edu.itba.pod.grpc.requests.QueryConfirmedModel;
import ar.edu.itba.pod.grpc.requests.SlotsReplyModel;
import org.checkerframework.checker.units.qual.K;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static ar.edu.itba.pod.grpc.models.ReservationStatus.*;

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
        printRepositoryStatus();
        return updateReservations(name, day, capacity);
    }

    public synchronized Attraction addRide(Attraction att){
        attractions.add(att);
        reservations.put(att.getName(), new ArrayList<>());
        printRepositoryStatus();
        return att;
    }

    public synchronized boolean addPass(AttractionPass pass){
        passes.add(pass);
        printRepositoryStatus();
        return true;
    }

    public Attraction getAttractionByName(String name){
        Optional<Attraction> att = attractions.stream().filter(a -> name.equals(a.getName())).findFirst();
        return att.orElse(null);

    }

    public boolean isValidSlot(String attraction, LocalTime slot) {
        Attraction att = getAttractionByName(attraction);
        return !slot.isBefore(att.getOpening()) && !slot.isAfter(att.getClosing());
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

    public List<LocalTime> getSlotsInterval(String name, List<String> slots) {

        Attraction attraction = getAttractionByName(name);
        List<LocalTime> finalSlots = new ArrayList<>();

        if(slots.size() == 1) {
            finalSlots.add(attraction.getSlot(LocalTime.parse(slots.get(0))));
        } else {
            int i = 0;
            LocalTime opening = LocalTime.parse(slots.get(0));
            LocalTime closing = LocalTime.parse(slots.get(1));
            int minsPerSlot = attraction.getMinsPerSlot();
            while((opening.plusMinutes((long) minsPerSlot * i)).isBefore(closing) || (opening.plusMinutes((long) minsPerSlot * i)).equals(closing)){
                finalSlots.add(opening.plusMinutes(minsPerSlot * i));
                i++;
            }
        }

        return finalSlots;

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
        printRepositoryStatus();
        return true;

    }

    public int getReservations(String attraction, int day, LocalTime slot, ReservationStatus status) {
        List<Reservation> reservationsForAttraction = reservations.get(attraction);
        return (int) reservationsForAttraction.stream().filter(a ->  a.getDay() == day && a.getSlot().equals(slot) && a.getStatus() == status).count();
    }

    public List<QueryCapacityModel> getPendingReservationsByDay(int day) {
        List<Attraction> attractionList = getAttractions();
        List<Reservation> PRDay;
        List<QueryCapacityModel> capacityList = new ArrayList<>();
        for (Attraction attr : attractionList) {
            if (!attractionHasCapacityAlready(attr.getName(), day)) {
                PRDay = reservations.get(attr.getName()).stream().filter(a -> a.getDay() == day && a.getStatus() == PENDING).toList();
                for (Reservation reservation : PRDay) {
                    Map<LocalTime, Long> acc = PRDay.stream().collect(
                            Collectors.groupingBy(Reservation::getSlot, Collectors.counting())
                    );
                    LocalTime maxSlot = acc.entrySet().stream().max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey).orElse(null);
                    QueryCapacityModel capacityModel = QueryCapacityModel.newBuilder()
                            .setSlot(maxSlot.toString())
                            .setCapacity(acc.get(maxSlot).intValue())
                            .setAttraction(reservation.getAttractionName())
                            .build();
                    capacityList.add(capacityModel);
                }
            }
        }
        capacityList.sort((o1, o2) -> {
            int diff = o2.getCapacity() - o1.getCapacity();
            if(diff == 0)
                diff = o1.getAttraction().compareTo(o2.getAttraction());
            return diff;
        });
        return capacityList;
    }

    public List<QueryConfirmedModel> getConfirmedReservationsByDay(int day) {
        List<Attraction> attractionList = getAttractions();
        List<Reservation> CRDay;
        List<QueryConfirmedModel> confirmedList = new ArrayList<>();
        for (Attraction attr : attractionList) {
            CRDay = reservations.get(attr.getName()).stream().filter(a -> a.getDay() == day && a.getStatus() == CONFIRMED).toList();
            for (Reservation reservation : CRDay) {
                QueryConfirmedModel capacityModel = QueryConfirmedModel.newBuilder()
                        .setSlot(reservation.getSlot().toString())
                        .setVisitor(reservation.getVisitorId().toString())
                        .setAttraction(reservation.getAttractionName())
                        .build();
                confirmedList.add(capacityModel);
            }
        }
        confirmedList.sort((o1, o2) -> {
            int diff = o1.getAttraction().compareTo(o2.getAttraction());
            if(diff == 0) {
                diff = LocalTime.parse(o1.getSlot()).compareTo(LocalTime.parse(o2.getSlot()));
                if ((diff) == 0)
                    diff = o1.getVisitor().compareTo(o2.getVisitor());
            }
            return diff;
        });
        return confirmedList;
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

        for(Reservation res : attReservs)
            System.out.println(res.getDay() + " - " + res.getSlot());

        //Tengo las reservas del día que todavía estan pendientes (es decir, las otras que estaban para ese dia
        // estan confirmadas o canceladas).
        // Ahora, tengo que confirmar las primeras N que llegaron
        Map<LocalTime, Integer> capacities = getAttractionByName(name).getSpaceAvailable().get(day);

        for(Reservation r : attReservs){
            boolean updated = false;
            LocalTime prevSlot = r.getSlot();
            int vacants = capacities.get(prevSlot);

            if(vacants > 0){
                // Tengo lugar para la reserva pedida. Confirmo, y bajo capacidad en mapa
                r.setStatus(ReservationStatus.CONFIRMED);
                capacities.put(prevSlot, capacities.get(r.getSlot()) - 1);
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

    public int confirmReservation(String attraction, int day, LocalTime slot, UUID visitorId) {
        return setReservationStatus(attraction, day, slot, visitorId, new ArrayList<>(List.of(PENDING)), CONFIRMED);
    }

    public int cancelReservation(String attraction, int day, LocalTime slot, UUID visitorId) {
        return setReservationStatus(attraction, day, slot, visitorId, new ArrayList<>(List.of(PENDING, CONFIRMED)), CANCELLED);
    }

    private int setReservationStatus(String attraction, int day, LocalTime slot, UUID visitorId,
                                         List<ReservationStatus> fromStatus, ReservationStatus toStatus) {

        List<Reservation> reservs = reservations.get(attraction);
        if(reservs.isEmpty())
            return -1; //si no hay reservas para esa atracción, falla

        int found = 0; //si hay al menos una, devuelvo true

        for(Reservation r : reservs) {
            if(r.getDay() == day && r.getSlot().equals(slot) && r.getVisitorId().equals(visitorId) && fromStatus.contains(r.getStatus())) {
                r.setStatus(toStatus);
                found++;
            }
        }
        printRepositoryStatus();
        return found;
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

    public void printRepositoryStatus(){
        System.out.println("Attractions\n");
        for(Attraction a : attractions){
            System.out.println(a.getName() + " | " + a.getOpening() + " | " + a.getMinsPerSlot());
            for(Map.Entry<Integer, Integer> i : a.getCapacities().entrySet()){
                System.out.println("Day " + i.getKey() + ", Value " + i.getValue());
            }
        }
        System.out.println("\nPasses\n");
        for(AttractionPass p : passes){
            System.out.println(p.getDay() + " | " + p.getType() + " | " + p.getVisitor());
        }
        System.out.println("\nReservations\n");
        for(List<Reservation> r : reservations.values()){
            for(Reservation p : r)
                System.out.println(p.getStatus() + " | " + p.getDay() + " | " + p.getVisitorId());
        }
    }

}
