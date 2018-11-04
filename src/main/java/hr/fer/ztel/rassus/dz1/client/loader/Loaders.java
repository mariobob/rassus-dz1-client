package hr.fer.ztel.rassus.dz1.client.loader;

public class Loaders {

    private static MeasurementLoader measurementLoader;

    public static MeasurementLoader getMeasurementLoader() {
        if (measurementLoader == null) {
            measurementLoader = new MeasurementCSVLoader();
        }

        return measurementLoader;
    }
}
