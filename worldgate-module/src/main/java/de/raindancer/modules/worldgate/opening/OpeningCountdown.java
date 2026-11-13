package de.raindancer.modules.worldgate.opening;

import de.raindancer.modules.worldgate.model.Dimension;
import de.raindancer.modules.worldgate.model.Openings;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The way to a scheduled opening, asked once a second: what is due to be said or shown right now. Pure; the
 * module turns the beats into chat lines, a boss bar and titles. A beat fires when its moment is crossed between
 * two asks, so a skipped second or a lagging server never says one twice, and a server started late does not
 * catch up on what it missed.
 */
public final class OpeningCountdown {

    static final List<Duration> REMINDERS = List.of(Duration.ofDays(1), Duration.ofHours(1), Duration.ofMinutes(10),
            Duration.ofMinutes(1));
    static final Duration BAR = Duration.ofMinutes(10);
    static final int TITLE_SECONDS = 10;

    public sealed interface Beat permits Reminder, Bar, Tick {
        Dimension dimension();
    }

    /** A line in chat: it opens in {@code left}. */
    public record Reminder(Dimension dimension, Duration left) implements Beat {
    }

    /** The boss bar, {@code progress} from 1 (ten minutes to go) to 0. */
    public record Bar(Dimension dimension, Duration left, float progress) implements Beat {
    }

    /** One of the last ten seconds, as a title. */
    public record Tick(Dimension dimension, int seconds) implements Beat {
    }

    private final Map<Dimension, Instant> lastAsked = new EnumMap<>(Dimension.class);

    public synchronized List<Beat> step(Openings openings, Instant now) {
        List<Beat> beats = new ArrayList<>();
        for (Dimension dimension : Dimension.values()) {
            Optional<Instant> at = openings.of(dimension);
            Instant before = lastAsked.put(dimension, now);
            if (at.isEmpty()) {
                continue;
            }
            Duration left = Duration.between(now, at.get());
            if (left.isZero() || left.isNegative()) {
                continue;
            }
            Duration leftBefore = before == null ? null : Duration.between(before, at.get());
            for (Duration reminder : REMINDERS) {
                if (leftBefore != null && leftBefore.compareTo(reminder) > 0 && left.compareTo(reminder) <= 0) {
                    beats.add(new Reminder(dimension, reminder));
                }
            }
            if (left.compareTo(BAR) <= 0) {
                beats.add(new Bar(dimension, left, (float) left.toMillis() / BAR.toMillis()));
            }
            int seconds = seconds(left);
            if (seconds <= TITLE_SECONDS && (leftBefore == null || seconds(leftBefore) > seconds)) {
                beats.add(new Tick(dimension, seconds));
            }
        }
        return beats;
    }

    /** {@code 9:42}, rounded up so the bar never shows 0:00 before the opening. */
    public static String clock(Duration left) {
        int seconds = seconds(left);
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    private static int seconds(Duration left) {
        return (int) Math.ceil(left.toMillis() / 1000.0);
    }
}
