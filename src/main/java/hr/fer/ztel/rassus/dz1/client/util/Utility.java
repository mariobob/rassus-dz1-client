package hr.fer.ztel.rassus.dz1.client.util;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;

public class Utility {

    /** Keyword used between two sensor clients to obtain a measurement over the network. */
    public static final String GET_MEASUREMENT_KEYWORD = "GET_MEASUREMENT";
    /** Sleep time for retry logic, in milliseconds. */
    private static final long RETRY_LOGIC_SLEEP_MILLIS = 1000;

    /** Disable instantiation. */
    private Utility() {}

    /**
     * Returns the average values between these nullable integers.
     * If only one integers is present, it is returned, otherwise
     * if both integers are <tt>null</tt>, <tt>null</tt> is returned.
     *
     * @param n1 nullable integer
     * @param n2 nullable integer
     * @return the average value of two integers
     */
    public static Integer averageNullableInt(Integer n1, Integer n2) {
        if (n1 != null && n2 != null) {
            return (n1 + n2) / 2;
        } else if (n1 != null) {
            return n1;
        } else if (n2 != null) {
            return n2;
        } else {
            return null;
        }
    }

    /**
     * Checks to see if a specific port is in use.
     *
     * @param port the port to check
     */
    public static boolean isPortInUse(String host, int port) throws IOException {
        try {
            new Socket(host, port).close();
            // Successful connection means the port is taken.
            return true;
        } catch (SocketException e) {
            // Could not connect.
            return false;
        }
    }

    public static void retry(int times, Callable<Boolean> callable) {
        for (int i = 0; i < times; i++) {
            try {
                callable.call();
            } catch (IOException e) {
                try {
                    Thread.sleep(RETRY_LOGIC_SLEEP_MILLIS);
                } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
