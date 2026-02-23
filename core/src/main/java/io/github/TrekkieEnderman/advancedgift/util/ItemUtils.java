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
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

@UtilityClass
public class ItemUtils {

    public static boolean isPatternedBanner(final ItemStack itemstack) {
        if (itemstack.getType().toString().toUpperCase().contains("BANNER")) {
            if (itemstack.getItemMeta() instanceof BannerMeta meta) {
                return meta.numberOfPatterns() > 0;
            }
        }
        return false;
    }

    // TODO improve this so certain words like "of" don't get capitalized. Try to get closer to how Minecraft shows it.
    //  E.g. "Totem of Undying"
    public static String getPrettyMaterialName(ItemStack itemStack) {
        return WordUtils.capitalizeFully(itemStack.getType().getKey().getKey().replace("_", " "));
    }
}
