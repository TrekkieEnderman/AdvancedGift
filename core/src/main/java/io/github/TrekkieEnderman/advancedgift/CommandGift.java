package io.github.TrekkieEnderman.advancedgift;

import java.util.*;

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

public class CommandGift implements CommandExecutor {
    private final AdvancedGift plugin;
    private final String prefix;
    private final boolean enableMessage;
    private final String usage;
    private final static char[] SPACE_DELIMITER = new char[]{' '};

    CommandGift(AdvancedGift plugin) {
        this.plugin = plugin;
        this.prefix = this.plugin.prefix;
        this.enableMessage = this.plugin.getConfigFile().getBoolean("allow-gift-message");
        this.usage = ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/gift [player]" + ChatColor.GRAY + " <amount | hand | all>" + (enableMessage ? " <your message>" : "");
    }

    private final HashMap<UUID, Long> cooldown = new HashMap<>();
    private final String agLog = "[AG LOG] > ";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
        } else {
            Player s = (Player) sender;
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                s.sendMessage(prefix + ChatColor.YELLOW + "Send your friend or foe a gift, anywhere and anytime, in an instant!");
                s.sendMessage(ChatColor.YELLOW + "Hold something in your hand and use the following command. White text is required, and gray text is optional.");
                s.sendMessage(usage);
            } else {
                Player target = null;
                PlayerInventory sinv = s.getInventory();
                List<Player> matchList = Bukkit.matchPlayer(args[0]);
                matchList.remove(s);

                if (matchList.size() == 1) {
                    target = matchList.get(0);
                } else if (matchList.size() > 1) {
                    s.sendMessage(prefix + ChatColor.YELLOW + "Several matches found. Please pick one you want to give.");
                    ComponentBuilder builder = new ComponentBuilder();

                    final HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to send this player a gift").create());
                    final String[] argsClone = args.clone(); //we want to reuse the exact command the player used, and just change the target name
                    for (Player player : matchList) {
                        TextComponent textComponent = new TextComponent(TextComponent.fromLegacyText(player.getDisplayName()));
                        textComponent.setHoverEvent(hoverEvent);

                        argsClone[0] = player.getName(); //replace the original 1st argument with new name
                        final String commandString = "/gift " + String.join(" ", argsClone);
                        textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandString));

                        if (builder.getCursor() != 0) builder.append(" ", ComponentBuilder.FormatRetention.NONE);
                        builder.append(textComponent);
                    }
                    s.spigot().sendMessage(builder.create());
                    return true;
                }

                @SuppressWarnings("deprecation")
                ItemStack itemstack = (ServerVersion.getMinorVersion() < 9 ? sinv.getItemInHand() : sinv.getItemInMainHand());
                if (!(s.hasPermission("advancedgift.gift.send"))) {
                    s.sendMessage(prefix + ChatColor.RED + "You don't have permission to use this command!");
                } else if (target == null || (isVanished(target) && !s.hasPermission("advancedgift.bypass.vanish"))) {
                    s.sendMessage(prefix + ChatColor.RED + args[0] + " is not online!");
                } else if (target == s.getPlayer()) {
                    s.sendMessage(prefix + ChatColor.RED + "You can't send yourself a gift!");
                } else if (itemstack.getType() == Material.AIR) {
                    s.sendMessage(prefix + ChatColor.RED + "You need to hold something in your hand!");
                } else {
                    if (args.length == 1) {
                        sendItem(s, target, itemstack, itemstack.getAmount(), "");
                    } else {
                        checkAmountInput(s, target, itemstack, args);
                    }
                }
            }
        }
        return true;
    }

    private int getTotalAmountHas(PlayerInventory sinv, ItemStack itemstack) {
        int hasAmount = 0;
        for (ItemStack item : sinv.getStorageContents()) { //getContents also returns offhand and armor slots, which we don't want to
            if (itemstack.isSimilar(item)) {
                hasAmount += item.getAmount();
            }
        }
        return hasAmount;
    }

    private void checkAmountInput(Player s, Player target, ItemStack itemstack, String[] args) {
        PlayerInventory sinv = s.getInventory();
        final String amountAsString = args[1];
        int giveAmount;
        if (amountAsString.equalsIgnoreCase("hand")) {
            giveAmount = itemstack.getAmount();
        } else if (amountAsString.equalsIgnoreCase("all")){
            giveAmount = getTotalAmountHas(sinv, itemstack);
        } else {
            //Try to parse string as an integer, first checking if it is all digits
            if (!NumberUtils.isDigits(amountAsString)) {
                s.sendMessage(prefix + ChatColor.RED + "Invalid amount! It must be a whole number above zero.");
                return;
            } else {
                giveAmount = NumberUtils.toInt(amountAsString);
                if (giveAmount < 1) {
                    s.sendMessage(prefix + ChatColor.RED + "Invalid amount! It must be a whole number above zero.");
                    return;
                }
                if (!sinv.containsAtLeast(itemstack, giveAmount)) {
                    s.sendMessage(prefix + ChatColor.RED + "You don't have that much of this item! Please specify a smaller amount or use \"all\".");
                    logGiftDenied(s.getName(), s.getName() + " doesn't have the amount specified.");
                    return;
                }
            }
        }
        if (args.length > 2) checkMessageInput(s, target, itemstack, giveAmount, args);
        else sendItem(s, target, itemstack, giveAmount, "");
    }

    private boolean canSendGift(Player s, Player target, ItemStack itemstack) {
        UUID senderUUID = s.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        PlayerInventory tinv = target.getInventory();
        boolean enableWorldRestrict = plugin.getConfigFile().getBoolean("restrict-interworld-gift");
        String sName = s.getName();
        String tName = target.getName();

        if (enableWorldRestrict) {
            int sWorldGroup = plugin.getPlayerWorldGroup(s);
            int tWorldGroup = plugin.getPlayerWorldGroup(target);
            if (sWorldGroup == -1 && !(s.hasPermission("advancedgift.bypass.world.blacklist"))) {
                s.sendMessage(prefix + ChatColor.RED + "Sorry! The world you are in is blacklisted from gift activities.");
                logGiftDenied(sName, sName + " is in " + s.getWorld().getName() + ", a blacklisted world.");
                return false;
            }
            if (tWorldGroup == -1 && !(s.hasPermission("advancedgift.bypass.world.blacklist"))) {
                s.sendMessage(prefix + ChatColor.RED + "Sorry! " + tName + " is in a world blacklisted from gift activities.");
                logGiftDenied(sName, "Target " + tName + " is in " + target.getWorld().getName() + ", a blacklisted world.");
                return false;
            }
            if (sWorldGroup != (tWorldGroup) && !(s.hasPermission("advancedgift.bypass.world.restriction"))) {
                s.sendMessage(prefix + ChatColor.RED + "Sorry! You and " + tName + " are not in the same world or group of worlds.");
                s.sendMessage(ChatColor.RED + "You cannot send the gift due to an interworld gift restriction.");
                logGiftDenied(sName, sName + " and " + tName + " are not in the same group of worlds.");
                return false;
            }
        }
        if (!(target.hasPermission("advancedgift.gift.receive"))) {
            s.sendMessage(prefix + ChatColor.RED + "Sorry! " + tName + " doesn't have permission to receive gifts.");
            logGiftDenied(sName, tName + " is missing permission node 'advancedgift.gift.receive'.");
            return false;
        }
        if (plugin.hasArtMap) {
            ArtMap artMap = ArtMap.instance();
            /* ArtMap has to be compiled and added locally for the IDE to refer to. */
            if (artMap.getConfiguration().FORCE_ART_KIT) {
                ArtistHandler artistHandler = artMap.getArtistHandler();
                if (artistHandler.containsPlayer(s)) {
                    s.sendMessage(prefix + ChatColor.RED + "Sorry! You cannot send gifts while painting!");
                    logGiftDenied(sName, "ArtMap has force-artkit enabled and " + sName + "is currently making an artmap.");
                    return false;
                } else if (artistHandler.containsPlayer(target)) {
                    s.sendMessage(prefix + ChatColor.RED + "Sorry! " + target.getName() + " is currently painting and cannot receive gifts.");
                    logGiftDenied(sName, "ArtMap has force-artkit enabled and " + tName + "is currently making an artmap.");
                    return false;
                }

            }
        }
        if (plugin.containsUUID(targetUUID, "tg", null)) {
            s.sendMessage(prefix + ChatColor.RED + "Sorry! " + tName + " has disabled their ability to receive gifts.");
            logGiftDenied(sName, tName + " has their ability to receive gifts disabled.");
            return false;
        }
        if (plugin.containsUUID(targetUUID, "block", senderUUID)) {
            s.sendMessage(prefix + ChatColor.RED + "Sorry! " + tName + " is blocking gifts from you.");
            logGiftDenied(sName, tName + " has " + sName + " on their gift block list.");
            return false;
        }
        int timeRemaining;
        if ((timeRemaining = getPlayerCooldownTime(s)) > 0) {
            s.sendMessage(prefix + ChatColor.RED + "Please wait another " + ChatColor.YELLOW + (timeRemaining) + ((timeRemaining) != 1 ? " seconds " : " second ") + ChatColor.RED + "before /gift can be used again.");
            logGiftDenied(sName, sName + "'s /gift cooldown hasn't ended yet.");
            return false;
        }
        if (tinv.firstEmpty() == -1) {
            int space = 0;
            for (ItemStack item: tinv.getContents()) {
                if (itemstack.isSimilar(item)) {
                    space = item.getMaxStackSize() - item.getAmount();
                    if (space > 0) break;
                }
            }
            if (space == 0) {
                s.sendMessage(prefix + ChatColor.RED + "Sorry! " + tName + "'s inventory is full.");
                target.sendMessage(prefix + ChatColor.RED + s.getName() + " attempted to send you a gift, but your inventory is full.");
                logGiftDenied(sName, tName + "'s inventory is full.");
                return false;
            }
        }
        return true;
    }

    private boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

    private int getPlayerCooldownTime(Player s) {
        if (!plugin.getConfigFile().getBoolean("enable-cooldown")) return 0;
        if (s.hasPermission("advancedgift.bypass.cooldown")) return 0;
        UUID senderUUID = s.getUniqueId();
        if (!cooldown.containsKey(senderUUID)) return 0;
        return (int) (plugin.getConfigFile().getInt("cooldown-time") - System.currentTimeMillis()/1000 - cooldown.get(senderUUID)/1000);
    }

    private void checkMessageInput(Player s, Player target, ItemStack itemstack, int giveAmount, String[] args) {
        if (!enableMessage) {
            sendItem(s, target, itemstack, giveAmount, "");
            return;
        }
        if (!s.hasPermission("advancedgift.gift.message")) {
            s.sendMessage(prefix + ChatColor.RED + "You don't have permission to send messages!");
            logGiftDenied(s.getName(), s.getName() + "is missing permission node 'advancedgift.gift.message'.");
            return;
        }

        boolean enableCensorship = plugin.getConfigFile().getBoolean("message-censorship");
        String[] messageArray = Arrays.copyOfRange(args, 2, args.length);
        if (enableCensorship) {
            StringBuilder censoredList = new StringBuilder();
            for (int i = 0; i < messageArray.length; i++) {
                String word = messageArray[i].replaceAll("[^\\w]", "").replace("_", "").toLowerCase();
                if (word.isEmpty()) continue;
                boolean isBlockedWord;
                for (String blockedWord : plugin.getConfigFile().getStringList("word-filter")) {
                    String blockedWordCleaned = blockedWord.replace("*", "").toLowerCase();
                    if (blockedWord.startsWith("*") && blockedWord.endsWith("*")) isBlockedWord = word.contains(blockedWordCleaned);
                    else if (blockedWord.startsWith("*")) isBlockedWord = word.endsWith(blockedWordCleaned);
                    else if (blockedWord.endsWith("*")) isBlockedWord = word.startsWith(blockedWordCleaned);
                    else isBlockedWord = word.equalsIgnoreCase(blockedWord);
                    if (isBlockedWord) {
                        censoredList.append((censoredList.length() == 0) ? "" : ", ").append(word);
                        messageArray[i] = "***";
                    }
                }
            }
            String message = String.join(" ", messageArray);
            if (censoredList.length() == 0) {
                sendItem(s, target, itemstack, giveAmount, message);
            } else {
                String sendCensoredMessage = plugin.getConfigFile().getString("send-censored-message");
                if (sendCensoredMessage.equalsIgnoreCase("with")) {
                    sendItem(s, target, itemstack, giveAmount, message);
                    //Add a warning to the sender?
                    logMessage("WARNING: Censored the banned words from " + s.getName() + "'s gift message: " + censoredList);
                } else if (sendCensoredMessage.equalsIgnoreCase("without")) {
                    sendItem(s, target, itemstack, giveAmount, "");
                    s.sendMessage(ChatColor.DARK_RED + "Warning: " + ChatColor.RED + "Your message was not sent because it contains the following blocked words: " + censoredList + "."); //todo fix this, will still show even if gift was not sent
                    logMessage("WARNING: Removed " +s.getName() + "'s gift message: it contains the following blacklisted words: " + censoredList + ".");
                } else {
                    s.sendMessage(ChatColor.DARK_RED + "Warning: " + ChatColor.RED + "Your gift was not sent because your message contains the following blocked words: " + censoredList + ".");
                    logGiftDenied(s.getName(), s.getName() + "'s gift message contains the following blacklisted words: " + censoredList + ".");
                }
            }
        } else {
            sendItem(s, target, itemstack, giveAmount, String.join(" ", messageArray));
        }
    }

    private void sendItem (Player s, Player target, ItemStack itemstack, int giveAmount, String message) {
        if (!canSendGift(s, target, itemstack))
            return;

        if (plugin.getConfigFile().getBoolean("enable-cooldown"))
            cooldown.put(s.getUniqueId(), System.currentTimeMillis());
        plugin.getGiftCounter().increment();
        PlayerInventory sinv = s.getInventory();
        PlayerInventory tinv = target.getInventory();
        List<ItemStack> itemList = new ArrayList<>();
        int amountLeft = giveAmount;
        ItemStack[] contents = ServerVersion.getMinorVersion() > 8 ? sinv.getStorageContents() : sinv.getContents();
        for (ItemStack item : contents) {
            if (itemstack.isSimilar(item)) {
                int itemAmount = item.getAmount();
                ItemStack itemToAdd = item.clone();
                if (itemAmount <= amountLeft) {
                    amountLeft -= itemAmount;
                    sinv.removeItem(item);
                } else {
                    itemToAdd.setAmount(amountLeft);
                    item.setAmount(itemAmount - amountLeft);
                    amountLeft = 0;
                }
                itemList.add(itemToAdd);
                if (amountLeft == 0) break;
            }
        }
        HashMap<Integer, ItemStack> excess = tinv.addItem(itemList.toArray(new ItemStack[0]));
        if (!excess.isEmpty()) {
            s.sendMessage(prefix + ChatColor.RED + target.getName() + "'s inventory was nearly full when you sent the gift. Only part of the gift was sent.");
            target.sendMessage(prefix + ChatColor.RED + "Your inventory was nearly full when the gift was sent. Only part of the gift was received.");
            logMessage("WARNING: Sent only a part of " + s.getName() + "'s gift: " + target.getName() + "'s inventory was nearly full.");
            for (ItemStack extra : excess.values()) {
                giveAmount -= extra.getAmount();
                sinv.addItem(extra);
            }
        }
        sendGiftNotification(s, target, itemstack, giveAmount, message);
    }

    private void sendGiftNotification(Player s, Player target, ItemStack itemstack, int amountSent, String message) {
        StringJoiner joiner = new StringJoiner(" ");
        String material;
        String extLib = plugin.extLib;
        if (extLib.equals("LangUtils")) {
            material = LanguageHelper.getItemName(itemstack, "en_us");
        }
        else {
            material = itemstack.getType().toString().replace("_", " ").toLowerCase();
        }

        joiner.add(String.valueOf(amountSent));

        boolean hasItemMeta = itemstack.hasItemMeta();
        ItemMeta meta = itemstack.getItemMeta();

        //add prefixes
        if (hasItemMeta && meta.hasEnchants()) {
            joiner.add("Enchanted");
        }
        if (isPatternedBanner(itemstack)) {
            joiner.add("Patterned");
        }

        //add material name
        joiner.add(WordUtils.capitalize(material, SPACE_DELIMITER));

        //add suffixes
        if (hasItemMeta && meta.hasDisplayName()) {
            joiner.add("named").add(meta.getDisplayName());
        }
        String itemDetails = joiner.toString();

        String sName = s.getName();
        String tName = target.getName();

        final String senderNotification = prefix + ChatColor.WHITE + "You gave " + ChatColor.GOLD + tName + " " + ChatColor.YELLOW + itemDetails + ChatColor.WHITE + ".";
        final String targetNotification = prefix + ChatColor.WHITE + "You received " + ChatColor.YELLOW + itemDetails + ChatColor.WHITE + " from " + ChatColor.GOLD + sName + ".";
        final String spyNotification = prefix + ChatColor.GRAY + sName + " gave " + tName + " " + ChatColor.stripColor(itemDetails) + ".";
        final TextComponent senderComponent = new TextComponent(TextComponent.fromLegacyText(senderNotification));
        final TextComponent targetComponent = new TextComponent(TextComponent.fromLegacyText(targetNotification));
        final TextComponent spyComponent = new TextComponent(TextComponent.fromLegacyText(spyNotification));

        if (plugin.canUseTooltips) {
            BaseComponent[] hoverMessage = new ComponentBuilder(AdvancedGift.nms.convertItemToJson(itemstack)).create();
            HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverMessage);

            senderComponent.setHoverEvent(event);
            targetComponent.setHoverEvent(event);
            spyComponent.setHoverEvent(event);
        }

        s.spigot().sendMessage(senderComponent);
        target.spigot().sendMessage(targetComponent);
        if (!message.isEmpty()) {
            s.sendMessage("Your message: " + message);
            target.sendMessage("Gift message: " + message);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == s || player == target) continue;
            if (plugin.containsUUID(player.getUniqueId(), "spy", null)) {
                player.spigot().sendMessage(spyComponent);
                if (!message.isEmpty()) player.sendMessage("Gift message: " + message);
            }
        }

        logGiftSent(message, sName, tName, itemstack, itemDetails);
    }

    private boolean isPatternedBanner(ItemStack itemstack) {
        if (itemstack.getType().toString().toUpperCase().contains("BANNER")) {
            if (itemstack.getItemMeta() instanceof BannerMeta) {
                BannerMeta meta = (BannerMeta)itemstack.getItemMeta();
                return (meta.numberOfPatterns() > 0);
            }
        }
        return false;
    }

    private void logMessage(String message) { plugin.getLogger().info(agLog + message); }

    private void logGiftDenied(String sName, String reason) { plugin.getLogger().info(agLog + "Denied " + sName + "'s /gift use: " + reason); }

    @SuppressWarnings("deprecation")
    private void logGiftSent(String message, String sName, String tName, ItemStack itemstack, String itemDetails) {
        itemDetails = ChatColor.stripColor(itemDetails);
        logMessage(sName + " gave " + tName + " " + itemDetails + ".");
        if (itemstack.hasItemMeta()) {
            ItemMeta itemmeta = itemstack.getItemMeta();
            if (itemmeta.hasEnchants() || itemmeta.hasLore() || (ServerVersion.getMinorVersion() >= 11 && itemmeta.isUnbreakable())) {
                plugin.getLogger().info("   More item info on " + sName + "'s gift:");
                if (ServerVersion.getMinorVersion() >= 11) { if (itemmeta.isUnbreakable()) plugin.getLogger().info("   - Unbreakable"); }
                if (itemmeta.hasLore()) {
                    ArrayList<String> loreList = new ArrayList<>();
                    for (String lore : itemmeta.getLore()) {
                        loreList.add("[" + lore + "]");
                    }
                    plugin.getLogger().info("   - Lore: " + String.join("; ", loreList));
                }
                if (itemmeta.hasEnchants()) {
                    ArrayList<String> enchantmentList= new ArrayList<>();
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
                    plugin.getLogger().info("   - Enchantments: " + String.join(", ", enchantmentList));
                }
            }
        }
        if (!message.isEmpty()) plugin.getLogger().info(agLog + sName + "'s gift message: " + message);
    }
}