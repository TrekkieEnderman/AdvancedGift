/*
 * Copyright (c) 2024 TrekkieEnderman
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

package io.github.TrekkieEnderman.advancedgift;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandGiftToggle implements CommandExecutor {
    private final AdvancedGift plugin;

    CommandGiftToggle(AdvancedGift plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender commandSender, @NotNull final Command cmd, @NotNull final String label, @NotNull final String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This command can only be run by a player.");
            return true;
        }
        final String usage = ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/togglegift " + ChatColor.GRAY + "<on/off>";
        final Player sender = (Player) commandSender;
        final UUID senderUUID = sender.getUniqueId();
        if (args.length == 0) {
            if (!plugin.getPlayerDataManager().containsUUID(senderUUID, "tg", null)) {
                plugin.getPlayerDataManager().addUUID(senderUUID, "tg", null);
                sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "You won't receive any more gifts now.");
            } else {
                plugin.getPlayerDataManager().removeUUID(senderUUID, "tg", null);
                sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "You will receive gifts from now on.");
            }
        } else {
            if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("disable")) {
                if (!plugin.getPlayerDataManager().containsUUID(senderUUID, "tg", null)) {
                    plugin.getPlayerDataManager().addUUID(senderUUID, "tg", null);
                    sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "You won't receive any more gifts now.");
                } else {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Your ability to receive gifts is already disabled.");
                }
            } else if (args[0].equalsIgnoreCase("on") || args [0].equalsIgnoreCase("enable")) {
                if (plugin.getPlayerDataManager().containsUUID(senderUUID, "tg", null)) {
                    plugin.getPlayerDataManager().removeUUID(senderUUID, "tg", null);
                    sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "You will receive gifts from now on.");
                } else {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Your ability to receive gifts is already enabled.");
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Cannot understand " + args[0] + "!");
                sender.sendMessage(usage);
            }
        }
        return true;
    }
}