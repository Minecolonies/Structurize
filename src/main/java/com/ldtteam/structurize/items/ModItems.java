package com.ldtteam.structurize.items;

import com.ldtteam.structurize.api.util.constant.Constants;
import com.ldtteam.structurize.creativetab.ModCreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.TallBlockItem;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

/**
 * Class handling the registering of the mod items.
 * <p>
 * We disabled the following finals since we are neither able to mark the items as final, nor do we want to provide public accessors.
 */
@SuppressWarnings({"squid:ClassVariableVisibilityCheck", "squid:S2444", "squid:S1444"})
@ObjectHolder(Constants.MOD_ID)
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);

    public static ItemBuildTool buildTool;
    public static ItemShapeTool shapeTool;
    public static ItemScanTool scanTool;
    public static ItemTagTool tagTool;
    public static ItemCaliper caliper;
    public static TallBlockItem cactusDoor;

    /**
     * Private constructor to hide the implicit public one.
     */
    private ModItems()
    {
        /*
         * Intentionally left empty.
         */
    }

    /**
     * Initates all the items. At the correct time.
     * @param event the registry event object.
     */
    @SubscribeEvent
    public static void registerItems(final RegistryEvent.Register<Item> event)
    {
        final Item.Properties properties = new Item.Properties().group(ModCreativeTabs.STRUCTURIZE);

        final IForgeRegistry<Item> registry = event.getRegistry();

        buildTool = new ItemBuildTool(properties);
        shapeTool = new ItemShapeTool(properties);
        scanTool = new ItemScanTool(ModCreativeTabs.STRUCTURIZE);
        tagTool = new ItemTagTool(ModCreativeTabs.STRUCTURIZE);
        caliper = new ItemCaliper(properties);

        registry.register(buildTool);
        registry.register(shapeTool);
        registry.register(scanTool);
        registry.register(tagTool);
        registry.register(caliper);
        registry.register(cactusDoor);
    }
}
