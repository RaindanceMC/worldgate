package de.raindancer.modules.worldgate.model;

import java.time.Instant;
import java.util.Optional;

/** When each dimension opens by itself; null where nothing is scheduled. */
public record Openings(Instant nether, Instant end) {

    public static final Openings NONE = new Openings(null, null);

    public Optional<Instant> of(Dimension dimension) {
        return Optional.ofNullable(dimension == Dimension.NETHER ? nether : end);
    }

    public Openings with(Dimension dimension, Instant at) {
        return dimension == Dimension.NETHER ? new Openings(at, end) : new Openings(nether, at);
    }
}
