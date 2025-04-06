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

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CommandSpy implements CommandExecutor {
    private final AdvancedGift plugin;

    public CommandSpy(final AdvancedGift plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix()+ Message.COMMAND_FOR_PLAYER_ONLY.translate());
            return true;
        }

        if (!sender.hasPermission("advancedgift.gift.spy")) {
            sender.sendMessage(plugin.getPrefix() + Message.COMMAND_NO_PERMISSION.translate());
            return true;
        }

        final UUID uuid = ((Player) sender).getUniqueId();

        if (args.length < 1) {
            final boolean spy = !plugin.getPlayerDataManager().containsUUID(uuid, "spy", null);
            setSpy(uuid, spy);
            if (spy) {
                sender.sendMessage(plugin.getPrefix() + Message.SPY_ENABLED.translate());
            } else {
                sender.sendMessage(plugin.getPrefix() + Message.SPY_DISABLED.translate());
            }
            return true;
        }

        final String arg = args[0];
        if (arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("on") || arg.equalsIgnoreCase("enable")) {
            if (setSpy(uuid, true)) {
                sender.sendMessage(plugin.getPrefix() + Message.SPY_ENABLED.translate());
            } else {
                sender.sendMessage(plugin.getPrefix() + Message.SPY_ALREADY_ENABLED.translate());
            }
            return true;
        }
        if (arg.equalsIgnoreCase("false") || arg.equalsIgnoreCase("off") || arg.equalsIgnoreCase("disable")) {
            if (setSpy(uuid, false)) {
                sender.sendMessage(plugin.getPrefix() + Message.SPY_DISABLED.translate());
            } else {
                sender.sendMessage(plugin.getPrefix() + Message.SPY_ALREADY_DISABLED.translate());
            }
            return true;
        }

        sender.sendMessage(plugin.getPrefix() + Message.ARGUMENT_NOT_RECOGNIZED.translate(args[0]));
        sender.sendMessage(Message.COMMAND_SPY_USAGE.translate());
        return true;
    }

    /* Returns true if successfully changes player's spy mode */
    private boolean setSpy(final UUID uuid, final boolean bool) {
        if (bool) {
            if (!plugin.getPlayerDataManager().containsUUID(uuid, "spy", null)) {
                plugin.getPlayerDataManager().addUUID(uuid, "spy", null);
                return true;
            }
        } else {
            if (plugin.getPlayerDataManager().containsUUID(uuid, "spy", null)) {
                plugin.getPlayerDataManager().removeUUID(uuid, "spy", null);
                return true;
            }
        }
        return false;
    }
}
