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

public class CommandReload implements CommandExecutor {
    private final AdvancedGift plugin;

    public CommandReload(AdvancedGift plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("advancedgift.reload")) {
            sender.sendMessage(plugin.getPrefix() + Message.COMMAND_NO_PERMISSION.translate());
            return true;
        }

        if (plugin.loadConfigFile()) {
            sender.sendMessage(plugin.getPrefix() + Message.CONFIG_RELOADED.translate());
        } else {
            sender.sendMessage(plugin.getPrefix() + Message.CONFIG_NOT_RELOADED.translate());
            if (sender instanceof Player) sender.sendMessage(plugin.getPrefix() + Message.CHECK_CONSOLE.translate());
        }
        return true;
    }
}
