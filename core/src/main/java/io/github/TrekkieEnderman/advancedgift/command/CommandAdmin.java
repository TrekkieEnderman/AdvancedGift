package io.github.TrekkieEnderman.advancedgift.command;

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class CommandAdmin {
    private final AdvancedGift plugin;
    String prefix;

    public CommandAdmin(AdvancedGift plugin) {
        this.plugin = plugin;
        prefix = plugin.prefix;
    }

    public void onCommand(CommandSender sender, Command command, String[] args) {
        switch (command.getName().toLowerCase()) {
            //More admin commands to come...
            case "agreload":
                reloadConfig(sender);
            case "giftspy":
                toggleGiftSpy(sender, args);
        }
    }

    private void reloadConfig(CommandSender sender) {
        if (!(sender instanceof Player)) {
            if (plugin.loadConfig()) getServer().getConsoleSender().sendMessage(prefix + ChatColor.GREEN + "Reloaded the config.");
            else getServer().getConsoleSender().sendMessage(prefix + ChatColor.RED + "Failed to reload the config.");
        } else {
            if (sender.hasPermission("advancedgift.reload")) {
                if (plugin.loadConfig()) sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded the config.");
                else sender.sendMessage(prefix + ChatColor.RED + "Failed to reload the config. Check the console for errors.");
            } else sender.sendMessage(prefix + ChatColor.RED + "You don't have permission to use this command!");
        }
    }

    private void toggleGiftSpy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) sender.sendMessage("This command can only be run by a player.");
        else {
            if (sender.hasPermission("advancedgift.gift.spy")) {
                String usage = ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/giftspy " + ChatColor.GRAY + "<on/off>";
                Player s = (Player) sender;
                UUID senderUUID = s.getUniqueId();
                String spyEnabled = prefix + ChatColor.GREEN + "Gift Spy enabled.";
                String spyDisabled = prefix + ChatColor.RED + "Gift Spy disabled.";
                if (args.length == 0) {
                    if (!plugin.containsUUID(senderUUID, "spy", "")) {
                        plugin.addUUID(senderUUID, "spy", "");
                        s.sendMessage(spyEnabled);
                    } else {
                        plugin.removeUUID(senderUUID, "spy", "");
                        s.sendMessage(spyDisabled);
                    }
                } else {
                    if (args[0].equalsIgnoreCase("on") || args[0].equalsIgnoreCase("enable")) {
                        if (!plugin.containsUUID(senderUUID, "spy", "")) {
                            plugin.addUUID(senderUUID, "spy", "");
                            s.sendMessage(spyEnabled);
                        } else {
                            s.sendMessage(prefix + ChatColor.GRAY + "Gift Spy is already enabled.");
                        }
                    } else if (args[0].equalsIgnoreCase("off") || args [0].equalsIgnoreCase("disable")) {
                        if (plugin.containsUUID(senderUUID, "spy", "")) {
                            plugin.removeUUID(senderUUID, "spy", "");
                            s.sendMessage(spyDisabled);
                        } else {
                            s.sendMessage(prefix + ChatColor.GRAY + "Gift Spy is already disabled.");
                        }
                    } else {
                        s.sendMessage(prefix + ChatColor.RED + "Cannot understand " + args[0] + "!");
                        s.sendMessage(usage);
                    }
                }
            } else {
                sender.sendMessage(prefix + ChatColor.RED + "You don't have permission to use this!");
            }
        }
    }
}
