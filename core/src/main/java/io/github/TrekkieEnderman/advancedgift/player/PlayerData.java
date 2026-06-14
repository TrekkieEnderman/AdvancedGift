/*
 * Copyright (c) 2026 TrekkieEnderman
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.TrekkieEnderman.advancedgift.player;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@NullMarked
public class PlayerData {
    @Getter
    @Setter
    private boolean giftEnabled = true;
    @Getter
    @Setter
    private boolean spy = false;
    private Set<UUID> blocked = new HashSet<>();

    public PlayerData() {}

    public PlayerData(boolean giftEnabled, boolean spy, Set<UUID> blocked) {
        this.giftEnabled = giftEnabled;
        this.spy = spy;
        this.blocked = blocked;
    }

    public boolean blockPlayer(UUID other) {
        return blocked.add(other);
    }

    public boolean unblockPlayer(UUID other) {
        return blocked.remove(other);
    }

    public boolean hasPlayerBlocked(UUID other) {
        return blocked.contains(other);
    }

    public Set<UUID> getBlockList() {
        return Set.copyOf(blocked);
    }

    public void clearBlockList() {
        if (blocked.isEmpty()) return;
        blocked.clear();
    }
}
