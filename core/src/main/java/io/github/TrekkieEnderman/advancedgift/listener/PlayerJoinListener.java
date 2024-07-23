package io.github.TrekkieEnderman.advancedgift.listener;

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    final private AdvancedGift plugin;

    public PlayerJoinListener(final AdvancedGift plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent joinEvent) {
        Player player = joinEvent.getPlayer();
        if (player.isOp() && plugin.isConfigOutdated()) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Config is outdated.");
        }
    }
}
