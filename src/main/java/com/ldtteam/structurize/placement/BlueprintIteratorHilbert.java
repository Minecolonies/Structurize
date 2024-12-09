package com.ldtteam.structurize.placement;

import com.ldtteam.structurize.placement.structure.IStructureHandler;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A hilbert space-filling curve, generalised to any dimensions.
 */
public class BlueprintIteratorHilbert extends AbstractBlueprintIterator
{
    /**
     * Ordered list of positions for each layer.
     */
    private final List<BlockPos> positions = new ArrayList<>();
    /**
     * Current index into the positions list.
     */
    private int                  index;

    public BlueprintIteratorHilbert(@NotNull final IStructureHandler structureHandler)
    {
        super(structureHandler);

        generateLayerPattern();
    }

    @Override
    public Result increment()
    {
        this.prevProgressPos.set(this.progressPos);
        if (this.progressPos.equals(NULL_POS))
        {
            this.index = 0;
            this.progressPos.set(this.positions.get(0));
            return Result.NEW_BLOCK;
        }

        return iterate((this.progressPos.getY() & 1) == 0, true);
    }

    @Override
    public Result decrement()
    {
        this.prevProgressPos.set(this.progressPos);
        if (this.progressPos.equals(NULL_POS))
        {
            this.index = (this.size.getY() & 1) == 0 ? this.positions.size() - 1 : 0;
            this.progressPos.set(this.positions.get(this.index).above(this.size.getY() - 1));
            return Result.NEW_BLOCK;
        }

        return iterate((this.progressPos.getY() & 1) == 0, false);
    }

    @Override
    public void setProgressPos(@NotNull BlockPos localPosition)
    {
        super.setProgressPos(localPosition);

        if (!this.progressPos.equals(NULL_POS))
        {
            for (int i = 0; i < this.positions.size(); ++i)
            {
                final BlockPos pos = this.positions.get(i);
                if (pos.getX() == this.progressPos.getX() && pos.getZ() == this.progressPos.getZ())
                {
                    this.index = i;
                    return;
                }
            }
            this.index = 0;
        }
    }

    private Result iterate(final boolean forward, final boolean up)
    {
        if (forward)
        {
            if (this.index < this.positions.size() - 1)
            {
                ++this.index;
                this.progressPos.set(this.positions.get(this.index).above(this.progressPos.getY()));
                return Result.NEW_BLOCK;
            }
        }
        else
        {
            if (this.index > 0)
            {
                --this.index;
                this.progressPos.set(this.positions.get(this.index).above(this.progressPos.getY()));
                return Result.NEW_BLOCK;
            }
        }

        return up ? moveUp() : moveDown();
    }

    private Result moveUp()
    {
        final int y = this.progressPos.getY() + 1;
        if (y < this.size.getY())
        {
            this.progressPos.set(this.positions.get(this.index).above(y));
            return Result.NEW_BLOCK;
        }
        return Result.AT_END;
    }

    private Result moveDown()
    {
        final int y = this.progressPos.getY() - 1;
        if (y >= 0)
        {
            this.progressPos.set(this.positions.get(this.index).above(y));
            return Result.NEW_BLOCK;
        }
        return Result.AT_END;
    }

    private void generateLayerPattern()
    {
        if (this.size.getX() >= this.size.getZ())
        {
            generateHilbert(0, 0, this.size.getX(), 0, 0, this.size.getZ());
        }
        else
        {
            generateHilbert(0, 0, 0, this.size.getZ(), this.size.getX(), 0);
        }
    }

    // Based on https://stackoverflow.com/a/58603668/43534
    private void generateHilbert(int x, int z, final int ax, final int az, final int bx, final int bz)
    {
        final int width = Math.abs(ax + az);
        final int height = Math.abs(bx + bz);
        final int dax = Integer.compare(ax, 0), daz = Integer.compare(az, 0);
        final int dbx = Integer.compare(bx, 0), dbz = Integer.compare(bz, 0);

        // trivial row fill
        if (height == 1)
        {
            for (int i = 0; i < width; ++i, x += dax, z += daz)
            {
                this.positions.add(new BlockPos(x, 0, z));
            }
            return;
        }

        // trivial column fill
        if (width == 1)
        {
            for (int i = 0; i < height; ++i, x += dbx, z += dbz)
            {
                this.positions.add(new BlockPos(x, 0, z));
            }
            return;
        }

        int ax2 = ax / 2, az2 = az / 2;
        int bx2 = bx / 2, bz2 = bz / 2;
        final int width2 = Math.abs(ax2 + az2);
        final int height2 = Math.abs(bx2 + bz2);

        if (2 * width > 3 * height)
        {
            if ((width2 & 1) != 0 && width > 2)
            {
                // prefer even steps
                ax2 += dax;
                az2 += daz;
            }

            // long case: split in two parts only
            generateHilbert(x, z, ax2, az2, bx, bz);
            generateHilbert(x + ax2, z + az2, ax - ax2, az - az2, bx, bz);
        }
        else
        {
            if ((height2 & 1) != 0 && height > 2)
            {
                // prefer even steps
                bx2 += dbx;
                bz2 += dbz;
            }

            // standard case: one step up, one long horizontal, one step down
            generateHilbert(x, z, bx2, bz2, ax2, az2);
            generateHilbert(x + bx2, z + bz2, ax, az, bx - bx2, bz - bz2);
            generateHilbert(x + (ax - dax) + (bx2 - dbx), z + (az - daz) + (bz2 - dbz),
                    -bx2, -bz2, -(ax - ax2), -(az - az2));
        }
    }
}
