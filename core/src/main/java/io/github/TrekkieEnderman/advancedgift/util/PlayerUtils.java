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

package io.github.TrekkieEnderman.advancedgift.util;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;

@UtilityClass
public class PlayerUtils {

    public static boolean isVanished(final Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

    @SuppressWarnings("DataFlowIssue") // Item couldn't be null if #isSimilar() passes.
    public static boolean hasSpace(final Player player, final ItemStack match) {
        Inventory inventory = player.getInventory();
        if (inventory.firstEmpty() != -1) return true;
        int space = 0;
        for (ItemStack item: inventory.getStorageContents()) {
            if (match.isSimilar(item)) {
                space = item.getMaxStackSize() - item.getAmount();
                if (space > 0) break;
            }
        }
        return space == 0;
    }

    @SuppressWarnings("DataFlowIssue") // Item couldn't be null if #isSimilar() passes.
    public static int getTotalAmountHas(final Player player, final ItemStack match) {
        Inventory inventory = player.getInventory();
        int hasAmount = 0;
        for (ItemStack item : inventory.getStorageContents()) {
            if (match.isSimilar(item)) {
                hasAmount += item.getAmount();
            }
        }
        return hasAmount;
    }
}
