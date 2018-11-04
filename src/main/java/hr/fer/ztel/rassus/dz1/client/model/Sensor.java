package hr.fer.ztel.rassus.dz1.client.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@ToString
@EqualsAndHashCode
public class Sensor {

    private static final double LATITUDE_MIN = 45.75;
    private static final double LATITUDE_MAX = 45.85;
    private static final double LONGITUDE_MIN = 15.87;
    private static final double LONGITUDE_MAX = 16.00;

    private final String username;
    private final String ipAddress;
    private final int port;
    private final MapLocation location;

    public Sensor(String ipAddress, int port) {
        this.username = UUID.randomUUID().toString();
        this.ipAddress = ipAddress;
        this.port = port;
        this.location = new MapLocation(
                ThreadLocalRandom.current().nextDouble(LATITUDE_MIN, LATITUDE_MAX),
                ThreadLocalRandom.current().nextDouble(LONGITUDE_MIN, LONGITUDE_MAX)
        );
    }

}
