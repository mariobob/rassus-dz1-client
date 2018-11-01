package hr.fer.ztel.rassus.dz1.client.model;

import hr.fer.ztel.rassus.dz1.client.util.Utility;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class Measurement {

    private final int temperature;
    private final int pressure;
    private final int humidity;
    private final Integer co;
    private final Integer no2;
    private final Integer so2;

    public static Measurement parseFromCSV(String s) {
        try {
            // Split tokens and extract required parameters
            String[] tokens = s.split(",", -1);
            int temperature = Integer.parseInt(tokens[0]);
            int pressure = Integer.parseInt(tokens[1]);
            int humidity = Integer.parseInt(tokens[2]);

            // Create builder and set required parameters
            MeasurementBuilder builder = new MeasurementBuilder()
                    .temperature(temperature)
                    .pressure(pressure)
                    .humidity(humidity);

            // Set optional parameters
            if (!tokens[3].isEmpty()) {
                builder.co(Integer.parseInt(tokens[3]));
            }
            if (!tokens[4].isEmpty()) {
                builder.no2(Integer.parseInt(tokens[4]));
            }
            if (!tokens[5].isEmpty()) {
                builder.so2(Integer.parseInt(tokens[5]));
            }

            return builder.build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Can not parse string as measurement: " + s, e);
        }
    }

    public static Measurement average(Measurement m1, Measurement m2) {
        MeasurementBuilder builder = new MeasurementBuilder()
                .temperature((m1.temperature + m2.temperature) / 2)
                .pressure((m1.pressure + m2.pressure) / 2)
                .humidity((m1.humidity + m2.humidity) / 2)
                .co(Utility.averageNullableInt(m1.co, m2.co))
                .no2(Utility.averageNullableInt(m1.no2, m2.no2))
                .so2(Utility.averageNullableInt(m1.so2, m2.so2));
        return builder.build();
    }

    public String serializeToCSV() {
        return new StringBuilder()
                .append(temperature).append(",")
                .append(pressure).append(",")
                .append(humidity).append(",")
                .append(co != null ? co : "").append(",")
                .append(no2 != null ? no2 : "").append(",")
                .append(so2 != null ? so2 : "").append(",")
                .toString();
    }

}
