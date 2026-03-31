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

import io.github.TrekkieEnderman.advancedgift.data.GiftContent;
import io.github.TrekkieEnderman.advancedgift.data.PlayerData;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import io.github.TrekkieEnderman.advancedgift.util.ComponentUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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

    public void sendGift(final Player sender, final Player target, final GiftContent gift, String message) {
        // Validate gift message
        if (message != null && config.isGiftMessageEnabled()) {
            final String original = message;
            message = filterGiftMessage(message);
            if (!original.equals(message)) {
                final CensorshipOptions option = config.getCensorshipMode();
                if (option == CensorshipOptions.REMOVE) {
                    message = null;
                    sender.sendMessage(Message.MESSAGE_REMOVED_INAPPROPRIATE.translate());
                } else if (option == CensorshipOptions.DENY) {
                    sender.sendMessage(Message.GIFT_DENIED_INAPPROPRIATE_MESSAGE.translate());
                    gift.giveContent(sender);
                    return;
                }
            }
        }

        if (message != null && !config.isGiftMessageEnabled()) {
            message = null;
        }

        final Component giftMessage = message != null ? Component.text(message) : null;

        // Check if it is OK to send the gift
        if (!canSendGift(sender, target, gift)) {
            gift.giveContent(sender);
            return;
        }

        // Send gift
        if (config.isCooldownEnabled())
            cooldown.put(sender.getUniqueId(), System.currentTimeMillis() + config.getCooldownDuration()*1000);
        plugin.getGiftCounter().increment();
        gift.giveContent(target).ifPresent(excess -> {
            // TODO either generalize these messages or make new ones more suitable for this
            sender.sendMessage(Message.TARGET_INVENTORY_ALMOST_FULL.translatePrefixed(target.name()));
            target.sendMessage(Message.YOUR_INVENTORY_ALMOST_FULL.translatePrefixed(sender.name()));
            excess.giveContent(sender);
        });

        // Send notification
        Component giftDetails = gift.getDetails();
        sender.sendMessage(Message.GIFT_SENT.translatePrefixed(target.name(), giftDetails));
        target.sendMessage(Message.GIFT_RECEIVED.translatePrefixed(sender.name(), giftDetails));
        if (giftMessage != null) {
            sender.sendMessage(Message.MESSAGE_SENT.translate(giftMessage));
            target.sendMessage(Message.MESSAGE_RECEIVED.translate(giftMessage));
        }
        final Component spyNotification = Message.GIFT_LOGGED.translatePrefixed(sender.name(), target.name(), giftDetails);
        final Component spyMessage = giftMessage != null ? Message.MESSAGE_LOGGED.translate(sender.name(), giftMessage) : null;
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player == sender || player == target) continue;
            if (plugin.getPlayerDataManager().getData(player).isSpy()) {
                player.sendMessage(spyNotification);
                if (spyMessage != null) player.sendMessage(spyMessage);
            }
        }
        plugin.getLogger().info(ComponentUtils.toPlainText(spyNotification));
        if (spyMessage != null) plugin.getLogger().info(ComponentUtils.toPlainText(spyMessage));
    }

    private boolean canSendGift(Player sender, Player target, GiftContent gift) {
        final Component targetName = target.name();
        final PlayerData targetData = plugin.getPlayerDataManager().getData(target);
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
        if (!targetData.isGiftEnabled()) {
            sender.sendMessage(Message.TARGET_NOT_ACCEPTING_GIFTS_CURRENTLY.translatePrefixed(targetName));
            return false;
        }
        if (targetData.hasPlayerBlocked(sender.getUniqueId())) {
            sender.sendMessage(Message.TARGET_NOT_ACCEPTING_GIFTS_CURRENTLY.translatePrefixed(targetName));
            return false;
        }
        int timeRemaining;
        if ((timeRemaining = getPlayerCooldownTime(sender)) > 0) {
            sender.sendMessage(Message.GIFT_COOLDOWN_NOT_OVER.translatePrefixed(Component.text(timeRemaining), Argument.tagResolver(Formatter.choice("choice", timeRemaining))));
            return false;
        }
        if (!gift.canGive(target)) {
            sender.sendMessage(Message.TARGET_CANNOT_RECEIVE_GIFTS_CURRENTLY.translatePrefixed());
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
}
