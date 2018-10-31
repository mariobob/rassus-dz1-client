package hr.fer.ztel.rassus.dz1;

import hr.fer.ztel.rassus.dz1.model.Measurement;
import hr.fer.ztel.rassus.dz1.model.Sensor;
import hr.fer.ztel.rassus.dz1.util.MeasurementCSVLoader;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<Measurement> measurements = MeasurementCSVLoader.loadMeasurements();
        measurements.forEach(System.out::println);

        System.out.println(new Sensor("", 0).toString());
    }
}
