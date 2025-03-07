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

package io.github.TrekkieEnderman.advancedgift.locale;

import io.github.TrekkieEnderman.advancedgift.AdvancedGift;
import io.github.TrekkieEnderman.advancedgift.util.ChatFormatUtils;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

/* For now only server locale is used for translations. However, I want to allow for
expansion to per-player translations in the future, if not by me then by someone,
hence the way this class is structured. */
public class Translation {
    private static Translation instance;
    public static final Locale DEFAULT_LOCALE = Locale.US;
    private static final Locale SILLY_LOCALE = new Locale("sas", "SY");
    private static final Pattern REMOVE_DOUBLE_QUOTE = Pattern.compile("''");
    private static final String TRANSLATIONS_DIRECTORY_NAME = "translations";
    private static final String BASE_BUNDLE_NAME = TRANSLATIONS_DIRECTORY_NAME + ".messages";
    private static final ResourceBundle EMPTY_BUNDLE = new ResourceBundle() {
        @Nullable
        @Override
        protected Object handleGetObject(final @NotNull String key) {
            return null;
        }

        @NotNull
        @Override
        public Enumeration<String> getKeys() {
            return Collections.emptyEnumeration();
        }
    };

    private final AdvancedGift plugin;
    @Getter
    private static Locale serverLocale = DEFAULT_LOCALE;
    private final Map<Locale, ResourceBundle> loadedBundles = new HashMap<>();
    private final Map<Locale, Map<String, MessageFormat>> messageFormatCache = new HashMap<>();
    private final ClassLoader fileBundleClassLoader;
    private final ResourceBundle.Control utf8BundleCtrl = new UTF8ResourceBundleControl();
    private final ResourceBundle defaultBundle;

    public Translation(AdvancedGift plugin) {
        this.plugin = plugin;
        fileBundleClassLoader = new FileBundleClassLoader(plugin, getClass().getClassLoader());
        defaultBundle = getEmbeddedBundle(DEFAULT_LOCALE);
        loadedBundles.put(DEFAULT_LOCALE, defaultBundle);
    }

    // Static methods

    public static void init(final @NotNull AdvancedGift plugin) {
        if (instance == null) instance = new Translation(plugin);
    }

    /* Reloads and rereads custom language files */
    public static void updateLocale(final String locale) {
        if (instance == null) {
            throw new IllegalStateException("Translation class isn't initialized");
        }

        final Locale previousLocale = serverLocale;

        // Parse locale
        final Locale newLocale = parseLocale(locale);
        if (newLocale != null) {
            serverLocale = newLocale;
        }

        if (!previousLocale.equals(serverLocale)) {
            if (previousLocale.equals(SILLY_LOCALE)) {
                //Switching away from the silly locale makes the plugin sad
                instance.plugin.getLogger().info("Oh, I see how it is...");
            }
            instance.plugin.getLogger().info("Now using locale '" + serverLocale.toString() + "'.");
        }

        // Clear cache and reload files
        ResourceBundle.clearCache();
        instance.loadedBundles.clear();
        instance.messageFormatCache.clear();
        instance.getBundle(serverLocale);
    }

    /* Returns a translated string for the given locale, or the key itself if the class isn't initialized. */
    public static String translate(final Locale locale, final @NotNull String key, final Object... objects) {
        if (instance == null) {
            return key;
        }

        if (objects == null || objects.length == 0) {
            final String string = REMOVE_DOUBLE_QUOTE.matcher(instance.getString(locale, key)).replaceAll("'");
            return ChatFormatUtils.format(string);
        }
        return ChatFormatUtils.format(instance.format(locale, key, objects));
    }

    public static Locale parseLocale(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        final String[] parts = string.split("[_.-]");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else if (parts.length == 3) {
            return new Locale(parts[0], parts[1], parts[2]);
        }
        return null;
    }

    // Instance methods

    /* Returns the cached resource bundle for the given locale.
    If the bundle wasn't cached, then this looks at the language folder in the plugin directory
    for custom one first then in the embedded folder, and cache the bundle found. */
    private ResourceBundle getBundle(final @NotNull Locale locale) {
        return loadedBundles.computeIfAbsent(locale, loc -> {
            try {
                return ResourceBundle.getBundle(BASE_BUNDLE_NAME, loc, fileBundleClassLoader, utf8BundleCtrl);
            } catch (MissingResourceException ignored) {
                return getEmbeddedBundle(loc);
            }
        });
    }

    /* Returns the resource bundle stored within the jar. */
    private ResourceBundle getEmbeddedBundle(final @NotNull Locale locale) {
        try {
            return ResourceBundle.getBundle(BASE_BUNDLE_NAME, locale, utf8BundleCtrl);
        } catch (MissingResourceException ex) {
            // Not even the base bundle is found. Things must be messed up somewhere for this to happen.
            plugin.getLogger().log(Level.SEVERE, "Unable to find the embedded base translation file. This shouldn't happen.", ex);
            return EMPTY_BUNDLE;
        }
    }

    private String format(final Locale locale, final @NotNull String key, final @NotNull Object... objects) {
        MessageFormat messageFormat = messageFormatCache.computeIfAbsent(locale, loc -> new HashMap<>()).computeIfAbsent(key, k -> {
            String string = getString(locale, k);
            try {
                return new MessageFormat(string);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().severe("Invalid format for '" + string + "': " + e.getMessage());
                string = string.replaceAll("\\{(\\D*?)}", "[$1]");
                return new MessageFormat(string);
            }
        });
        return messageFormat.format(objects);
    }

    private String getString(final Locale locale, final @NotNull String key) {
        String string = null;
        try {
            string = getBundle(locale).getString(key);
        } catch (final MissingResourceException ex) {
            plugin.getLogger().warning(String.format("Missing translation key '%s' in translation file %s", ex.getKey(), serverLocale));
        }
        return string != null && !string.isEmpty() ? string : defaultBundle.getString(key);
    }

    /* This loader attempts to load bundles from the disk */
    private static class FileBundleClassLoader extends ClassLoader {
        private final File pluginDataFolder;

        FileBundleClassLoader(AdvancedGift plugin, ClassLoader classLoader) {
            super(classLoader);
            this.pluginDataFolder = plugin.getDataFolder();
            new File(pluginDataFolder, TRANSLATIONS_DIRECTORY_NAME).mkdirs();
        }

        @Override
        public URL getResource(final String string) {
            File file = new File(pluginDataFolder, string);
            if (file.exists()) {
                try {
                    return file.toURI().toURL();
                } catch (MalformedURLException ignored) {
                }
            }
            return null;
        }

        @Override
        public InputStream getResourceAsStream(final String string) {
            File file = new File(pluginDataFolder, string);
            if (file.exists()) {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException ignored) {
                }
            }
            return null;
        }
    }

    /* In Java 8, PropertiesResourceBundle doesn't read the file as UTF-8 by default, so this forces it to. */
    private static class UTF8ResourceBundleControl extends ResourceBundle.Control {
        public List<String> getFormats(String baseName) {
            if (baseName == null) {
                throw new NullPointerException();
            }
            return FORMAT_PROPERTIES;
        }

        public ResourceBundle newBundle(final String baseName, final Locale locale, final String format, final ClassLoader loader, final boolean reload) throws IOException {
            final String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    //This is when the language file gets loaded. Must use UTF-8 here.
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }

}
