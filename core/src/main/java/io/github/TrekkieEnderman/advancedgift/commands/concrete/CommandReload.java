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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandReload extends SimpleCommand {
    public CommandReload(AdvancedGift plugin) {
        super(plugin, "agreload", "advancedgift.reload");
    }

    @Override
    public void showUsage(CommandSender sender) {
        sender.sendMessage(Message.COMMAND_RELOAD_DESCRIPTION.translatePrefixed());
        sender.sendMessage(Message.COMMAND_RELOAD_USAGE.translate());
    }

    @Override
    public boolean run(@NotNull final CommandSender sender, @NotNull final String label,  @NotNull final String[] args) {
        if (plugin.loadConfigFile()) {
            sender.sendMessage(Message.CONFIG_RELOADED.translatePrefixed());
        } else {
            sender.sendMessage(Message.CONFIG_NOT_RELOADED.translatePrefixed());
            if (sender instanceof Player) sender.sendMessage(Message.CHECK_CONSOLE.translatePrefixed());
        }
        return true;
    }
}
