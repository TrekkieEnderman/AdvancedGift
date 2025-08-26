/*
 * Copyright (c) 2025 TrekkieEnderman
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

package io.github.TrekkieEnderman.advancedgift.nms;

import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface NMSInterface {
    /**
     * Converts the item to a JSON string representation
     * @param item item stack to be converted
     * @return a JSON string representing the item
     */
    @NotNull String getAsJsonString(ItemStack item);

    /**
     * Converts the item to a hover event representation
     * @param item item stack to be converted
     * @return a hover event representing the item
     */
    @NotNull Optional<HoverEvent> getAsHoverEvent(ItemStack item);
}