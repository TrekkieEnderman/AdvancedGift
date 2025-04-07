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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

public class CommandGiftBlock implements CommandExecutor {
    private final AdvancedGift plugin;

    public CommandGiftBlock(AdvancedGift plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(@NotNull final CommandSender commandSender, @NotNull final Command cmd, @NotNull final String label, @NotNull final String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(plugin.getPrefix() + Message.COMMAND_FOR_PLAYER_ONLY.translate());
            return true;
        }
        if (args.length == 0) {
            commandSender.sendMessage(plugin.getPrefix() + Message.COMMAND_BLOCK_DESCRIPTION.translate());
            commandSender.sendMessage(Message.COMMAND_BLOCK_USAGE.translate());
            return true;
        }

        final Player sender = (Player) commandSender;
        final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[0]);

        if (targetPlayer.equals(sender)) {
            sender.sendMessage(plugin.getPrefix() + Message.BLOCK_SELF.translate());
            return true;
        }
        if (!targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(plugin.getPrefix() + Message.PLAYER_NOT_FOUND.translate(args[0]));
            return true;
        }

        final UUID senderUUID = sender.getUniqueId();
        final UUID targetUUID = targetPlayer.getUniqueId();
        final String targetName = targetPlayer.getName();

        if (!plugin.getPlayerDataManager().containsUUID(senderUUID, "block", targetUUID)) {
            plugin.getPlayerDataManager().addUUID(senderUUID, "block", targetUUID);
            sender.sendMessage(plugin.getPrefix() + Message.BLOCK_OTHER.translate(targetName));
        } else {
            sender.sendMessage(plugin.getPrefix() + Message.OTHER_BLOCKED_ALREADY.translate(targetName));
        }
        return true;
    }

}