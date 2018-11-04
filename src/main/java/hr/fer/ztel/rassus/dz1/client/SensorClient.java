package hr.fer.ztel.rassus.dz1.client;

import com.google.gson.Gson;
import hr.fer.ztel.rassus.dz1.client.loader.Loaders;
import hr.fer.ztel.rassus.dz1.client.model.Measurement;
import hr.fer.ztel.rassus.dz1.client.model.Sensor;
import hr.fer.ztel.rassus.dz1.client.thread.ServerThread;
import hr.fer.ztel.rassus.dz1.client.util.Cache;
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
@ToString
@EqualsAndHashCode
public class SensorClient {

    private static final String SERVER_URL = "http://%s:%d/measurementhost/rest/sensors/";
    private static final long AUTO_MEASURE_SLEEP_MILLIS = 5000;
    private static final long MAX_CACHE_SECONDS = 24;

    /** Closest sensor that is cached temporarily. */
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Cache<Sensor> cachedClosestSensor;
    /** Socket of the closest sensor that is cached temporarily. */
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Cache<Socket> cachedClosestSensorSocket;
    /** Server thread of this sensor, used for serving other sensors. */
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private final ServerThread serverThread;

    /** Thread that runs the measurement process in a loop. */
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private final Thread measurementThread = new Thread("MeasuringThread") {
        @Override
        public void run() {
            while (!isInterrupted()) {
                try { measure(); } catch (HttpHostConnectException e) {
                    log.warn("Lost connection with server. Shutting down client...");
                    shutdown();
                    break;
                } catch (IOException e) {
                    log.warn("An IOException occurred", e);
                    break;
                }
                // Sleep until the new measurement cycle
                try { Thread.sleep(AUTO_MEASURE_SLEEP_MILLIS); } catch (InterruptedException e) { interrupt(); }
            }
        }
    };

    @Getter private boolean registeredToServer = false;
    @Getter private final Sensor sensor;
    @Getter private final String serverIpAddress;
    @Getter private final int serverPort;

    public SensorClient(String ipAddress, int port, String serverIpAddress, int serverPort) {
        this.serverThread = new ServerThread(ipAddress, port);
        this.sensor = new Sensor(ipAddress, port);
        this.serverIpAddress = serverIpAddress;
        this.serverPort = serverPort;
    }

    public boolean registerToServer() throws IOException {
        if (registeredToServer) {
            log.warn("Client already registered to server: {}:{}", serverIpAddress, serverPort);
            return false;
        }

        log.info("Registering sensor client to server {}:{}: {}", serverIpAddress, serverPort, sensor.getUsername());
        registeredToServer = postJson(sensor, SERVER_URL);
        if (registeredToServer) {
            log.info("Starting local server for other sensors at {}:{}", sensor.getIpAddress(), sensor.getPort());
            serverThread.setDaemon(true);
            serverThread.start();
        }

        return registeredToServer;
    }

    public void deregisterFromServer() throws IOException {
        if (!isRegisteredToServer()) {
            log.warn("Client is not registered to server");
            return;
        }

        log.info("Deregistering sensor client from server {}: {}", serverIpAddress, sensor.getUsername());

        String webpageUrl = SERVER_URL + sensor.getUsername();
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpDelete httpDelete = new HttpDelete(String.format(webpageUrl, serverIpAddress, serverPort));

            // Loop until client is successfully deleted
            while (client.execute(httpDelete).getStatusLine().getStatusCode() != 200) {
            }
        } finally {
            serverThread.interrupt();
        }

        log.info("Successfully deregistered sensor");
    }

    public void shutdown() {
        log.info("Shutting down client for sensor: {}", sensor.getUsername());
        try { deregisterFromServer(); } catch (IOException connectionClosed) {}
        log.info("Successfully shut down sensor client");
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
            measurement = getAverageMeasurement(closestSensor, measurement);
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

    /**
     * Method that fetches a measurement from <tt>otherSensor</tt> and calculates
     * and returns the average value for all attributes of a measurement.
     *
     * @param otherSensor sensor whose measurement is to be fetched
     * @param measurement measurement of this sensor to make an average from
     * @return the average measurement between this sensor and other sensor
     * @throws IOException in client communication error occurs
     */
    private Measurement getAverageMeasurement(Sensor otherSensor, Measurement measurement) throws IOException {
        Socket socket;
        if (cachedClosestSensorSocket != null && !cachedClosestSensorSocket.isExpired()) {
            // Use cached sensor socket, if exists and is not expired
            socket = cachedClosestSensorSocket.get();
        } else {
            // Create a new socket and cache it
            socket = new Socket(otherSensor.getIpAddress(), otherSensor.getPort());
            cachedClosestSensorSocket = new Cache<>(socket, MAX_CACHE_SECONDS);
            cachedClosestSensorSocket.onExpiration(() -> {
                synchronized (socket) {
                    log.info("Closing connection with sensor: {}", otherSensor.getUsername());
                    try { socket.close(); } catch (Exception e) {}
                }
            });
        }

        // Lock socket until measurement fetching is finished
        synchronized (socket) {
            // Initialize input and output
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Signalize the need of a measurement
            out.println(Utility.GET_MEASUREMENT_KEYWORD);

            // Obtain the measurement and convert from json
            String json = in.readLine();
            Gson gson = new Gson();
            Measurement otherMeasurement = gson.fromJson(json, Measurement.class);

            return Measurement.average(measurement, otherMeasurement);
        }
    }

    private Sensor getClosestSensor() throws IOException {
        // First try to obtain closest sensor from cache
        if (cachedClosestSensor != null && !cachedClosestSensor.isExpired()) {
            log.info("Obtaining closest sensor from cache...");
            return cachedClosestSensor.get();
        }

        // If cached sensor does not exist or is expired, ask the server
        String webpageUrl = String.format(SERVER_URL, serverIpAddress, serverPort) + sensor.getUsername() + "/closest";
        log.info("Asking server to return closest sensor at {}", webpageUrl);
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet(webpageUrl);
            HttpEntity response = client.execute(httpGet).getEntity();

            String json = EntityUtils.toString(response, StandardCharsets.UTF_8);
            if (json == null || json.equals("null") || json.isEmpty()) {
                return null;
            }

            Gson gson = new Gson();
            Sensor sensor = gson.fromJson(json, Sensor.class);
            cachedClosestSensor = new Cache<>(sensor, MAX_CACHE_SECONDS);
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
     * Starts the client measurement loop in a new thread and
     * runs measurements every n seconds.
     */
    public void startClientLoop() {
        log.info("Starting client measurement loop...");
        if (!isRegisteredToServer()) {
            log.warn("Client is not registered to server");
            return;
        }

        if (isClientLoopRunning()) {
            log.warn("Client loop is already running");
            return;
        }

        measurementThread.start();
    }

    /**
     * Stops the client measurement loop, if it is running.
     */
    public void stopClientLoop() {
        if (!isClientLoopRunning()) {
            log.warn("Client loop is not running");
            return;
        }

        measurementThread.interrupt();
    }

    /**
     * Returns true if the client measurement loop is running, false otherwise.
     *
     * @return true if the client measurement loop is running, false otherwise
     */
    public boolean isClientLoopRunning() {
        return measurementThread.isAlive();
    }

}
