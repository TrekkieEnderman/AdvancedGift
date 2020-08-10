package io.github.TrekkieEnderman.advancedgift;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class CommandGiftBlock implements CommandExecutor {
    private final AdvancedGift plugin;
    private String prefix;

    CommandGiftBlock(AdvancedGift plugin) {
        this.plugin = plugin;
        this.prefix = this.plugin.prefix;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }
        Player s = (Player) sender;
        UUID senderUUID = s.getUniqueId();
        if (cmd.getName().equalsIgnoreCase("giftblock")) {
            if (args.length == 0) {
                s.sendMessage(prefix + ChatColor.YELLOW + "Block gifts from a player you dislike or find annoying!" + ChatColor.GRAY + " ...Or unblock them.");
                s.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/giftblock [player]" + ChatColor.GRAY + "  ||  " + ChatColor.WHITE + "/giftunblock [player]");
            } else {
                String target = args[0];
                Player targetPlayer = Bukkit.getServer().getPlayer(target);
                String targetUUID;
                if (targetPlayer == null) {
                    OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(target);
                    if (!targetOffline.hasPlayedBefore()) {
                        s.sendMessage(prefix + ChatColor.RED + "No player going by " + target + " has played on here before.");
                        return true;
                    }
                    targetUUID = targetOffline.getUniqueId().toString();
                    target = targetOffline.getName();
                } else {
                    if (targetPlayer == s.getPlayer()) {
                        s.sendMessage(prefix + ChatColor.RED + "Are you trying to block yourself?");
                        return true;
                    }
                    targetUUID = targetPlayer.getUniqueId().toString();
                    target = targetPlayer.getName();
                }
                if (label.equalsIgnoreCase("giftblock") || label.equalsIgnoreCase("blockgift") || label.equalsIgnoreCase("gblock")) {
                    if (!plugin.containsUUID(senderUUID, "block", targetUUID)) {
                        plugin.addUUID(senderUUID, "block", targetUUID);
                        s.sendMessage(prefix + ChatColor.GREEN + "Added " + target + " to your gift block list!");
                    } else {
                        s.sendMessage(prefix + ChatColor.GRAY + target + " is already on your gift block list.");
                    }
                } else {
                    if (plugin.containsUUID(senderUUID, "block", targetUUID)) {
                        plugin.removeUUID(senderUUID, "block", targetUUID);
                        s.sendMessage(prefix + ChatColor.AQUA + "Removed " + target + " from your gift block list!");
                    } else {
                        s.sendMessage(prefix + ChatColor.GRAY + target + " is not on your gift block list.");
                    }
                }
            }
        } else {
            if (args.length == 0) {
                String blockList = plugin.getBlockList(senderUUID);
                if (blockList.isEmpty()) s.sendMessage(prefix + ChatColor.GRAY + "Your gift block list is empty.");
                else {
                    s.sendMessage(ChatColor.GRAY + "Your gift block list: " + ChatColor.DARK_AQUA + blockList);
                    s.sendMessage(ChatColor.AQUA + "To clear the list, " + ChatColor.WHITE + "/giftblocklist clear");
                }
            } else {
                if (args[0].equalsIgnoreCase("clear")) {
                    if (plugin.clearBlockList(senderUUID)) s.sendMessage(prefix + ChatColor.GREEN + "Cleared your gift block list!");
                    else s.sendMessage(prefix + ChatColor.GRAY + "Your gift block list is already empty.");
                } else {
                    s.sendMessage(prefix + ChatColor.RED + "Cannot understand " + args[0] + "!");
                }
            }

        }
        return true;
    }

}