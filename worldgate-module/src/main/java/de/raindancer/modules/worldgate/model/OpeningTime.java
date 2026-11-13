package de.raindancer.modules.worldgate.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/** When a scheduled dimension opens: typed and shown in German time, like the proxy's launch time. */
public final class OpeningTime {

    public static final ZoneId GERMANY = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm").withZone(GERMANY);

    private OpeningTime() {
    }

    /** {@code 2026-11-24T16:00} is German time; one with an offset or {@code Z} is taken as written. */
    public static Optional<Instant> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String raw = text.strip();
        try {
            return Optional.of(OffsetDateTime.parse(raw).toInstant());
        } catch (DateTimeParseException noOffset) {
            try {
                return Optional.of(LocalDateTime.parse(raw).atZone(GERMANY).toInstant());
            } catch (DateTimeParseException notATime) {
                return Optional.empty();
            }
        }
    }

    public static String when(Instant at) {
        return WHEN.format(at);
    }
}
