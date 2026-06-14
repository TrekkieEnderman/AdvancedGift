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

package io.github.TrekkieEnderman.advancedgift.gift;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface GiftContent {
    /**
     * Whether the target can receive the gift content. Depending on the content type,
     * the target may not be in the right mode, not have enough inventory space, etc.
     * @param target player.
     * @return {@code true} if the target can take the gift, {@code false} otherwise.
     */
    boolean canGive(Player target);

    /**
     * Gives the content of the gift to the target.
     * @param target player.
     * @return excess if any.
     */
    Optional<? extends GiftContent> giveContent(Player target);

    /**
     * Gets details about the content of the gift, typically for a gift notification.
     * @return component,
     */
    Component getDetails();
}
