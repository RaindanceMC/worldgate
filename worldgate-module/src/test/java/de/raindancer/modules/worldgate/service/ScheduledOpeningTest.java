package de.raindancer.modules.worldgate.service;

import de.raindancer.core.platform.log.Log;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.worldgate.WorldGateSettings;
import de.raindancer.modules.worldgate.model.Dimension;
import de.raindancer.modules.worldgate.model.GateState;
import de.raindancer.modules.worldgate.model.OpeningTime;
import de.raindancer.modules.worldgate.store.GateStateStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** A launch keeps the Nether shut for three days and the End for a week, then opens them by itself. */
class ScheduledOpeningTest {

    private static final Instant NETHER_OPENS = OpeningTime.parse("2026-11-24T16:00").orElseThrow();

    @TempDir
    Path folder;

    private WorldGateService service() {
        WorldGateService service = new WorldGateService(new GateStateStore(folder), Log.of("worldgate-test"),
                new Messages(folder.resolve("messages.yml")), WorldGateSettings.DEFAULTS);
        service.load();
        return service;
    }

    @Test
    @DisplayName("a time is German time unless it says otherwise, and reads back the way it was meant")
    void germanTime() {
        assertThat(NETHER_OPENS).isEqualTo(Instant.parse("2026-11-24T15:00:00Z"));
        assertThat(OpeningTime.parse("2026-07-01T16:00")).contains(Instant.parse("2026-07-01T14:00:00Z"));
        assertThat(OpeningTime.parse("2026-11-24T16:00Z")).contains(Instant.parse("2026-11-24T16:00:00Z"));
        assertThat(OpeningTime.parse("next tuesday")).isEmpty();
        assertThat(OpeningTime.when(NETHER_OPENS)).isEqualTo("24.11.2026, 16:00");
    }

    @Test
    @DisplayName("a scheduled dimension is closed until then, and that survives a restart")
    void closedUntilThen() {
        assertThat(service().schedule(Dimension.NETHER, NETHER_OPENS)).isTrue();

        WorldGateService restarted = service();
        assertThat(restarted.state(Dimension.NETHER)).isEqualTo(GateState.CLOSED);
        assertThat(restarted.opensAt(Dimension.NETHER)).contains(NETHER_OPENS);
        assertThat(restarted.opensAt(Dimension.END)).isEmpty();
        assertThat(restarted.refusal("world_nether", false)).contains(GateState.CLOSED);
        assertThat(restarted.refusal("world_nether", true)).isEmpty();
    }

    @Test
    @DisplayName("it opens by itself once the time has come, not a moment before, and only once")
    void opensWhenDue() {
        WorldGateService service = service();
        service.schedule(Dimension.NETHER, NETHER_OPENS);
        service.schedule(Dimension.END, NETHER_OPENS.plusSeconds(4 * 86_400));

        assertThat(service.anyDue(NETHER_OPENS.minusSeconds(1))).isFalse();
        assertThat(service.anyDue(NETHER_OPENS)).isTrue();
        assertThat(service.openDue(NETHER_OPENS.minusSeconds(1))).isEmpty();
        assertThat(service.state(Dimension.NETHER)).isEqualTo(GateState.CLOSED);

        assertThat(service.openDue(NETHER_OPENS)).containsExactly(Dimension.NETHER);
        assertThat(service.openDue(NETHER_OPENS.plusSeconds(60))).isEmpty();

        WorldGateService restarted = service();
        assertThat(restarted.state(Dimension.NETHER)).isEqualTo(GateState.OPEN);
        assertThat(restarted.opensAt(Dimension.NETHER)).isEmpty();
        assertThat(restarted.state(Dimension.END)).isEqualTo(GateState.CLOSED);
    }

    @Test
    @DisplayName("an opening missed while the server was down happens on the first check after it starts")
    void missedWhileDown() {
        service().schedule(Dimension.END, NETHER_OPENS);
        assertThat(service().openDue(NETHER_OPENS.plusSeconds(86_400))).containsExactly(Dimension.END);
    }

    @Test
    @DisplayName("opening by hand, or locking again, drops the schedule so it cannot reopen later")
    void handOverridesSchedule() {
        WorldGateService service = service();
        service.schedule(Dimension.NETHER, NETHER_OPENS);
        service.set(Dimension.NETHER, GateState.OPEN);
        assertThat(service.opensAt(Dimension.NETHER)).isEmpty();

        service.schedule(Dimension.END, NETHER_OPENS);
        service.set(Dimension.END, GateState.CLOSED);
        assertThat(service.openDue(NETHER_OPENS.plusSeconds(60))).isEmpty();
        assertThat(service.state(Dimension.END)).isEqualTo(GateState.CLOSED);
    }

    @Test
    @DisplayName("an unreadable time in the file is no schedule, and the lock itself stays")
    void unreadableTime() throws Exception {
        Files.writeString(folder.resolve("worldgate.yml"), "nether: CLOSED\nnether-opens-at: soon\n");
        WorldGateService service = service();
        assertThat(service.state(Dimension.NETHER)).isEqualTo(GateState.CLOSED);
        assertThat(service.opensAt(Dimension.NETHER)).isEmpty();
    }
}
