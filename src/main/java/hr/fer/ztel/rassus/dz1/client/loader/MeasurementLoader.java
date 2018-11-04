package hr.fer.ztel.rassus.dz1.client.loader;

import hr.fer.ztel.rassus.dz1.client.model.Measurement;

import java.util.List;

public interface MeasurementLoader {

    Measurement getMeasurement(int index);

    List<Measurement> getMeasurements();

}
