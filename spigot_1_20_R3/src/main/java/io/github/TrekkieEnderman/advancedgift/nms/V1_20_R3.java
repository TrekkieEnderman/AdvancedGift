package io.github.TrekkieEnderman.advancedgift.nms;

import net.minecraft.nbt.CompoundTag;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class V1_20_R3 implements NMSInterface {
    @Override
    public String convertItemToJson(ItemStack item) {
        net.minecraft.world.item.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(item);
        CompoundTag compound = new CompoundTag();
        return nmsItemStack.save(compound).toString();
    }
}
