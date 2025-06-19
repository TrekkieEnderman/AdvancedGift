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
import io.github.TrekkieEnderman.advancedgift.ServerVersion;
import io.github.TrekkieEnderman.advancedgift.commands.SimpleCommand;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class CommandGiftBlockList extends SimpleCommand {
    private final static String BLANK_SPACE = " ";

    public CommandGiftBlockList(final AdvancedGift plugin) {
        super(plugin, null);
    }

    @Override
    protected void showUsage(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + Message.BLOCK_LIST_DESCRIPTION.translate());
        sender.sendMessage(Message.BLOCK_LIST_USAGE.translate());
    }

    @Override
    protected boolean run(@NotNull Player sender, @NotNull String label, @NotNull String[] args) {
        final UUID senderUUID = sender.getUniqueId();

        if (args.length == 0) {
            final Set<UUID> blockList = plugin.getPlayerDataManager().getBlockList(senderUUID);
            if (blockList == null || blockList.isEmpty()) sender.sendMessage(plugin.getPrefix() + Message.BLOCK_LIST_EMPTY.translate());
            else {
                sender.sendMessage(plugin.getPrefix() + Message.BLOCK_LIST_SHOW.translate());
                ComponentBuilder builder = new ComponentBuilder(""); //main builder for showing the list

                final HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Message.TIP_CLICK_TO_UNBLOCK.translate()).create());

                for (final UUID playerUUID : blockList) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                    TextComponent textComponent = offlinePlayer.isOnline()
                            ? new TextComponent(TextComponent.fromLegacyText(offlinePlayer.getPlayer().getDisplayName()))
                            : new TextComponent(offlinePlayer.getName());
                    textComponent.setColor(ChatColor.DARK_AQUA);
                    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/giftunblock " + offlinePlayer.getName());

                    builder.append(BLANK_SPACE);

                    if (ServerVersion.getMinorVersion() < 12) {
                        builder.append(textComponent.toLegacyText());
                    } else {
                        builder.append(textComponent);
                    }
                    builder.event(hoverEvent).event(clickEvent);
                }
                sender.spigot().sendMessage(builder.create());
                sender.sendMessage(Message.BLOCK_LIST_USAGE.translate());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (plugin.getPlayerDataManager().clearBlockList(senderUUID)) sender.sendMessage(plugin.getPrefix() + Message.BLOCK_LIST_CLEARED.translate());
            else sender.sendMessage(plugin.getPrefix() + Message.BLOCK_LIST_ALREADY_CLEARED.translate());
            return true;
        }

        sender.sendMessage(plugin.getPrefix() + Message.ARGUMENT_NOT_RECOGNIZED.translate(args[0]));
        return true;
    }
}
