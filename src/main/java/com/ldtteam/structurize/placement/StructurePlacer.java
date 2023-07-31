package com.ldtteam.structurize.placement;

import com.ldtteam.structurize.Structurize;
import com.ldtteam.structurize.api.util.ItemStackUtils;
import com.ldtteam.structurize.api.util.Log;
import com.ldtteam.structurize.blockentities.BlockEntityTagSubstitution;
import com.ldtteam.structurize.blocks.ModBlocks;
import com.ldtteam.structurize.placement.handlers.placement.IPlacementHandler;
import com.ldtteam.structurize.placement.handlers.placement.PlacementHandlers;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
import com.ldtteam.structurize.util.BlockUtils;
import com.ldtteam.structurize.util.ChangeStorage;
import com.ldtteam.structurize.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Structure placement class that will actually execute the placement of a structure.
 * It will iterate for different phases over the structure and maintain the iterator.
 */
public class StructurePlacer
{
    /**
     * The structure iterator.
     */
    protected final AbstractBlueprintIterator iterator;

    /**
     * The handler.
     */
    protected final IStructureHandler handler;

    /**
     * Create a new structure placer.
     *
     * @param handler the structure handler.
     */
    public StructurePlacer(final IStructureHandler handler)
    {
        this.iterator = StructureIterators.getIterator(Structurize.getConfig().getServer().iteratorType.get().toString(), handler);
        this.handler = handler;
    }

    /**
     * Create a new structure placer.
     * @param handler the structure handler.
     * @param id the unique id of the handler.
     */
    public StructurePlacer(final IStructureHandler handler, final String id)
    {
        this.iterator = StructureIterators.getIterator(id, handler);
        this.handler = handler;
    }

    /**
     * Execute structure placement.
     * @param world the world.
     * @param storage the change storage.
     * @param inputPos the pos to start from.
     * @param operation the operation to execute.
     * @param iterateFunction the function to iterate.
     * @param includeEntities if entities should be included.
     * @return the result.
     */
    public StructurePhasePlacementResult executeStructureStep(
      final Level world,
      final ChangeStorage storage,
      final BlockPos inputPos,
      final Operation operation,
      final Supplier<AbstractBlueprintIterator.Result> iterateFunction,
      final boolean includeEntities)
    {
        final List<ItemStack> requiredItems = new ArrayList<>();

        if (includeEntities)
        {
            iterator.includeEntities();
        }

        iterator.setProgressPos(new BlockPos(inputPos.getX(), inputPos.getY(), inputPos.getZ()));

        AbstractBlueprintIterator.Result iterationResult = iterateFunction.get();
        BlockPos lastPos = inputPos;
        int count = 0;

        while (iterationResult == AbstractBlueprintIterator.Result.NEW_BLOCK)
        {
            final BlockPos localPos = iterator.getProgressPos();
            final BlockPos worldPos = handler.getProgressPosInWorld(localPos);

            if (count >= handler.getStepsPerCall())
            {
                return new StructurePhasePlacementResult(lastPos, new BlockPlacementResult(worldPos, BlockPlacementResult.Result.LIMIT_REACHED, requiredItems));
            }

            final BlockState localState = handler.getBluePrint().getBlockState(localPos);
            if (localState == null || world.isOutsideBuildHeight(worldPos))
            {
                lastPos = localPos;
                iterationResult = iterateFunction.get();
                continue;
            }

            if (storage != null)
            {
                storage.addPreviousDataFor(worldPos, world);
            }

            final BlockPlacementResult result;
            switch (operation)
            {
                case BLOCK_REMOVAL:
                    if (!handler.isCreative() && !(world.getBlockState(worldPos).getBlock() instanceof AirBlock))
                    {
                        result = new BlockPlacementResult(worldPos, BlockPlacementResult.Result.BREAK_BLOCK);
                    }
                    else
                    {
                        world.removeBlock(worldPos, false);
                        result = new BlockPlacementResult(worldPos, BlockPlacementResult.Result.SUCCESS);
                    }
                    break;
                case WATER_REMOVAL:
                    final BlockState worldState = world.getBlockState(worldPos);
                    if (worldState.getBlock() instanceof BucketPickup || BlockUtils.isLiquidOnlyBlock(worldState.getBlock()) || !worldState.getFluidState().isEmpty())
                    {
                        BlockUtils.removeFluid(world, worldPos);
                    }
                    result = new BlockPlacementResult(worldPos, BlockPlacementResult.Result.SUCCESS);
                    break;
                case GET_RES_REQUIREMENTS:
                    result = getResourceRequirements(world, worldPos, localPos, localState, handler.getBluePrint().getTileEntityData(worldPos, localPos));
                    requiredItems.addAll(result.getRequiredItems());
                    break;
                default:
                    result = handleBlockPlacement(world, worldPos, localPos, storage, localState, handler.getBluePrint().getTileEntityData(worldPos, localPos));
            }
            count++;

            if (storage != null)
            {
                storage.addPostDataFor(worldPos, world);
            }

            if (operation != Operation.GET_RES_REQUIREMENTS && (result.getResult() == BlockPlacementResult.Result.MISSING_ITEMS
                                                                  || result.getResult() == BlockPlacementResult.Result.FAIL
                                                                  || result.getResult() == BlockPlacementResult.Result.BREAK_BLOCK))
            {
                return new StructurePhasePlacementResult(lastPos, result);
            }

            lastPos = localPos;
            iterationResult = iterateFunction.get();

            if (operation != Operation.GET_RES_REQUIREMENTS && count >= handler.getStepsPerCall())
            {
                return new StructurePhasePlacementResult(lastPos, result);
            }
        }

        if (iterationResult == AbstractBlueprintIterator.Result.AT_END)
        {
            iterator.reset();
            return new StructurePhasePlacementResult(iterator.getProgressPos(),
              new BlockPlacementResult(iterator.getProgressPos(), BlockPlacementResult.Result.FINISHED, requiredItems));
        }
        return new StructurePhasePlacementResult(iterator.getProgressPos(), new BlockPlacementResult(this.handler.getProgressPosInWorld(iterator.getProgressPos()), BlockPlacementResult.Result.LIMIT_REACHED, requiredItems));
    }

    /**
     * This method handles the block placement.
     * When we extract this into another mod, we have to override the method.
     * @param world          the world.
     * @param worldPos       the world position.
     * @param localPos       the local pos
     * @param storage        the change storage.
     * @param localState     the local state.
     * @param tileEntityData the tileEntity.
     */
    public BlockPlacementResult handleBlockPlacement(
      final Level world,
      final BlockPos worldPos,
      final BlockPos localPos,
      final ChangeStorage storage,
      BlockState localState,
      CompoundTag tileEntityData)
    {
        final BlockState worldState = world.getBlockState(worldPos);
        boolean sameBlockInWorld = worldState.getBlock() == localState.getBlock();

        if (!(worldState.getBlock() instanceof AirBlock))
        {
            if (!handler.allowReplace())
            {
                return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.BREAK_BLOCK);
            }
        }

        for (final CompoundTag compound : this.iterator.getBluePrintPositionInfo(localPos).getEntities())
        {
            if (compound != null)
            {
                try
                {
                    final BlockPos pos = this.handler.getWorldPos().subtract(handler.getBluePrint().getPrimaryBlockOffset());

                    final Optional<EntityType<?>> type = EntityType.by(compound);
                    if (type.isPresent())
                    {
                        final Entity entity = type.get().create(world);
                        if (entity != null)
                        {
                            entity.deserializeNBT(compound);

                            entity.setUUID(UUID.randomUUID());
                            Vec3 posInWorld = entity.position().add(pos.getX(), pos.getY(), pos.getZ());
                            if (entity instanceof HangingEntity hang)
                            {
                                posInWorld = posInWorld.subtract(Vec3.atLowerCornerOf(hang.blockPosition().subtract(hang.getPos())));
                            }
                            entity.moveTo(posInWorld.x, posInWorld.y, posInWorld.z, entity.getYRot(), entity.getXRot());

                            final List<? extends Entity> list = world.getEntitiesOfClass(entity.getClass(), new AABB(posInWorld.add(1,1,1), posInWorld.add(-1,-1,-1)));
                            boolean foundEntity = false;
                            for (Entity worldEntity: list)
                            {
                                if (worldEntity.position().equals(posInWorld))
                                {
                                    foundEntity = true;
                                    break;
                                }
                            }

                            if (foundEntity || (entity instanceof Mob && !handler.isCreative()))
                            {
                                continue;
                            }

                            final List<ItemStack> requiredItems = new ArrayList<>();
                            if (!handler.isCreative())
                            {
                                requiredItems.addAll(ItemStackUtils.getListOfStackForEntity(entity, pos));
                                if (!InventoryUtils.hasRequiredItems(handler.getInventory(), requiredItems))
                                {
                                    return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.MISSING_ITEMS, requiredItems);
                                }
                            }

                            world.addFreshEntity(entity);
                            if (storage != null)
                            {
                                storage.addToBeKilledEntity(entity);
                            }

                            for (final ItemStack tempStack : requiredItems)
                            {
                                if (!ItemStackUtils.isEmpty(tempStack))
                                {
                                    InventoryUtils.consumeStack(tempStack, handler.getInventory());
                                }
                            }
                            this.handler.triggerEntitySuccess(localPos, requiredItems, true);
                        }
                    }
                }
                catch (final RuntimeException e)
                {
                    Log.getLogger().info("Couldn't restore entity", e);
                }
            }
        }

        BlockEntity worldEntity = null;
        if (tileEntityData != null)
        {
            worldEntity = world.getBlockEntity(worldPos);
        }

        if (localState.getBlock() == ModBlocks.blockSolidSubstitution.get() && handler.fancyPlacement())
        {
            localState = this.handler.getSolidBlockForPos(worldPos, handler.getBluePrint().getRawBlockStateFunction().compose(handler::getStructurePosFromWorld));
        }
        if (localState.getBlock() == ModBlocks.blockTagSubstitution.get() && handler.fancyPlacement())
        {
            if (tileEntityData != null && BlockEntity.loadStatic(localPos, localState, tileEntityData) instanceof BlockEntityTagSubstitution tagEntity)
            {
                localState = tagEntity.getReplacement().getBlockState();
                tileEntityData = tagEntity.getReplacement().getBlockEntityTag();
            }
            else
            {
                localState = Blocks.AIR.defaultBlockState();
            }
        }

        if (BlockUtils.areBlockStatesEqual(localState, worldState, handler::replaceWithSolidBlock, handler.fancyPlacement(), handler::shouldBlocksBeConsideredEqual, tileEntityData, worldEntity))
        {
            return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.SUCCESS);
        }

        for (final IPlacementHandler placementHandler : PlacementHandlers.handlers)
        {
            if (placementHandler.canHandle(world, worldPos, localState))
            {
                final List<ItemStack> requiredItems = new ArrayList<>();

                if (!sameBlockInWorld && !this.handler.isCreative())
                {
                    for (final ItemStack stack : placementHandler.getRequiredItems(world, worldPos, localState, tileEntityData, false))
                    {
                        if (!stack.isEmpty() && !this.handler.isStackFree(stack))
                        {
                            requiredItems.add(stack);
                        }
                    }

                    if (!this.handler.hasRequiredItems(requiredItems))
                    {
                        return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.MISSING_ITEMS, requiredItems);
                    }
                }

                if (!(worldState.getBlock() instanceof AirBlock))
                {
                    if (!sameBlockInWorld
                          && !worldState.isAir()
                          && !(worldState.getBlock() instanceof DoublePlantBlock && worldState.getValue(DoublePlantBlock.HALF).equals(DoubleBlockHalf.UPPER)))
                    {
                        placementHandler.handleRemoval(handler, world, worldPos, tileEntityData);
                    }
                }

                this.handler.prePlacementLogic(worldPos, localState, requiredItems);

                final IPlacementHandler.ActionProcessingResult result = placementHandler.handle(getHandler().getBluePrint(), world, worldPos, localState, tileEntityData, !this.handler.fancyPlacement(), this.handler.getWorldPos(), this.handler.getSettings());
                if (result == IPlacementHandler.ActionProcessingResult.DENY)
                {
                    placementHandler.handleRemoval(handler, world, worldPos, tileEntityData);
                    return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.FAIL);
                }

                this.handler.triggerSuccess(localPos, requiredItems, true);

                if (result == IPlacementHandler.ActionProcessingResult.PASS)
                {
                    return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.SUCCESS);
                }

                if (!this.handler.isCreative() && !sameBlockInWorld)
                {
                    for (final ItemStack tempStack : requiredItems)
                    {
                        if (!ItemStackUtils.isEmpty(tempStack))
                        {
                            InventoryUtils.consumeStack(tempStack, handler.getInventory());
                        }
                    }
                }

                return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.SUCCESS);
            }
        }
        return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.FAIL);
    }

    /**
     * This method handles the block placement.
     * When we extract this into another mod, we have to override the method.
     *  @param world          the world.
     * @param worldPos       the world position.
     * @param localPos       the local pos.
     * @param localState     the local state.
     * @param tileEntityData the tileEntity.
     */
    public BlockPlacementResult getResourceRequirements(
      final Level world,
      final BlockPos worldPos,
      final BlockPos localPos,
      BlockState localState,
      CompoundTag tileEntityData)
    {
        final BlockState worldState = world.getBlockState(worldPos);
        boolean sameBlockInWorld = false;
        if (worldState.getBlock() == localState.getBlock())
        {
            if (tileEntityData == null)
            {
                sameBlockInWorld = true;
            }
            else
            {
                sameBlockInWorld = ItemStackUtils.compareItemStacksIgnoreStackSize(
                    BlockUtils.getItemStackFromBlockState(localState), 
                    BlockUtils.getItemStackFromBlockState(worldState), 
                    false, 
                    true, 
                    false, 
                    false);
            }
        }

        final List<ItemStack> requiredItems = new ArrayList<>();
        for (final CompoundTag compound : iterator.getBluePrintPositionInfo(localPos).getEntities())
        {
            if (compound != null)
            {
                try
                {
                    final BlockPos pos = this.handler.getWorldPos().subtract(handler.getBluePrint().getPrimaryBlockOffset());

                    final Optional<EntityType<?>> type = EntityType.by(compound);
                    if (type.isPresent())
                    {
                        final Entity entity = type.get().create(world);
                        if (entity != null)
                        {
                            entity.deserializeNBT(compound);

                            final Vec3 posInWorld = entity.position().add(pos.getX(), pos.getY(), pos.getZ());
                            final List<? extends Entity> list = world.getEntitiesOfClass(entity.getClass(), new AABB(posInWorld.add(1,1,1), posInWorld.add(-1,-1,-1)));
                            boolean foundEntity = false;
                            for (Entity worldEntity: list)
                            {
                                if (worldEntity.position().equals(posInWorld))
                                {
                                    foundEntity = true;
                                    break;
                                }
                            }

                            if (foundEntity)
                            {
                                continue;
                            }

                            requiredItems.addAll(ItemStackUtils.getListOfStackForEntity(entity, pos));
                        }
                    }
                }
                catch (final RuntimeException e)
                {
                    Log.getLogger().info("Couldn't restore entity", e);
                }
            }
        }

        if (localState.getBlock() == ModBlocks.blockSolidSubstitution.get() && handler.fancyPlacement())
        {
            localState = this.handler.getSolidBlockForPos(worldPos, handler.getBluePrint().getRawBlockStateFunction().compose(handler::getStructurePosFromWorld));
        }
        if (localState.getBlock() == ModBlocks.blockTagSubstitution.get() && handler.fancyPlacement())
        {
            if (tileEntityData != null && BlockEntity.loadStatic(localPos, localState, tileEntityData) instanceof BlockEntityTagSubstitution tagEntity)
            {
                localState = tagEntity.getReplacement().getBlockState();
                tileEntityData = tagEntity.getReplacement().getBlockEntityTag();
            }
            else
            {
                localState = Blocks.AIR.defaultBlockState();
            }
        }

        for (final IPlacementHandler placementHandler : PlacementHandlers.handlers)
        {
            if (placementHandler.canHandle(world, worldPos, localState))
            {
                if (!sameBlockInWorld)
                {
                    for (final ItemStack stack : placementHandler.getRequiredItems(world, worldPos, localState, tileEntityData, false))
                    {
                        if (!stack.isEmpty() && !this.handler.isStackFree(stack))
                        {
                            requiredItems.add(stack);
                        }
                    }
                }
                return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.MISSING_ITEMS, requiredItems);
            }
        }
        return new BlockPlacementResult(worldPos, BlockPlacementResult.Result.MISSING_ITEMS, requiredItems);
    }

    /**
     * Get the iterator instance.
     * @return the BlueprintIterator.
     */
    public AbstractBlueprintIterator getIterator()
    {
        return iterator;
    }

    /**
     * Get the handler instance.
     * @return the IStructureHandler.
     */
    public IStructureHandler getHandler()
    {
        return handler;
    }

    /**
     * Check if the structure placer is ready.
     * @return true if so.
     */
    public boolean isReady()
    {
        return getHandler().isReady();
    }

    /**
     * The different operations.
     */
    public enum Operation
    {
        WATER_REMOVAL,
        BLOCK_REMOVAL,
        BLOCK_PLACEMENT,
        GET_RES_REQUIREMENTS
    }

}
