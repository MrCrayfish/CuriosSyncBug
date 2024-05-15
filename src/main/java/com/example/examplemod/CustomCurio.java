package com.example.examplemod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class CustomCurio implements ICurioItem
{
    @Override
    public boolean canSync(SlotContext context, ItemStack stack)
    {
        return true;
    }

    @NotNull
    @Override
    public CompoundTag writeSyncData(SlotContext context, ItemStack stack)
    {
        // Quick test sending a string that will update the client's item name
        CompoundTag tag = new CompoundTag();
        tag.putString("Message", "Hello, TheIllusiveC4! " + context.entity().tickCount); // Append entity tick count on every sync
        return tag;
    }

    @Override
    public void readSyncData(SlotContext context, CompoundTag compound, ItemStack stack)
    {
        // Try to set the custom name of the item, this won't work
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(compound.getString("Message")));

        // Try to break the counter custom data, this will not work either
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(compound));
    }
}