package com.ldtteam.structurize.client.gui;

import com.ldtteam.blockui.Pane;
import com.ldtteam.blockui.controls.ItemIcon;
import com.ldtteam.blockui.controls.Text;
import com.ldtteam.blockui.views.BOWindow;
import com.ldtteam.blockui.views.ScrollingList;
import com.ldtteam.common.util.BlockToItemHelper;
import com.ldtteam.structurize.api.ItemStackUtils;
import com.ldtteam.structurize.api.ItemStorage;
import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes as exact as possible contents of given AABB. This should be used as base for item related analysis
 */
public class WindowBlockGetterContents extends BOWindow
{
    public WindowBlockGetterContents(final Blueprint blueprint, final Level realLevel, final Collection<Entity> boundedEntities)
    {
        this(blueprint,
            realLevel,
            new BlockPos(blueprint.getMinX(), blueprint.getMinBuildHeight(), blueprint.getMinZ()),
            new BlockPos(blueprint.getMaxX() - 1, blueprint.getMaxBuildHeight() - 1, blueprint.getMaxZ() - 1),
            boundedEntities);
    }

    /**
     * @param blockGetter     level
     * @param realLevel       vanilla level instance in case of blockGetter not being one
     * @param start           inclusive from
     * @param end             inclusive to
     * @param boundedEntities entities bounded by [start, end] parameters
     */
    public WindowBlockGetterContents(final BlockGetter blockGetter,
        @Nullable final Level realLevel,
        final BlockPos start,
        final BlockPos end,
        final Collection<Entity> boundedEntities)
    {
        super(Constants.resLocStruct("gui/windowcontents.xml"));

        final Map<Item, ItemStorage> blocks = new HashMap<>();
        final Map<Item, ItemStorage> blockItemHandlers = new HashMap<>();
        final Map<Item, ItemStorage> entities = new HashMap<>();
        final Map<Item, ItemStorage> entityItemHandlers = new HashMap<>();

        for (final BlockPos pos : BlockPos.betweenClosed(start, end))
        {
            final BlockState blockState = blockGetter.getBlockState(pos);
            final BlockEntity blockEntity = blockGetter.getBlockEntity(pos);

            // blockstate
            addAmountToMap(blocks, BlockToItemHelper.getItemStack(blockState, blockEntity, Minecraft.getInstance().player));

            // blockentity content
            if (blockEntity != null && blockEntity.getLevel() == null)
            {
                ItemStackUtils.ITEM_HANDLER_FAKE_LEVEL.get(realLevel).withFakeLevelContext(blockState, blockEntity, realLevel, level -> {
                    ItemStackUtils.getItemHandlersFromProvider(blockEntity, pos, blockState)
                        .forEach(i -> ItemStackUtils.deepExtractItemHandler(i, stack -> addAmountToMap(blockItemHandlers, stack)));
                });
            }
            else
            {
                ItemStackUtils.getItemHandlersFromProvider(blockEntity, pos, blockState)
                    .forEach(i -> ItemStackUtils.deepExtractItemHandler(i, stack -> addAmountToMap(blockItemHandlers, stack)));
            }
        }

        for (final Entity entity : boundedEntities)
        {
            addAmountToMap(entities, ItemStackUtils.getEntitySpawningItem(entity));
            ItemStackUtils.getItemStacksOfEntity(entity).forEach(stack -> addAmountToMap(entityItemHandlers, stack));
        }

        final List<ItemStorage> blockList = new ArrayList<>(blocks.values());
        final List<ItemStorage> blockItemHandlerList = new ArrayList<>(blockItemHandlers.values());
        final List<ItemStorage> entityList = new ArrayList<>(entities.values());
        final List<ItemStorage> entityItemHandlerList = new ArrayList<>(entityItemHandlers.values());

        final Comparator<ItemStorage> alphabeticalOrder =
            (i1, i2) -> i1.getItemStack().getHoverName().getString().compareTo(i2.getItemStack().getHoverName().getString());
        blockList.sort(alphabeticalOrder);
        blockItemHandlerList.sort(alphabeticalOrder);
        entityList.sort(alphabeticalOrder);
        entityItemHandlerList.sort(alphabeticalOrder);

        findPaneOfTypeByID("blocks", ScrollingList.class).setDataProvider(blockList::size,
            (idx, pane) -> updateItem(blockList.get(idx), pane));
        findPaneOfTypeByID("block_item_handlers", ScrollingList.class).setDataProvider(blockItemHandlerList::size,
            (idx, pane) -> updateItem(blockItemHandlerList.get(idx), pane));
        findPaneOfTypeByID("entities", ScrollingList.class).setDataProvider(entityList::size,
            (idx, pane) -> updateItem(entityList.get(idx), pane));
        findPaneOfTypeByID("entity_item_handlers", ScrollingList.class).setDataProvider(entityItemHandlerList::size,
            (idx, pane) -> updateItem(entityItemHandlerList.get(idx), pane));
    }

    private void addAmountToMap(final Map<Item, ItemStorage> itemSet, @Nullable final ItemStack blockAsItem)
    {
        if (blockAsItem != null)
        {
            itemSet.computeIfAbsent(blockAsItem.getItem(), i -> new ItemStorage(blockAsItem.copyWithCount(1), 0, true))
                .addAmount(Math.max(1, blockAsItem.getCount()));
        }
    }

    private void updateItem(final ItemStorage itemStorage, final Pane pane)
    {
        pane.findPaneOfTypeByID("icon", ItemIcon.class).setItem(itemStorage.getItemStack());
        pane.findPaneOfTypeByID("registry_key", Text.class).setText(itemStorage.getItemStack().getHoverName());
        pane.findPaneOfTypeByID("amount", Text.class).setText(Component.literal(Integer.toString(itemStorage.getAmount())));
    }
}
