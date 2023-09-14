package ar.edu.itba.pod.grpc.persistance;

import ar.edu.itba.pod.grpc.requests.*;
import ar.edu.itba.pod.grpc.models.Attraction;
import ar.edu.itba.pod.grpc.models.AttractionPass;
import ar.edu.itba.pod.grpc.models.Reservation;
import ar.edu.itba.pod.grpc.models.ReservationStatus;


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


    public static ParkRepository getRepository(){
        if(repository == null){
            repository = new ParkRepository();
        }
        return repository;
    }

    public boolean attractionExists(String name){
        lockRead(attrLock);
        boolean exists = attractions.stream().anyMatch(a -> name.equals(a.getName()));
        unlockRead(attrLock);
        return exists;
    }

    public boolean attractionHasCapacityAlready(String name, int day){
        boolean has = false;
        lockRead(attrLock);
        if(attractionExists(name)){
            Optional<Attraction> att = attractions.stream().filter(a -> name.equals(a.getName())).findFirst();
            if(att.isPresent()) has = att.get().hasCapacityForDay(day);
        }
        unlockRead(attrLock);
        return has;
    }

    //Con att, dia y capacidad, genera la capcaidad para todos los slots de ese día. Hace falta validar
    // las reservas en espera, y confirmarlas/cancelarlas/reubicarlas.
    public SlotsReplyModel addSlots(String name, int day, int capacity) throws RuntimeException{
        Attraction att = getAttractionByName(name);
        if(att == null) //TODO esto no hay chance que esté bien sincronizado, lo hago por devolver un error mas prolijo
            throw new RuntimeException("Attraction does not exist");
        try {
            att.initializeSlots(day, capacity);
        } catch (RuntimeException e) {
            throw e;
        }
        //TODO: ver si ese chequeo en el if está bien en términos de sync
        return updateReservations(name, day);
    }

    public Attraction addRide(Attraction att) throws RuntimeException{
        lockWrite(attrLock);

        if(attractions.contains(att)) {
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

    public boolean addPass(AttractionPass pass) throws RuntimeException{
        lockWrite(passLock);
        if(passes.stream().anyMatch(a ->
                a.getVisitor().equals(pass.getVisitor()) && a.getDay()==pass.getDay())){
            throw new RuntimeException("Visitor " + pass.getVisitor() + " already has pass for day "
                    + pass.getDay() + ".");
        }
        passes.add(pass);
        unlockWrite(passLock);
        return true;
    }

    public Attraction getAttractionByName(String name){
        lockRead(attrLock);
        Optional<Attraction> att = attractions.stream().filter(a -> name.equals(a.getName())).findFirst();
        unlockRead(attrLock);
        return att.orElse(null);

    }

    public boolean isValidSlot(String attraction, LocalTime slot) {
        Attraction att = getAttractionByName(attraction);
        return !slot.isBefore(att.getOpening()) && !slot.isAfter(att.getClosing());
    }

    public boolean visitorHasPass(UUID id, int day){
        lockRead(passLock);
        boolean valid = isValidDay(day) &&
                passes.stream().anyMatch(a -> a.getVisitor().equals(id) && a.getDay()==day);
        unlockRead(passLock);
        return valid;
    }

    public List<Attraction> getAttractions() {
        return attractions;
    }

    public void manageNotifications(Reservation reservation){
        Attraction a = getAttractionByName(reservation.getAttractionName());
        if(a.checkToNotify(reservation))
            a.notifyReservation(reservation.getDay(), reservation.getVisitorId(), reservation);
    }

    public void manageNotifications(Reservation reservation, String message){
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

    public ResStatus addReservation(Reservation reservation) throws RuntimeException {

        ResStatus status;

        Attraction attraction = getAttractionByName(reservation.getAttractionName());

        //TODO!!
        //chequeo si tiene pase válido
        AttractionPass pass = getAttractionPass(reservation.getVisitorId(), reservation.getDay());
        if(!pass.rideConsumption()) {
            throw new RuntimeException("Invalid pass");
        }

        if(attraction.hasCapacityAlready(reservation.getDay())) {
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

        //necesito el lock para escribir en la clase reservations
        lockWrite(reservsLock);

        List<Reservation> reservs = reservations.get(reservation.getAttractionName());

        if(reservs.contains(reservation)) {
            unlockWrite(reservsLock);
            throw new RuntimeException("Reservation already exists");
        }
        reservs.add(reservation);

        reservations.put(reservation.getAttractionName(), reservs);

        unlockWrite(reservsLock);

        manageNotifications(reservation);

        return status;

    }

    public AttractionPass getAttractionPass(UUID visitorId, int day) {
        lockRead(passLock);
        AttractionPass pass = passes.stream().filter(a -> a.getVisitor().equals(visitorId) && a.getDay() == day)
                .findFirst().orElseThrow();
        unlockRead(passLock);
        return pass;
    }

    public Reservation getReservation(String attraction, int day, LocalTime slot, UUID visitorId) {
        lockRead(reservsLock);
        Reservation r = reservations.get(attraction).stream().filter(a -> a.getDay() == day && a.getSlot().equals(slot) && a.getVisitorId().equals(visitorId)).findFirst().orElseThrow();
        unlockRead(reservsLock);
        return r; //TODO esta forma de hacerlo no se si me convence. Pienso en que uno podría:
        /*
        1) Lockea el read (nadie modifica)
        2) Consigue reserva
        3) Deslocka el read
        4) Inmediatamente después entra alguien a modificar la reserva y yo me quede con la referencia de la reserva con un estado
        cuando inmediatamente después puede cambiar. Es decir, no se si es algo solucionable, pero me deja pensando.
         */
    }

    public int getReservations(String attraction, int day, LocalTime slot, ReservationStatus status) {
        lockRead(reservsLock);
        List<Reservation> reservationsForAttraction = reservations.get(attraction);
        int n = (int) reservationsForAttraction.stream()
                .filter(a ->  a.getDay() == day && a.getSlot().equals(slot) && a.getStatus() == status).count();
        unlockRead(reservsLock);
        return n;
    }

    public List<QueryCapacityModel> getPendingReservationsByDay(int day) {
        List<Attraction> attractionList = getAttractions();
        List<Reservation> PRDay;
        List<QueryCapacityModel> capacityList = new ArrayList<>();
        lockRead(reservsLock);
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
        unlockRead(reservsLock);
        capacityList.sort((o1, o2) -> {
            int diff = o2.getCapacity() - o1.getCapacity();
            if(diff == 0)
                diff = o1.getAttraction().compareTo(o2.getAttraction());
            return diff;
        }); //TODO CAMBIAR POR ORDEN DE CONFIRMACIÓN
        return capacityList;
    }

    public List<QueryConfirmedModel> getConfirmedReservationsByDay(int day) {
        List<Attraction> attractionList = getAttractions();
        List<Reservation> CRDay;
        List<QueryConfirmedModel> confirmedList = new ArrayList<>();
        lockRead(reservsLock);
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
        unlockRead(reservsLock);
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

    private SlotsReplyModel updateReservations(String name, int day){
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

        for(Reservation res : attReservs)
            System.out.println(res.getDay() + " - " + res.getSlot());

        // (2) Acá se está haciendo un acceso directo a los spacesAvailable (mal). Toca modificarlo,
        // y además cuidar que mientras analice las reservas pendientes no me confirmen cambien ninguna
        // (recordar que holdeo lock de reservas)

        lockRead(attrLock);
        Attraction attr = getAttractionByName(name);
        unlockRead(attrLock);

        // Para esto, creo método que permite acceder a la instancia de la atracción, y holdear el lock de escritura
        Map<LocalTime, Integer> capacities = attr.lockWriteAndGetSpacesAvailableForDay(day);
        // IMPORTANTE: Recordar que acá estoy holdeando el lock de spacesavailable. No se puede adulterar
        // Sigo holdeando el lock de reservsLock

        for(Reservation r : attReservs){
            boolean updated = false;
            LocalTime prevSlot = r.getSlot();
            int vacants = capacities.get(prevSlot);

            if(vacants > 0){
                // Tengo lugar para la reserva pedida. Confirmo, y bajo capacidad en mapa
                confirmReservation(r);
                capacities.put(prevSlot, capacities.get(r.getSlot()) - 1);
                r.setReserved(true);
                //Puedo hacer la línea de arriba xq holdeo el lock
                confirmed++;
            }
            else{
                // Si no hay lugar, tengo que buscar el primer slot disponible posterior a este para confirmarla
                // Si no encuentro slot, la cancelo
                AttractionPass pass = getAttractionPass(r.getVisitorId(), r.getDay());
                LocalTime firstAvailable = null;
                TreeSet<LocalTime> keySet = new TreeSet<>(capacities.keySet());
                List<LocalTime> orderedKeys = new ArrayList<>(keySet).stream()
                        .filter(a -> a.isAfter(r.getSlot())) // No testeo el == porque con vacants lo chequee
                        .toList();
                for(LocalTime t : orderedKeys){ //TODO: Debería hacerlo pero corroborar que itere en orden
                    if(pass.getType() == HALF_DAY && t.isAfter(LocalTime.of(14,0))) {
                        r.setStatus(CANCELLED);
                        cancelled++;
                        break;
                    }
                    if(capacities.get(t) > 0){
                        firstAvailable = t;
                        break;
                    }
                }
                if(firstAvailable != null){
                    r.setStatus(PENDING);
                    r.setSlot(firstAvailable);
                    capacities.put(firstAvailable, capacities.get(firstAvailable)-1);
                    r.setReserved(true);
                    relocated++;
                    updated = true;
                }
                else{
                    r.setStatus(CANCELLED); // No hay espacio en ningún slot, se cancela;
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

    public boolean confirmReservation(Reservation reservation) {
        if(reservation == null)
            return false;
        return setReservationStatus(reservation.getAttractionName(), reservation.getDay(), reservation.getSlot(), reservation.getVisitorId(), new ArrayList<>(List.of(PENDING)), CONFIRMED);
    }

    public boolean cancelReservation(Reservation reservation) {
        if(reservation == null)
            return false;
        AttractionPass pass = getAttractionPass(reservation.getVisitorId(), reservation.getDay());
        if(pass.getType() == THREE)
            pass.cancelConsumption();
        return setReservationStatus(reservation.getAttractionName(), reservation.getDay(), reservation.getSlot(), reservation.getVisitorId(), new ArrayList<>(List.of(PENDING, CONFIRMED)), CANCELLED);
    }

    private boolean setReservationStatus(String attraction, int day, LocalTime slot, UUID visitorId,
                                         List<ReservationStatus> fromStatus, ReservationStatus toStatus) {

        List<Reservation> reservs = reservations.get(attraction);
        if(reservs.isEmpty())
            return false; //si no hay reservas para esa atracción, falla

        boolean ret = false;

        for(Reservation r : reservs) {
            if(r.getDay() == day && r.getSlot().equals(slot) && r.getVisitorId().equals(visitorId) && fromStatus.contains(r.getStatus())) {
                r.setStatus(toStatus);
                ret = true;
            }
        }

        return ret;
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
        Map<LocalTime, Integer> slots = repository.getAttractionByName(name).getSpaceAvailable().get(day);
        return slots.get(slot);
    }

    public boolean isValidDay(int day){
        return day > 0 && day <= 365;
    }


}
