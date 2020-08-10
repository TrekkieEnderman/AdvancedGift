package io.github.TrekkieEnderman.advancedgift;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandGiftToggle implements CommandExecutor {
    private final AdvancedGift plugin;
    private String prefix;

    CommandGiftToggle(AdvancedGift plugin) {
        this.plugin = plugin;
        this.prefix = this.plugin.prefix;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }
        String usage = ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/togglegift " + ChatColor.GRAY + "<on/off>";
        Player s = (Player) sender;
        UUID senderUUID = s.getUniqueId();
        if (args.length == 0) {
            if (!plugin.containsUUID(senderUUID, "tg", "")) {
                plugin.addUUID(senderUUID, "tg", "");
                s.sendMessage(prefix + ChatColor.YELLOW + "You won't receive any more gifts now.");
            } else {
                plugin.removeUUID(senderUUID, "tg", "");
                s.sendMessage(prefix + ChatColor.GREEN + "You will receive gifts from now on.");
            }
        } else {
            if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("disable")) {
                if (!plugin.containsUUID(senderUUID, "tg", "")) {
                    plugin.addUUID(senderUUID, "tg", "");
                    s.sendMessage(prefix + ChatColor.YELLOW + "You won't receive any more gifts now.");
                } else {
                    s.sendMessage(prefix + ChatColor.RED + "Your ability to receive gifts is already disabled.");
                }
            } else if (args[0].equalsIgnoreCase("on") || args [0].equalsIgnoreCase("enable")) {
                if (plugin.containsUUID(senderUUID, "tg", "")) {
                    plugin.removeUUID(senderUUID, "tg", "");
                    s.sendMessage(prefix + ChatColor.GREEN + "You will receive gifts from now on.");
                } else {
                    s.sendMessage(prefix + ChatColor.RED + "Your ability to receive gifts is already enabled.");
                }
            } else {
                s.sendMessage(prefix + ChatColor.RED + "Cannot understand " + args[0] + "!");
                s.sendMessage(usage);
            }
        }
        return true;
    }
}