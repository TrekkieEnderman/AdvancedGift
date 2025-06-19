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
import io.github.TrekkieEnderman.advancedgift.commands.SimpleCommand;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandGiftToggle extends SimpleCommand {
    public CommandGiftToggle(AdvancedGift plugin) {
        super(plugin, null);
    }

    @Override
    public void showUsage(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + Message.COMMAND_TOGGLE_DESCRIPTION.translate());
        sender.sendMessage(Message.COMMAND_TOGGLE_USAGE.translate());
    }

    @Override
    public boolean run(@NotNull final Player sender, @NotNull final String label, @NotNull final String[] args) {
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
                return false;
            }
        }
        return true;
    }
}