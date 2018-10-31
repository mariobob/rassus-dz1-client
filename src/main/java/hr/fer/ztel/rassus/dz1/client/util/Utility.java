package hr.fer.ztel.rassus.dz1.client.util;

import java.util.OptionalInt;

public class Utility {

    private Utility() {}

    public static OptionalInt averageOptionalInt(OptionalInt n1, OptionalInt n2) {
        if (n1.isPresent() && n2.isPresent()) {
            return OptionalInt.of((n1.getAsInt() + n2.getAsInt()) / 2);
        } else if (n1.isPresent()) {
            return n1;
        } else if (n2.isPresent()) {
            return n2;
        } else {
            return OptionalInt.empty();
        }
    }

}
