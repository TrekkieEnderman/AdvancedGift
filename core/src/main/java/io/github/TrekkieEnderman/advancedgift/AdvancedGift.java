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

package io.github.TrekkieEnderman.advancedgift;

import io.github.TrekkieEnderman.advancedgift.commands.concrete.*;
import io.github.TrekkieEnderman.advancedgift.data.PlayerDataManager;
import io.github.TrekkieEnderman.advancedgift.data.StandardDataManager;
import io.github.TrekkieEnderman.advancedgift.listener.PlayerJoinListener;
import io.github.TrekkieEnderman.advancedgift.locale.Message;
import io.github.TrekkieEnderman.advancedgift.locale.TranslationManager;
import io.github.TrekkieEnderman.advancedgift.metrics.GiftCounter;
import io.github.TrekkieEnderman.advancedgift.util.ComponentUtils;
import lombok.Getter;
import me.Fupery.ArtMap.ArtMap;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AdvancedGift extends JavaPlugin {
    private boolean hasArtMap = false;
    @Getter
    private final GiftCounter giftCounter = new GiftCounter();
    @Getter
    private PlayerDataManager playerDataManager;
    @Getter
    private Config configuration;
    @Getter
    private GiftManager giftManager;
    @Getter
    private TranslationManager localization;

    @Override
    public void onEnable() {
        if(!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }
        localization = new TranslationManager(this);
        configuration = new Config(this);
        if (configuration.isOutdated()) {
            getLogger().warning(ComponentUtils.toPlainText(Message.OUTDATED_CONFIG.translate()));
        }
        playerDataManager = new StandardDataManager(this);
        this.getPlayerDataManager().load();
        giftManager = new GiftManager(this);
        this.getCommand("gift").setExecutor(new CommandGift(this));
        this.getCommand("togglegift").setExecutor(new CommandGiftToggle(this));
        this.getCommand("giftblock").setExecutor(new CommandGiftBlock(this));
        this.getCommand("giftunblock").setExecutor(new CommandGiftUnblock(this));
        this.getCommand("giftblocklist").setExecutor(new CommandGiftBlockList(this));
        this.getCommand("agreload").setExecutor(new CommandReload(this));
        this.getCommand("giftspy").setExecutor(new CommandSpy(this));
        hasArtMap = Bukkit.getPluginManager().isPluginEnabled("ArtMap");
        if (configuration.isMetricsEnabled()) {
            startMetrics();
        }
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }

    @Override
    public void onDisable() {
        this.getPlayerDataManager().save();
    }

    private void startMetrics() {
        Metrics metrics = new Metrics(this, 13627);
        metrics.addCustomChart(new SingleLineChart("gifts_sent", giftCounter::collect));
    }

    public boolean hasArtMap() {
        return hasArtMap;
    }

    public boolean isPainting(Player player) {
        if (!hasArtMap) return false;
        final ArtMap artMap = ArtMap.instance();
        if (!artMap.getConfiguration().FORCE_ART_KIT) return false;
        return artMap.getArtistHandler().containsPlayer(player);
    }
}