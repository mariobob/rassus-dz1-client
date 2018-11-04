package hr.fer.ztel.rassus.dz1.client.loader;

import hr.fer.ztel.rassus.dz1.client.model.Measurement;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for loading measurements from a CSV file on disk.
 */
public class MeasurementCSVLoader implements MeasurementLoader {

    /** Name of the file from which all lines are read. */
    private static final String DEFAULT_MEASUREMENTS_FILE = "src/main/resources/measurements.csv";

    /** List of measurements cached after the first load. */
    private List<Measurement> cachedMeasurements;

    private Path measurementsFile;

    public MeasurementCSVLoader() {
        this(DEFAULT_MEASUREMENTS_FILE);
    }

    public MeasurementCSVLoader(String filePath) {
        this.measurementsFile = Paths.get(filePath);
    }

    @Override
    public Measurement getMeasurement(int index) {
        return getMeasurements().get(index);
    }

    @Override
    public List<Measurement> getMeasurements() {
        if (cachedMeasurements == null) {
            cachedMeasurements = loadMeasurements();
        }

        return cachedMeasurements;
    }

    private List<Measurement> loadMeasurements() {
        try {
            return Files.lines(measurementsFile, StandardCharsets.UTF_8)
                    .skip(1) // skip header
                    .filter(s -> !s.isEmpty()) // filter out empty lines
                    .map(Measurement::parseFromCSV) // convert string to Measurement
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Unable to load measurements from file.", e);
        }
    }
}
