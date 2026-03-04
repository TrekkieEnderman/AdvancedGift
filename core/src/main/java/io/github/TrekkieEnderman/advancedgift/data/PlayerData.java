/*
 * Copyright (c) 2026 TrekkieEnderman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.TrekkieEnderman.advancedgift.data;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Set;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class PlayerData {
    private final PlayerDataManager manager;
    private final UUID uuid;

    public void setGiftEnabled(boolean enabled) {
        manager.setGiftDisabled(uuid, !enabled);
    }

    public boolean isGiftEnabled() {
        return !manager.isGiftDisabled(uuid);
    }

    public void setSpy(boolean enabled) {
        manager.setSpy(uuid, enabled);
    }

    public boolean isSpy() {
        return manager.isSpy(uuid);
    }

    public boolean blockPlayer(UUID other) {
        return manager.blockPlayer(uuid, other);
    }

    public boolean unblockPlayer(UUID other) {
        return manager.unblockPlayer(uuid, other);
    }

    public boolean hasPlayerBlocked(UUID other) {
        return manager.hasPlayerBlocked(uuid, other);
    }

    public Set<UUID> getBlockList() {
        return Set.copyOf(manager.getBlockList(uuid));
    }

    public void clearBlockList() {
        manager.clearBlockList(uuid);
    }
}
