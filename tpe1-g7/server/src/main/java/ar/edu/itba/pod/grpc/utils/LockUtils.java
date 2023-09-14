package ar.edu.itba.pod.grpc.utils;

import java.util.concurrent.locks.ReadWriteLock;

public class LockUtils {
    public final static boolean fairness4locks = true;

    public static void lockRead(ReadWriteLock lock) { lock.readLock().lock();}
    public static void lockWrite(ReadWriteLock lock) { lock.readLock().lock();}

    public static void unlockRead(ReadWriteLock lock) { lock.readLock().unlock();}
    public static void unlockWrite(ReadWriteLock lock) { lock.readLock().unlock();}
}
