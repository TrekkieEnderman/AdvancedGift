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

package io.github.TrekkieEnderman.advancedgift;

import io.github.TrekkieEnderman.advancedgift.locale.Message;
import io.github.TrekkieEnderman.advancedgift.util.ComponentUtils;
import io.github.TrekkieEnderman.advancedgift.util.ItemUtils;
import io.github.TrekkieEnderman.advancedgift.util.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class GiftManager {
    private final AdvancedGift plugin;
    private final Config config;

    // Keep these two separate as one of them may change independently of other
    private final static String WILDCARD = "*";
    private final static String MASK = "*";

    private final HashMap<UUID, Long> cooldown = new HashMap<>();

    public GiftManager(AdvancedGift plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
    }

    public void sendGift(final Player sender, final Player target, final ItemStack itemStack, int amount, String giftMessage) {
        // Validate gift message
        if (giftMessage != null && config.isGiftMessageEnabled()) {
            final String original = giftMessage;
            giftMessage = filterGiftMessage(giftMessage);
            if (!original.equals(giftMessage)) {
                final CensorshipOptions option = config.getCensorshipMode();
                if (option == CensorshipOptions.REMOVE) {
                    giftMessage = null;
                    sender.sendMessage(Message.MESSAGE_REMOVED_INAPPROPRIATE.translate());
                } else if (option == CensorshipOptions.DENY) {
                    sender.sendMessage(Message.GIFT_DENIED_INAPPROPRIATE_MESSAGE.translate());
                    return;
                }
            }
        }

        if (giftMessage != null && !config.isGiftMessageEnabled()) {
            giftMessage = null;
        }

        if (!canSendGift(sender, target, itemStack)) return;

        // Send item
        final ItemStack giftItem = itemStack.asQuantity(amount);
        if (config.isCooldownEnabled())
            cooldown.put(sender.getUniqueId(), System.currentTimeMillis() + config.getCooldownDuration()*1000);
        plugin.getGiftCounter().increment();
        final PlayerInventory senderInventory = sender.getInventory();
        final PlayerInventory targetInventory = target.getInventory();
        senderInventory.removeItem(giftItem);
        final HashMap<Integer, ItemStack> excess = targetInventory.addItem(giftItem);
        if (!excess.isEmpty()) {
            sender.sendMessage(Message.TARGET_INVENTORY_ALMOST_FULL.translatePrefixed(target.getName()));
            target.sendMessage(Message.YOUR_INVENTORY_ALMOST_FULL.translatePrefixed(sender.getName()));
            for (ItemStack extra : excess.values()) {
                amount -= extra.getAmount();
                senderInventory.addItem(extra);
            }
        }

        // Send notification
        /*
         TODO GlobalTranslator has render() I can use for translating materials. Use this
            after the component situation with Messages enum is figured out
         */
        String itemDetails = ItemUtils.getPrettyMaterialName(giftItem);

        final boolean hasItemMeta = giftItem.hasItemMeta();
        final ItemMeta meta = giftItem.getItemMeta();

        // Add prefix
        if (hasItemMeta && meta.hasEnchants()) {
            itemDetails = Message.ENCHANTED_ITEM.translateRaw(itemDetails);
        }
        if (ItemUtils.isPatternedBanner(giftItem)) {
            itemDetails = Message.PATTERNED_ITEM.translateRaw(itemDetails);
        }

        // Add suffix
        if (hasItemMeta && meta.hasDisplayName()) {
            // TODO revise this later. I prefer to add the display name as a component without converting it
            itemDetails = Message.NAMED_ITEM.translateRaw(itemDetails, ComponentUtils.toLegacyText(meta.displayName()));
        }

        final HoverEvent<HoverEvent.ShowItem> hover = giftItem.asHoverEvent();
        final Component senderComp = Message.GIFT_SENT.translatePrefixed(target.getName(), amount, itemDetails)
                .hoverEvent(hover);
        final Component targetComp = Message.GIFT_RECEIVED.translatePrefixed(sender.getName(), amount, itemDetails)
                .hoverEvent(hover);
        final Component spyComp = Message.GIFT_LOGGED.translatePrefixed(sender.getName(), target.getName(), amount, itemDetails)
                .hoverEvent(hover);

        sender.sendMessage(senderComp);
        target.sendMessage(targetComp);
        if (giftMessage != null) {
            sender.sendMessage(Message.MESSAGE_SENT.translate(giftMessage));
            target.sendMessage(Message.MESSAGE_RECEIVED.translate(giftMessage));
        }

        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player == sender || player == target) continue;
            if (plugin.getPlayerDataManager().isSpy(player.getUniqueId())) {
                player.sendMessage(spyComp);
                if (giftMessage != null) player.sendMessage(Message.MESSAGE_LOGGED.translate(sender.getName(), giftMessage));
            }
        }

        logGiftSent(giftMessage, sender.getName(), target.getName(), giftItem, itemDetails);
    }

    private boolean canSendGift(final Player sender, final Player target, final ItemStack itemstack) {
        final UUID senderUUID = sender.getUniqueId();
        final UUID targetUUID = target.getUniqueId();
        final String senderName = sender.getName();
        final String targetName = target.getName();

        if (config.isInterworldGiftRestricted()) {
            final int senderWorldGroup = config.getGroupID(sender.getWorld());
            final int targetWorldGroup = config.getGroupID(target.getWorld());
            if (senderWorldGroup == -1 && !(sender.hasPermission("advancedgift.bypass.world.blacklist"))) {
                sender.sendMessage(Message.SENDER_IN_BLACKLISTED_WORLD.translatePrefixed());
                return false;
            }
            if (targetWorldGroup == -1 && !(sender.hasPermission("advancedgift.bypass.world.blacklist"))) {
                sender.sendMessage(Message.TARGET_IN_BLACKLISTED_WORLD.translatePrefixed(targetName));
                return false;
            }
            if (senderWorldGroup != (targetWorldGroup) && !(sender.hasPermission("advancedgift.bypass.world.restriction"))) {
                sender.sendMessage(Message.INTERWORLD_GIFT_PROHIBITED.translatePrefixed(targetName));
                return false;
            }
        }
        if (!(target.hasPermission("advancedgift.gift.receive"))) {
            sender.sendMessage(Message.TARGET_CANNOT_RECEIVE_GIFTS_CURRENTLY.translatePrefixed(targetName));
            return false;
        }
        if (plugin.isPainting(sender)) {
            sender.sendMessage(Message.GIFT_DENIED_GENERIC.translatePrefixed());
            return false;
        }
        if (plugin.isPainting(target)) {
            sender.sendMessage(Message.TARGET_CANNOT_RECEIVE_GIFTS_CURRENTLY.translatePrefixed(targetName));
            return false;
        }
        if (plugin.getPlayerDataManager().isGiftDisabled(targetUUID)) {
            sender.sendMessage(Message.TARGET_NOT_ACCEPTING_GIFTS_CURRENTLY.translatePrefixed(targetName));
            return false;
        }
        if (plugin.getPlayerDataManager().hasPlayerBlocked(targetUUID, senderUUID)) {
            sender.sendMessage(Message.TARGET_NOT_ACCEPTING_GIFTS_CURRENTLY.translatePrefixed(targetName));
            return false;
        }
        int timeRemaining;
        if ((timeRemaining = getPlayerCooldownTime(sender)) > 0) {
            sender.sendMessage(Message.GIFT_COOLDOWN_NOT_OVER.translatePrefixed(timeRemaining));
            return false;
        }
        if (!PlayerUtils.hasSpace(target, itemstack)) {
            sender.sendMessage(Message.TARGET_INVENTORY_FULL.translatePrefixed(targetName));
            target.sendMessage(Message.YOUR_INVENTORY_FULL.translatePrefixed(senderName));
            return false;
        }
        return true;
    }

    private int getPlayerCooldownTime(final Player player) {
        if (!config.isCooldownEnabled()) return 0;
        if (player.hasPermission("advancedgift.bypass.cooldown")) return 0;
        final UUID senderUUID = player.getUniqueId();
        if (!cooldown.containsKey(senderUUID)) return 0;
        //Adds 1 more second, as long division would truncate remainders. Simpler than other solutions, and off by only one millisecond.
        //Which is fine for a command cooldown. Nobody but a computer or a supernerd is going to notice it.
        return (int) ((cooldown.get(senderUUID) - System.currentTimeMillis())/1000 + 1);
    }

    private @Nullable String filterGiftMessage(final String original) {
        if (original == null || original.isBlank()) {
            return original;
        }
        final List<String> filters = config.getWordFilters();
        final String[] cleaned = original.split(" ");
        for (int i = 0; i < cleaned.length; i++) {
            String word = cleaned[i].replaceAll("\\W_", "").toLowerCase();
            if (word.isBlank()) continue;
            boolean matched;
            for (String filter : filters) {
                String compare = filter.replace(WILDCARD, "").toLowerCase();
                if (filter.startsWith(WILDCARD) && filter.endsWith(WILDCARD)) {
                    matched = word.contains(compare);
                } else if (filter.startsWith(WILDCARD)) {
                    matched = word.endsWith(compare);
                } else if (filter.endsWith(WILDCARD)) {
                    matched = word.startsWith(compare);
                } else {
                    matched = word.equals(compare);
                }

                if (matched) {
                    cleaned[i] = MASK.repeat(cleaned[i].length());
                }
            }
        }
        return String.join(" ", cleaned);
    }

    @SuppressWarnings({"deprecation", "DataFlowIssue"})
    private void logGiftSent(final String message, final String senderName, final String targetName, final ItemStack itemstack, final String itemDetails) {
        plugin.getLogger().info(senderName + " gave " + targetName + " " + itemDetails + ".");
        if (itemstack.hasItemMeta()) {
            final ItemMeta itemmeta = itemstack.getItemMeta();
            if (itemmeta.hasEnchants() || itemmeta.hasLore()) {
                plugin.getLogger().info("   More item info on " + senderName + "'s gift:");
                if (itemmeta.isUnbreakable()) plugin.getLogger().info("   - Unbreakable");
                if (itemmeta.hasLore()) {
                    final ArrayList<String> loreList = new ArrayList<>();
                    for (String lore : itemmeta.getLore()) {
                        loreList.add("[" + lore + "]");
                    }
                    plugin.getLogger().info("   - Lore: " + String.join("; ", loreList));
                }
                if (itemmeta.hasEnchants()) {
                    final ArrayList<String> enchantmentList= new ArrayList<>();
                    for (Enchantment key : itemstack.getEnchantments().keySet()) {
                        String name = key.getKey().toString().replace("minecraft:", "").toUpperCase();
                        enchantmentList.add(name + " " + itemstack.getEnchantments().get(key));
                    }
                    plugin.getLogger().info("   - Enchantments: " + String.join(", ", enchantmentList));
                }
            }
        }
        if (!message.isEmpty()) plugin.getLogger().info(senderName + "'s gift message: " + message);
    }
}
