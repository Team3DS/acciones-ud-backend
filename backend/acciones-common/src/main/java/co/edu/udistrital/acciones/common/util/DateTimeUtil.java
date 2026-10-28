package co.edu.udistrital.acciones.common.util;

import java.time.Instant;

public final class DateTimeUtil {
    private DateTimeUtil() {}

    public static Instant nowUtc() {
        return Instant.now();
    }

    public static String toIsoString(Instant instant) {
        return instant.toString();
    }

    public static Instant fromIsoString(String isoString) {
        return Instant.parse(isoString);
    }
}