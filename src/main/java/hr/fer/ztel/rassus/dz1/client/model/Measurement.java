package hr.fer.ztel.rassus.dz1.client.model;

import hr.fer.ztel.rassus.dz1.client.util.Utility;
import lombok.*;

import java.util.OptionalInt;

@Getter
@ToString
@EqualsAndHashCode
public class Measurement {

    private final int temperature;
    private final int pressure;
    private final int humidity;
    private final OptionalInt co;
    private final OptionalInt no2;
    private final OptionalInt so2;

    private Measurement(int temperature, int pressure, int humidity, OptionalInt co, OptionalInt no2, OptionalInt so2) {
        this.temperature = temperature;
        this.pressure = pressure;
        this.humidity = humidity;
        this.co = co;
        this.no2 = no2;
        this.so2 = so2;
    }

    public static Measurement parseFromCSV(String s) {
        try {
            // Split tokens and extract required parameters
            String[] tokens = s.split(",", -1);
            int temperature = Integer.parseInt(tokens[0]);
            int pressure = Integer.parseInt(tokens[1]);
            int humidity = Integer.parseInt(tokens[2]);

            // Create builder and set required parameters
            Builder builder = new Builder()
                    .setTemperature(temperature)
                    .setPressure(pressure)
                    .setHumidity(humidity);

            // Set optional parameters
            if (!tokens[3].isEmpty()) {
                builder.setCo(Integer.parseInt(tokens[3]));
            }
            if (!tokens[4].isEmpty()) {
                builder.setNo2(Integer.parseInt(tokens[4]));
            }
            if (!tokens[5].isEmpty()) {
                builder.setSo2(Integer.parseInt(tokens[5]));
            }

            return builder.createMeasurement();
        } catch (Exception e) {
            throw new IllegalArgumentException("Can not parse string as measurement: " + s, e);
        }
    }

    public static Measurement average(Measurement m1, Measurement m2) {
        Builder builder = new Builder()
                .setTemperature((m1.temperature + m2.temperature) / 2)
                .setPressure((m1.pressure + m2.pressure) / 2)
                .setHumidity((m1.humidity + m2.humidity) / 2);
        builder.co = Utility.averageOptionalInt(m1.co, m2.co);
        builder.no2 = Utility.averageOptionalInt(m1.no2, m2.no2);
        builder.so2 = Utility.averageOptionalInt(m1.so2, m2.so2);
        return builder.createMeasurement();
    }

    public String serializeToCSV() {
        return new StringBuilder()
                .append(temperature).append(",")
                .append(pressure).append(",")
                .append(humidity).append(",")
                .append(co.isPresent() ? co.getAsInt() : "").append(",")
                .append(no2.isPresent() ? no2.getAsInt() : "").append(",")
                .append(so2.isPresent() ? so2.getAsInt() : "").append(",")
                .toString();
    }

    public static class Builder {
        private int temperature;
        private int pressure;
        private int humidity;
        private OptionalInt co = OptionalInt.empty();
        private OptionalInt no2 = OptionalInt.empty();
        private OptionalInt so2 = OptionalInt.empty();

        public Builder setTemperature(int temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder setPressure(int pressure) {
            this.pressure = pressure;
            return this;
        }

        public Builder setHumidity(int humidity) {
            this.humidity = humidity;
            return this;
        }

        public Builder setCo(int co) {
            this.co = OptionalInt.of(co);
            return this;
        }

        public Builder setNo2(int no2) {
            this.no2 = OptionalInt.of(no2);
            return this;
        }

        public Builder setSo2(int so2) {
            this.so2 = OptionalInt.of(so2);
            return this;
        }

        public Measurement createMeasurement() {
            return new Measurement(temperature, pressure, humidity, co, no2, so2);
        }
    }

}
