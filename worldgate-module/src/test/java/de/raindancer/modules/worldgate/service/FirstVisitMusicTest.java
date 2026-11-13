package de.raindancer.modules.worldgate.service;

import de.raindancer.core.platform.log.Log;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.worldgate.WorldGateSettings;
import de.raindancer.modules.worldgate.model.Dimension;
import de.raindancer.modules.worldgate.model.GateState;
import de.raindancer.modules.worldgate.store.GateStateStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

/** Whoever walks into a dimension for the first time after its scheduled opening hears its music, once. */
class FirstVisitMusicTest {

    private static final Instant OPENS = Instant.parse("2026-11-24T15:00:00Z");

    @TempDir
    Path folder;

    private WorldGateService service() {
        WorldGateService service = new WorldGateService(new GateStateStore(folder), Log.of("worldgate-test"),
                new Messages(folder.resolve("messages.yml")), WorldGateSettings.DEFAULTS);
        service.load();
        return service;
    }

    @Test
    @DisplayName("a scheduled opening is remembered as the dimension's opening, across a restart")
    void openingRemembered() {
        WorldGateService service = service();
        service.schedule(Dimension.NETHER, OPENS);
        assertThat(service.openedAt(Dimension.NETHER)).isEmpty();
        service.openDue(OPENS);
        assertThat(service().openedAt(Dimension.NETHER)).contains(OPENS);
        assertThat(service().openedAt(Dimension.END)).isEmpty();
    }

    @Test
    @DisplayName("opening by hand is no opening with music; a dimension never scheduled has none")
    void byHandNoMusic() {
        WorldGateService service = service();
        service.set(Dimension.END, GateState.OPEN);
        assertThat(service.openedAt(Dimension.END)).isEmpty();
    }

    @Test
    @DisplayName("the first visit after the opening plays it; a second visit, or a visit while closed, does not")
    void firstVisitOnly() {
        WorldGateService service = service();
        service.schedule(Dimension.NETHER, OPENS);
        assertThat(service.firstVisit(Dimension.NETHER, OptionalLong.empty())).as("closed").isEmpty();

        service.openDue(OPENS);
        assertThat(service.firstVisit(Dimension.NETHER, OptionalLong.empty())).hasValue(OPENS.toEpochMilli());
        assertThat(service.firstVisit(Dimension.NETHER, OptionalLong.of(OPENS.toEpochMilli()))).isEmpty();
        assertThat(service.firstVisit(Dimension.END, OptionalLong.empty())).as("never scheduled").isEmpty();

        service.set(Dimension.NETHER, GateState.DRAINED);
        assertThat(service.firstVisit(Dimension.NETHER, OptionalLong.empty())).as("locked again").isEmpty();
    }

    @Test
    @DisplayName("a later opening — after a reset, say — greets everybody again")
    void laterOpening() {
        WorldGateService service = service();
        service.schedule(Dimension.END, OPENS);
        service.openDue(OPENS);
        Instant again = OPENS.plusSeconds(30L * 86_400);
        service.schedule(Dimension.END, again);
        service.openDue(again);
        assertThat(service.firstVisit(Dimension.END, OptionalLong.of(OPENS.toEpochMilli())))
                .hasValue(again.toEpochMilli());
    }
}
