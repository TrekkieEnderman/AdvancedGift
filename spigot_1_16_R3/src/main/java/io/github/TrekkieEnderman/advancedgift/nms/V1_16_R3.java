package io.github.TrekkieEnderman.advancedgift.nms;

import net.minecraft.server.v1_16_R3.NBTTagCompound;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class V1_16_R3 implements NMSInterface {
    public String convertItemToJson(ItemStack item) {
        net.minecraft.server.v1_16_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound compound = new NBTTagCompound();
        return nmsItemStack.save(compound).toString();
    }
}
