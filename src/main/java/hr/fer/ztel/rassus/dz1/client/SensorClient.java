package hr.fer.ztel.rassus.dz1.client;

import hr.fer.ztel.rassus.dz1.client.model.Measurement;
import hr.fer.ztel.rassus.dz1.client.model.Sensor;
import hr.fer.ztel.rassus.dz1.client.util.MeasurementCSVLoader;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Scanner;

@Log4j2
public class SensorClient {

    private static final int SERVER_PORT = 8080;
    private static List<Measurement> measurements = MeasurementCSVLoader.getMeasurements();

    private final long clientStartTime = System.currentTimeMillis();
    private Sensor sensor;

    public SensorClient(String ipAddress, int port, String serverIpAddress) {
        this.sensor = new Sensor(ipAddress, port);
    }

    public long getClientStartTime() {
        return clientStartTime;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Expected 3 arguments: Sensor IP address, Sensor port and Server IP address.");
            return;
        }

        SensorClient client;
        try {
            String ipAddress = args[0];
            int port = Integer.parseInt(args[1]);
            String serverIpAddress = args[2];

            client = new SensorClient(ipAddress, port, serverIpAddress);
            while (!client.registerToServer()) {
                log.info("Registering client {} to server at {}", client.getSensor().getUsername(), serverIpAddress);
            }
            log.info("Successfully registered client {} to server.", client.getSensor().getUsername());

            log.info("Starting local server for other sensors...");
//            client.serverThread.setDaemon(true); // TODO
//            client.serverThread.start();
        } catch (Exception e) {
            log.error("Could not initialize sensor client.", e);
            return;
        }

        System.out.println("Welcome to sensor management interface of sensor " + client.getSensor().getUsername());
        System.out.println("Enter a command or 'END' to shutdown sensor.");

        Scanner sc = new Scanner(System.in);
l:      while (sc.hasNext()) {
            System.out.print("> ");

            String command = sc.nextLine();
            switch (command.toUpperCase()) {
                case "MEASURE":
                    client.measure();
                    break;

                case "END":
                    client.shutdown();
                    break l;

                default:
                    System.out.println("Unknown command: " + command);
            }
        }

        sc.close();
        System.out.println("Sensor client has shut down. Goodbye!");
    }

    private boolean registerToServer() {
        // TODO
        return false;
    }

    private void shutdown() {
        deregisterFromServer();
        // TODO
    }

    private void deregisterFromServer() {
        // TODO
    }

    private void measure() {
        // TODO
    }

    private void sendMeasurement(Measurement m) {
        // TODO
    }

    private void getAverageMeasurement(Sensor otherSensor, Measurement m) {
        // TODO
    }

    private Sensor getClosestNeighbour() {
        // TODO
        return null;
    }

}
