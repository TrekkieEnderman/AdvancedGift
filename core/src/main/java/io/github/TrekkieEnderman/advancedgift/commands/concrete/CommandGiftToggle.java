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

import java.util.UUID;

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandGiftToggle implements CommandExecutor {
    private final AdvancedGift plugin;

    public CommandGiftToggle(AdvancedGift plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender commandSender, @NotNull final Command cmd, @NotNull final String label, @NotNull final String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(plugin.getPrefix() + Message.COMMAND_FOR_PLAYER_ONLY.translate());
            return true;
        }
        final Player sender = (Player) commandSender;
        final UUID senderUUID = sender.getUniqueId();
        if (args.length == 0) {
            if (!plugin.getPlayerDataManager().containsUUID(senderUUID, "tg", null)) {
                plugin.getPlayerDataManager().addUUID(senderUUID, "tg", null);
                sender.sendMessage(plugin.getPrefix() + Message.TOGGLED_OFF.translate());
            } else {
                plugin.getPlayerDataManager().removeUUID(senderUUID, "tg", null);
                sender.sendMessage(plugin.getPrefix() + Message.TOGGLED_ON.translate());
            }
        } else {
            if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("disable")) {
                if (!plugin.getPlayerDataManager().containsUUID(senderUUID, "tg", null)) {
                    plugin.getPlayerDataManager().addUUID(senderUUID, "tg", null);
                    sender.sendMessage(plugin.getPrefix() + Message.TOGGLED_OFF.translate());
                } else {
                    sender.sendMessage(plugin.getPrefix() + Message.ALREADY_TOGGLED_OFF.translate());
                }
            } else if (args[0].equalsIgnoreCase("on") || args [0].equalsIgnoreCase("enable")) {
                if (plugin.getPlayerDataManager().containsUUID(senderUUID, "tg", null)) {
                    plugin.getPlayerDataManager().removeUUID(senderUUID, "tg", null);
                    sender.sendMessage(plugin.getPrefix() + Message.TOGGLED_ON.translate());
                } else {
                    sender.sendMessage(plugin.getPrefix() + Message.ALREADY_TOGGLED_ON.translate());
                }
            } else {
                sender.sendMessage(plugin.getPrefix() + Message.ARGUMENT_NOT_RECOGNIZED.translate(args[0]));
                sender.sendMessage(Message.COMMAND_TOGGLE_USAGE.translate());
            }
        }
        return true;
    }
}