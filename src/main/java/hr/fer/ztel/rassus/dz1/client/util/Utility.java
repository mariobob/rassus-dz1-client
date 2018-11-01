package hr.fer.ztel.rassus.dz1.client.util;

public class Utility {

    /** Disable instantiation. */
    private Utility() {}

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

}
