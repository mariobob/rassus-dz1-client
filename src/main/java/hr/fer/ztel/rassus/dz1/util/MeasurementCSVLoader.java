package hr.fer.ztel.rassus.dz1.util;

import hr.fer.ztel.rassus.dz1.model.Measurement;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for loading measurements from a CSV file on disk.
 */
public class MeasurementCSVLoader {

    /** Name of the file from which all lines are read. */
    private static final Path MEASUREMENTS_FILE = Paths.get("./measurements.csv");

    /** Disable instantiation. */
    private MeasurementCSVLoader() {}

    public static List<Measurement> loadMeasurements() {
        try {
            return Files.lines(MEASUREMENTS_FILE, StandardCharsets.UTF_8)
                    .skip(1) // skip header
                    .filter(s -> !s.isEmpty()) // filter out empty lines
                    .map(Measurement::parseMeasurement) // convert string to Measurement
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Unable to load measurements from file.", e);
        }
    }
}
