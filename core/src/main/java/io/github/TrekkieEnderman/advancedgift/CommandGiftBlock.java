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
    private String prefix;

    CommandGiftBlock(AdvancedGift plugin) {
        this.plugin = plugin;
        this.prefix = this.plugin.prefix;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }
        Player s = (Player) sender;
        UUID senderUUID = s.getUniqueId();
        if (cmd.getName().equalsIgnoreCase("giftblock")) {
            if (args.length == 0) {
                s.sendMessage(prefix + ChatColor.YELLOW + "Block gifts from a player you dislike or find annoying!" + ChatColor.GRAY + " ...Or unblock them.");
                s.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/giftblock [player]" + ChatColor.GRAY + "  ||  " + ChatColor.WHITE + "/giftunblock [player]");
            } else {
                String target = args[0];
                Player targetPlayer = Bukkit.getServer().getPlayer(target);
                UUID targetUUID;
                if (targetPlayer == null) {
                    OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(target);
                    if (!targetOffline.hasPlayedBefore()) {
                        s.sendMessage(prefix + ChatColor.RED + "No player going by " + target + " has played on here before.");
                        return true;
                    }
                    targetUUID = targetOffline.getUniqueId();
                    target = targetOffline.getName();
                } else {
                    if (targetPlayer == s.getPlayer()) {
                        s.sendMessage(prefix + ChatColor.RED + "Are you trying to block yourself?");
                        return true;
                    }
                    targetUUID = targetPlayer.getUniqueId();
                    target = targetPlayer.getName();
                }
                if (label.equalsIgnoreCase("giftblock") || label.equalsIgnoreCase("blockgift") || label.equalsIgnoreCase("gblock")) {
                    if (!plugin.getPlayerDataManager().containsUUID(senderUUID, "block", targetUUID)) {
                        plugin.getPlayerDataManager().addUUID(senderUUID, "block", targetUUID);
                        s.sendMessage(prefix + ChatColor.GREEN + "Added " + target + " to your gift block list!");
                    } else {
                        s.sendMessage(prefix + ChatColor.GRAY + target + " is already on your gift block list.");
                    }
                } else {
                    if (plugin.getPlayerDataManager().containsUUID(senderUUID, "block", targetUUID)) {
                        plugin.getPlayerDataManager().removeUUID(senderUUID, "block", targetUUID);
                        s.sendMessage(prefix + ChatColor.AQUA + "Removed " + target + " from your gift block list!");
                    } else {
                        s.sendMessage(prefix + ChatColor.GRAY + target + " is not on your gift block list.");
                    }
                }
            }
        } else {
            if (args.length == 0) {
                Set<UUID> blockList = plugin.getPlayerDataManager().getBlockList(senderUUID);
                if (blockList == null || blockList.isEmpty()) s.sendMessage(prefix + ChatColor.GRAY + "Your gift block list is empty.");
                else {
                    s.sendMessage(prefix + ChatColor.GRAY + "Your gift block list:");
                    ComponentBuilder builder = new ComponentBuilder(""); //main builder for showing the list

                    final String blankSpaceString = " ";
                    final HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to unblock this player").create());

                    for (UUID playerUUID : blockList) {
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
                    s.spigot().sendMessage(builder.create());
                    s.sendMessage(ChatColor.AQUA + "To unblock a player, click on their name in the list or use " + ChatColor.WHITE + "/giftunblock <player>");
                    s.sendMessage(ChatColor.AQUA + "To clear the list, use " + ChatColor.WHITE + "/giftblocklist clear");
                }
            } else {
                if (args[0].equalsIgnoreCase("clear")) {
                    if (plugin.getPlayerDataManager().clearBlockList(senderUUID)) s.sendMessage(prefix + ChatColor.GREEN + "Cleared your gift block list!");
                    else s.sendMessage(prefix + ChatColor.GRAY + "Your gift block list is already empty.");
                } else {
                    s.sendMessage(prefix + ChatColor.RED + "Cannot understand " + args[0] + "!");
                }
            }

        }
        return true;
    }

}