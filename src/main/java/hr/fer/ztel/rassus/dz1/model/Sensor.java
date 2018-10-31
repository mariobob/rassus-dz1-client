package hr.fer.ztel.rassus.dz1.model;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Sensor {

    private static final double LATITUDE_MIN = 15.87;
    private static final double LATITUDE_MAX = 16.00;
    private static final double LONGITUDE_MIN = 45.75;
    private static final double LONGITUDE_MAX = 45.85;

    private String username;
    private double latitude;
    private double longitude;
    private String ipAddress;
    private int port;

    public Sensor(String ipAddress, int port) {
        this.username = UUID.randomUUID().toString();
        this.latitude = ThreadLocalRandom.current().nextDouble(LATITUDE_MIN, LATITUDE_MAX);
        this.longitude = ThreadLocalRandom.current().nextDouble(LONGITUDE_MIN, LONGITUDE_MAX);
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return username;
    }
}
