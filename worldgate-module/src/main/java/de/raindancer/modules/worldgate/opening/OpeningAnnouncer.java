package de.raindancer.modules.worldgate.opening;

import de.raindancer.core.ui.bossbar.BarPriority;
import de.raindancer.core.ui.bossbar.BarStyle;
import de.raindancer.core.ui.bossbar.BossBars;
import de.raindancer.core.ui.chat.Style;
import de.raindancer.core.ui.effect.Cues;
import de.raindancer.core.ui.effect.Effects;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.text.Markup;
import de.raindancer.core.world.time.Times;
import de.raindancer.modules.worldgate.model.Dimension;
import de.raindancer.modules.worldgate.model.OpeningTime;
import de.raindancer.modules.worldgate.service.WorldGateService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Everybody online, on the way to a scheduled opening and at it: reminders in chat, a boss bar for the last ten
 * minutes, the last ten seconds as titles, and a title when it opens. Runs on the global thread.
 */
public final class OpeningAnnouncer {

    private static final Title.Times TICK = Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ofMillis(150));
    private static final Title.Times OPENED = Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4),
            Duration.ofSeconds(1));

    private final Server server;
    private final Messages messages;
    private final Effects effects;
    private final BossBars bars;
    private final Set<Dimension> barShown = EnumSet.noneOf(Dimension.class);

    public OpeningAnnouncer(Server server, Messages messages, Effects effects, BossBars bars) {
        this.server = server;
        this.messages = messages;
        this.effects = effects;
        this.bars = bars;
    }

    public void show(List<OpeningCountdown.Beat> beats, WorldGateService gate) {
        Set<Dimension> barred = EnumSet.noneOf(Dimension.class);
        for (OpeningCountdown.Beat beat : beats) {
            Markup name = branded(beat.dimension());
            switch (beat) {
                case OpeningCountdown.Reminder reminder -> everybody(player -> {
                    messages.send(player, "worldgate.countdown-reminder", "dimension", name,
                            "remaining", Times.describe(reminder.left()),
                            "when", gate.opensAt(beat.dimension()).map(OpeningTime::when).orElse(""));
                    effects.play(player.getUniqueId(), Cues.NOTIFY);
                });
                case OpeningCountdown.Bar bar -> {
                    barred.add(bar.dimension());
                    BarStyle style = BarStyle.of(messages.get("worldgate.countdown-bar", "dimension", name,
                                    "clock", OpeningCountdown.clock(bar.left())))
                            .progress(bar.progress()).colour(BossBar.Color.YELLOW);
                    everybody(player -> bars.show(player.getUniqueId(), owner(bar.dimension()), style, BarPriority.HIGH));
                }
                case OpeningCountdown.Tick tick -> everybody(player -> {
                    player.showTitle(Title.title(
                            messages.get("worldgate.countdown-title", "seconds", tick.seconds()),
                            messages.get("worldgate.countdown-subtitle", "dimension", name), TICK));
                    effects.play(player.getUniqueId(), Cues.COUNTDOWN);
                });
            }
        }
        for (Dimension dimension : Dimension.values()) {
            if (!barred.contains(dimension) && barShown.contains(dimension)) {
                everybody(player -> bars.clear(player.getUniqueId(), owner(dimension)));
            }
        }
        barShown.clear();
        barShown.addAll(barred);
    }

    public void opened(List<Dimension> opened) {
        for (Dimension dimension : opened) {
            Markup name = branded(dimension);
            everybody(player -> {
                bars.clear(player.getUniqueId(), owner(dimension));
                player.showTitle(Title.title(messages.get("worldgate.opened-title", "dimension", name),
                        messages.get("worldgate.opened-subtitle", "dimension", name), OPENED));
                messages.send(player, "worldgate.opened-now", "dimension", name);
                effects.play(player.getUniqueId(), Cues.COUNTDOWN_DONE);
            });
            barShown.remove(dimension);
        }
    }

    /** The dimension's name in the server's own gradient, as the chat prefix wears it. */
    public static Markup branded(Dimension dimension) {
        return Markup.of("<gradient:" + Style.brandFrom() + ":" + Style.brandTo() + ">" + dimension.label()
                + "</gradient>");
    }

    private static String owner(Dimension dimension) {
        return "worldgate:" + dimension.name().toLowerCase(Locale.ROOT);
    }

    private void everybody(Consumer<Player> action) {
        for (Player player : List.copyOf(server.getOnlinePlayers())) {
            action.accept(player);
        }
    }
}
