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
import io.github.TrekkieEnderman.advancedgift.locale.Translation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class CommandTranslate implements CommandExecutor {
    private final AdvancedGift plugin;
    private final Path translationsDirectory;

    public CommandTranslate(final AdvancedGift plugin) {
        this.plugin = plugin;
        translationsDirectory = plugin.getDataFolder().toPath().resolve(Translation.TRANSLATIONS_DIRECTORY_NAME);
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final @NotNull String[] args) {
        if (!sender.hasPermission("advancedgift.translate")) {
            sender.sendMessage(plugin.getPrefix() + Message.COMMAND_NO_PERMISSION.translate());
            return true;
        }

        if (!Files.exists(translationsDirectory)) {
            try {
                Files.createDirectories(translationsDirectory);
            } catch (IOException ignored) {
            }
        }

        final Locale targetLocale;

        if (args.length == 0) {
            targetLocale = Translation.getServerLocale();
        } else {
            targetLocale = Translation.parseLocale(args[0]);
        }

        if (targetLocale == null) {
            sender.sendMessage(plugin.getPrefix() + Message.UNKNOWN_LOCALE.translate(args[0]));
            return false;
        }

        if (Translation.exportTranslation(targetLocale)) {
            sender.sendMessage(plugin.getPrefix() + Message.TRANSLATION_CREATED.translate(targetLocale, translationsDirectory));
        } else {
            sender.sendMessage(plugin.getPrefix() + Message.TRANSLATION_NOT_CREATED.translate(targetLocale, translationsDirectory));
            if (sender instanceof Player) sender.sendMessage(plugin.getPrefix() + Message.CHECK_CONSOLE.translate());
        }
        return true;
    }
}
