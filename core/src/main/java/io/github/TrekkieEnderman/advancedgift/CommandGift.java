package io.github.TrekkieEnderman.advancedgift;

import java.util.*;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Painting.ArtistHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
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

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;

import com.meowj.langutils.lang.LanguageHelper;

public class CommandGift implements CommandExecutor {
    private final AdvancedGift plugin;
    private String prefix;
    private boolean enableMessage;
    private String usage;

    CommandGift(AdvancedGift plugin) {
        this.plugin = plugin;
        this.prefix = this.plugin.prefix;
        this.enableMessage = this.plugin.getConfigFile().getBoolean("allow-gift-message");
        this.usage = ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/gift [player]" + ChatColor.GRAY + " <amount | hand | all>" + (enableMessage ? " <your message>" : "");
    }

    private HashMap<UUID, Long> cooldown = new HashMap<>();
    private String agLog = "[AG LOG] > ";
    private long diff;

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
                Player target = (Bukkit.getServer().getPlayer(args[0]));
                PlayerInventory sinv = s.getInventory();
                @SuppressWarnings("deprecation")
                ItemStack itemstack = (plugin.isBefore1_9 ? sinv.getItemInHand() : sinv.getItemInMainHand());
                if (!(s.hasPermission("advancedgift.gift.send"))) {
                    s.sendMessage(prefix + ChatColor.RED + "You don't have permission to use this command!");
                } else if (target == null || (isVanished(target) && !s.hasPermission("advancedgift.bypass.vanish"))) {
                    s.sendMessage(prefix + ChatColor.RED + args[0] + " is not online!");
                } else if (target == s.getPlayer()) {
                    s.sendMessage(prefix + ChatColor.RED + "You can't send yourself a gift!");
                } else if (itemstack.getType() == Material.AIR) {
                    s.sendMessage(prefix + ChatColor.RED + "You need to hold something in your hand!");
                } else {
                    PlayerInventory tinv = target.getInventory();
                    if (args.length == 1) {
                        if (canSendGift(s, target, tinv, itemstack, args)) {
                            sendItem(s, target, sinv, tinv, itemstack, itemstack.getAmount(), "");
                        }
                    } else {
                        checkAmountInput(s, target, sinv, tinv, itemstack, args, args.length > 2);
                    }
                }
            }
        }
        return true;
    }

    private int getTotalAmountHas(PlayerInventory sinv, ItemStack itemstack) {
        int hasAmount = 0;
        for (ItemStack item : sinv.getStorageContents()) { //getContents also returns offhand and armor slots, which we don't want to
            if ((item != null) && (item.isSimilar(itemstack))) {
                hasAmount += item.getAmount();
            }
        }
        return hasAmount;
    }

    private void checkAmountInput(Player s, Player target, PlayerInventory sinv, PlayerInventory tinv, ItemStack itemstack, String[] args, boolean hasMessage) {
        String amountAsString = args[1];
        int hasAmount = getTotalAmountHas(sinv, itemstack);
        int giveAmount;
        if (amountAsString.equalsIgnoreCase("hand")) {
            giveAmount = itemstack.getAmount();
        } else if (amountAsString.equalsIgnoreCase("all")){
            giveAmount = hasAmount;
        } else try {
            giveAmount = Integer.parseInt(amountAsString);
            if (giveAmount > hasAmount) {
                s.sendMessage(prefix + ChatColor.RED + "You don't have that much of that item! Please specify lower amount or use \"all\".");
                logGiftDenied(s.getName(), s.getName() + " doesn't have the amount specified.");
                return;
            } else if (giveAmount == 0) {
                s.sendMessage(prefix + ChatColor.RED + "You can't give your friend nothing!");
                return;
                //Add console log here?
            } else if (giveAmount < 0){
                s.sendMessage(prefix + ChatColor.RED + "You can't have negative amount!");
                return;
            }
        } catch (NumberFormatException e) {
            s.sendMessage(prefix + ChatColor.RED + amountAsString + " is not an integer!");
            s.sendMessage(usage);
            return;
            //Add console log here?
        }
        if (canSendGift(s, target, tinv, itemstack, args)) {
            if (hasMessage) checkMessageInput(s, target, sinv, tinv, itemstack, giveAmount, args);
            else sendItem(s, target, sinv, tinv, itemstack, giveAmount, "");
        }
    }

    private boolean canSendGift(Player s, Player target, PlayerInventory tinv, ItemStack itemstack, String[] args) {
        UUID senderUUID = s.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        int cooldownTime = plugin.getConfigFile().getInt("cooldown-time");
        boolean enableCooldown = plugin.getConfigFile().getBoolean("enable-cooldown");
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
        if (plugin.containsUUID(targetUUID, "tg", "")) {
            s.sendMessage(prefix + ChatColor.RED + "Sorry! " + tName + " has disabled their ability to receive gifts.");
            logGiftDenied(sName, tName + " has their ability to receive gifts disabled.");
            return false;
        }
        if (plugin.containsUUID(targetUUID, "block", senderUUID.toString())) {
            s.sendMessage(prefix + ChatColor.RED + "Sorry! " + tName + " is blocking gifts from you.");
            logGiftDenied(sName, tName + " has " + sName + " on their gift block list.");
            return false;
        }
        if (hasCooldownPassed(s, senderUUID, cooldownTime, enableCooldown)) {
            if (tinv.firstEmpty() == -1) {
                int space = 0;
                for (ItemStack item: tinv.getContents()) {
                    if (item != null && item.isSimilar(itemstack)) {
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
            if (enableCooldown) cooldown.put(senderUUID, System.currentTimeMillis());
        } else {
            s.sendMessage(prefix + ChatColor.RED + "Please wait another " + ChatColor.YELLOW + (cooldownTime - diff) + ((cooldownTime-diff) != 1 ? " seconds " : " second ") + ChatColor.RED + "before /gift can be used again.");
            logGiftDenied(sName, sName + "'s /gift cooldown hasn't ended yet.");
            return false;
        }
        return true;
    }

    private boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }

    private boolean hasCooldownPassed(Player s, UUID senderUUID, int cooldownTime, boolean enableCooldown) {
        if (!enableCooldown) return true;
        if (!cooldown.containsKey(s.getUniqueId())) return true;
        else {
            diff = (System.currentTimeMillis()/1000 - cooldown.get(senderUUID)/1000);
            return (diff >= cooldownTime || s.hasPermission("advancedgift.bypass.cooldown"));
        }
    }

    private void checkMessageInput(Player s, Player target, PlayerInventory sinv, PlayerInventory tinv, ItemStack itemstack, int giveAmount, String[] args) {
        if (!enableMessage) {
            sendItem(s, target, sinv, tinv, itemstack, giveAmount, "");
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
                sendItem(s, target, sinv, tinv, itemstack, giveAmount, message);
            } else {
                String sendCensoredMessage = plugin.getConfigFile().getString("send-censored-message");
                if (sendCensoredMessage.equalsIgnoreCase("with")) {
                    sendItem(s, target, sinv, tinv, itemstack, giveAmount, message);
                    //Add a warning to the sender?
                    logMessage("WARNING: Censored the banned words from " + s.getName() + "'s gift message: " + censoredList);
                } else if (sendCensoredMessage.equalsIgnoreCase("without")) {
                    sendItem(s, target, sinv, tinv, itemstack, giveAmount, "");
                    s.sendMessage(ChatColor.DARK_RED + "Warning: " + ChatColor.RED + "Your message was not sent because it contains the following blocked words: " + censoredList + ".");
                    logMessage("WARNING: Removed " +s.getName() + "'s gift message: it contains the following blacklisted words: " + censoredList + ".");
                } else if (sendCensoredMessage.equalsIgnoreCase("block")) {
                    s.sendMessage(ChatColor.DARK_RED + "Warning: " + ChatColor.RED + "Your gift was not sent because your message contains the following blocked words: " + censoredList + ".");
                    logGiftDenied(s.getName(), s.getName() + "'s gift message contains the following blacklisted words: " + censoredList + ".");
                }
            }
        } else {
            sendItem(s, target, sinv, tinv, itemstack, giveAmount, String.join(" ", messageArray));
        }
    }

    private void sendItem (Player s, Player target, PlayerInventory sinv, PlayerInventory tinv, ItemStack itemstack, int giveAmount, String message) {
        List<ItemStack> itemList = new ArrayList<>();
        int amountLeft = giveAmount;
        for (ItemStack item : sinv.getStorageContents()) {
            if (item != null && item.isSimilar(itemstack) && amountLeft != 0) {
                int itemAmount = item.getAmount();
                if (itemAmount <= amountLeft) {
                    itemList.add(new ItemStack(item));
                    amountLeft -= itemAmount;
                    sinv.removeItem(item);
                } else {
                    item.setAmount(amountLeft);
                    itemList.add(new ItemStack(item));
                    item.setAmount(itemAmount - amountLeft);
                    amountLeft = 0;
                }
                if (amountLeft == 0) break;
            }
        }
        ItemStack[] itemToSend = itemList.toArray(new ItemStack[0]);
        HashMap<Integer, ItemStack> excess = tinv.addItem(itemToSend);
        if (!excess.isEmpty()) {
            s.sendMessage(prefix + ChatColor.RED + target.getName() + "'s inventory was nearly full when you sent the gift. Only part of the gift was sent.");
            target.sendMessage(prefix + ChatColor.RED + "Your inventory was nearly full when the gift was sent. Only part of the gift was received.");
            logMessage("WARNING: Sent only a part of " + s.getName() + "'s gift: " + target.getName() + "'s inventory was nearly full.");
            for (Map.Entry<Integer, ItemStack> me : excess.entrySet()) {
                int itemAmount = me.getValue().getAmount();
                giveAmount -= itemAmount;
                sinv.addItem(me.getValue());
            }
        }
        sendGiftNotification(s, target, itemstack, giveAmount, message);
        if (!message.isEmpty()) {
            s.sendMessage(ChatColor.GOLD + "Your message: " + ChatColor.WHITE + message);
            target.sendMessage(ChatColor.GOLD + s.getName() + "'s message: " + ChatColor.WHITE + message);
        }
    }

    private void sendGiftNotification(Player s, Player target, ItemStack itemstack, int amountSent, String message) {
        String material;
        String extLib = plugin.extLib;
        if (extLib.equals("LangUtils")) material = LanguageHelper.getItemName(itemstack, "en_us").toUpperCase();
        else if (extLib.equals("none")) material = itemstack.getType().toString().replace("_", " ");
        else material = ChatColor.RED + "yeet";
        String sName = s.getName();
        String tName = target.getName();
        boolean hasItemMeta = itemstack.hasItemMeta();
        String itemDisplayName = (hasItemMeta && itemstack.getItemMeta().hasDisplayName() ? ChatColor.WHITE + " named " + ChatColor.GREEN + itemstack.getItemMeta().getDisplayName() : "");
        String itemPatterned = (isPatternedBanner(itemstack) ? "PATTERNED " : "");
        String itemEnchanted = (hasItemMeta && itemstack.getItemMeta().hasEnchants() ? " ENCHANTED " : " ");

        if (plugin.canUseTooltips) {
            TextComponent materialComp = new TextComponent(material);
            materialComp.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
            BaseComponent [] hoverMessage = new BaseComponent[] { new TextComponent(AdvancedGift.nms.convertItemToJson(itemstack)) };
            HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverMessage);
            materialComp.setHoverEvent(event);

            //Message to the sender
            TextComponent sMessage = new TextComponent("");
            TextComponent sBefore = new TextComponent(TextComponent.fromLegacyText(prefix + ChatColor.WHITE + "You gave " + ChatColor.GOLD + tName +
                    ChatColor.YELLOW + " " + amountSent + itemEnchanted + itemPatterned));
            TextComponent sAfter = new TextComponent(TextComponent.fromLegacyText(itemDisplayName + ChatColor.WHITE + "."));
            sMessage.addExtra(sBefore);
            sMessage.addExtra(materialComp);
            sMessage.addExtra(sAfter);
            s.spigot().sendMessage(sMessage);

            //Message to the target
            TextComponent tMessage = new TextComponent("");
            TextComponent tBefore = new TextComponent(TextComponent.fromLegacyText(prefix + ChatColor.WHITE + "You received " + ChatColor.YELLOW + amountSent + itemEnchanted + itemPatterned));
            TextComponent tAfter = new TextComponent(TextComponent.fromLegacyText(itemDisplayName + ChatColor.WHITE + " from " + ChatColor.GOLD + sName + ChatColor.WHITE + "."));
            tMessage.addExtra(tBefore);
            tMessage.addExtra(materialComp);
            tMessage.addExtra(tAfter);
            target.spigot().sendMessage(tMessage);

        } else {
            s.sendMessage(prefix + ChatColor.WHITE + "You gave " + ChatColor.GOLD + tName + ChatColor.YELLOW + " " + amountSent + itemEnchanted +
                    itemPatterned + material + itemDisplayName + ChatColor.WHITE + ".");
            target.sendMessage(prefix + ChatColor.WHITE + "You received " + ChatColor.YELLOW + amountSent + itemEnchanted + itemPatterned + material +
                    itemDisplayName + ChatColor.WHITE + " from " + ChatColor.GOLD + sName + ChatColor.WHITE + ".");
        }
        logGiftSent(message, sName, tName, amountSent, material, itemstack, itemPatterned, itemEnchanted, itemDisplayName);
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
    private void logGiftSent(String message, String sName, String tName, int amount, String material, ItemStack itemstack, String itemPatterned, String itemEnchanted, String itemDisplayName) {
        itemDisplayName = ChatColor.stripColor(itemDisplayName);
        logMessage(sName + " gave " + tName + " " + amount + " " + itemPatterned + material + itemDisplayName + ".");
        if (itemstack.hasItemMeta()) {
            ItemMeta itemmeta = itemstack.getItemMeta();
            if (itemmeta.hasEnchants() || itemmeta.hasLore() || (!plugin.isBefore1_11 && itemmeta.isUnbreakable())) {
                plugin.getLogger().info("   More item info on " + sName + "'s gift:");
                if (!plugin.isBefore1_11) { if (itemmeta.isUnbreakable()) plugin.getLogger().info("   - Unbreakable"); }
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
                        if (plugin.isBefore1_13) {
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

        for (String spy : plugin.spyPlayers) {
            Player spyPlayer = Bukkit.getPlayer(UUID.fromString(spy));
            if (spyPlayer != null && !(spyPlayer.getName().equalsIgnoreCase(sName)) && !(spyPlayer.getName().equalsIgnoreCase(tName))) {
                if (plugin.canUseTooltips) {
                    TextComponent component = new TextComponent("");
                    component.setColor(net.md_5.bungee.api.ChatColor.GRAY);
                    TextComponent logPrefix = new TextComponent(agLog);
                    logPrefix.setColor(net.md_5.bungee.api.ChatColor.WHITE);
                    component.addExtra(logPrefix);
                    component.addExtra(sName + " gave " + tName + " " + amount + itemEnchanted + itemPatterned);
                    TextComponent itemHover = new TextComponent(material);
                    BaseComponent [] hoverMessage = new BaseComponent[] {new TextComponent(AdvancedGift.nms.convertItemToJson(itemstack))};
                    HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_ITEM, hoverMessage);
                    itemHover.setHoverEvent(event);
                    component.addExtra(itemHover);
                    component.addExtra(itemDisplayName + ".");
                    spyPlayer.spigot().sendMessage(component);
                } else spyPlayer.sendMessage(ChatColor.WHITE + agLog + ChatColor.GRAY + sName + " gave " + tName + " " + amount + itemEnchanted + itemPatterned + material + itemDisplayName + ".");
                if (!message.isEmpty()) spyPlayer.sendMessage(ChatColor.WHITE + agLog + ChatColor.GRAY + sName + "'s gift message: " + message);
            }
        }
    }
}