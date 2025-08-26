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

package io.github.TrekkieEnderman.advancedgift.nms;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.minecraft.core.component.DataComponentPatch;
import org.bukkit.craftbukkit.v1_20_R4.CraftRegistry;
import org.bukkit.craftbukkit.v1_20_R4.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class V1_20_R4 implements NMSInterface {
    private final DynamicOps<JsonElement> dynamicOps = CraftRegistry.getMinecraftRegistry()
            .createSerializationContext(JsonOps.INSTANCE);

    @Override
    @NotNull public String getAsJsonString(ItemStack item) {
        net.minecraft.world.item.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);
        return net.minecraft.world.item.ItemStack.CODEC.encodeStart(dynamicOps, nmsItemStack)
                .result().orElseGet(JsonObject::new).toString();
    }

    @Override
    @NotNull public Optional<HoverEvent> getAsHoverEvent(ItemStack item) {
        return Optional.of(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                new Item(item.getType().getKey().toString(), item.getAmount(), getComponents(item))));
    }

    private JsonObject getComponents(ItemStack itemStack) {
        net.minecraft.world.item.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
        return (JsonObject) DataComponentPatch.CODEC.encodeStart(dynamicOps, nmsItemStack.getComponentsPatch())
                .result().orElseGet(JsonObject::new);
    }

    /**
     * Mimics {@link net.md_5.bungee.api.chat.hover.content.Item} but with data components instead of item tags.
     */
    @ToString
    @Getter
    @AllArgsConstructor
    static class Item extends Content {
        private final String id;
        private final int count;
        private final Object components;

        @Override
        public HoverEvent.Action requiredAction() {
            return HoverEvent.Action.SHOW_ITEM;
        }
    }
}
