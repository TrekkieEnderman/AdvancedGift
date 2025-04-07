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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class CommandGiftUnblock implements CommandExecutor {
    private final AdvancedGift plugin;

    public CommandGiftUnblock(final AdvancedGift plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(plugin.getPrefix() + Message.COMMAND_FOR_PLAYER_ONLY.translate());
            return true;
        }
        if (args.length == 0) {
            commandSender.sendMessage(plugin.getPrefix() + Message.COMMAND_UNBLOCK_DESCRIPTION.translate());
            commandSender.sendMessage(Message.COMMAND_UNBLOCK_USAGE.translate());
            return true;
        }

        final Player sender = (Player) commandSender;
        final UUID senderUUID = sender.getUniqueId();
        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (plugin.getPlayerDataManager().containsUUID(senderUUID, "block", target.getUniqueId())) {
            plugin.getPlayerDataManager().removeUUID(senderUUID, "block", target.getUniqueId());
            sender.sendMessage(plugin.getPrefix() + Message.UNBLOCK_OTHER.translate(target.getName()));
        } else {
            sender.sendMessage(plugin.getPrefix() + Message.OTHER_UNBLOCKED_ALREADY.translate(target.getName()));
        }
        return true;
    }
}
