package io.github.TrekkieEnderman.advancedgift.nms;

import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class V1_17_R1 implements NMSInterface {
    public String convertItemToJson(ItemStack item) {
        net.minecraft.world.item.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound compound = new NBTTagCompound();
        return nmsItemStack.save(compound).toString();
    }
}
