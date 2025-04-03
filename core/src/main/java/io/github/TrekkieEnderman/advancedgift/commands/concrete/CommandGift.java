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

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import io.github.TrekkieEnderman.advancedgift.ServerVersion;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import io.github.TrekkieEnderman.advancedgift.locale.Translation;
import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Painting.ArtistHandler;
import net.md_5.bungee.api.chat.*;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;

import net.md_5.bungee.api.ChatColor;

import com.meowj.langutils.lang.LanguageHelper;
import org.jetbrains.annotations.NotNull;

public class CommandGift implements CommandExecutor {
    private final AdvancedGift plugin;
    private final static char[] SPACE_DELIMITER = new char[]{' '};
    private final HashMap<UUID, Long> cooldown = new HashMap<>();

    public CommandGift(AdvancedGift plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender commandSender, @NotNull final Command cmd, @NotNull final String label, @NotNull final String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(plugin.getPrefix() + Message.COMMAND_FOR_PLAYER_ONLY.translate());
            return true;
        }
        final Player sender = (Player) commandSender;
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(plugin.getPrefix() + Message.COMMAND_GIFT_DESCRIPTION.translate());
            sender.sendMessage(Message.COMMAND_GIFT_USAGE_1.translate());
            sender.sendMessage(Message.COMMAND_GIFT_USAGE_2.translate()); // TODO any good way to hide the last argument if messages are disabled?
            return true;
        }
        if (!(sender.hasPermission("advancedgift.gift.send"))) {
            sender.sendMessage(plugin.getPrefix() + Message.COMMAND_NO_PERMISSION.translate());
            logGiftDenied(sender.getName(), "Permission 'advancedgift.gift.send' is required to send gifts.");
            return true;
        }

        // Get target
        Player target = null;
        final PlayerInventory senderInventory = sender.getInventory();
        final List<Player> matchList = Bukkit.matchPlayer(args[0]);
        matchList.remove(sender);

        if (matchList.size() == 1) {
            target = matchList.get(0);
        } else if (matchList.size() > 1) {
            sender.sendMessage(plugin.getPrefix() + Message.MULTIPLE_TARGET_FOUND.translate());
            final ComponentBuilder builder = new ComponentBuilder("");

            final HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Message.TIP_CLICK_TO_SEND_GIFT.translate()).create());
            final String[] argsClone = args.clone(); //we want to reuse the exact command the player used, and just change the target name
            for (Player player : matchList) {
                TextComponent textComponent = new TextComponent(TextComponent.fromLegacyText(player.getDisplayName()));

                argsClone[0] = player.getName(); //replace the original 1st argument with new name
                final String commandString = "/gift " + String.join(" ", argsClone);
                ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandString);

                if (builder.getCursor() != 0) builder.append(" ", ComponentBuilder.FormatRetention.NONE);
                if (ServerVersion.getMinorVersion() < 12) {
                    builder.append(textComponent.toLegacyText());
                } else {
                    builder.append(textComponent);
                }
                builder.event(hoverEvent).event(clickEvent);
            }
            sender.spigot().sendMessage(builder.create());
            return true;
        }

        if (target == null || (isVanished(target) && !sender.hasPermission("advancedgift.bypass.vanish"))) {
            sender.sendMessage(plugin.getPrefix() + Message.TARGET_NOT_ONLINE.translate(args[0]));
            return true;
        }
        if (target == sender.getPlayer()) {
            sender.sendMessage(plugin.getPrefix() + Message.SEND_GIFT_SELF.translate());
            return true;
        }

        // Get ItemStack
        @SuppressWarnings("deprecation")
        final ItemStack giftItem = (ServerVersion.getMinorVersion() < 9 ? senderInventory.getItemInHand() : senderInventory.getItemInMainHand()).clone();
        if (giftItem.getType() == Material.AIR) {
            sender.sendMessage(plugin.getPrefix() + Message.GIFT_EMPTY.translate());
            return true;
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
            sender.sendMessage(plugin.getPrefix() + Message.INVALID_GIFT_AMOUNT.translate());
            return true;
        }
        if (!senderInventory.containsAtLeast(giftItem, giftAmount)) {
            sender.sendMessage(plugin.getPrefix() + Message.INSUFFICIENT_GIFT_AMOUNT.translate());
            logGiftDenied(sender.getName(), sender.getName() + " doesn't have the amount specified.");
            return true;
        }

        if (args.length == 2 || !this.plugin.getConfigFile().getBoolean("allow-gift-message")) {
            sendItem(sender, target, giftItem, giftAmount, null);
            return true;
        }
        if (!sender.hasPermission("advancedgift.gift.message")) {
            sender.sendMessage(plugin.getPrefix() + Message.MESSAGE_REMOVED_NO_PERMISSION.translate());
            logGiftWarning("Removed " + sender.getName() + "'s gift message; 'advancedgift.gift.message' is required to send messages");
            sendItem(sender, target, giftItem, giftAmount, null);
            return true;
        }

        // Get message
        final String[] originalMessage = Arrays.copyOfRange(args, 2, args.length);
        String[] giftMessage = getGiftMessage(originalMessage);

        // Validate message
        if (!Arrays.equals(originalMessage, giftMessage)) {
            final String sendCensoredMessage = plugin.getConfigFile().getString("send-censored-message");
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

    private int getTotalAmountHas(final PlayerInventory senderInventory, final ItemStack itemstack) {
        int hasAmount = 0;
        final ItemStack[] contents = ServerVersion.getMinorVersion() > 8 ? senderInventory.getStorageContents() : senderInventory.getContents();
        for (ItemStack item : contents) { //getContents also returns offhand and armor slots, which we don't want to
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
                sender.sendMessage(plugin.getPrefix() + Message.SENDER_IN_BLACKLISTED_WORLD.translate());
                logGiftDenied(senderName, senderName + " is in " + sender.getWorld().getName() + ", a blacklisted world.");
                return false;
            }
            if (targetWorldGroup == -1 && !(sender.hasPermission("advancedgift.bypass.world.blacklist"))) {
                sender.sendMessage(plugin.getPrefix() + Message.TARGET_IN_BLACKLISTED_WORLD.translate(targetName));
                logGiftDenied(senderName, "Target " + targetName + " is in " + target.getWorld().getName() + ", a blacklisted world.");
                return false;
            }
            if (senderWorldGroup != (targetWorldGroup) && !(sender.hasPermission("advancedgift.bypass.world.restriction"))) {
                sender.sendMessage(plugin.getPrefix() + Message.INTERWORLD_GIFT_PROHIBITED.translate(targetName));
                logGiftDenied(senderName, senderName + " and " + targetName + " are not in the same group of worlds.");
                return false;
            }
        }
        if (!(target.hasPermission("advancedgift.gift.receive"))) {
            sender.sendMessage(plugin.getPrefix() + Message.TARGET_CANNOT_RECEIVE_GIFTS_CURRENTLY.translate(targetName));
            logGiftDenied(senderName, "permission 'advancedgift.gift.receive' is required to receive gifts.");
            return false;
        }
        if (plugin.hasArtMap()) {
            final ArtMap artMap = ArtMap.instance();
            /* ArtMap has to be compiled and added locally for the IDE to refer to. */
            if (artMap.getConfiguration().FORCE_ART_KIT) {
                ArtistHandler artistHandler = artMap.getArtistHandler();
                if (artistHandler.containsPlayer(sender)) {
                    sender.sendMessage(plugin.getPrefix() + Message.GIFT_DENIED_GENERIC.translate());
                    logGiftDenied(senderName, "ArtMap has force-artkit enabled and " + senderName + "is currently making an artmap.");
                    return false;
                } else if (artistHandler.containsPlayer(target)) {
                    sender.sendMessage(plugin.getPrefix() + Message.TARGET_CANNOT_RECEIVE_GIFTS_CURRENTLY.translate(targetName));
                    logGiftDenied(senderName, "ArtMap has force-artkit enabled and " + targetName + "is currently making an artmap.");
                    return false;
                }

            }
        }
        if (plugin.getPlayerDataManager().containsUUID(targetUUID, "tg", null)) {
            sender.sendMessage(plugin.getPrefix() + Message.TARGET_NOT_ACCEPTING_GIFTS_CURRENTLY.translate(targetName));
            logGiftDenied(senderName, targetName + " doesn't currently accept gifts.");
            return false;
        }
        if (plugin.getPlayerDataManager().containsUUID(targetUUID, "block", senderUUID)) {
            sender.sendMessage(plugin.getPrefix() + Message.TARGET_NOT_ACCEPTING_GIFTS_CURRENTLY.translate(targetName));
            logGiftDenied(senderName, targetName + " has " + senderName + " blocked.");
            return false;
        }
        int timeRemaining;
        if ((timeRemaining = getPlayerCooldownTime(sender)) > 0) {
            sender.sendMessage(plugin.getPrefix() + Message.GIFT_COOLDOWN_NOT_OVER.translate(timeRemaining));
            logGiftDenied(senderName, senderName + "is still on a /gift cooldown.");
            return false;
        }
        if (targetInventory.firstEmpty() == -1) {
            int space = 0;
            for (ItemStack item: targetInventory.getContents()) { // TODO change this to getStorageContents()
                if (itemstack.isSimilar(item)) {
                    space = item.getMaxStackSize() - item.getAmount();
                    if (space > 0) break;
                }
            }
            if (space == 0) {
                sender.sendMessage(plugin.getPrefix() + Message.TARGET_INVENTORY_FULL.translate(targetName));
                target.sendMessage(plugin.getPrefix() + Message.YOUR_INVENTORY_FULL.translate(senderName));
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
            sender.sendMessage(plugin.getPrefix() + Message.TARGET_INVENTORY_ALMOST_FULL.translate(target.getName()));
            target.sendMessage(plugin.getPrefix() + Message.YOUR_INVENTORY_ALMOST_FULL.translate(sender.getName()));
            logGiftWarning("Sent only a part of " + sender.getName() + "'s gift: " + target.getName() + "'s inventory was nearly full.");
            for (ItemStack extra : excess.values()) {
                giftAmount -= extra.getAmount();
                senderInventory.addItem(extra);
            }
        }
        sendNotification(sender, target, itemStack, giftAmount, message == null ? "" : String.join(" ", message));
    }

    private void sendNotification(final Player sender, final Player target, final ItemStack itemstack, final int giftAmount, final String message) {
        final String material;
        if (plugin.getExtLib().equals("LangUtils")) {
            material = LanguageHelper.getItemName(itemstack, Translation.getServerLocale().toString());
        } else {
            // TODO need a good and simple way to get a translated material name
            material = itemstack.getType().toString().replace("_", " ").toLowerCase();
        }
        String itemDetails = WordUtils.capitalize(material, SPACE_DELIMITER);

        final boolean hasItemMeta = itemstack.hasItemMeta();
        final ItemMeta meta = itemstack.getItemMeta();

        //add prefix
        if (hasItemMeta && meta.hasEnchants()) {
            itemDetails = Message.ENCHANTED_ITEM.translate(itemDetails);
        }
        if (isPatternedBanner(itemstack)) {
            itemDetails = Message.PATTERNED_ITEM.translate(itemDetails);
        }

        //add suffix
        if (hasItemMeta && meta.hasDisplayName()) {
            itemDetails = Message.NAMED_ITEM.translate(itemDetails, meta.getDisplayName());
        }

        final String senderName = sender.getName();
        final String targetName = target.getName();

        final String senderNotification = plugin.getPrefix() + Message.GIFT_SENT.translate(targetName, giftAmount, itemDetails);
        final String targetNotification = plugin.getPrefix() + Message.GIFT_RECEIVED.translate(senderName, giftAmount, itemDetails);
        final String spyNotification = plugin.getPrefix() + Message.GIFT_LOGGED.translate(senderName, targetName, giftAmount, itemDetails);
        final TextComponent senderComponent = new TextComponent(TextComponent.fromLegacyText(senderNotification));
        final TextComponent targetComponent = new TextComponent(TextComponent.fromLegacyText(targetNotification));
        final TextComponent spyComponent = new TextComponent(TextComponent.fromLegacyText(spyNotification));

        if (plugin.isTextTooltipEnabled()) {
            final BaseComponent[] hoverMessage = new ComponentBuilder(plugin.getNms().convertItemToJson(itemstack)).create();
            final HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverMessage);

            senderComponent.setHoverEvent(event);
            targetComponent.setHoverEvent(event);
            spyComponent.setHoverEvent(event);
        }

        sender.spigot().sendMessage(senderComponent);
        target.spigot().sendMessage(targetComponent);
        if (!message.isEmpty()) {
            sender.sendMessage(Message.MESSAGE_SENT.translate(message));
            target.sendMessage(Message.MESSAGE_RECEIVED.translate(message));
        }

        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player == sender || player == target) continue;
            if (plugin.getPlayerDataManager().containsUUID(player.getUniqueId(), "spy", null)) {
                player.spigot().sendMessage(spyComponent);
                if (!message.isEmpty()) player.sendMessage(Message.MESSAGE_LOGGED.translate(senderName, message));
            }
        }

        logGiftSent(message, senderName, targetName, itemstack, itemDetails);
    }

    private boolean isPatternedBanner(final ItemStack itemstack) {
        if (itemstack.getType().toString().toUpperCase().contains("BANNER")) {
            if (itemstack.getItemMeta() instanceof BannerMeta) {
                BannerMeta meta = (BannerMeta)itemstack.getItemMeta();
                return (meta.numberOfPatterns() > 0);
            }
        }
        return false;
    }

    private void logMessage(final String message) { plugin.getLogger().info("[AG LOG] > " + message); }

    private void logGiftDenied(final String senderName, final String reason) { logMessage("Denied " + senderName + "'s gift: " + reason); }

    @SuppressWarnings("deprecation")
    private void logGiftSent(final String message, final String senderName, final String targetName, final ItemStack itemstack, final String itemDetails) {
        logMessage(senderName + " gave " + targetName + " " + ChatColor.stripColor(itemDetails) + ".");
        if (itemstack.hasItemMeta()) {
            final ItemMeta itemmeta = itemstack.getItemMeta();
            if (itemmeta.hasEnchants() || itemmeta.hasLore() || (ServerVersion.getMinorVersion() >= 11 && itemmeta.isUnbreakable())) {
                logMessage("   More item info on " + senderName + "'s gift:");
                if (ServerVersion.getMinorVersion() >= 11) {
                    if (itemmeta.isUnbreakable()) plugin.getLogger().info("   - Unbreakable");
                }
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
                        String name;
                        if (ServerVersion.getMinorVersion() < 13) {
                            switch(key.getName()) {
                                case "ARROW_DAMAGE": name = "POWER"; break;
                                case "ARROW_FIRE": name = "FLAME"; break;
                                case "ARROW_INFINITE": name = "INFINITY"; break;
                                case "ARROW_KNOCKBACK": name = "PUNCH"; break;
                                case "BINDING_CURSE": name = "CURSE OF BINDING"; break;
                                case "DAMAGE_ALL": name = "SHARPNESS"; break;
                                case "DAMAGE_ARTHROPODS": name = "BANE OF ARTHROPODS"; break;
                                case "DAMAGE_UNDEAD": name = "SMITE"; break;
                                case "DEPTH_STRIDER": name = "DEPTH STRIDER"; break;
                                case "DIG_SPEED": name = "EFFICIENCY"; break;
                                case "DURABILITY": name = "UNBREAKING"; break;
                                case "FIRE_ASPECT": name = "FIRE ASPECT"; break;
                                case "FROST_WALKER": name = "FROST WALKER"; break;
                                case "KNOCKBACK": name = "KNOCKBACK"; break;
                                case "LOOT_BONUS_BLOCKS": name = "FORTUNE"; break;
                                case "LOOT_BONUS_MOBS": name = "LOOTING"; break;
                                case "LUCK": name = "LUCK OF THE SEA"; break;
                                case "LURE": name = "LURE"; break;
                                case "MENDING": name = "MENDING"; break;
                                case "OXYGEN": name = "RESPIRATION"; break;
                                case "PROTECTION_ENVIRONMENTAL": name = "PROTECTION"; break;
                                case "PROTECTION_EXPLOSIONS": name = "BLAST PROTECTION"; break;
                                case "PROTECTION_FALL": name = "FEATHER FALLING"; break;
                                case "PROTECTION_FIRE": name = "FIRE PROTECTION"; break;
                                case "PROTECTION_PROJECTILE": name = "PROJECTILE PROTECTION"; break;
                                case "SILK_TOUCH": name = "SILK TOUCH"; break;
                                case "SWEEPING_EDGE": name = "SWEEPING EDGE"; break;
                                case "THORNS": name = "THORNS"; break;
                                case "VANISHING_CURSE": name = "CURSE OF VANISHING"; break;
                                case "WATER_WORKER": name = "AQUA AFFINITY"; break;
                                default: name = "invalid enchantment"; break;
                            }
                        } else name = key.getKey().toString().replace("minecraft:", "").toUpperCase();
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