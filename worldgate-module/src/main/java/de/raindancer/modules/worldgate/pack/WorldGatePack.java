package de.raindancer.modules.worldgate.pack;

import de.raindancer.core.RainsCore;
import de.raindancer.core.content.pack.JarPack;
import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.modules.worldgate.model.Dimension;
import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/** The music a dimension greets its first visitors with after its opening, offered to Core's resource pack. */
public final class WorldGatePack {

    public static final String ROOT = "/de/raindancer/modules/worldgate/pack/";
    public static final List<String> FILES = List.of(
            "CREDITS.txt",
            "assets/rainsworldgate/sounds.json",
            "assets/rainsworldgate/sounds/music/end.ogg",
            "assets/rainsworldgate/sounds/music/nether.ogg",
            "pack.mcmeta");

    private static final String OWNER = "WorldGate";
    private static final String NAME = "first-visit-music";

    /** @param length how long it runs, so the game's own music can be kept quiet that long */
    public record Track(Key sound, Duration length, String title, String by) {
    }

    private static final Track NETHER = new Track(Key.key("rainsworldgate", "music.nether"),
            Duration.ofMillis(108_900), "Ancient Power Of Serpents", "Kevin MacLeod");
    private static final Track END = new Track(Key.key("rainsworldgate", "music.end"),
            Duration.ofMillis(53_200), "Fantasy Choir", "César da Rocha");

    private WorldGatePack() {
    }

    public static Track track(Dimension dimension) {
        return dimension == Dimension.NETHER ? NETHER : END;
    }

    public static void contribute(Plugin plugin, RainsCore core, LogChannel log) {
        try {
            JarPack.offer(plugin, core.resourcePacks(), WorldGatePack.class, ROOT, FILES, OWNER, NAME,
                    "The music of a first visit to the Nether and the End");
        } catch (IOException failure) {
            log.warn("The World Gate music could not be written ({}); a first visit is silent.", failure.getMessage());
        }
    }

    public static void withdraw(RainsCore core) {
        core.resourcePacks().withdraw(OWNER.toLowerCase(Locale.ROOT) + ":" + NAME);
    }
}
