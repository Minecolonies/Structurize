package com.ldtteam.structurize.event;

import com.ldtteam.structurize.blockentities.ModBlockEntities;
import com.ldtteam.structurize.blocks.ModBlocks;
import com.ldtteam.structurize.client.*;
import com.ldtteam.structurize.api.Log;
import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.client.model.OverlaidModelLoader;
import com.ldtteam.structurize.items.ItemStackTooltip;
import com.ldtteam.structurize.items.ModItems;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers.ContainerPlacementHandler;
import com.ldtteam.structurize.storage.ClientStructurePackLoader;
import com.ldtteam.structurize.util.WorldRenderMacros;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class ClientLifecycleSubscriber
{
    @SubscribeEvent
    public static void onClientInit(final FMLClientSetupEvent event)
    {
        ClientStructurePackLoader.onClientLoading();
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener(new SimplePreparableReloadListener<>()
        {
            @Override
            protected Object prepare(final ResourceManager manager, final ProfilerFiller profiler)
            {
                return new Object();
            }

            @Override
            protected void apply(final Object source, final ResourceManager manager, final ProfilerFiller profiler)
            {
                Log.getLogger().debug("Clearing blueprint renderer cache.");
                BlueprintHandler.getInstance().clearCache();
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void doClientStuff(final EntityRenderersEvent.RegisterRenderers event)
    {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.blockSubstitution.get(), RenderType.translucent());
    }

    @SubscribeEvent
    public static void registerGeometry(final ModelEvent.RegisterGeometryLoaders event)
    {
        event.register(Constants.resLocStruct("overlaid"), new OverlaidModelLoader());
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(ModBlockEntities.TAG_SUBSTITUTION.get(), TagSubstitutionRenderer::new);
    }

    @SubscribeEvent
    public static void registerTooltips(final RegisterClientTooltipComponentFactoriesEvent event)
    {
        event.register(ItemStackTooltip.class, ClientItemStackTooltip::new);
    }

    @SubscribeEvent
    public static void registerKeys(final RegisterKeyMappingsEvent event)
    {
        ModKeyMappings.register(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerCaps(final RegisterCapabilitiesEvent event)
    {
        final Set<Block> containerBlocks = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final Block block : BuiltInRegistries.BLOCK)
        {
            if (event.isBlockRegistered(ItemHandler.BLOCK, block))
            {
                containerBlocks.add(block);
            }
        }
        ContainerPlacementHandler.CONTAINERS = containerBlocks;
    }

    @SubscribeEvent
    public static void registerGlobablRenderBuffers(final RegisterRenderBuffersEvent event)
    {
        WorldRenderMacros.RenderTypes.registerBuffer(event);
    }

    @SubscribeEvent
    public static void registerClientExtensions(final RegisterClientExtensionsEvent event)
    {
        event.registerItem(new IClientItemExtensions()
        {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer()
            {
                return TagSubstitutionRenderer.getInstance();
            }
        }, ModItems.blockTagSubstitution.get());
    }
}
