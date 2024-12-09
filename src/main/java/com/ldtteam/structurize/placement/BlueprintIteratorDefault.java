package com.ldtteam.structurize.placement;

import com.ldtteam.structurize.placement.structure.IStructureHandler;
import net.minecraft.core.BlockPos;

/**
 * This is the default blueprint iterator.
 * It's a helper class used to track the progress of one iteration over the structure.
 */
public class BlueprintIteratorDefault extends AbstractBlueprintIterator
{
    /**
     * Initialize the blueprint iterator with the structure handler.
     * @param structureHandler the structure handler.
     */
    public BlueprintIteratorDefault(final IStructureHandler structureHandler)
    {
        super(structureHandler, new BlockPos(structureHandler.getBluePrint().getSizeX(), structureHandler.getBluePrint().getSizeY(), structureHandler.getBluePrint().getSizeZ()));
    }

    /**
     * Increment progressPos.
     *
     * @return false if the all the block have been incremented through.
     */
    @Override
    public Result increment()
    {
        return iterate(true);
    }

    private Result iterate(boolean up)
    {
        this.prevProgressPos.set(this.progressPos);
        if (this.progressPos.equals(NULL_POS))
        {
            this.progressPos.set(-1, up ? 0 : this.size.getY() -1, 0);
        }

        if (progressPos.getZ() % 2 == 0)
        {
            this.progressPos.set(this.progressPos.getX() + 1, this.progressPos.getY(), this.progressPos.getZ());
            if (this.progressPos.getX() >= this.size.getX())
            {
                this.progressPos.set(this.size.getX() - 1, this.progressPos.getY(), this.progressPos.getZ() + 1);
                if (this.progressPos.getZ() >= this.size.getZ())
                {
                    this.progressPos.set(0, up ? this.progressPos.getY() + 1 : this.progressPos.getY() - 1, 0);
                    if ((up && this.progressPos.getY() >= this.size.getY()) || (!up && this.progressPos.getY() <= -1))
                    {
                        this.reset();
                        return Result.AT_END;
                    }
                }
            }
        }
        else
        {
            this.progressPos.set(this.progressPos.getX() - 1, this.progressPos.getY(), this.progressPos.getZ());
            if (this.progressPos.getX() <= -1)
            {
                this.progressPos.set(0, this.progressPos.getY(), this.progressPos.getZ() + 1);
                if (this.progressPos.getZ() >= this.size.getZ())
                {
                    this.progressPos.set(0, up ? this.progressPos.getY() + 1 : this.progressPos.getY() - 1, 0);
                    if ((up && this.progressPos.getY() >= this.size.getY()) || (!up && this.progressPos.getY() <= -1))
                    {
                        this.reset();
                        return Result.AT_END;
                    }
                }
            }
        }

        return Result.NEW_BLOCK;
    }

    @Override
    public Result decrement()
    {
       return iterate(false);
    }
}
