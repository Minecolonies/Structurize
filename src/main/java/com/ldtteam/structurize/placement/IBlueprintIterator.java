package com.ldtteam.structurize.placement;

import com.ldtteam.structurize.placement.structure.IStructureHandler;
import com.ldtteam.structurize.util.BlueprintPositionInfo;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.util.TriPredicate;

public interface IBlueprintIterator
{

    /**
     * Increment the structure with a certain skipCondition (jump over blocks that fulfill skipCondition).
     * @param skipCondition the skipCondition.
     * @return Result of increment.
     */
    AbstractBlueprintIterator.Result increment(TriPredicate<BlueprintPositionInfo, BlockPos, IStructureHandler> skipCondition);

    /**
     * Increment method, create in implementation.
     *
     * @return increment the structure position (y++).
     */
    AbstractBlueprintIterator.Result increment();

    /**
     * Decrement method, create in implementation.
     *
     * @return decrement the structure position (y--).
     */
    AbstractBlueprintIterator.Result decrement();

    /**
     * Decrement the structure with a certain skipCondition (jump over blocks that fulfill skipCondition).
     * @param skipCondition the skipCondition.
     * @return Result of decrement.
     */
    AbstractBlueprintIterator.Result decrement(TriPredicate<BlueprintPositionInfo, BlockPos, IStructureHandler> skipCondition);

    /**
     * Change the current progressPos. Used when loading progress.
     *
     * @param localPosition new progressPos.
     */
    void setProgressPos(BlockPos localPosition);

    /**
     * Get the blueprint info from the position.
     * @param localPos the position.
     * @return the info object.
     */
    BlueprintPositionInfo getBluePrintPositionInfo(BlockPos localPos);

    /**
     * Set the iterator to include entities.
     */
    void includeEntities();

    /**
     * Retrieve whether the iterator is taking entities into account as well
     * @return whether the iterator is taking entities into account as well
     */
    boolean hasEntities();

    /**
     * Set the iterator to removal mode.
     */
    void setRemoving();

    /**
     * Retrieve whether we're removing blocks at the moment
     * @return Whether we are removing blocks at the moment
     */
    boolean isRemoving();

    /**
     * Reset the progressPos.
     */
    void reset();

    /**
     * Get the progress pos of the iterator.
     * @return the progress pos.
     */
    BlockPos getProgressPos();

    /**
     * Get the last position before the progress pos of the iterator.
     * @return the prev progress pos.
     */
    BlockPos getPrevProgressPos();

    /**
     * Get the size of the blueprint which is iterated over
     * @return the size
     */
    BlockPos getSize();
}
