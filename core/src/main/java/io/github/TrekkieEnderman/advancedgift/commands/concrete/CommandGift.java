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

package io.github.TrekkieEnderman.advancedgift.commands.concrete;

import java.util.*;
import java.util.stream.Collectors;

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import io.github.TrekkieEnderman.advancedgift.commands.SimpleCommand;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Painting.ArtistHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;

import org.jetbrains.annotations.NotNull;

public class CommandGift extends SimpleCommand {
    private final static char[] SPACE_DELIMITER = new char[]{' '};
    private final HashMap<UUID, Long> cooldown = new HashMap<>();

    public CommandGift(AdvancedGift plugin) {
        super(plugin, "gift", "advancedgift.gift.send");
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage(Message.COMMAND_GIFT_DESCRIPTION.translatePrefixed());
        sender.sendMessage(Message.COMMAND_GIFT_USAGE.translate()); // TODO any good way to hide the last argument if messages are disabled?
    }

    @Override
    public boolean run(@NotNull final Player sender, @NotNull final String label, @NotNull final String[] args) {
        if (args.length == 0) {
            showUsage(sender);
            return true;
        }

        // Get target
        Player target = null;
        final PlayerInventory senderInventory = sender.getInventory();
        List<Player> matchList = Bukkit.matchPlayer(args[0]);
        if (!sender.hasPermission("advancedgift.bypass.vanish")) {
            matchList = matchList.stream()
                    .filter(player -> !isVanished(player))
                    .collect(Collectors.toList());
        }

        if (matchList.size() == 1 && matchList.getFirst().equals(sender)) {
            sender.sendMessage(Message.SEND_GIFT_SELF.translatePrefixed());
            return false;
        }

        matchList = matchList.stream().filter(player -> !player.equals(sender)).collect(Collectors.toList());
        if (matchList.size() == 1) {
            target = matchList.getFirst();
        } else if (matchList.size() > 1) {
            sender.sendMessage(Message.MULTIPLE_TARGET_FOUND.translatePrefixed());
            final ComponentBuilder<TextComponent, TextComponent.Builder> builder = Component.text();

            HoverEvent<Component> hoverEvent = Message.TIP_CLICK_TO_SEND_GIFT.translate().asHoverEvent();
            final String[] argsClone = args.clone(); //we want to reuse the exact command the player used, and just change the target name
            boolean first = true;
            for (Player player : matchList) {
                argsClone[0] = player.getName(); //replace the original 1st argument with new name
                final String commandString = "/gift " + String.join(" ", argsClone);
                ClickEvent clickEvent = net.kyori.adventure.text.event.ClickEvent.suggestCommand(commandString);
                Component entry = player.displayName().hoverEvent(hoverEvent).clickEvent(clickEvent);

                if (!first) builder.appendSpace();
                first = false;
                builder.append(entry);
            }
            sender.sendMessage(builder);
            return true;
        }

        if (target == null) {
            sender.sendMessage(Message.TARGET_NOT_ONLINE.translatePrefixed(args[0]));
            return false;
        }

        // Get ItemStack
        final ItemStack giftItem = senderInventory.getItemInMainHand().clone();
        if (giftItem.getType() == Material.AIR) {
            sender.sendMessage(Message.GIFT_EMPTY.translatePrefixed());
            return false;
        }

        if (!canSendGift(sender, target, giftItem)) return true;
        if (args.length == 1) {
            sendItem(sender, target, giftItem, giftItem.getAmount(), null);
            return true;
        }

        // Get gift amount
        int giftAmount = getGiftAmount(sender, giftItem, args[1]);

        // Validate gift amount
        if (giftAmount < 1) {
            sender.sendMessage(Message.INVALID_GIFT_AMOUNT.translatePrefixed());
            return false;
        }
        if (!senderInventory.containsAtLeast(giftItem, giftAmount)) {
            sender.sendMessage(Message.INSUFFICIENT_GIFT_AMOUNT.translatePrefixed());
            logGiftDenied(sender.getName(), sender.getName() + " doesn't have the amount specified.");
            return false;
        }

        if (args.length == 2 || !this.plugin.getConfigFile().getBoolean("allow-gift-message")) {
            sendItem(sender, target, giftItem, giftAmount, null);
            return true;
        }
        if (!sender.hasPermission("advancedgift.gift.message")) {
            sender.sendMessage(Message.MESSAGE_REMOVED_NO_PERMISSION.translatePrefixed());
            logGiftWarning("Removed " + sender.getName() + "'s gift message; 'advancedgift.gift.message' is required to send messages");
            sendItem(sender, target, giftItem, giftAmount, null);
            return true;
        }

        // Get message
        final String[] originalMessage = Arrays.copyOfRange(args, 2, args.length);
        String[] giftMessage = getGiftMessage(originalMessage);

        // Validate message
        if (!Arrays.equals(originalMessage, giftMessage)) {
            final String sendCensoredMessage = plugin.getConfigFile().getString("send-censored-message", "block");
            if (sendCensoredMessage.equalsIgnoreCase("with")) {
                logGiftWarning("Censored the blocked words in " + sender.getName() + "'s gift message.");
            } else if (sendCensoredMessage.equalsIgnoreCase("without")) {
                giftMessage = null;
                sender.sendMessage(Message.MESSAGE_REMOVED_INAPPROPRIATE.translate());
                logGiftWarning("Removed " +sender.getName() + "'s gift message: contains blocked words.");
            } else if (sendCensoredMessage.equalsIgnoreCase("block")) {
                sender.sendMessage(Message.GIFT_DENIED_INAPPROPRIATE_MESSAGE.translate());
                logGiftDenied(sender.getName(), sender.getName() + "'s gift message contains blocked words.");
                return true;
            }
        }
        sendItem(sender, target, giftItem, giftAmount, giftMessage);
        return true;
    }

    @SuppressWarnings("DataFlowIssue") // Item couldn't be null if #isSimilar() passes.
    private int getTotalAmountHas(final PlayerInventory senderInventory, final @NotNull ItemStack itemstack) {
        int hasAmount = 0;
        for (ItemStack item : senderInventory.getStorageContents()) {
            if (itemstack.isSimilar(item)) {
                hasAmount += item.getAmount();
            }
        }
        return hasAmount;
    }

    private int getGiftAmount(final Player sender, final ItemStack itemStack, final String amountInput) {
        final PlayerInventory senderInventory = sender.getInventory();
        if (amountInput.equalsIgnoreCase("hand")) {
            return itemStack.getAmount();
        }
        if (amountInput.equalsIgnoreCase("all")) {
            return getTotalAmountHas(senderInventory, itemStack);
        }
        return NumberUtils.toInt(amountInput);
    }

    private boolean canSendGift(final Player sender, final Player target, final ItemStack itemstack) {
        final UUID senderUUID = sender.getUniqueId();
        final UUID targetUUID = target.getUniqueId();
        final PlayerInventory targetInventory = target.getInventory();
        final String senderName = sender.getName();
        final String targetName = target.getName();

        if (plugin.getConfigFile().getBoolean("restrict-interworld-gift")) {
            final int senderWorldGroup = plugin.getPlayerWorldGroup(sender);
            final int targetWorldGroup = plugin.getPlayerWorldGroup(target);
            if (senderWorldGroup == -1 && !(sender.hasPermission("advancedgift.bypass.world.blacklist"))) {
                sender.sendMessage(Message.SENDER_IN_BLACKLISTED_WORLD.translatePrefixed());
                logGiftDenied(senderName, senderName + " is in " + sender.getWorld().getName() + ", a blacklisted world.");
                return false;
            }
            if (targetWorldGroup == -1 && !(sender.hasPermission("advancedgift.bypass.world.blacklist"))) {
                sender.sendMessage(Message.TARGET_IN_BLACKLISTED_WORLD.translatePrefixed(targetName));
                logGiftDenied(senderName, "Target " + targetName + " is in " + target.getWorld().getName() + ", a blacklisted world.");
                return false;
            }
            if (senderWorldGroup != (targetWorldGroup) && !(sender.hasPermission("advancedgift.bypass.world.restriction"))) {
                sender.sendMessage(Message.INTERWORLD_GIFT_PROHIBITED.translatePrefixed(targetName));
                logGiftDenied(senderName, senderName + " and " + targetName + " are not in the same group of worlds.");
                return false;
            }
        }
        if (!(target.hasPermission("advancedgift.gift.receive"))) {
            sender.sendMessage(Message.TARGET_CANNOT_RECEIVE_GIFTS_CURRENTLY.translatePrefixed(targetName));
            logGiftDenied(senderName, "permission 'advancedgift.gift.receive' is required to receive gifts.");
            return false;
        }
        if (plugin.hasArtMap()) {
            final ArtMap artMap = ArtMap.instance();
            if (artMap.getConfiguration().FORCE_ART_KIT) {
                ArtistHandler artistHandler = artMap.getArtistHandler();
                if (artistHandler.containsPlayer(sender)) {
                    sender.sendMessage(Message.GIFT_DENIED_GENERIC.translatePrefixed());
                    logGiftDenied(senderName, "ArtMap has force-artkit enabled and " + senderName + "is currently making an artmap.");
                    return false;
                } else if (artistHandler.containsPlayer(target)) {
                    sender.sendMessage(Message.TARGET_CANNOT_RECEIVE_GIFTS_CURRENTLY.translatePrefixed(targetName));
                    logGiftDenied(senderName, "ArtMap has force-artkit enabled and " + targetName + "is currently making an artmap.");
                    return false;
                }

            }
        }
        if (plugin.getPlayerDataManager().containsUUID(targetUUID, "tg", null)) {
            sender.sendMessage(Message.TARGET_NOT_ACCEPTING_GIFTS_CURRENTLY.translatePrefixed(targetName));
            logGiftDenied(senderName, targetName + " doesn't currently accept gifts.");
            return false;
        }
        if (plugin.getPlayerDataManager().containsUUID(targetUUID, "block", senderUUID)) {
            sender.sendMessage(Message.TARGET_NOT_ACCEPTING_GIFTS_CURRENTLY.translatePrefixed(targetName));
            logGiftDenied(senderName, targetName + " has " + senderName + " blocked.");
            return false;
        }
        int timeRemaining;
        if ((timeRemaining = getPlayerCooldownTime(sender)) > 0) {
            sender.sendMessage(Message.GIFT_COOLDOWN_NOT_OVER.translatePrefixed(timeRemaining));
            logGiftDenied(senderName, senderName + "is still on a /gift cooldown.");
            return false;
        }
        if (targetInventory.firstEmpty() == -1) {
            int space = 0;
            for (ItemStack item: targetInventory.getStorageContents()) {
                if (itemstack.isSimilar(item)) {
                    //noinspection DataFlowIssue Item couldn't be null if #isSimilar() passes.
                    space = item.getMaxStackSize() - item.getAmount();
                    if (space > 0) break;
                }
            }
            if (space == 0) {
                sender.sendMessage(Message.TARGET_INVENTORY_FULL.translatePrefixed(targetName));
                target.sendMessage(Message.YOUR_INVENTORY_FULL.translatePrefixed(senderName));
                logGiftDenied(senderName, targetName + "'s inventory is full.");
                return false;
            }
        }
        return true;
    }

    private boolean isVanished(final Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

    private int getPlayerCooldownTime(final Player player) {
        if (!plugin.getConfigFile().getBoolean("enable-cooldown")) return 0;
        if (player.hasPermission("advancedgift.bypass.cooldown")) return 0;
        final UUID senderUUID = player.getUniqueId();
        if (!cooldown.containsKey(senderUUID)) return 0;
        //Adds 1 more second, as long division would truncate remainders. Simpler than other solutions, and off by only one millisecond.
        //Which is fine for a command cooldown. Nobody but a computer or a supernerd is going to notice it.
        return (int) ((cooldown.get(senderUUID) - System.currentTimeMillis())/1000 + 1);
    }

    private String[] getGiftMessage(final String[] originalMessage) {
        if (originalMessage == null || originalMessage.length == 0) {
            return originalMessage;
        }
        if (!plugin.getConfigFile().getBoolean("message-censorship")) {
            return originalMessage;
        }
        final String[] cleanedMessage = originalMessage.clone();
        for (int i = 0; i < cleanedMessage.length; i++) {
            String word = cleanedMessage[i].replaceAll("\\W", "").replace("_", "").toLowerCase();
            if (word.isEmpty()) continue;
            boolean isBlockedWord;
            for (String blockedWord : plugin.getConfigFile().getStringList("word-filter")) {
                String blockedWordCleaned = blockedWord.replace("*", "").toLowerCase();
                if (blockedWord.startsWith("*") && blockedWord.endsWith("*"))
                    isBlockedWord = word.contains(blockedWordCleaned);
                else if (blockedWord.startsWith("*")) isBlockedWord = word.endsWith(blockedWordCleaned);
                else if (blockedWord.endsWith("*")) isBlockedWord = word.startsWith(blockedWordCleaned);
                else isBlockedWord = word.equalsIgnoreCase(blockedWord);
                if (isBlockedWord) {
                    cleanedMessage[i] = "***";
                }
            }
        }
        return cleanedMessage;
    }

    private void sendItem (final Player sender, final Player target, final ItemStack itemStack, int giftAmount, final String[] message) {
        final ItemStack giftItem = itemStack.clone();
        giftItem.setAmount(giftAmount);

        if (plugin.getConfigFile().getBoolean("enable-cooldown"))
            cooldown.put(sender.getUniqueId(), System.currentTimeMillis() + plugin.getConfigFile().getLong("cooldown-time")*1000);
        plugin.getGiftCounter().increment();
        final PlayerInventory senderInventory = sender.getInventory();
        final PlayerInventory targetInventory = target.getInventory();
        senderInventory.removeItem(giftItem);
        final HashMap<Integer, ItemStack> excess = targetInventory.addItem(giftItem);
        if (!excess.isEmpty()) {
            sender.sendMessage(Message.TARGET_INVENTORY_ALMOST_FULL.translatePrefixed(target.getName()));
            target.sendMessage(Message.YOUR_INVENTORY_ALMOST_FULL.translatePrefixed(sender.getName()));
            logGiftWarning("Sent only a part of " + sender.getName() + "'s gift: " + target.getName() + "'s inventory was nearly full.");
            for (ItemStack extra : excess.values()) {
                giftAmount -= extra.getAmount();
                senderInventory.addItem(extra);
            }
        }
        sendNotification(sender, target, itemStack, giftAmount, message == null ? "" : String.join(" ", message));
    }

    private void sendNotification(final Player sender, final Player target, final ItemStack itemstack, final int giftAmount, final String message) {
        // TODO need a good and simple way to get a translated material name
        final String material = itemstack.getType().toString().replace("_", " ").toLowerCase();
        String itemDetails = WordUtils.capitalize(material, SPACE_DELIMITER);

        final boolean hasItemMeta = itemstack.hasItemMeta();
        final ItemMeta meta = itemstack.getItemMeta();

        //add prefix
        if (hasItemMeta && meta.hasEnchants()) {
            itemDetails = Message.ENCHANTED_ITEM.translateRaw(itemDetails);
        }
        if (isPatternedBanner(itemstack)) {
            itemDetails = Message.PATTERNED_ITEM.translateRaw(itemDetails);
        }

        //add suffix
        if (hasItemMeta && meta.hasDisplayName()) {
            itemDetails = Message.NAMED_ITEM.translateRaw(itemDetails, meta.getDisplayName());
        }

        final String senderName = sender.getName();
        final String targetName = target.getName();

        final Component senderComp = Message.GIFT_SENT.translatePrefixed(targetName, giftAmount, itemDetails).hoverEvent(itemstack.asHoverEvent());
        final Component targetComp = Message.GIFT_RECEIVED.translatePrefixed(senderName, giftAmount, itemDetails).hoverEvent(itemstack.asHoverEvent());
        final Component spyComp = Message.GIFT_LOGGED.translatePrefixed(senderName, targetName, giftAmount, itemDetails).hoverEvent(itemstack.asHoverEvent());

        sender.sendMessage(senderComp);
        target.sendMessage(targetComp);
        if (!message.isEmpty()) {
            sender.sendMessage(Message.MESSAGE_SENT.translate(message));
            target.sendMessage(Message.MESSAGE_RECEIVED.translate(message));
        }

        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player == sender || player == target) continue;
            if (plugin.getPlayerDataManager().containsUUID(player.getUniqueId(), "spy", null)) {
                player.sendMessage(spyComp);
                if (!message.isEmpty()) player.sendMessage(Message.MESSAGE_LOGGED.translate(senderName, message));
            }
        }

        logGiftSent(message, senderName, targetName, itemstack, itemDetails);
    }

    private boolean isPatternedBanner(final ItemStack itemstack) {
        if (itemstack.getType().toString().toUpperCase().contains("BANNER")) {
            if (itemstack.getItemMeta() instanceof BannerMeta meta) {
                return meta.numberOfPatterns() > 0;
            }
        }
        return false;
    }

    private void logMessage(final String message) { plugin.getLogger().info("[AG LOG] > " + message); }

    private void logGiftDenied(final String senderName, final String reason) { logMessage("Denied " + senderName + "'s gift: " + reason); }

    @SuppressWarnings("deprecation")
    private void logGiftSent(final String message, final String senderName, final String targetName, final ItemStack itemstack, final String itemDetails) {
        logMessage(senderName + " gave " + targetName + " " + itemDetails + ".");
        if (itemstack.hasItemMeta()) {
            final ItemMeta itemmeta = itemstack.getItemMeta();
            if (itemmeta.hasEnchants() || itemmeta.hasLore()) {
                logMessage("   More item info on " + senderName + "'s gift:");
                if (itemmeta.isUnbreakable()) plugin.getLogger().info("   - Unbreakable");
                if (itemmeta.hasLore()) {
                    final ArrayList<String> loreList = new ArrayList<>();
                    for (String lore : itemmeta.getLore()) {
                        loreList.add("[" + lore + "]");
                    }
                    logMessage("   - Lore: " + String.join("; ", loreList));
                }
                if (itemmeta.hasEnchants()) {
                    final ArrayList<String> enchantmentList= new ArrayList<>();
                    for (Enchantment key : itemstack.getEnchantments().keySet()) {
                        String name = key.getKey().toString().replace("minecraft:", "").toUpperCase();
                        enchantmentList.add(name + " " + itemstack.getEnchantments().get(key));
                    }
                    logMessage("   - Enchantments: " + String.join(", ", enchantmentList));
                }
            }
        }
        if (!message.isEmpty()) logMessage(senderName + "'s gift message: " + message);
    }

    private void logGiftWarning(final String message) {logMessage("Warning: " + message);}
}