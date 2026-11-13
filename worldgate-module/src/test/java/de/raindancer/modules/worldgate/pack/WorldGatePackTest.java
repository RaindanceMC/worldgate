package de.raindancer.modules.worldgate.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.raindancer.modules.worldgate.model.Dimension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** The music each dimension greets its first visitors with, as Core's resource pack carries it. */
class WorldGatePackTest {

    private static final Path ROOT = Path.of("src/main/resources" + WorldGatePack.ROOT);

    @Test
    @DisplayName("FILES is exactly what is in the pack folder, so nothing is left behind when it is copied out")
    void filesListed() throws IOException {
        Set<String> onDisk = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(ROOT)) {
            walk.filter(Files::isRegularFile).forEach(file -> onDisk.add(ROOT.relativize(file).toString()
                    .replace('\\', '/')));
        }
        assertThat(new TreeSet<>(WorldGatePack.FILES)).isEqualTo(onDisk);
    }

    @Test
    @DisplayName("each dimension's track is a streamed sound whose file is in the pack, and it knows how long it runs")
    void eachDimensionHasItsTrack() throws IOException {
        JsonObject sounds = JsonParser.parseString(Files.readString(ROOT.resolve("assets/rainsworldgate/sounds.json")))
                .getAsJsonObject();
        for (Dimension dimension : Dimension.values()) {
            WorldGatePack.Track track = WorldGatePack.track(dimension);
            assertThat(track.sound().namespace()).isEqualTo("rainsworldgate");
            JsonObject sound = sounds.getAsJsonObject(track.sound().value()).getAsJsonArray("sounds").get(0)
                    .getAsJsonObject();
            assertThat(sound.get("stream").getAsBoolean()).as("music is streamed, not loaded whole").isTrue();
            String file = sound.get("name").getAsString().replace("rainsworldgate:", "");
            assertThat(ROOT.resolve("assets/rainsworldgate/sounds/" + file + ".ogg")).isRegularFile();
            assertThat(track.length()).isPositive();
            assertThat(track.title()).isNotBlank();
            assertThat(track.by()).isNotBlank();
        }
    }
}
