package de.raindancer.modules.worldgate.store;

import de.raindancer.core.data.store.YamlStore;
import de.raindancer.core.platform.log.Log;
import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.modules.worldgate.model.Dimension;
import de.raindancer.modules.worldgate.model.GateState;
import de.raindancer.modules.worldgate.model.GateStates;
import de.raindancer.modules.worldgate.model.OpeningTime;
import de.raindancer.modules.worldgate.model.Openings;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;

/**
 * Whether the Nether and the End are open, on disk.
 *
 * <h2>Why this is not a {@code SettingsStore}</h2>
 * A lock is something an admin flips at short notice — winding a dimension down before a wipe, or
 * shutting it entirely — not something written once when the server is set up. That is exactly the
 * distinction {@code RtpLocationStorage} draws against {@code RtpSettings}: an owner's preference goes
 * through {@code context.settings(...)}, live admin-toggled state gets its own small file on top of
 * {@link YamlStore}.
 *
 * <h2>One file, two lines</h2>
 * There is no list here, so this reads and writes the whole thing at once rather than keying entries
 * by id the way {@code RtpLocationStorage} does for a pool of many spots.
 */
public final class GateStateStore {

    private static final LogChannel log = Log.of("worldgate");

    private final YamlStore store;

    public GateStateStore(Path dataFolder) {
        this.store = new YamlStore(dataFolder.resolve("worldgate.yml"));
    }

    /** Where this is kept, for a diagnostic and for a test that wants to break the file. */
    public Path file() {
        return store.file();
    }

    /**
     * Both dimensions' state.
     *
     * <p>A missing or unreadable file is a fresh install: both open. A dimension whose value will not
     * parse is reset to open on its own, named in the log — one mangled line must not cost the server
     * the other dimension's lock.
     */
    public GateStates load() {
        YamlConfiguration yaml = store.read();
        GateState nether = read(yaml.getString("nether"), Dimension.NETHER);
        GateState end = read(yaml.getString("end"), Dimension.END);
        return new GateStates(nether, end);
    }

    /** When each dimension opens by itself. An unreadable time is no schedule; the lock stays as written. */
    public Openings loadOpenings() {
        YamlConfiguration yaml = store.read();
        return new Openings(readTime(yaml.getString("nether-opens-at"), Dimension.NETHER),
                readTime(yaml.getString("end-opens-at"), Dimension.END));
    }

    /** Writes both states and keeps whatever openings are on disk. @return whether it reached the disk */
    public boolean save(GateStates states) {
        return save(states, loadOpenings());
    }

    /** Writes everything. @return whether it reached the disk */
    public boolean save(GateStates states, Openings openings) {
        if (states == null || openings == null) {
            return false;
        }
        return store.write(yaml -> {
            yaml.set("nether", states.nether().name());
            yaml.set("end", states.end().name());
            yaml.set("nether-opens-at", openings.nether() == null ? null : openings.nether().toString());
            yaml.set("end-opens-at", openings.end() == null ? null : openings.end().toString());
        });
    }

    private static Instant readTime(String text, Dimension dimension) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Instant at = OpeningTime.parse(text).orElse(null);
        if (at == null) {
            log.warn("'{}' is not a time for {} to open, so nothing is scheduled.", text, dimension.label());
        }
        return at;
    }

    private static GateState read(String text, Dimension dimension) {
        if (text == null || text.isBlank()) {
            return GateState.OPEN;
        }
        try {
            return GateState.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException notAState) {
            log.warn("'{}' is not a state for {}, so it is treated as open.", text, dimension.label());
            return GateState.OPEN;
        }
    }
}
