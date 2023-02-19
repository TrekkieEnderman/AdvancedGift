package io.github.TrekkieEnderman.advancedgift.nms;

import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.logging.Level;

public class Reflect implements NMSInterface {
    MethodHandle asNMSCopyMH;
    MethodHandle saveTagMH;
    public Reflect(final String nmsVersion) throws Throwable {
        //runs a test on initialization to ensure nothing will break while using reflection. If anything breaks, throws an exception which the main class will catch.
        final Class<?> classy = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".inventory.CraftItemStack");
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodType CraftMethodType = MethodType.methodType(net.minecraft.world.item.ItemStack.class, ItemStack.class);
        asNMSCopyMH = publicLookup.findStatic(classy, "asNMSCopy", CraftMethodType);
        for (Method method : net.minecraft.world.item.ItemStack.class.getMethods()) {
            if (method.getReturnType().equals(NBTTagCompound.class) && method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(NBTTagCompound.class)) {
                saveTagMH = publicLookup.unreflect(method);
                break;
            }
        }

        //Construct a simple ItemStack with ItemMeta to test on
        ItemStack item = new ItemStack(Material.DIRT);
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Material.DIRT);
        meta.setDisplayName("Test Name");
        item.setItemMeta(meta);
        test(item);
    }

    @Override
    public String convertItemToJson(ItemStack item) {
        try {
            net.minecraft.world.item.ItemStack nmsItemStack = (net.minecraft.world.item.ItemStack) asNMSCopyMH.invoke(item);
            NBTTagCompound compound = new NBTTagCompound();
            return saveTagMH.invoke(nmsItemStack, compound).toString();
        } catch (Throwable e) {
            Bukkit.getPluginManager().getPlugin("AdvancedGift").getLogger().log(Level.SEVERE, "Something went wrong with getting an item tooltip", e);
            return "";
        }
    }

    public void test(ItemStack item) throws Throwable {
        if (asNMSCopyMH == null || saveTagMH == null) return;
        net.minecraft.world.item.ItemStack nmsItemStack = (net.minecraft.world.item.ItemStack) asNMSCopyMH.invoke(item);
        NBTTagCompound compound = new NBTTagCompound();
        saveTagMH.invoke(nmsItemStack, compound);
    }
}
