package hr.fer.ztel.rassus.dz1.client.util;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Cache class that keeps an element for a specified amount of time
 * before marking it as expired.
 *
 * @author Mario Bobic
 */
@ToString
@EqualsAndHashCode
public class Cache<V> {

    /** Default maximum age of a cache element, in seconds. */
    private static final int DEFAULT_MAX_AGE = 30;

    /** Value of this cache. */
    private final V value;
    /** Expiration time of this cache. */
    private final LocalDateTime expirationTime;

    /**
     * Default constructor. Sets the expiration time to default expiration time.
     */
    public Cache(V value) {
        this(value, DEFAULT_MAX_AGE);
    }

    /**
     * Constructs an instance of {@code Cache} with the specified maximum age in seconds.
     *
     * @param maxAgeSeconds maximum age before the cache expires, in seconds
     */
    public Cache(V value, long maxAgeSeconds) {
        this.value = value;
        this.expirationTime = LocalDateTime.now().plusSeconds(maxAgeSeconds);
    }

    /**
     * Returns the value of this cache, if it exists and is not expired.
     * Otherwise it returns <tt>null</tt>.
     *
     * @return the value associated with this cache, or <tt>null</tt> if expired
     */
    public V get() {
        return !isExpired() ? value : null;
    }

    /**
     * Returns true if the value associated with this cache is expired,
     * false otherwise.
     *
     * @return true if value is expired, false otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationTime);
    }

    /**
     * Returns the expiration time of the value in this cache.
     *
     * @return the expiration time of the value in this cache
     */
    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }

    public void onExpiration(ScheduledExecutorService executor, Runnable runnable) {
        long millis = LocalDateTime.now().until(expirationTime, ChronoUnit.MILLIS);
        if (millis < 0) {
            millis = 0;
        }

        executor.schedule(runnable, millis, TimeUnit.MILLISECONDS);
    }
}
