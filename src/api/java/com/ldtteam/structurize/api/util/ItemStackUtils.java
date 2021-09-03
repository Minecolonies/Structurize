package com.ldtteam.structurize.api.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;

/**
 * Utility methods for the inventories.
 */
public final class ItemStackUtils
{
    /**
     * Private constructor to hide the implicit one.
     */
    private ItemStackUtils()
    {
        /*
         * Intentionally left empty.
         */
    }

    /**
     * Get itemStack of tileEntityData. Retrieve the data from the tileEntity.
     *
     * @param compound the tileEntity stored in a compound.
     * @param state the block.
     * @return the list of itemstacks.
     */
    public static List<ItemStack> getItemStacksOfTileEntity(final CompoundTag compound, final BlockState state)
    {
        if (state.getBlock() instanceof BaseEntityBlock && compound.contains("Items"))
        {
            final NonNullList<ItemStack> items = NonNullList.create();
            ContainerHelper.loadAllItems(compound, items);
            return items;
        }
        BlockPos blockpos = new BlockPos(compound.getInt("x"), compound.getInt("y"), compound.getInt("z"));
        final BlockEntity tileEntity = BlockEntity.loadStatic(blockpos, state, compound);
        if (tileEntity == null)
        {
            return Collections.emptyList();
        }

        final List<ItemStack> items = new ArrayList<>();
        for (final IItemHandler handler : getItemHandlersFromProvider(tileEntity))
        {
            for (int slot = 0; slot < handler.getSlots(); slot++)
            {
                final ItemStack stack = handler.getStackInSlot(slot);
                if (!ItemStackUtils.isEmpty(stack))
                {
                    items.add(stack);
                }
            }
        }

        return items;
    }

    /**
     * Method to get all the IItemHandlers from a given Provider.
     *
     * @param provider The provider to get the IItemHandlers from.
     * @return A list with all the unique IItemHandlers a provider has.
     */
        public static Set<IItemHandler> getItemHandlersFromProvider(final ICapabilityProvider provider)
    {
        final Set<IItemHandler> handlerSet = new HashSet<>();
        for (final Direction side : Direction.values())
        {
           provider.getCapability(ITEM_HANDLER_CAPABILITY, side).ifPresent(handlerSet::add);
        }
        provider.getCapability(ITEM_HANDLER_CAPABILITY, null).ifPresent(handlerSet::add);
        return handlerSet;
    }

    /**
     * Wrapper method to check if a stack is empty.
     * Used for easy updating to 1.11.
     *
     * @param stack The stack to check.
     * @return True when the stack is empty, false when not.
     */
        public static Boolean isEmpty(@Nullable final ItemStack stack)
    {
        return stack == null || stack == ItemStack.EMPTY || stack.getCount() <= 0;
    }

    /**
     * get the size of the stack.
     * This is for compatibility between 1.10 and 1.11
     *
     * @param stack to get the size from
     * @return the size of the stack
     */
    public static int getSize(final ItemStack stack)
    {
        if (ItemStackUtils.isEmpty(stack))
        {
            return 0;
        }

        return stack.getCount();
    }

    /**
     * Get the list of required resources for entities.
     *
     * @param entity the entity object.
     * @param pos the placer pos..
     * @return a list of stacks.
     */
    public static List<ItemStack> getListOfStackForEntity(final Entity entity, final BlockPos pos)
    {
        if (entity != null)
        {
            final List<ItemStack> request = new ArrayList<>();
            if (entity instanceof ItemFrame)
            {
                final ItemStack stack = ((ItemFrame) entity).getItem();
                if (!ItemStackUtils.isEmpty(stack))
                {
                    stack.setCount(1);
                    request.add(stack);
                }
                request.add(new ItemStack(Items.ITEM_FRAME, 1));
            }
            else if (entity instanceof ArmorStand)
            {
                request.add(entity.getPickedResult(new HitResult(Vec3.atLowerCornerOf(pos)) {
                                        @Override
                    public Type getType()
                    {
                        return Type.ENTITY;
                    }
                }));
                entity.getArmorSlots().forEach(request::add);
                entity.getHandSlots().forEach(request::add);
            }

            return request.stream().filter(stack -> !stack.isEmpty()).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
