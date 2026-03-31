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

package io.github.TrekkieEnderman.advancedgift.locale;

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

@NullMarked
public class TranslationManager {
    public static final Locale DEFAULT_LOCALE = Locale.US;
    public static final Locale SILLY_LOCALE = Locale.of("sas", "SY");
    public static final String TRANSLATIONS_DIRECTORY_NAME = "translations";
    public static final String BASE_BUNDLE_NAME = TRANSLATIONS_DIRECTORY_NAME + ".messages";
    private static final ResourceBundle EMPTY_BUNDLE = new ResourceBundle() {
        @Nullable @Override
        protected Object handleGetObject(final String key) {
            return null;
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.emptyEnumeration();
        }
    };
    private final AdvancedGift plugin;
    private MiniMessageTranslationStore store;
    @Getter
    private static Locale serverLocale = DEFAULT_LOCALE;
    private final ResourceBundle defaultBundle;

    public TranslationManager(AdvancedGift plugin) {
        this.plugin = plugin;
        this.defaultBundle = getEmbeddedBundle(DEFAULT_LOCALE);
        // TODO Need to add a way to handle or migrate translation files with legacy formatting
        reload(DEFAULT_LOCALE);
    }

    /**
     * Reloads translation files.
     * @param newLocale new locale to use.
     */
    public void reload(final Locale newLocale) {
        final Locale previousLocale = serverLocale;
        serverLocale = newLocale;

        if (!previousLocale.equals(serverLocale)) {
            if (previousLocale.equals(SILLY_LOCALE)) {
                // Switching away from the silly locale makes the plugin sad
                plugin.getLogger().info("Oh, I see how it is...");
            }
            plugin.getLogger().info("Now using locale '" + serverLocale + "'.");
        }
        exportTranslation(serverLocale);

        GlobalTranslator.translator().removeSource(store);
        store = MiniMessageTranslationStore.create(Key.key("advancedgift", "main"));
        store.defaultLocale(DEFAULT_LOCALE);

        final Path path = getCustomTranslationPath(serverLocale);
        if (Files.exists(path)) {
            registerTranslation(serverLocale, getCustomTranslationPath(serverLocale));
        }
        registerTranslation(serverLocale, getEmbeddedBundle(serverLocale));
        registerTranslation(DEFAULT_LOCALE, defaultBundle);

        GlobalTranslator.translator().addSource(store);
    }

    private void registerTranslation(final Locale locale, final ResourceBundle bundle) {
        try {
            store.registerAll(locale, bundle, false);
        } catch (IllegalArgumentException ignored) {}
    }

    private void registerTranslation(final Locale locale, final Path path) {
        try {
            store.registerAll(locale, path, false);
        } catch (IllegalArgumentException ignored) {}
    }

    /**
     * Parses locale.
     * @param string input to parse.
     * @return a Locale instance or null if the parsing fails.
     */
    public static @Nullable Locale parseLocale(final @Nullable String string) {
        if (string == null) return null;
        return Translator.parseLocale(string);
    }

    /**
     * Parses locale.
     * @param string input to parse.
     * @param def locale to fall back on if the parsing fails.
     * @return a Locale instance.
     */
    public static Locale parseLocale(final @Nullable String string, final Locale def) {
        if (string == null) return def;
        Locale locale = parseLocale(string);
        return locale == null ? def : locale;
    }

    private void exportTranslation(final Locale targetLocale) {
        final String targetBundle = resolveTranslationName(targetLocale);
        final Path destination = plugin.getDataPath().resolve(targetBundle);

        if (Files.exists(destination)) {
            return;
        }

        URL resource = getClass().getClassLoader().getResource(targetBundle);
        if (resource == null) {
            resource = getClass().getClassLoader().getResource(resolveTranslationName(Locale.ROOT));
        }
        if (resource == null) {
            plugin.getLogger().warning("Unable to find the embedded base translation file. This shouldn't happen.");
            return;
        }

        try (InputStream stream = resource.openStream()) {
            Files.copy(stream, destination);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Exception occurred while creating a custom translation file for " + targetLocale, e);
        }
    }

    private ResourceBundle getEmbeddedBundle(final Locale locale) {
        try {
            return ResourceBundle.getBundle(BASE_BUNDLE_NAME, locale);
        } catch (MissingResourceException ex) {
            // Not even the base bundle is found. Things must be messed up somewhere for this to happen.
            plugin.getLogger().log(Level.SEVERE, "Unable to find the embedded base translation file. This shouldn't happen.", ex);
            return EMPTY_BUNDLE;
        }
    }

    private String resolveTranslationName(final Locale locale) {
        final String suffix = "properties";
        final ResourceBundle.Control control = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);
        return control.toResourceName(control.toBundleName(BASE_BUNDLE_NAME, locale), suffix);
    }

    private Path getCustomTranslationPath(final Locale locale) {
        return plugin.getDataPath().resolve(resolveTranslationName(locale));
    }

    /**
     * Renders a component with the given translation key, using the global render.
     * The locale set in the config would be used.
     * @param key the translation key to use for rendering a component.
     * @return the rendered component.
     */
    public static Component render(String key) {
        return render(key, serverLocale);
    }

    /**
     * Renders a component with the given translation key, using the global renderer.
     * @param key the translation key to use for a rendering a component.
     * @param locale the locale to use when rendering.
     * @return the rendered component.
     */
    public static Component render(String key, @Nullable Locale locale) {
        return render(Component.translatable(key), locale);
    }

    /**
     * Renders a component using the global renderer. The locale set in config would be used.
     * @param component the component to render.
     * @return the rendered component.
     */
    public static Component render(Component component) {
        return render(component, serverLocale);
    }

    /**
     * Renders a component using the global renderer.
     * @param component the component to render.
     * @param locale the locale to use when rendering.
     * @return the rendered component.
     */
    public static Component render(Component component, @Nullable Locale locale) {
        if (locale == null) {
            locale = serverLocale;
        }
        return GlobalTranslator.render(component, locale);
    }
}
