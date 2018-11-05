package hr.fer.ztel.rassus.dz1.client;

import hr.fer.ztel.rassus.dz1.client.util.Utility;
import lombok.extern.log4j.Log4j2;
import org.apache.http.conn.HttpHostConnectException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Log4j2
public class SensorClientConsole {

    private static final String DEFAULT_SENSOR_IP_ADDRESS = "localhost";
    private static final int DEFAULT_SENSOR_PORT = 10000;
    private static final String DEFAULT_SERVER_IP_ADDRESS = "localhost";
    private static final int DEFAULT_SERVER_PORT = 8080;

    /**
     * Client program entry point.
     *
     * @param args sensor IP, sensor port, server IP, server port
     */
    public static void main(String[] args) throws IOException {
        // Take values from command line arguments, or fall back to default
        String ipAddress       = args.length >= 1 ? args[0]                   : DEFAULT_SENSOR_IP_ADDRESS;
        int port               = args.length >= 2 ? Integer.parseInt(args[1]) : DEFAULT_SENSOR_PORT;
        String serverIpAddress = args.length >= 3 ? args[2]                   : DEFAULT_SERVER_IP_ADDRESS;
        int serverPort         = args.length >= 4 ? Integer.parseInt(args[3]) : DEFAULT_SERVER_PORT;

        // If specified port is in use, increment it by 1
        while (Utility.isPortInUse(ipAddress, port)) {
            log.warn("Port {} is unavailable, incrementing to {}...", port, port+1);
            port++;
        }

        // Initialize client and register it to server
        SensorClient client;
        try {
            client = new SensorClient(ipAddress, port, serverIpAddress, serverPort);
            log.info("Client: {}", client);
            while (!client.registerToServer()) {
            }
        } catch (Exception e) {
            log.error("Could not initialize sensor client.", e);
            return;
        }

        // Print out the welcome text
        System.out.println("Welcome to sensor management interface of sensor " + client.getSensor().getUsername());
        System.out.println("Enter a command or 'EXIT' to shutdown sensor.");

        // Start the command prompt, listen for user input and loop through it
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
                        client.startClientLoop();
                        break;

                    case "STOP":
                        client.stopClientLoop();
                        break;

                    case "EXIT":
                        client.shutdown();
                        break l;

                    default:
                        System.out.println("Unknown command: " + command);
                }
            }

            reader.close();
            System.out.println("Sensor client console has shut down. Goodbye!");
        } catch (HttpHostConnectException e) {
            System.out.println("Lost connection with server. Shutting down client.");
            throw e;
        } catch (Exception e) {
            System.out.println("A critical error occurred... shutting down client.");
            try { client.deregisterFromServer(); } catch (Exception ignorable) {}
            throw e;
        }
    }
}
