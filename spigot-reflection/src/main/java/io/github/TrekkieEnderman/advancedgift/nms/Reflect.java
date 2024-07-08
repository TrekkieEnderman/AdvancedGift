package io.github.TrekkieEnderman.advancedgift.nms;

import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.NBTBase;
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
    final MethodHandle asNMSCopyMH;
    final MethodHandle saveTagMH;
    final MethodHandle getRegistryMH;

    public Reflect(final String nmsVersion) throws Throwable {
        // Run a test on initialization to ensure nothing will break while using reflection. If anything breaks, throws an exception which the main class will catch.
        // Get the method that converts a Bukkit ItemStack to a Minecraft ItemStack
        final Class<?> classCraftItemStack = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".inventory.CraftItemStack");
        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodType CraftNMSMethodType = MethodType.methodType(net.minecraft.world.item.ItemStack.class, ItemStack.class);
        asNMSCopyMH = publicLookup.findStatic(classCraftItemStack, "asNMSCopy", CraftNMSMethodType);

        // Get the method that returns the Minecraft registry I need
        final Class<?> classCraftRegistry = Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".CraftRegistry");
        MethodType RegistryMethodType = MethodType.methodType(IRegistryCustom.class);
        getRegistryMH = publicLookup.findStatic(classCraftRegistry, "getMinecraftRegistry", RegistryMethodType);

        // Search for the method that will return the NBT component
        MethodHandle temp = null;
        for (Method method : net.minecraft.world.item.ItemStack.class.getMethods()) {
            if (method.getReturnType().equals(NBTBase.class) && method.getParameterCount() == 1) {
                temp = publicLookup.unreflect(method);
                break;
            }
        }
        saveTagMH = temp;

        test();

    }

    @Override
    public String convertItemToJson(ItemStack item) {
        try {
            return getNBTString(item);
        } catch (Throwable e) {
            Bukkit.getPluginManager().getPlugin("AdvancedGift").getLogger().log(Level.SEVERE, "Something went wrong with getting an item tooltip", e);
            return "{}";
        }
    }

    private void test() throws Throwable {
        //Construct a simple ItemStack with ItemMeta to test on
        ItemStack item = new ItemStack(Material.DIRT);
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Material.DIRT);
        meta.setDisplayName("Test Name");
        item.setItemMeta(meta);

        getNBTString(item);
    }

    private String getNBTString(ItemStack craftItemStack) throws Throwable {
        net.minecraft.world.item.ItemStack nmsItemStack = (net.minecraft.world.item.ItemStack) asNMSCopyMH.invoke(craftItemStack);
        IRegistryCustom registryCustom = (IRegistryCustom) getRegistryMH.invoke();
        return saveTagMH.invoke(nmsItemStack, registryCustom).toString();
    }
}
