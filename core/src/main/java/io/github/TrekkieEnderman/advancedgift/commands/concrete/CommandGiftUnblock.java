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

import java.util.UUID;

public class CommandGiftUnblock extends SimpleCommand {
    public CommandGiftUnblock(final AdvancedGift plugin) {
        super(plugin, "giftunblock", null);
    }

    @Override
    public void showUsage(CommandSender sender) {
        sender.sendMessage(Message.COMMAND_UNBLOCK_DESCRIPTION.translatePrefixed());
        sender.sendMessage(Message.COMMAND_UNBLOCK_USAGE.translate());
    }

    @Override
    public boolean run(@NotNull Player sender, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            showUsage(sender);
            return true;
        }

        final UUID senderUUID = sender.getUniqueId();
        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (plugin.getPlayerDataManager().containsUUID(senderUUID, "block", target.getUniqueId())) {
            plugin.getPlayerDataManager().removeUUID(senderUUID, "block", target.getUniqueId());
            sender.sendMessage(Message.UNBLOCK_OTHER.translatePrefixed(target.getName()));
        } else {
            sender.sendMessage(Message.OTHER_UNBLOCKED_ALREADY.translatePrefixed(target.getName()));
        }
        return true;
    }
}
