package io.github.TrekkieEnderman.advancedgift.nms;

import net.minecraft.server.v1_14_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class V1_14_R1 implements NMSInterface {
    public String convertItemToJson(ItemStack item) {
        net.minecraft.server.v1_14_R1.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound compound = new NBTTagCompound();
        return nmsItemStack.save(compound).toString();
    }
}