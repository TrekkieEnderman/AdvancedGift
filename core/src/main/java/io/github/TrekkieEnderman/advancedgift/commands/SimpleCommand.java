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

package io.github.TrekkieEnderman.advancedgift.commands;

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public abstract class SimpleCommand implements CommandExecutor {
    protected final AdvancedGift plugin;
    protected final String name;
    @Getter
    private final String permission;

    public boolean isAuthorized(@NotNull final CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) return true;
        if (permission == null || permission.isEmpty()) return true;
        return sender.hasPermission(permission);
    }

    protected abstract void showUsage(CommandSender sender);

    protected boolean run(@NotNull final Player sender, @NotNull final String commandLabel, @NotNull final String[] args) {
        return false;
    }

    protected boolean run(@NotNull final ConsoleCommandSender sender, @NotNull final String commandLabel, @NotNull final String[] args) {
        sender.sendMessage(Message.COMMAND_FOR_PLAYER_ONLY.translate());
        return true;
    }

    protected boolean run(@NotNull final CommandSender sender, @NotNull final String label, @NotNull final String[] args) {
        if (sender instanceof Player) {
            return run((Player) sender, label, args);
        }

        return run((ConsoleCommandSender) sender, label, args);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!isAuthorized(sender)) {
            sender.sendMessage(Message.COMMAND_NO_PERMISSION.translate());
            return true;
        }

        if (!(sender instanceof Player || sender instanceof ConsoleCommandSender)) {
            // Exit silently for unsupported command sender types (command blocks, non-player entities, etc.) to avoid unexpected behaviors
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("?")) {
            showUsage(sender);
            return true;
        }

        if (!run(sender, label, args)) {
            sender.sendMessage(Message.COMMAND_USAGE_TIP.translate(name));
        }
        return true;
    }
}
