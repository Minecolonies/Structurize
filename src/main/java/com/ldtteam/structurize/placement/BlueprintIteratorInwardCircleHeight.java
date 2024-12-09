package com.ldtteam.structurize.placement;

import com.ldtteam.structurize.api.util.BlockPosUtil;
import com.ldtteam.structurize.placement.structure.IStructureHandler;
import net.minecraft.core.BlockPos;

/**
 * Iterator which walks the structure from outside in , in a clockwise rotation
 */
public class BlueprintIteratorInwardCircleHeight extends AbstractBlueprintIterator
{
    private final BlockPos topRightCorner;
    private final int      height;

    public BlueprintIteratorInwardCircleHeight(final IStructureHandler structureHandler, final int height)
    {
        super(structureHandler);
        topRightCorner = size.offset(-1, -1, -1);
        this.height = height;
    }

    @Override
    public Result increment()
    {
        this.prevProgressPos.set(this.progressPos);
        if (this.progressPos.equals(NULL_POS))
        {
            this.progressPos.set(0, 0, 0);
            return Result.NEW_BLOCK;
        }

        final BlockPos next = BlockPosUtil.getNextPosInCircleFrom(progressPos, BlockPos.ZERO, topRightCorner, height);
        if (next.equals(progressPos))
        {
            this.reset();
            return Result.AT_END;
        }

        progressPos.set(next);
        return Result.NEW_BLOCK;
    }

    @Override
    public Result decrement()
    {
        this.prevProgressPos.set(this.progressPos);
        if (this.progressPos.equals(NULL_POS))
        {
            this.progressPos.set(0, topRightCorner.getY(), 0);
            return Result.NEW_BLOCK;
        }

        progressPos.setY(topRightCorner.getY() - progressPos.getY());
        final BlockPos next = BlockPosUtil.getNextPosInCircleFrom(progressPos, BlockPos.ZERO, topRightCorner, 1);

        if (next == progressPos)
        {
            this.reset();
            return Result.AT_END;
        }

        progressPos.set(next.getX(), topRightCorner.getY() - next.getY(), next.getZ());
        return Result.NEW_BLOCK;
    }
}
