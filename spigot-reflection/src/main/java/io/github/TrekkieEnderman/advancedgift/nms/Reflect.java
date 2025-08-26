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
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public class Reflect implements NMSInterface {
    final MethodHandle asNMSCopyMH;
    final MethodHandle getCompPatchMH;
    final DynamicOps<JsonElement> dynamicOps;
    final Codec<DataComponentPatch> patchCodec;
    final Codec<ItemStack> stackCodec;

    @SuppressWarnings({"unchecked"})
    public Reflect() throws Throwable {
        // Run a test on initialization to ensure nothing will break while using reflection. If anything breaks, throws an exception which the main class will catch.
        // Get the method that converts a Bukkit ItemStack to a Minecraft ItemStack
        final Class<?> classCraftItemStack = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".inventory.CraftItemStack");
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodType CraftNMSMethodType = MethodType.methodType(ItemStack.class, org.bukkit.inventory.ItemStack.class);
        asNMSCopyMH = publicLookup.findStatic(classCraftItemStack, "asNMSCopy", CraftNMSMethodType);
        if (asNMSCopyMH == null) {
            throw new NoSuchMethodException("Couldn't find the method for converting Bukkit ItemStack to Minecraft ItemStack.");
        }

        // Get the method that returns the Minecraft registry I need
        final Class<?> classCraftRegistry = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".CraftRegistry");
        MethodType RegistryMethodType = MethodType.methodType(IRegistryCustom.class);
        MethodHandle  getRegistryMH = publicLookup.findStatic(classCraftRegistry, "getMinecraftRegistry", RegistryMethodType);
        if (getRegistryMH == null) {
            throw new NoSuchMethodException("Couldn't obtain the Minecraft registry.");
        }
        IRegistryCustom registry = (IRegistryCustom) getRegistryMH.invoke();

        // Set up the dynamic ops I need from the Minecraft registry.
        MethodHandle serializationContextMH = null;
        for (Method method : registry.getClass().getMethods()) {
            if (method.getParameterCount() != 1) {
                continue;
            }
            if (!(method.getGenericReturnType() instanceof ParameterizedType returnType) || // Not a parameterized type
                    !returnType.getRawType().equals(RegistryOps.class)) { // Not the right return type
                continue;
            }
            if (!(method.getGenericParameterTypes()[0] instanceof ParameterizedType parameterType) || // Not a parameterized type
                    !parameterType.getRawType().equals(DynamicOps.class)) {// Not the right parameter type
                continue;
            }
            serializationContextMH = publicLookup.unreflect(method);
            break;
        }
        if (serializationContextMH == null) {
            throw new NoSuchMethodException("Couldn't find the serialization method in the Minecraft registry.");
        }
        dynamicOps = (DynamicOps<JsonElement>) serializationContextMH.invoke(registry, JsonOps.INSTANCE);

        // Find the DataComponentPatch codec I need for hover event
        VarHandle patchCodecVH = null;
        for (Field field : DataComponentPatch.class.getFields()) {
            if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
                if (!parameterizedType.getRawType().equals(Codec.class)) {
                    continue;
                }
                for (Type typeArg : parameterizedType.getActualTypeArguments()) {
                    if (typeArg.equals(DataComponentPatch.class)) {
                        patchCodecVH = publicLookup.unreflectVarHandle(field);
                        break;
                    }
                }
            }
        }
        if (patchCodecVH == null) {
            throw new NoSuchFieldException("Couldn't find the DataComponentPatch codec I'm looking for.");
        }
        patchCodec = (Codec<DataComponentPatch>) patchCodecVH.get();

        // Find the ItemStack codec I need for JSON string
        VarHandle stackCodecVH = null;
        for (Field field : ItemStack.class.getFields()) {
            if (field.getGenericType() instanceof ParameterizedType parameterizedType) {
                if (!parameterizedType.getRawType().equals(Codec.class)) {
                    continue;
                }
                for (Type typeArg : parameterizedType.getActualTypeArguments()) {
                    if (typeArg.equals(ItemStack.class)) {
                        stackCodecVH = publicLookup.unreflectVarHandle(field);
                        break;
                    }
                }
            }
        }
        if (stackCodecVH == null) {
            throw new NoSuchFieldException("Couldn't find the ItemStack codec I'm looking for.");
        }
        stackCodec = (Codec<ItemStack>) stackCodecVH.get();

        // Find the method for getting a component patch from an item stack
        MethodHandle temp = null;
        for (Method method : ItemStack.class.getMethods()) {
            if (method.getReturnType().equals(DataComponentPatch.class) && method.getParameterCount() == 0) {
                temp = publicLookup.unreflect(method);
                break;
            }
        }
        if (temp == null) {
            throw new NoSuchMethodException("Couldn't find the method for getting an ItemStack's component patch.");
        }
        getCompPatchMH = temp;

        test();

    }

    @NotNull
    @Override
    public String getAsJsonString(org.bukkit.inventory.ItemStack item) {
        try {
            return stackCodec.encodeStart(dynamicOps, toNMS(item))
                    .result().orElseGet(JsonObject::new).toString();
        } catch (Throwable e) {
            return "{}";
        }
    }

    @Override
    @NotNull public Optional<HoverEvent> getAsHoverEvent(org.bukkit.inventory.ItemStack item) {
        try {
            return Optional.of(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                    new Item(getKey(item), item.getAmount(), getComponents(item))));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    private ItemStack toNMS(org.bukkit.inventory.ItemStack craftItemStack) throws Throwable {
        return (ItemStack) asNMSCopyMH.invoke(craftItemStack);
    }

    private void test() throws Throwable {
        //Construct a simple ItemStack with ItemMeta to test on
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(Material.DIRT);
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Material.DIRT);
        meta.setDisplayName("Test Name");
        item.setItemMeta(meta);

        getComponents(item);
    }

    private JsonObject getComponents(org.bukkit.inventory.ItemStack craftItemStack) throws Throwable {
        ItemStack nmsItemStack = toNMS(craftItemStack);
        return (JsonObject) patchCodec.encodeStart(dynamicOps, (DataComponentPatch) getCompPatchMH.invoke(nmsItemStack))
                .result().orElseGet(JsonObject::new);
    }

    private @Nullable String getKey(org.bukkit.inventory.ItemStack item) {
        final Material material = item.getType();
        try {
            return material.getKey().toString();
        } catch (IllegalStateException ignored) {
            return null;
        }
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
