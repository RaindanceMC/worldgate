package de.raindancer.modules.worldgate.opening;

import de.raindancer.modules.worldgate.model.Dimension;
import de.raindancer.modules.worldgate.model.Openings;
import de.raindancer.modules.worldgate.opening.OpeningCountdown.Bar;
import de.raindancer.modules.worldgate.opening.OpeningCountdown.Beat;
import de.raindancer.modules.worldgate.opening.OpeningCountdown.Reminder;
import de.raindancer.modules.worldgate.opening.OpeningCountdown.Tick;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** What players see and hear on the way to an opening: reminders, a bar for the last minutes, a title for the last seconds. */
class OpeningCountdownTest {

    private static final Instant AT = Instant.parse("2026-11-24T15:00:00Z");
    private final Openings nether = Openings.NONE.with(Dimension.NETHER, AT);

    @Test
    @DisplayName("a reminder a day, an hour, ten minutes and a minute before — each once, as the time is crossed")
    void reminders() {
        OpeningCountdown countdown = new OpeningCountdown();
        List<Duration> said = new ArrayList<>();
        for (Instant now = AT.minus(Duration.ofDays(2)); now.isBefore(AT); now = now.plusSeconds(1)) {
            countdown.step(nether, now).stream().filter(Reminder.class::isInstance)
                    .map(beat -> ((Reminder) beat).left()).forEach(said::add);
        }
        assertThat(said).containsExactly(Duration.ofDays(1), Duration.ofHours(1), Duration.ofMinutes(10),
                Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("the last ten seconds are counted down one by one, and a missed second is not repeated later")
    void ticks() {
        OpeningCountdown countdown = new OpeningCountdown();
        List<Integer> counted = new ArrayList<>();
        for (Instant now = AT.minusSeconds(15); now.isBefore(AT); now = now.plusSeconds(1)) {
            countdown.step(nether, now).stream().filter(Tick.class::isInstance)
                    .map(beat -> ((Tick) beat).seconds()).forEach(counted::add);
        }
        assertThat(counted).containsExactly(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);

        OpeningCountdown late = new OpeningCountdown();
        late.step(nether, AT.minusSeconds(9));
        assertThat(late.step(nether, AT.minusSeconds(5))).filteredOn(Tick.class::isInstance)
                .containsExactly(new Tick(Dimension.NETHER, 5));
    }

    @Test
    @DisplayName("a bar for the last ten minutes, emptying as the time runs out, none before")
    void bar() {
        OpeningCountdown countdown = new OpeningCountdown();
        assertThat(countdown.step(nether, AT.minus(Duration.ofMinutes(11)))).noneMatch(Bar.class::isInstance);
        List<Beat> beats = countdown.step(nether, AT.minus(Duration.ofMinutes(5)));
        assertThat(beats).filteredOn(Bar.class::isInstance).singleElement()
                .satisfies(beat -> {
                    assertThat(((Bar) beat).left()).isEqualTo(Duration.ofMinutes(5));
                    assertThat(((Bar) beat).progress()).isEqualTo(0.5f);
                });
    }

    @Test
    @DisplayName("a server started an hour before says nothing it has missed — only what comes next")
    void startedLate() {
        OpeningCountdown countdown = new OpeningCountdown();
        assertThat(countdown.step(nether, AT.minus(Duration.ofMinutes(59)))).isEmpty();
        assertThat(countdown.step(nether, AT.minus(Duration.ofSeconds(9 * 60 + 59))))
                .filteredOn(Reminder.class::isInstance).hasSize(1);
    }

    @Test
    @DisplayName("nothing scheduled, or the time already here, is no countdown")
    void nothing() {
        OpeningCountdown countdown = new OpeningCountdown();
        assertThat(countdown.step(Openings.NONE, AT)).isEmpty();
        assertThat(countdown.step(nether, AT)).isEmpty();
        assertThat(countdown.step(nether, AT.plusSeconds(3))).isEmpty();
    }

    @Test
    @DisplayName("the bar's clock reads like one")
    void clock() {
        assertThat(OpeningCountdown.clock(Duration.ofSeconds(9 * 60 + 42))).isEqualTo("9:42");
        assertThat(OpeningCountdown.clock(Duration.ofMillis(4_200))).isEqualTo("0:05");
    }
}
