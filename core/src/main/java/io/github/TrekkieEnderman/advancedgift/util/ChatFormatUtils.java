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

package io.github.TrekkieEnderman.advancedgift.util;

import io.github.TrekkieEnderman.advancedgift.ServerVersion;
import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class ChatFormatUtils {

    public static final char COLOR_CHAR = '\u00A7';
    private static final Pattern RGB_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    public static String format(final String input) {
        if (input == null) {
            return null;
        }

        // Format legacy colors first
        StringBuffer stringBuilder = new StringBuffer();
        final Matcher legacyMatcher = LEGACY_COLOR_PATTERN.matcher(input);
        while (legacyMatcher.find()) {
            legacyMatcher.appendReplacement(stringBuilder, COLOR_CHAR + "$1");
        }
        legacyMatcher.appendTail(stringBuilder);

        // If the server version is below 1.16, do not proceed further
        if (ServerVersion.getMinorVersion() < 16) return stringBuilder.toString();

        // Format hex colors next
        final Matcher rgbMatcher = RGB_PATTERN.matcher(stringBuilder.toString());
        stringBuilder = new StringBuffer();
        while (rgbMatcher.find()) {
            String hex = rgbMatcher.group(1);
            rgbMatcher.appendReplacement(stringBuilder, parseHexCode(hex));
        }
        rgbMatcher.appendTail(stringBuilder);
        return stringBuilder.toString();
    }

    public static String parseHexCode(String hex) {
        if (ServerVersion.getMinorVersion() < 16) return hex;
        if (hex == null || hex.isEmpty()) return hex;

        StringBuilder hexBuilder = new StringBuilder(COLOR_CHAR + "x");
        for (final char c : hex.toCharArray()) {
            hexBuilder.append(COLOR_CHAR).append(c);
        }
        return hexBuilder.toString();
    }
}
