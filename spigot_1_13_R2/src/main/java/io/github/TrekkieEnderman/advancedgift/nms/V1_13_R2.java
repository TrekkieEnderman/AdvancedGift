package io.github.TrekkieEnderman.advancedgift.nms;

import net.minecraft.server.v1_13_R2.NBTTagCompound;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class V1_13_R2 implements NMSInterface {
    public String convertItemToJson(ItemStack item) {
        net.minecraft.server.v1_13_R2.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound compound = new NBTTagCompound();
        return nmsItemStack.save(compound).toString();
    }
}