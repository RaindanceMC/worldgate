package de.raindancer.modules.worldgate.opening;

import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.worldgate.model.Dimension;
import de.raindancer.modules.worldgate.pack.WorldGatePack;
import de.raindancer.modules.worldgate.service.WorldGateService;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A dimension's music, once per player, on their first visit after its scheduled opening. Which opening they last
 * heard is kept on the player, so a later opening greets them again. Played on the records slider: the game's own
 * dimension music is stopped while it runs, and players who turned music off still get their welcome.
 */
public final class FirstVisitMusic implements Listener {

    private static final long QUIET_EVERY_TICKS = 40L;

    private final Plugin plugin;
    private final WorldGateService gate;
    private final Messages messages;

    public FirstVisitMusic(Plugin plugin, WorldGateService gate, Messages messages) {
        this.plugin = plugin;
        this.gate = gate;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArrive(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        gate.dimensionOf(player.getWorld().getName()).ifPresent(dimension -> greet(player, dimension));
    }

    private void greet(Player player, Dimension dimension) {
        NamespacedKey key = new NamespacedKey("rainsworldgate", "heard_" + dimension.name().toLowerCase(Locale.ROOT));
        PersistentDataContainer data = player.getPersistentDataContainer();
        Long heard = data.get(key, PersistentDataType.LONG);
        OptionalLong play = gate.firstVisit(dimension, heard == null ? OptionalLong.empty() : OptionalLong.of(heard));
        if (play.isEmpty()) {
            return;
        }
        data.set(key, PersistentDataType.LONG, play.getAsLong());

        WorldGatePack.Track track = WorldGatePack.track(dimension);
        player.stopSound(SoundStop.source(Sound.Source.MUSIC));
        player.playSound(Sound.sound(track.sound(), Sound.Source.RECORD, 1f, 1f), Sound.Emitter.self());
        player.sendActionBar(messages.get("worldgate.now-playing", "title", track.title(), "by", track.by()));

        // The game starts its own Nether or End music on a timer of its own; kept quiet until ours has ended.
        long rounds = track.length().toMillis() / (QUIET_EVERY_TICKS * 50) + 1;
        AtomicInteger left = new AtomicInteger((int) rounds);
        Scheduling.entityTimer(plugin, player, QUIET_EVERY_TICKS, QUIET_EVERY_TICKS, task -> {
            if (left.decrementAndGet() < 0) {
                task.cancel();
                return;
            }
            player.stopSound(SoundStop.source(Sound.Source.MUSIC));
        });
    }
}
