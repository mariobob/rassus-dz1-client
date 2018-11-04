package hr.fer.ztel.rassus.dz1.client;

import com.google.gson.Gson;
import hr.fer.ztel.rassus.dz1.client.loader.Loaders;
import hr.fer.ztel.rassus.dz1.client.model.Measurement;
import hr.fer.ztel.rassus.dz1.client.model.Sensor;
import hr.fer.ztel.rassus.dz1.client.thread.ServerThread;
import hr.fer.ztel.rassus.dz1.client.util.Utility;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Log4j2
@Getter
@ToString
@EqualsAndHashCode
public class SensorClient {

    private static final String SERVER_URL = "http://%s:%d/measurements/rest/sensors/";
    private static final long AUTO_MEASURE_SLEEP_MILLIS = 5000;

    private static final String DEFAULT_SENSOR_IP_ADDRESS = "localhost";
    private static int DEFAULT_SENSOR_PORT = 10000;
    private static final String DEFAULT_SERVER_IP_ADDRESS = "localhost";
    private static final int DEFAULT_SERVER_PORT = 8080;

    @ToString.Exclude
    private final ServerThread serverThread;
    private final Sensor sensor;
    private final String serverIpAddress;
    private final int serverPort;

    public SensorClient(String ipAddress, int port, String serverIpAddress, int serverPort) {
        this.serverThread = new ServerThread(ipAddress, port);
        this.sensor = new Sensor(ipAddress, port);
        this.serverIpAddress = serverIpAddress;
        this.serverPort = serverPort;
    }

    public boolean registerToServer() throws IOException {
        log.info("Client: {}", this);
        log.info("Registering sensor client to server {}: {}", serverIpAddress, sensor.getUsername());
        return postJson(sensor, SERVER_URL);
    }

    public void deregisterFromServer() throws IOException {
        log.info("Deregistering client from server {}: {}", serverIpAddress, sensor.getUsername());

        String webpageUrl = SERVER_URL + sensor.getUsername();
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpDelete httpDelete = new HttpDelete(String.format(webpageUrl, serverIpAddress, serverPort));

            // Loop until client is successfully deleted
            while (client.execute(httpDelete).getStatusLine().getStatusCode() != 200) {
            }
        }

        log.info("Successfully deregistered sensor");
    }

    public void shutdown() throws IOException {
        log.info("Shutting down client for sensor: {}", sensor.getUsername());
        deregisterFromServer();
        serverThread.interrupt();
        log.info("Successfully down client for sensor");
    }

    public void measure() throws IOException {
        // Generate measurement
        int secondsActive = Math.toIntExact((System.currentTimeMillis() - serverThread.getStartTime()) / 1000);
        int ordinalNumber = (secondsActive % 100) + 2;
        log.info("Seconds active: {}s; Ordinal number: {} given by the formula ({} % 100) + 2 = {}",
                secondsActive, ordinalNumber, secondsActive, ordinalNumber);
        Measurement measurement = Loaders.getMeasurementLoader().getMeasurement(secondsActive % 100);
        log.info("Generated measurement: {}", measurement);

        // Find closest sensor (and make average)
        Sensor closestSensor = getClosestSensor();
        if (closestSensor == null) {
            log.info("There is no neighbouring sensor. Sending generated measurement...");
        } else {
            log.info("Found closest sensor: {}", closestSensor.getUsername());
            measurement = getAverageMeasurement(sensor, measurement);
        }

        // Loop until measurement is successfully sent
        while (!sendMeasurement(measurement)) {
        }
    }

    private boolean sendMeasurement(Measurement measurement) throws IOException {
        log.info("Sending measurement: {}", measurement);
        String webpageUrl = SERVER_URL + sensor.getUsername() + "/measurements";
        return postJson(measurement, webpageUrl);
    }

    private Measurement getAverageMeasurement(Sensor otherSensor, Measurement measurement) throws IOException {
        try (Socket socket = new Socket(otherSensor.getIpAddress(), otherSensor.getPort())) {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String json = in.readLine();
            Gson gson = new Gson();
            Measurement otherMeasurement = gson.fromJson(json, Measurement.class);

            return Measurement.average(measurement, otherMeasurement);
        }
    }

    private Sensor getClosestSensor() throws IOException {
        String webpageUrl = SERVER_URL + sensor.getUsername() + "/closest";
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet(String.format(webpageUrl, serverIpAddress, serverPort));
            HttpEntity response = client.execute(httpGet).getEntity();

            String json = EntityUtils.toString(response, StandardCharsets.UTF_8);
            if (json == null || json.equals("null") || json.isEmpty()) {
                return null;
            }

            Gson gson = new Gson();
            Sensor sensor = gson.fromJson(json, Sensor.class);
            return sensor;
        }
    }

    private boolean postJson(Object objectToPost, String webpageUrl) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(objectToPost);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost(String.format(webpageUrl, serverIpAddress, serverPort));

            HttpEntity request = new StringEntity(json, ContentType.APPLICATION_JSON);
            httpPost.setEntity(request);

            HttpEntity response = client.execute(httpPost).getEntity();
            String responseStr = EntityUtils.toString(response, StandardCharsets.UTF_8);
            boolean success = Boolean.parseBoolean(responseStr);
            if (success) {
                log.info("Successfully posted: {}", objectToPost);
            } else {
                log.warn("Failed to post: {}", objectToPost);
            }

            return success;
        }
    }

    /**
     * Client program entry point.
     *
     * @param args sensor IP, sensor port, server IP, server port
     */
    public static void main(String[] args) throws IOException {
        String ipAddress       = args.length >= 1 ? args[0]                   : DEFAULT_SENSOR_IP_ADDRESS;
        int port               = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_SENSOR_PORT;
        String serverIpAddress = args.length >= 3 ? args[2]                   : DEFAULT_SERVER_IP_ADDRESS;
        int serverPort         = args.length >= 4 ? Integer.parseInt(args[3]) : DEFAULT_SERVER_PORT;


        while (Utility.isPortInUse(ipAddress, port)) {
            log.warn("Port {} is unavailable, incrementing to {}...", port, port+1);
            port++;
        }

        SensorClient client;
        try {
            // Loop until client is successfully registered to server
            client = new SensorClient(ipAddress, port, serverIpAddress, serverPort);
            while (!client.registerToServer()) {
            }

            log.info("Starting local server for other sensors...");
            client.getServerThread().setDaemon(true);
            client.getServerThread().start();
        } catch (Exception e) {
            log.error("Could not initialize sensor client.", e);
            return;
        }

        System.out.println("Welcome to sensor management interface of sensor " + client.getSensor().getUsername());
        System.out.println("Enter a command or 'END' to shutdown sensor.");

        // Thread for automatic measurement
        Thread measuringThread = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try { client.measure(); } catch (IOException e) {}
                    try { Thread.sleep(AUTO_MEASURE_SLEEP_MILLIS); } catch (InterruptedException e) { interrupt(); }
                }
            }
        };

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
l:
            while (true) {
                System.out.print("> ");

                String command = reader.readLine();
                if (command == null) break;
                if (command.trim().isEmpty()) continue;

                switch (command.toUpperCase()) {
                    case "MEASURE":
                        client.measure();
                        break;

                    case "START":
                        if (!measuringThread.isAlive()) measuringThread.start();

                    case "STOP":
                        measuringThread.interrupt();
                        break;

                    case "END":
                        client.shutdown();
                        break l;

                    case "AUTO":
                        System.out.print("n = ");
                        int n = Integer.parseInt(reader.readLine().trim());
                        autoMeasure(client, n);
                        break;

                    default:
                        System.out.println("Unknown command: " + command);
                }
            }

            reader.close();
            System.out.println("Sensor client has shut down. Goodbye!");
        } catch (HttpHostConnectException e) {
            System.out.println("Lost connection with server. Shutting down client.");
            throw e;
        } catch (Exception e) {
            System.out.println("A critical error occurred... shutting down client.");
            try { client.deregisterFromServer(); } catch (Exception ignorable) {}
            throw e;
        }
    }

    private static void autoMeasure(SensorClient client, int times) throws IOException {
        for (int i = 0; i < times; i++) {
            client.measure();
        }
        log.info("Successfully sent {} measurements", times);
    }

}
