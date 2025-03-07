/*
 * Copyright (c) 2024 TrekkieEnderman
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

package io.github.TrekkieEnderman.advancedgift;

import java.util.Set;
import java.util.UUID;

import io.github.TrekkieEnderman.advancedgift.locale.Message;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

public class CommandGiftBlock implements CommandExecutor {
    private final AdvancedGift plugin;

    CommandGiftBlock(AdvancedGift plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(@NotNull final CommandSender commandSender, @NotNull final Command cmd, @NotNull final String label, @NotNull final String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(plugin.getPrefix() + Message.COMMAND_FOR_PLAYER_ONLY.translate());
            return true;
        }
        final Player sender = (Player) commandSender;
        final UUID senderUUID = sender.getUniqueId();
        if (cmd.getName().equalsIgnoreCase("giftblock")) {
            if (args.length == 0) {
                sender.sendMessage(plugin.getPrefix() + Message.COMMAND_BLOCK_DESCRIPTION.translate());
                sender.sendMessage(Message.COMMAND_BLOCK_USAGE.translate());
            } else {
                String targetName = args[0];
                Player targetPlayer = Bukkit.getServer().getPlayer(targetName);
                UUID targetUUID;
                if (targetPlayer == null) {
                    OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);
                    if (!targetOffline.hasPlayedBefore()) {
                        sender.sendMessage(plugin.getPrefix() + Message.PLAYER_NOT_FOUND.translate(targetName));
                        return true;
                    }
                    targetUUID = targetOffline.getUniqueId();
                    targetName = targetOffline.getName();
                } else {
                    if (targetPlayer == sender.getPlayer()) {
                        sender.sendMessage(plugin.getPrefix() + Message.BLOCK_SELF.translate());
                        return true;
                    }
                    targetUUID = targetPlayer.getUniqueId();
                    targetName = targetPlayer.getName();
                }
                if (label.equalsIgnoreCase("giftblock") || label.equalsIgnoreCase("blockgift") || label.equalsIgnoreCase("gblock")) {
                    if (!plugin.getPlayerDataManager().containsUUID(senderUUID, "block", targetUUID)) {
                        plugin.getPlayerDataManager().addUUID(senderUUID, "block", targetUUID);
                        sender.sendMessage(plugin.getPrefix() + Message.BLOCK_OTHER.translate(targetName));
                    } else {
                        sender.sendMessage(plugin.getPrefix() + Message.OTHER_BLOCKED_ALREADY.translate(targetName));
                    }
                } else {
                    if (plugin.getPlayerDataManager().containsUUID(senderUUID, "block", targetUUID)) {
                        plugin.getPlayerDataManager().removeUUID(senderUUID, "block", targetUUID);
                        sender.sendMessage(plugin.getPrefix() + Message.UNBLOCK_OTHER.translate(targetName));
                    } else {
                        sender.sendMessage(plugin.getPrefix() + Message.OTHER_UNBLOCKED_ALREADY.translate(targetName));
                    }
                }
            }
        } else {
            if (args.length == 0) {
                final Set<UUID> blockList = plugin.getPlayerDataManager().getBlockList(senderUUID);
                if (blockList == null || blockList.isEmpty()) sender.sendMessage(plugin.getPrefix() + Message.BLOCK_LIST_EMPTY.translate());
                else {
                    sender.sendMessage(plugin.getPrefix() + Message.BLOCK_LIST_SHOW.translate());
                    ComponentBuilder builder = new ComponentBuilder(""); //main builder for showing the list

                    final String blankSpaceString = " ";
                    final HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Message.TIP_CLICK_TO_UNBLOCK.translate()).create());

                    for (final UUID playerUUID : blockList) {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                        TextComponent textComponent = offlinePlayer.isOnline()
                                ? new TextComponent(TextComponent.fromLegacyText(offlinePlayer.getPlayer().getDisplayName()))
                                : new TextComponent(offlinePlayer.getName());
                        textComponent.setColor(ChatColor.DARK_AQUA);
                        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/giftunblock " + offlinePlayer.getName());

                        builder.append(blankSpaceString);

                        if (ServerVersion.getMinorVersion() < 12) {
                            builder.append(textComponent.toLegacyText());
                        } else {
                            builder.append(textComponent);
                        }
                        builder.event(hoverEvent).event(clickEvent);
                    }
                    sender.spigot().sendMessage(builder.create());
                    sender.sendMessage(Message.BLOCK_LIST_USAGE_1.translate());
                    sender.sendMessage(Message.BLOCK_LIST_USAGE_2.translate());
                }
            } else {
                if (args[0].equalsIgnoreCase("clear")) {
                    if (plugin.getPlayerDataManager().clearBlockList(senderUUID)) sender.sendMessage(plugin.getPrefix() + Message.BLOCK_LIST_CLEARED.translate());
                    else sender.sendMessage(plugin.getPrefix() + Message.BLOCK_LIST_ALREADY_CLEARED.translate());
                } else {
                    sender.sendMessage(plugin.getPrefix() + Message.ARGUMENT_NOT_RECOGNIZED.translate(args[0]));
                }
            }

        }
        return true;
    }

}