package com.example.examplemod;

import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Objects;
import java.util.Optional;

@Mod(ExampleMod.MODID)
public class ExampleMod
{
    /*
     * To test the bug, place the item "examplemod:curios_item" into the "back" curio slot.
     * Everytime you left click a block, a counter on the itemstack in the curio slot on the server
     * side will be incremented. Curios API should detect the change in the itemstack and resync the
     * changes the client. In the console, I am printing the counter of the client and server version
     * of the itemstack.
     *
     * On initial load of a world, the data will be synced correctly but not because of writeSyncData/readSyncData.
     * Those methods just seem to be broken completely. I recommend incrementing the counter, exit the world, and
     * join back. The console will then print the counter as being exactly the same but after you start
     * left click more blocks, you will see the client itemstack become out of sync.
     */
    public static final String MODID = "examplemod";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("curios_item", new Item.Properties());

    public ExampleMod(IEventBus bus)
    {
        ITEMS.register(bus);
        bus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(this::onTouchBlock);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> CuriosApi.registerCurio(EXAMPLE_ITEM.get(), new CustomCurio()));
    }

    private ItemStack getCurioStack(Player player)
    {
        Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player);
        if(optional.isPresent())
        {
            Optional<ICurioStacksHandler> stacksOptional = optional.get().getStacksHandler("back");
            if(stacksOptional.isPresent())
            {
                return stacksOptional.get().getStacks().getStackInSlot(0);
            }
        }
        return ItemStack.EMPTY;
    }

    private void onTouchBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        if(!event.getLevel().isClientSide())
        {
            Player player = event.getEntity();
            ItemStack stack = this.getCurioStack(player);
            if(stack.is(EXAMPLE_ITEM.get()))
            {
                // Setup some custom data on the itemstack on the server side
                if(!stack.has(DataComponents.CUSTOM_DATA)) {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
                }

                // Simple counter. Get the current count on the item, then increment, and set
                CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                CompoundTag tag = Objects.requireNonNull(data).copyTag();
                tag.putInt("Count", tag.getInt("Count") + 1);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

                // Curios api should then detect the change and update the item to the client
            }
        }
    }

    private void onPlayerTick(PlayerTickEvent.Post event)
    {
        Player player = event.getEntity();

        // Only print one a second instead of spamming console
        if(player.tickCount % 20 != 0)
            return;

        ItemStack stack = this.getCurioStack(player);
        if(stack.is(EXAMPLE_ITEM.get()))
        {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if(data != null)
            {
                CompoundTag tag = data.copyTag();
                int count = tag.getInt("Count");
                if(player.level().isClientSide())
                {
                    LOGGER.info("# CLIENT: " + count);
                }
                else
                {
                    LOGGER.info("# SERVER: " + count);
                    LOGGER.info("#############");
                }
            }
        }
    }
}
