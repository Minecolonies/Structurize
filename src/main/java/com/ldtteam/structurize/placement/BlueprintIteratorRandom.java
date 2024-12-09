package com.ldtteam.structurize.placement;

import com.ldtteam.structurize.placement.structure.IStructureHandler;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Random blueprint iterator.
 * Creates a seeded list of positions and iterates it.
 */
public class BlueprintIteratorRandom extends AbstractBlueprintIterator
{
    /**
     * Random ordered list of positions.
     */
    final List<BlockPos> positions = new ArrayList<>();

    /**
     * Initialize the blueprint iterator with the structure handler.
     * @param structureHandler the structure handler.
     */
    public BlueprintIteratorRandom(final IStructureHandler structureHandler)
    {
        super(structureHandler, new BlockPos(structureHandler.getBluePrint().getSizeX(), structureHandler.getBluePrint().getSizeY(), structureHandler.getBluePrint().getSizeZ()));
        for (int x = 0; x < this.size.getX(); x++)
        {
            for (int z = 0; z < this.size.getZ(); z++)
            {
                positions.add(new BlockPos(x, 0, z));
            }
        }
        Collections.shuffle(positions, new Random(this.size.hashCode()));
    }

    /**
     * Increment progressPos.
     *
     * @return false if the all the block have been incremented through.
     */
    public Result increment()
    {
        this.prevProgressPos.set(this.progressPos);
        if (this.progressPos.equals(NULL_POS))
        {
            this.progressPos.set(this.positions.get(0).getX(), 0, this.positions.get(0).getZ());
            return Result.NEW_BLOCK;
        }
        else
        {
            int index = this.positions.indexOf(new BlockPos(this.progressPos.getX(), 0, this.progressPos.getZ())) + 1;
            if (index >= this.positions.size())
            {
                this.progressPos.set(this.positions.get(0).getX(), this.progressPos.getY() + 1, this.positions.get(0).getZ());
                if (this.progressPos.getY() >= this.size.getY())
                {
                    this.reset();
                    return Result.AT_END;
                }

                return Result.NEW_BLOCK;
            }
            this.progressPos.set(this.positions.get(index).getX(), this.progressPos.getY(), this.positions.get(index).getZ());
        }

        return Result.NEW_BLOCK;
    }

    /**
     * Decrement progressPos.
     *
     * @return false if progressPos can't be decremented any more.
     */
    public Result decrement()
    {
        this.prevProgressPos.set(this.progressPos);
        if (this.progressPos.equals(NULL_POS))
        {
            this.progressPos.set(this.positions.get(0).getX(), this.size.getY() - 1, this.positions.get(0).getZ());
            return Result.NEW_BLOCK;
        }
        else
        {
            int index = this.positions.indexOf(new BlockPos(this.progressPos.getX(), 0, this.progressPos.getZ())) + 1;
            if (index >= this.positions.size())
            {
                this.progressPos.set(this.positions.get(0).getX(), this.progressPos.getY() - 1, this.positions.get(0).getZ());
                if (this.progressPos.getY() < 0)
                {
                    this.reset();
                    return Result.AT_END;
                }
                return Result.NEW_BLOCK;
            }
            this.progressPos.set(this.positions.get(index).getX(), this.progressPos.getY(), this.positions.get(index).getZ());
        }

        return Result.NEW_BLOCK;
    }
}
