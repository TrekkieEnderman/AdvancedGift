/*
 * Copyright (c) 2026 TrekkieEnderman
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

import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class ComponentUtils {
    private final static PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();
    private final static LegacyComponentSerializer LEGACY_TEXT_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Converts a component to a plain text, stripping it of all chat formatting as the result.
     * @param component component to convert.
     * @return plain text.
     */
    public static @NotNull String toPlainText(@Nullable Component component) {
        return PLAIN_TEXT_SERIALIZER.serializeOr(component, "");
    }

    /**
     * Does the same thing as {@link #toPlainText(Component)}, but returns it as a new component instead for convenience.
     * @param fancy component to strip.
     * @return new component without chat formatting.
     */
    public static @Nullable Component stripFormatting(@Nullable Component fancy) {
        if (fancy == null) return null;
        return Component.text(PLAIN_TEXT_SERIALIZER.serialize(fancy));
    }

    /**
     * Converts text formatted with ampersands {@code &} to a component with the correct formatting applied.
     * @param legacy text to convert.
     * @return component.
     */
    public static @Nullable Component fromLegacyText(@Nullable String legacy) {
        if (legacy == null) return null;
        //legacy = legacy.replaceAll("§", "&");
        return LEGACY_TEXT_SERIALIZER.deserialize(legacy);
    }
}
