package io.github.TrekkieEnderman.advancedgift;

import java.util.Set;
import java.util.UUID;

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

public class CommandGiftBlock implements CommandExecutor {
    private final AdvancedGift plugin;

    CommandGiftBlock(AdvancedGift plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(final CommandSender commandSender, final Command cmd, final String label, final String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("This command can only be run by a player.");
            return true;
        }
        final Player sender = (Player) commandSender;
        final UUID senderUUID = sender.getUniqueId();
        if (cmd.getName().equalsIgnoreCase("giftblock")) {
            if (args.length == 0) {
                sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Block gifts from a player you dislike or find annoying!" + ChatColor.GRAY + " ...Or unblock them.");
                sender.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/giftblock [player]" + ChatColor.GRAY + "  ||  " + ChatColor.WHITE + "/giftunblock [player]");
            } else {
                String targetName = args[0];
                Player targetPlayer = Bukkit.getServer().getPlayer(targetName);
                UUID targetUUID;
                if (targetPlayer == null) {
                    OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);
                    if (!targetOffline.hasPlayedBefore()) {
                        sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "No player going by " + targetName + " has played on here before.");
                        return true;
                    }
                    targetUUID = targetOffline.getUniqueId();
                    targetName = targetOffline.getName();
                } else {
                    if (targetPlayer == sender.getPlayer()) {
                        sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Are you trying to block yourself?");
                        return true;
                    }
                    targetUUID = targetPlayer.getUniqueId();
                    targetName = targetPlayer.getName();
                }
                if (label.equalsIgnoreCase("giftblock") || label.equalsIgnoreCase("blockgift") || label.equalsIgnoreCase("gblock")) {
                    if (!plugin.getPlayerDataManager().containsUUID(senderUUID, "block", targetUUID)) {
                        plugin.getPlayerDataManager().addUUID(senderUUID, "block", targetUUID);
                        sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Added " + targetName + " to your gift block list!");
                    } else {
                        sender.sendMessage(plugin.getPrefix() + ChatColor.GRAY + targetName + " is already on your gift block list.");
                    }
                } else {
                    if (plugin.getPlayerDataManager().containsUUID(senderUUID, "block", targetUUID)) {
                        plugin.getPlayerDataManager().removeUUID(senderUUID, "block", targetUUID);
                        sender.sendMessage(plugin.getPrefix() + ChatColor.AQUA + "Removed " + targetName + " from your gift block list!");
                    } else {
                        sender.sendMessage(plugin.getPrefix() + ChatColor.GRAY + targetName + " is not on your gift block list.");
                    }
                }
            }
        } else {
            if (args.length == 0) {
                final Set<UUID> blockList = plugin.getPlayerDataManager().getBlockList(senderUUID);
                if (blockList == null || blockList.isEmpty()) sender.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "Your gift block list is empty.");
                else {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "Your gift block list:");
                    ComponentBuilder builder = new ComponentBuilder(""); //main builder for showing the list

                    final String blankSpaceString = " ";
                    final HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to unblock this player").create());

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
                    sender.sendMessage(ChatColor.AQUA + "To unblock a player, click on their name in the list or use " + ChatColor.WHITE + "/giftunblock <player>");
                    sender.sendMessage(ChatColor.AQUA + "To clear the list, use " + ChatColor.WHITE + "/giftblocklist clear");
                }
            } else {
                if (args[0].equalsIgnoreCase("clear")) {
                    if (plugin.getPlayerDataManager().clearBlockList(senderUUID)) sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Cleared your gift block list!");
                    else sender.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "Your gift block list is already empty.");
                } else {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Cannot understand " + args[0] + "!");
                }
            }

        }
        return true;
    }

}