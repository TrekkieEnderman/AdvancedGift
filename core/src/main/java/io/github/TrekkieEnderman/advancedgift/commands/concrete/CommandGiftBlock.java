/*
 * Copyright (c) 2025-2026 TrekkieEnderman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.TrekkieEnderman.advancedgift.commands.concrete;

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import io.github.TrekkieEnderman.advancedgift.commands.SimpleCommand;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandGiftBlock extends SimpleCommand {
    public CommandGiftBlock(AdvancedGift plugin) {
        super(plugin, "giftblock", null);
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage(Message.COMMAND_BLOCK_DESCRIPTION.translatePrefixed());
        sender.sendMessage(Message.COMMAND_BLOCK_USAGE.translate());
    }

    @Override
    protected boolean run(@NotNull final Player sender, @NotNull final String label, @NotNull final String[] args) {
        if (args.length == 0) {
            showUsage(sender);
            return true;
        }

        final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[0]);

        if (targetPlayer.equals(sender)) {
            sender.sendMessage(Message.BLOCK_SELF.translatePrefixed());
            return false;
        }
        if (!targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(Message.PLAYER_NOT_FOUND.translatePrefixed(args[0]));
            return false;
        }

        final String targetName = targetPlayer.getName();
        if (plugin.getPlayerDataManager().blockPlayer(sender.getUniqueId(), targetPlayer.getUniqueId())) {
            sender.sendMessage(Message.BLOCK_OTHER.translatePrefixed(targetName));
        } else {
            sender.sendMessage(Message.OTHER_BLOCKED_ALREADY.translatePrefixed(targetName));
        }
        return true;
    }

}