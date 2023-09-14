package ar.edu.itba.pod.grpc.persistance;

import ar.edu.itba.pod.grpc.requests.*;
import ar.edu.itba.pod.grpc.models.Attraction;
import ar.edu.itba.pod.grpc.models.AttractionPass;
import ar.edu.itba.pod.grpc.models.Reservation;
import ar.edu.itba.pod.grpc.models.ReservationStatus;


import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static ar.edu.itba.pod.grpc.models.ReservationStatus.*;
import static ar.edu.itba.pod.grpc.requests.PassType.HALF_DAY;
import static ar.edu.itba.pod.grpc.requests.PassType.THREE;
import static ar.edu.itba.pod.grpc.utils.LockUtils.*;

public class ParkRepository {
    private final List<Attraction> attractions = new ArrayList<>();
    private final List<AttractionPass> passes = new ArrayList<>();
    private final Map<String, List<Reservation>> reservations = new HashMap<>();
    private static ParkRepository repository;
    private static final ReadWriteLock attrLock = new ReentrantReadWriteLock(fairness4locks);
    private static final ReadWriteLock passLock = new ReentrantReadWriteLock(fairness4locks);
    private static final ReadWriteLock reservsLock = new ReentrantReadWriteLock(fairness4locks);


    public static ParkRepository getRepository() {
        if (repository == null) {
            repository = new ParkRepository();
        }
        return repository;
    }

    public Attraction getAttractionByName(String name) {
        lockRead(attrLock);
        Optional<Attraction> att = attractions.stream().filter(a -> name.equals(a.getName())).findFirst();
        unlockRead(attrLock);
        return att.orElse(null);
    }

    public boolean attractionExists(String name) {
        lockRead(attrLock);
        boolean exists = attractions.stream().anyMatch(a -> name.equals(a.getName()));
        unlockRead(attrLock);
        return exists;
    }

    //FUNCIONES ADMIN:


    public Attraction addRide(Attraction att) throws RuntimeException {
        lockWrite(attrLock);

        if (attractions.contains(att)) {
            unlockWrite(attrLock);
            throw new RuntimeException("Attraction already exists TOTO");
        }

        attractions.add(att);

        unlockWrite(attrLock);

        lockWrite(reservsLock);
        reservations.put(att.getName(), new ArrayList<>());
        unlockWrite(reservsLock);

        return att;
    }

    public boolean addPass(AttractionPass pass) throws RuntimeException {
        lockWrite(passLock);
        if (passes.stream().anyMatch(a ->
                a.getVisitor().equals(pass.getVisitor()) && a.getDay() == pass.getDay())) {
            throw new RuntimeException("Visitor " + pass.getVisitor() + " already has pass for day "
                    + pass.getDay() + ".");
        }
        passes.add(pass);
        unlockWrite(passLock);
        return true;
    }

    //Con att, dia y capacidad, genera la capcaidad para todos los slots de ese día. Hace falta validar
    // las reservas en espera, y confirmarlas/cancelarlas/reubicarlas.
    public SlotsReplyModel addSlots(String name, int day, int capacity) throws RuntimeException {
        Attraction att = getAttractionByName(name);
        if (att == null)
            throw new RuntimeException("Attraction does not exist");
        try {
            att.initializeSlots(day, capacity);
        } catch (RuntimeException e) {
            throw e;
        }
        return updateReservations(name, day); //tengo que ver si habían reservas pendientes y manejarlas
    }

    private SlotsReplyModel updateReservations(String name, int day) {
        int confirmed = 0, cancelled = 0, relocated = 0;

        // Siendo que este método es literalmente eterno, voy a documentar paso a paso como lo sincronice
        // (1) Necesito que, cuando este actualizando, no se sumen reervas nuevas (ni que queden pendientes ni se ocnfirmen)
        // Por ende tomo el writeLock hasta que no use mas la colección.

        lockWrite(reservsLock);

        List<Reservation> attReservs = new ArrayList<>(reservations.get(name).stream()
                .filter(r -> r.getDay() == day && r.getStatus() == PENDING)
                .toList());

        attReservs.sort((new Comparator<Reservation>() {
            @Override
            public int compare(Reservation o1, Reservation o2) {
                return o1.getCreatedAt().compareTo(o2.getCreatedAt());
            }
        }));


        // (2) Acá se está haciendo un acceso directo a los spacesAvailable (mal). Toca modificarlo,
        // y además cuidar que mientras analice las reservas pendientes no me confirmen cambien ninguna
        // (recordar que holdeo lock de reservas)

        Attraction attr = getAttractionByName(name);

        // Para esto, creo método que permite acceder a la instancia de la atracción, y holdear el lock de escritura
        Map<LocalTime, Integer> capacities = attr.lockWriteAndGetSpacesAvailableForDay(day);
        // IMPORTANTE: Recordar que acá estoy holdeando el lock de spacesavailable. No se puede adulterar
        // Sigo holdeando el lock de reservsLock

        for (Reservation r : attReservs) {
            boolean updated = false;
            LocalTime prevSlot = r.getSlot();
            int vacants = capacities.get(prevSlot);

            if (vacants > 0) {
                // Tengo lugar para la reserva pedida. Confirmo, y bajo capacidad en mapa
                confirmReservation(r);
                capacities.put(prevSlot, capacities.get(r.getSlot()) - 1);
                r.setReserved(true);
                //Puedo hacer la línea de arriba xq holdeo el lock
                confirmed++;
            } else {
                // Si no hay lugar, tengo que buscar el primer slot disponible posterior a este para confirmarla
                // Si no encuentro slot, la cancelo
                AttractionPass pass = getAttractionPass(r.getVisitorId(), r.getDay()).get();
                LocalTime firstAvailable = null;
                TreeSet<LocalTime> keySet = new TreeSet<>(capacities.keySet());
                List<LocalTime> orderedKeys = new ArrayList<>(keySet).stream()
                        .filter(a -> a.isAfter(r.getSlot())) // No testeo el == porque con vacants lo chequee
                        .toList();
                for (LocalTime t : orderedKeys) {
                    if (pass.getType() == HALF_DAY && t.isAfter(LocalTime.of(14, 0))) {
                        cancelReservation(r, pass);
                        break;
                    }
                    if (capacities.get(t) > 0) {
                        firstAvailable = t;
                        break;
                    }
                }
                if (firstAvailable != null) {
                    r.setStatus(PENDING);
                    r.setSlot(firstAvailable);
                    capacities.put(firstAvailable, capacities.get(firstAvailable) - 1);
                    r.setReserved(true);
                    relocated++;
                    updated = true;
                } else {
                    cancelReservation(r, pass);
                    cancelled++;
                }
            }
            if (updated) {
                manageNotifications(r, "The reservation for " + r.getAttractionName() + " at "
                        + prevSlot.toString() + " on the day " + day + " was moved to " + r.getSlot().toString()
                        + " and is " + r.getStatus().toString());
            } else {
                manageNotifications(r);
            }
        }

        attr.freeLockWriteForSpacesAvailable(); //Suelto candado de spaces available
        unlockWrite(reservsLock); //Suelto el 2do lock que tenía holdeado.

        //Cuando lean, revisen que no haya referencias en algún llamado en el medio para algún método que necesite usar
        // reservas o spacesAvailable.
        return SlotsReplyModel.newBuilder()
                .setCancelled(cancelled)
                .setConfirmed(confirmed)
                .setRelocated(relocated)
                .build();
    }

    private Optional<AttractionPass> getAttractionPass(UUID visitorId, int day) {
        lockRead(passLock);
        Optional<AttractionPass> pass = passes.stream().filter(a -> a.getVisitor().equals(visitorId) && a.getDay() == day)
                .findFirst();
        unlockRead(passLock);
        return pass;
    }

    private void confirmReservation(Reservation reservation) {
        reservation.setStatus(CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
    }

    private void cancelReservation(Reservation reservation, AttractionPass pass) {
        reservation.setStatus(CANCELLED);// No hay espacio en ningún slot, se cancela;
        if (pass.getType() == THREE)
            pass.cancelConsumption();
    }

    //FUNCIONES BOOKING:
    public List<Attraction> getAttractions() {
        lockRead(attrLock);
        List<Attraction> atts = attractions;
        unlockRead(attrLock);
        return atts;
    }


    public List<AvailabilityResponse> getAvailability(String name, int day, List<LocalTime> slots) {

        Attraction att = getAttractionByName(name);

        int capacity = att.getCapacityForDay(day);

        List<AvailabilityResponse> responseList = new ArrayList<>();

        lockWrite(reservsLock);
        for (LocalTime s : slots) {

            int pending = getReservations(name, day, s, PENDING);
            int confirmed = getReservations(name, day, s, CONFIRMED);

            AvailabilityResponse response = AvailabilityResponse.newBuilder().setAttraction(name)
                    .setSlot(String.valueOf(s))
                    .setCapacity(capacity)
                    .setPending(pending)
                    .setConfirmed(confirmed).build();
            responseList.add(response);
        }
        unlockWrite(reservsLock);

        return responseList;
    }

    public int getReservations(String attraction, int day, LocalTime slot, ReservationStatus status) {
        lockRead(reservsLock);
        List<Reservation> reservationsForAttraction = reservations.get(attraction);
        int n = (int) reservationsForAttraction.stream()
                .filter(a -> a.getDay() == day && a.getSlot().equals(slot) && a.getStatus() == status).count();
        unlockRead(reservsLock);
        return n;
    }

    public boolean isValidSlot(String attraction, LocalTime slot) {
        Attraction att = getAttractionByName(attraction);
        if (!att.getSlot(slot).equals(slot))
            return false;
        return !slot.isBefore(att.getOpening()) && !slot.isAfter(att.getClosing());
    }

    public List<LocalTime> getSlotsInterval(String name, List<String> slots) {

        Attraction attraction = getAttractionByName(name);
        List<LocalTime> finalSlots = new ArrayList<>();

        if (slots.size() == 1) {
            finalSlots.add(attraction.getSlot(LocalTime.parse(slots.get(0))));
        } else {
            int i = 0;
            LocalTime opening = LocalTime.parse(slots.get(0));
            LocalTime closing = LocalTime.parse(slots.get(1));
            int minsPerSlot = attraction.getMinsPerSlot();
            while ((opening.plusMinutes((long) minsPerSlot * i)).isBefore(closing) || (opening.plusMinutes((long) minsPerSlot * i)).equals(closing)) {
                finalSlots.add(opening.plusMinutes(minsPerSlot * i));
                i++;
            }
        }

        return finalSlots;

    }

    public ResStatus addReservation(Reservation reservation) throws RuntimeException {

        ResStatus status;

        Attraction attraction = getAttractionByName(reservation.getAttractionName());

        lockWrite(reservsLock);

        Optional<AttractionPass> pass = getAttractionPass(reservation.getVisitorId(), reservation.getDay());
        if (pass.isEmpty())
            throw new RuntimeException("Visitor does not have pass for that day");

        if (!visitorCanVisit(reservation, pass.get()))
            throw new RuntimeException("Invalid pass");


        if (attraction.hasCapacityAlready(reservation.getDay())) {
            try {
                attraction.addReservation(reservation);
            } catch (RuntimeException e) {
                throw e;
            }
            reservation.setStatus(CONFIRMED);
            reservation.setReserved(true);
            status = ResStatus.CONFIRMED;
        } else {
            reservation.setStatus(PENDING);
            status = ResStatus.PENDING;
        }


        List<Reservation> reservs = reservations.get(reservation.getAttractionName());

        if (reservs.contains(reservation)) {
            unlockWrite(reservsLock);
            throw new RuntimeException("Reservation already exists");
        }
        reservs.add(reservation);

        reservations.put(reservation.getAttractionName(), reservs);

        unlockWrite(reservsLock);

        manageNotifications(reservation);

        return status;

    }


    public void confirmReservation(String name, int day, LocalTime slot, UUID visitorId) throws RuntimeException {

        Attraction attraction = getAttractionByName(name);

        lockWrite(reservsLock);

        if (!attraction.hasCapacityAlready(day)) {
            unlockWrite(reservsLock);
            throw new RuntimeException("No capacity yet for this day");
        }

        Optional<Reservation> r = getReservation(name, day, slot, visitorId);
        if (r.isEmpty() || r.get().getStatus() != PENDING) {
            unlockWrite(reservsLock);
            throw new RuntimeException("No pending reservations found");
        }

        Reservation reservation = r.get();

        Optional<AttractionPass> pass = getAttractionPass(reservation.getVisitorId(), reservation.getDay());
        if (pass.isEmpty())
            throw new RuntimeException("Visitor does not have pass for that day");

        reservation.setStatus(CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        unlockWrite(reservsLock);
        manageNotifications(reservation);
    }


    public void cancelReservation(String name, int day, LocalTime slot, UUID visitorId) throws RuntimeException {

        lockWrite(reservsLock);
        Optional<Reservation> r = getReservation(name, day, slot, visitorId);
        if (r.isEmpty()) {
            unlockWrite(reservsLock);
            throw new RuntimeException("No reservations found");
        }
        Reservation reservation = r.get();

        Optional<AttractionPass> pass = getAttractionPass(reservation.getVisitorId(), reservation.getDay());
        if (pass.isEmpty())
            throw new RuntimeException("Visitor does not have pass for that day");
        if (pass.get().getType() == THREE)
            pass.get().cancelConsumption();
        if (reservation.isReserved()) {

            Attraction att = getAttractionByName(name);

            att.cancelReservation(reservation);
            reservation.setReserved(false);
        }
        reservation.setStatus(CANCELLED);
        manageNotifications(reservation);
    }


    public Optional<Reservation> getReservation(String attraction, int day, LocalTime slot, UUID visitorId) {
        return reservations.get(attraction).stream().filter(a -> a.getDay() == day && a.getSlot().equals(slot) && a.getVisitorId().equals(visitorId)).findFirst();
    }


    //FUNCIONES NOTIF:

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

    private boolean visitorCanVisit(Reservation reservation, AttractionPass pass) {

        PassType type = pass.getType();

        switch (type) {
            case THREE:
                return pass.rideConsumption();
            case HALF_DAY:
                if(reservation.getSlot().isAfter(LocalTime.of(14, 0)))
                    return false;
                break;
        }
        return true;
    }

    public boolean visitorHasPass(UUID id, int day){
        lockRead(passLock);
        boolean valid = isValidDay(day) &&
                passes.stream().anyMatch(a -> a.getVisitor().equals(id) && a.getDay()==day);
        unlockRead(passLock);
        return valid;
    }

    public boolean isValidDay(int day){
        return day > 0 && day <= 365;
    }

    //FUNCIONES QUERY:
    public List<QueryCapacityModel> getPendingReservationsByDay(int day) {
        List<Attraction> attractionList = getAttractions();
        List<Reservation> PRDay;
        List<QueryCapacityModel> capacityList = new ArrayList<>();
        lockRead(reservsLock);
        for (Attraction attr : attractionList) {
            if (!attr.hasCapacityAlready(day)) {
                PRDay = reservations.get(attr.getName()).stream().filter(a -> a.getDay() == day && a.getStatus() == PENDING).toList();
                if(!PRDay.isEmpty()) {
                    Map<LocalTime, Long> acc = PRDay.stream().collect(
                            Collectors.groupingBy(Reservation::getSlot, Collectors.counting())
                    );
                    LocalTime maxSlot = acc.entrySet().stream().sorted(Map.Entry.comparingByKey())
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey).orElse(null);
                    QueryCapacityModel capacityModel = QueryCapacityModel.newBuilder()
                            .setSlot(maxSlot.toString())
                            .setCapacity(acc.get(maxSlot).intValue())
                            .setAttraction(attr.getName())
                            .build();
                    capacityList.add(capacityModel);
                } else {
                    QueryCapacityModel capacityModel = QueryCapacityModel.newBuilder()
                            .setSlot(attr.getOpening().toString())
                            .setCapacity(0)
                            .setAttraction(attr.getName())
                            .build();
                    capacityList.add(capacityModel);
                }
            }
        }
        unlockRead(reservsLock);
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
        List<Reservation> CRDay = new ArrayList<>();
        List<QueryConfirmedModel> confirmedList = new ArrayList<>();
        lockRead(reservsLock);
        for (Attraction attr : attractionList) {
            CRDay.addAll(reservations.get(attr.getName()).stream().filter(a -> a.getDay() == day && a.getStatus() == CONFIRMED).toList());
        }
        CRDay.sort((r1, r2) -> {
            int diff = r1.getConfirmedAt().compareTo(r2.getConfirmedAt());
            if(diff == 0) {
                diff = r1.getAttractionName().compareTo(r2.getAttractionName());
                if(diff == 0)
                    diff = r1.getVisitorId().compareTo(r2.getVisitorId());
            }
            return diff;
        });
        for (Reservation reservation : CRDay) {
            QueryConfirmedModel capacityModel = QueryConfirmedModel.newBuilder()
                    .setSlot(reservation.getSlot().toString())
                    .setVisitor(reservation.getVisitorId().toString())
                    .setAttraction(reservation.getAttractionName())
                    .build();
            confirmedList.add(capacityModel);
        }
        unlockRead(reservsLock);
        return confirmedList;
    }


}
