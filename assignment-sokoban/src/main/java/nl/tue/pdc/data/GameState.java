package nl.tue.pdc.data;

import nl.tue.pdc.concurrent.MurmurHash3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * Represents the dynamic position of objects.
 * The array {@code crates} should always be sorted.
 * The solution is stored by referring to the direction from the previous state (i.e. in a sort of trie structure)
 */
public record GameState(Point player, Point[] crates, GameState previous, Direction dirFromPrevious) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameState gs)) return false;

        // Two states can be equivalent even if the paths are different
        // Use Arrays.equals since crates is assumed to be sorted.
        return Objects.equals(player, gs.player) && Arrays.equals(crates, gs.crates);
    }

    @Override
    public int hashCode() {
        byte[] bytes = new byte[2 + 2 * crates.length];
        bytes[0] = (byte) player.x();
        bytes[1] = (byte) player.y();
        for (int i = 0; i < crates.length; i++) {
            bytes[2 + 2 * i] = (byte) crates[i].x();
            bytes[2 + 2 * i + 1] = (byte) crates[i].y();
        }
        return MurmurHash3.murmurhash3_x86_32(bytes);
    }

    public List<Direction> getAccessSequence() {
        List<Direction> accessSequence = new ArrayList<>();
        GameState current = this;
        while (current.previous != null) {
            accessSequence.addFirst(current.dirFromPrevious());
            current = current.previous;
        }
        return accessSequence;
    }

    public String getLurdString() {
        StringBuilder sb = new StringBuilder();
        getAccessSequence().forEach(sb::append);
        return sb.toString();
    }
}
