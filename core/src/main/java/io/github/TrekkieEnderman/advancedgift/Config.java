/*
 * Copyright (c) 2026 TrekkieEnderman
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

import io.github.TrekkieEnderman.advancedgift.locale.TranslationManager;
import io.github.TrekkieEnderman.advancedgift.util.ComponentUtils;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.logging.Logger;

@NullMarked
public class Config {
    private final Logger logger;

    @Getter
    private int version = 2;
    @Getter
    private boolean outdated = false;
    @Getter
    private @Nullable Component prefix = Component.text("[AdvancedGift]").color(NamedTextColor.GOLD);
    @Getter
    private boolean metricsEnabled = true;
    @Getter
    private Locale locale = TranslationManager.DEFAULT_LOCALE;
    @Getter
    private boolean cooldownEnabled = false;
    @Getter
    private long cooldownDuration = 10;
    @Getter
    private boolean giftMessageEnabled = true;
    @Getter
    private boolean censorshipEnabled = false;
    @Getter
    private CensorshipOptions censorshipMode = CensorshipOptions.REMOVE;
    @Getter
    private List<String> wordFilters = Collections.emptyList();
    @Getter
    private boolean interworldGiftRestricted = false;
    private final Map<String, Integer> worldGroups = new HashMap<>();

    public Config(Logger logger) {
        this.logger = logger;
    }

    void load(Configuration config) {
        version = config.isSet("version") ? config.getInt("version") : -1;
        outdated = version != config.getDefaults().getInt("version");
        prefix = ComponentUtils.fromMiniMessage(config.getString("prefix"));
        metricsEnabled = config.getBoolean("enable-metrics");
        locale = TranslationManager.parseLocale(config.getString("locale"), locale);
        cooldownEnabled = config.getBoolean("cooldown.enabled");
        cooldownDuration = config.getLong("cooldown.duration");
        giftMessageEnabled = config.getBoolean("allow-gift-message");
        censorshipEnabled = config.getBoolean("message-censorship.enabled");
        try {
            censorshipMode = CensorshipOptions.valueOf(config.getString("message-censorship.mode"));
        } catch (IllegalArgumentException e) {
            logParseFailure("message-censorship.mode");
        }
        wordFilters = List.copyOf(config.getStringList("message-censorship.filter"));
        interworldGiftRestricted = config.getBoolean("interworld-restriction.enabled");
        final String worldGroupPath = "interworld-restriction.world-groups";
        if (!config.isConfigurationSection(worldGroupPath)) {
            logParseFailure(worldGroupPath);
        } else {
            final Map<String, Integer> map = new HashMap<>();
            final Set<String> groupKeys = config.getConfigurationSection(worldGroupPath).getKeys(false);
            int groupID = 0;
            for (String groupKey : groupKeys) {
                for (String world : config.getStringList(worldGroupPath + "." + groupKey)) {
                    map.putIfAbsent(world, groupID);
                }
                groupID++;
            }
            worldGroups.clear();
            worldGroups.putAll(map);
        }
    }

    public Map<String, Integer> getWorldGroups() {
        return Map.copyOf(worldGroups);
    }

    public int getGroupID(World world) {
        return worldGroups.getOrDefault(world.getName(), -1);
    }

    private void logParseFailure(final String path) {
        String message = "Could not parse '%s' in config.yml! Is it misconfigured?";
        logger.warning(message.formatted(path));
    }
}
