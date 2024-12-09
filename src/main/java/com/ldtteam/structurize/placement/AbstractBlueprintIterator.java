package com.ldtteam.structurize.placement;

import com.ldtteam.structurize.placement.structure.IStructureHandler;
import com.ldtteam.structurize.util.BlockUtils;
import com.ldtteam.structurize.util.BlueprintPositionInfo;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.util.TriPredicate;

import java.util.Collections;
import java.util.function.Supplier;

/**
 * This is the blueprint iterator.
 * It's a helper class used to track the progress of one iteration over the structure.
 */
public abstract class AbstractBlueprintIterator implements IBlueprintIterator
{
    /**
     * The position we use as our uninitialized value.
     */
    public static final BlockPos NULL_POS = new BlockPos(-1, -1, -1);

    /**
     * The Structure position we are at. Defaulted to NULL_POS.
     */
    protected final BlockPos.MutableBlockPos progressPos = new BlockPos.MutableBlockPos(-1, -1, -1);

    /**
     * The previous position before the current progress position.
     */
    protected final BlockPos.MutableBlockPos prevProgressPos = new BlockPos.MutableBlockPos(-1, -1, -1);

    /**
     * The size of the structure.
     */
    protected final BlockPos size;

    /**
     * The structure handler.
     */
    private final IStructureHandler structureHandler;

    /**
     * If entity info is required.
     */
    private boolean includeEntities;

    /**
     * If structure removal step.
     */
    private boolean isRemoving;

    /**
     * Initialize the blueprint iterator with the structure handler.
     * @param structureHandler the structure handler.
     */
    public AbstractBlueprintIterator(final IStructureHandler structureHandler)
    {
        this(structureHandler, new BlockPos(structureHandler.getBluePrint().getSizeX(), structureHandler.getBluePrint().getSizeY(), structureHandler.getBluePrint().getSizeZ()));
    }

    /**
     * Initialize the blueprint iterator with the structure handler.
     * @param size the size.
     * @param structureHandler the structure handler.
     */
    public AbstractBlueprintIterator(final IStructureHandler structureHandler, final BlockPos size)
    {
        this.structureHandler = structureHandler;
        this.size = size;
    }

    @Override
    public Result increment(final TriPredicate<BlueprintPositionInfo, BlockPos, IStructureHandler> skipCondition)
    {
        return iterateWithCondition(skipCondition, this::increment);
    }

    @Override
    public Result decrement(final TriPredicate<BlueprintPositionInfo, BlockPos, IStructureHandler> skipCondition)
    {
        return iterateWithCondition(skipCondition, this::decrement);
    }

    /**
     * Execute a supplier function to avoid duplicate code for increment and decrement functions.
     * @param skipCondition the skipCondition.
     * @param function the supplier function.
     * @return the Result.
     */
    private Result iterateWithCondition(final TriPredicate<BlueprintPositionInfo, BlockPos, IStructureHandler> skipCondition, final Supplier<Result> function)
    {
        int count = 0;
        do
        {
            if(function.get() == Result.AT_END)
            {
                return Result.AT_END;
            }
            final BlockPos progressPos = getProgressPos();
            final IStructureHandler structureHandler = this.getStructureHandler();
            final BlockPos worldPos = structureHandler.getProgressPosInWorld(progressPos);
            final BlueprintPositionInfo info = getBluePrintPositionInfo(progressPos);

            if (skipCondition.test(info, worldPos, structureHandler))
            {
                continue;
            }
            else if (!isRemoving() && BlockUtils.areBlockStatesEqual(info.getBlockInfo().getState(), structureHandler.getWorld().getBlockState(worldPos), structureHandler::replaceWithSolidBlock, structureHandler.fancyPlacement(), structureHandler::shouldBlocksBeConsideredEqual,
              info.getBlockInfo().getTileEntityData(),
              info.getBlockInfo().getTileEntityData() == null ? null : structureHandler.getWorld().getBlockEntity(worldPos)) && info.getEntities().length == 0)
            {
                structureHandler.triggerSuccess(progressPos, Collections.emptyList(), false);
                continue;
            }
            return Result.NEW_BLOCK;
        }
        while (count++ < structureHandler.getMaxBlocksCheckedPerCall());

        return Result.CONFIG_LIMIT;
    }

    @Override
    public void setProgressPos(final BlockPos localPosition)
    {
        this.prevProgressPos.set(this.progressPos);
        if (localPosition.equals(NULL_POS))
        {
            this.progressPos.set(localPosition);
        }
        else
        {
            this.progressPos.set(localPosition.getX() % size.getX(),
              localPosition.getY() % size.getY(),
              localPosition.getZ() % size.getZ());
        }
    }

        @Override
        public BlueprintPositionInfo getBluePrintPositionInfo(final BlockPos localPos)
    {
        return structureHandler.getBluePrint().getBluePrintPositionInfo(localPos, includeEntities);
    }

    @Override
    public void includeEntities()
    {
        this.includeEntities = true;
    }

    @Override
    public boolean hasEntities()
    {
        return includeEntities;
    }

    @Override
    public void setRemoving()
    {
        this.isRemoving = true;
    }

    @Override
    public boolean isRemoving()
    {
        return isRemoving;
    }

    @Override
    public void reset()
    {
        prevProgressPos.set(NULL_POS);
        progressPos.set(NULL_POS);
        includeEntities = false;
        isRemoving = false;
    }

    @Override
    public BlockPos getSize()
    {
        return size;
    }

    @Override
    public BlockPos getProgressPos()
    {
        return progressPos.immutable();
    }

    @Override
    public BlockPos getPrevProgressPos()
    {
        return prevProgressPos.immutable();
    }

    protected IStructureHandler getStructureHandler()
    {
        return structureHandler;
    }

    /**
     * The different results when advancing the structure.
     */
    public enum Result
    {
        NEW_BLOCK,
        AT_END,
        CONFIG_LIMIT
    }
}
