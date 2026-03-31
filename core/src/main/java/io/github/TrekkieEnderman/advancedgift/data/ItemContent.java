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

import io.github.TrekkieEnderman.advancedgift.locale.Message;
import io.github.TrekkieEnderman.advancedgift.locale.TranslationManager;
import io.github.TrekkieEnderman.advancedgift.util.ItemUtils;
import io.github.TrekkieEnderman.advancedgift.util.PlayerUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.Optional;

@NullMarked
public class ItemContent implements GiftContent {
    private final ItemStack itemStack;

    public ItemContent(ItemStack itemStack, int amount) {
        this.itemStack = itemStack.asQuantity(amount);
    }

    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    public int getAmount() {
        return itemStack.getAmount();
    }

    @Override
    public boolean canGive(Player target) {
        return PlayerUtils.hasSpace(target, itemStack);
    }

    @Override
    public Optional<ItemContent> giveContent(Player target) {
        // Item must be cloned or its amount would change as the result of this!
        final Map<Integer, ItemStack> excess = target.getInventory().addItem(itemStack.clone());
        if (excess.isEmpty()) {
            return Optional.empty();
        }
        // Since I only gave one entry for the vararg, I can just get the first index from map
        final int excessAmount = excess.get(0).getAmount();
        itemStack.subtract(excessAmount);
        return Optional.of(new ItemContent(itemStack, excessAmount));
    }

    public Component getDetails() {
        Component itemDetails = TranslationManager.render(itemStack.getType().translationKey());
        final boolean hasItemMeta = itemStack.hasItemMeta();
        final ItemMeta meta = itemStack.getItemMeta();

        // Add prefix
        if (hasItemMeta && meta.hasEnchants()) {
            itemDetails = Message.ENCHANTED_ITEM.translate(itemDetails);
        }
        if (ItemUtils.isPatternedBanner(itemStack)) {
            itemDetails = Message.PATTERNED_ITEM.translate(itemDetails);
        }

        // Add suffix
        if (hasItemMeta && meta.hasCustomName()) {
            //noinspection DataFlowIssue
            itemDetails = Message.NAMED_ITEM.translate(itemDetails, meta.customName());
        }

        return Message.ITEM_DETAILS_BASE.translate(Component.text(getAmount()), itemDetails)
                .hoverEvent(itemStack.asHoverEvent());
    }
}
