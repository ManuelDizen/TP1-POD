package ar.edu.itba.pod.grpc.models;


import ar.edu.itba.pod.grpc.utils.LockUtils;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static ar.edu.itba.pod.grpc.utils.LockUtils.*;

public class Attraction {
    private final String name;
    private final LocalTime opening;
    private final LocalTime closing;
    private final int minsPerSlot;
    private final Map<Integer, Map<LocalTime, Integer>> spaceAvailable = new HashMap<>();
    private final Map<Integer, Integer> capacities = new HashMap<>();
    private final List<Follower> followers = new ArrayList<>();
    private final ReadWriteLock spacesLock = new ReentrantReadWriteLock(fairness4locks);
    private final ReadWriteLock capacitiesLock = new ReentrantReadWriteLock(fairness4locks);
    private final ReadWriteLock followersLock = new ReentrantReadWriteLock(fairness4locks);

    public Attraction(String name, LocalTime opening, LocalTime closing, int minsPerSlot) {
        this.name = name;
        this.opening = opening;
        this.closing = closing;
        this.minsPerSlot = minsPerSlot;
    }

    //Nota a quién corresponda: Ciertos campos son final e inmutables, no es necesario protegerlos para su acceso.
    public String getName() {
        return name;
    }

    public LocalTime getOpening() {
        return opening;
    }

    public LocalTime getClosing() {
        return closing;
    }

    public int getMinsPerSlot() {
        return minsPerSlot;
    }

    public Map<Integer, Map<LocalTime, Integer>> getSpaceAvailable() {
        lockRead(spacesLock);
        Map<Integer, Map<LocalTime, Integer>> map = spaceAvailable;
        unlockRead(spacesLock);
        return map;
    }

    public Map<Integer, Integer> getCapacities() {
        lockRead(capacitiesLock);
        Map<Integer, Integer> map =  capacities;
        unlockRead(capacitiesLock);
        return map;
    }

    public void initializeSlots(int day, int capacity){
        lockWrite(spacesLock);

        spaceAvailable.put(day, new HashMap<>());

        int i = 0;
        // La cuenta es: Si opening + (mins*i) es menor a closing pongo. Va a haber un caso borde
        // donde la cuenta puede dar mas, que es el último. Cuando creamos atracción, ya chequeamos que
        // entre al menos un slot, y de acuerdo a la consigna, si entra un slot y queda tiempo extra que es
        // menro a un slot, se hace igual.
        while((opening.plusMinutes((long) minsPerSlot * i)).isBefore(closing)){
            spaceAvailable.get(day).put(opening.plusMinutes((long) minsPerSlot * i), capacity);
            i++;
        }

        unlockWrite(spacesLock);

        lockRead(followersLock);
        List<Follower> toNotify = followers.stream().filter(a -> a.getDay() == day).toList();
        for(Follower f : toNotify){
            f.sendMessage(name + " announced slot capacity for the day " + day +": " + capacity + " places.");
        }
        unlockRead(followersLock);
    }

    public LocalTime getSlot(LocalTime slot) {

        int i;

        for(i=0; opening.plusMinutes((long) minsPerSlot * i).isBefore(slot) ||
                opening.plusMinutes((long) minsPerSlot * i).equals(slot); i++);

        return opening.plusMinutes((long) minsPerSlot *(i-1));
    }

    public List<Follower> getFollowers() {
        lockRead(followersLock);
        List<Follower> f = followers;
        unlockRead(followersLock);
        return f;
    }

    public void addFollower(Follower f){
        lockWrite(followersLock);
        followers.add(f);
        unlockWrite(followersLock);
        f.sendMessage("User " + f.getVisitorId().toString() + " is now following attraction " + this.getName()
            + " for day " + f.getDay());
    }

    public boolean checkToNotify(Reservation reservation){
        lockRead(followersLock);
        boolean notify = followers.stream()
                .anyMatch(
                        a -> a.getDay() == reservation.getDay() && a.getVisitorId().equals(reservation.getVisitorId())
                );
        unlockRead(followersLock);
        return notify;
    }

    public void unsubscribeFollower(UUID visitorId, int day){
        lockWrite(followersLock);
        Follower f = followers.stream().filter(follower -> follower.getDay() == day
                && follower.getVisitorId().equals(visitorId)).findFirst().orElseThrow();
        f.close();
        followers.remove(f);
        unlockWrite(followersLock);
    }

    public boolean isVisitorSubscribedForDay(Follower f){
        lockRead(followersLock);
        boolean has = followers.contains(f);
        unlockRead(followersLock);
        return has;
    }

    public boolean isVisitorSubscribedForDay(UUID visitorId, int day){
        return isVisitorSubscribedForDay(new Follower(visitorId, day, null));
    }

    public void notifyReservation(int day, UUID id, Reservation r){
        lockRead(followersLock);
        Follower f = followers.stream().filter(a -> a.getDay() == day && a.getVisitorId().equals(id)).findFirst().orElseThrow(); // No debería pasar
        unlockRead(followersLock);
        f.sendMessage("The reservation for " + this.getName() + " at " + r.getSlot().toString() +
                " on the day " + day + " is " + r.getStatus().toString());
    }
    public void notifyReservation(int day, UUID id, Reservation r, String message){
        lockRead(followersLock);
        Follower f = followers.stream().filter(a -> a.getDay() == day && a.getVisitorId().equals(id)).findFirst().orElseThrow(); // No debería pasar
        unlockRead(followersLock);
        f.sendMessage(message);
    }

    public void updateSlots(int day, LocalTime slot, int capacity){
        lockWrite(spacesLock);
        spaceAvailable.get(day).put(slot, capacity);
        unlockWrite(spacesLock);
    }



    public ReadWriteLock getSpacesLock() {
        return spacesLock;
    }

    public ReadWriteLock getCapacitiesLock() {
        return capacitiesLock;
    }

    public ReadWriteLock getFollowersLock() {
        return followersLock;
    }
}
