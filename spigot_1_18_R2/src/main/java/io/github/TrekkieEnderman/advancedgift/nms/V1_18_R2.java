package io.github.TrekkieEnderman.advancedgift.nms;

import net.minecraft.nbt.CompoundTag;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class V1_18_R2 implements NMSInterface {
    @Override
    public String convertItemToJson(ItemStack item) {
        net.minecraft.world.item.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);
        CompoundTag compound = new CompoundTag();
        return nmsItemStack.save(compound).toString();
    }
}
