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
import io.github.TrekkieEnderman.advancedgift.data.PlayerData;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandSpy extends SimpleCommand {
    public CommandSpy(final AdvancedGift plugin) {
        super(plugin, "giftspy", "advancedgift.gift.spy");
    }

    @Override
    public void showUsage(CommandSender sender) {
        sender.sendMessage(Message.COMMAND_SPY_DESCRIPTION.translatePrefixed());
        sender.sendMessage(Message.COMMAND_SPY_USAGE.translate());
    }

    @Override
    public boolean run(@NotNull Player sender, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            toggle(sender, !plugin.getPlayerDataManager().getData(sender).isSpy());
            return true;
        }

        final String arg = args[0];
        if (arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("on") || arg.equalsIgnoreCase("enable")) {
            toggle(sender, true);
        } else if (arg.equalsIgnoreCase("false") || arg.equalsIgnoreCase("off") || arg.equalsIgnoreCase("disable")) {
            toggle(sender, false);
        } else {
            sender.sendMessage(Message.ARGUMENT_NOT_RECOGNIZED.translatePrefixed(arg));
            return false;
        }
        return true;
    }

    private void toggle(Player sender, boolean enabled) {
        final PlayerData data = plugin.getPlayerDataManager().getData(sender);
        if (data.isSpy() == enabled) {
            if (enabled) {
                sender.sendMessage(Message.SPY_ALREADY_ENABLED.translatePrefixed());
            } else {
                sender.sendMessage(Message.SPY_ALREADY_DISABLED.translatePrefixed());
            }
            return;
        }
        data.setSpy(enabled);
        if (enabled) {
            sender.sendMessage(Message.SPY_ENABLED.translatePrefixed());
        } else {
            sender.sendMessage(Message.SPY_DISABLED.translatePrefixed());
        }
    }
}
