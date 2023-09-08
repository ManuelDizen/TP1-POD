package ar.edu.itba.pod.grpc.models;

import org.checkerframework.checker.units.qual.A;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class Attraction {
    private final String name;
    private final LocalTime opening;
    private final LocalTime closing;
    private final int minsPerSlot;
    private final Map<Integer, Map<LocalTime, Integer>> spaceAvailable = new HashMap<>();
    private final Map<Integer, Integer> capacities = new HashMap<>();
    private final List<Follower> followers = new ArrayList<>();

    public Attraction(String name, LocalTime opening, LocalTime closing, int minsPerSlot) {
        this.name = name;
        this.opening = opening;
        this.closing = closing;
        this.minsPerSlot = minsPerSlot;
    }

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
        return spaceAvailable;
    }

    public Map<Integer, Integer> getCapacities() {
        return capacities;
    }

    public void initializeSlots(int day, int capacity){
        getSpaceAvailable().put(day, new HashMap<>());
        int i = 0;
        // La cuenta es: Si opening + (mins*i) es menor a closing pongo. Va a haber un caso borde
        // donde la cuenta puede dar mas, que es el último. Cuando creamos atracción, ya chequeamos que
        // entre al menos un slot, y de acuerdo a la consigna, si entra un slot y queda tiempo extra que es
        // menro a un slot, se hace igual.
        while((opening.plusMinutes((long) minsPerSlot * i)).isBefore(closing)){
            getSpaceAvailable().get(day).put(opening.plusMinutes((long) minsPerSlot * i), capacity);
            i++;
        }

        List<Follower> toNotify = followers.stream().filter(a -> a.getDay() == day).toList();
        for(Follower f : toNotify){
            f.sendMessage(name + " announced slot capacity for the day " + day +": " + capacity + " places.");
        }
    }

    public boolean isValidSlot(LocalTime slot) {
        return !slot.isBefore(opening) && !slot.isAfter(closing);
    }

    public LocalTime getSlot(LocalTime slot) {

        int i;

        for(i=0; opening.plusMinutes((long) minsPerSlot * i).isBefore(slot) || opening.plusMinutes((long) minsPerSlot * i).equals(slot); i++);

        return opening.plusMinutes((long) minsPerSlot *(i-1));
    }

    public List<Follower> getFollowers() {
        return followers;
    }

    public void addFollower(Follower f){
        followers.add(f);
        f.sendMessage("User " + f.getVisitorId().toString() + " is now following attraction " + this.getName()
            + " for day " + f.getDay());
    }

    public boolean checkToNotify(Reservation reservation){
        return followers.stream()
                .anyMatch(
                        a -> a.getDay() == reservation.getDay() && a.getVisitorId() == reservation.getVisitorId()
                );
    }

    public void unsubscribeFollower(UUID visitorId, int day){
        Follower f = followers.stream().filter(follower -> follower.getDay() == day
                && follower.getVisitorId().equals(visitorId)).findFirst().orElseThrow();
        f.close();
        followers.remove(f);
    }

    public boolean isVisitorSubscribedForDay(Follower f){
        return followers.contains(f);
    }

    public boolean isVisitorSubscribedForDay(UUID visitorId, int day){
        return isVisitorSubscribedForDay(new Follower(visitorId, day, null));
    }

    public void notifyReservation(int day, UUID id, Reservation r){
        Follower f = followers.stream().filter(a -> a.getDay() == day && a.getVisitorId() == id).findFirst().orElseThrow(); // No debería pasar
        f.sendMessage("The reservation for " + this.getName() + " at " + r.getSlot().toString() +
                " on the day " + day + " is " + r.getStatus().toString());
    }
    public void notifyReservation(int day, UUID id, Reservation r, String message){
        Follower f = followers.stream().filter(a -> a.getDay() == day && a.getVisitorId() == id).findFirst().orElseThrow(); // No debería pasar
        f.sendMessage(message);
    }
}
