package com.ldtteam.structurize.tileentities;

import com.google.common.primitives.Ints;
import com.ldtteam.structurize.Structurize;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.ldtteam.structurize.api.util.constant.Constants.*;
import static com.ldtteam.structurize.api.util.constant.NbtTagConstants.*;
import static net.minecraft.util.Direction.*;

/**
 * This Class is about the MultiBlock TileEntity which takes care of pushing others around (In a non mean way).
 */
public class TileEntityMultiBlock extends TileEntity implements ITickableTileEntity
{
    /**
     * Max block range.
     */
    private static final int MAX_RANGE = 10;

    /**
     * Max block speed.
     */
    private static final int MAX_SPEED = 3;

    /**
     * Min block speed.
     */
    private static final int MIN_SPEED = 1;

    /**
     * Default gate and bridge range.
     */
    public static final int DEFAULT_RANGE = 3;

    /**
     * Default gate and bridge range.
     */
    public static final int DEFAULT_SPEED = 2;

    /**
     * The last redstone state which got in.
     */
    private boolean on = false;

    /**
     * The direction it should push or pull rom.
     */
    private Direction direction = UP;

    /**
     * The output direction.
     */
    private Direction output = DOWN;

    /**
     * The range it should pull to.
     */
    private int range = DEFAULT_RANGE;

    /**
     * The direction it is going to.
     */
    private Direction currentDirection;

    /**
     * The progress it has made.
     */
    private int progress = 0;

    /**
     * Amount of ticks passed.
     */
    private int ticksPassed = 0;

    /**
     * Speed of the multiblock, max 3, min 1.
     */
    private int speed = 2;

    public TileEntityMultiBlock()
    {
        super(ModTileEntities.MULTIBLOCK);
    }

    /**
     * Handle redstone input.
     *
     * @param signal true if positive.
     */
    public void handleRedstone(final boolean signal)
    {
        if (speed == 0)
        {
            speed = DEFAULT_SPEED;
        }

        if (signal != on && progress == range)
        {
            on = signal;
            if (signal)
            {
                currentDirection = output;
            }
            else
            {
                currentDirection = direction;
            }
            progress = 0;
        }
    }

    @Override
    public void tick()
    {
        if (level == null || level.isClientSide)
        {
            return;
        }
        if (currentDirection == null && progress < range)
        {
            progress = range;
        }

        if (progress < range)
        {
            if (ticksPassed % (TICKS_SECOND / speed) == 0)
            {
                handleTick();
                ticksPassed = 1;
            }
            ticksPassed++;
        }
    }

    /**
     * Handle the tick, to finish the sliding.
     */
    public void handleTick()
    {
        final Direction currentOutPutDirection = currentDirection == direction ? output : direction;

        if (progress < range)
        {
            final BlockState blockToMove = level.getBlockState(worldPosition.relative(currentDirection, 1));
            if (blockToMove.getBlock() == Blocks.AIR
                  || blockToMove.getPistonPushReaction() == PushReaction.IGNORE
                  || blockToMove.getPistonPushReaction() == PushReaction.DESTROY
                  || blockToMove.getPistonPushReaction() == PushReaction.BLOCK
                  || blockToMove.getBlock().hasTileEntity(blockToMove)
                  || blockToMove.getBlock() == Blocks.BEDROCK)
            {
                progress++;
                return;
            }

            for (int i = 0; i < Math.min(range, MAX_RANGE); i++)
            {
                final int blockToGoTo = i - 1 - progress + (i - 1 - progress >= 0 ? 1 : 0);
                final int blockToGoFrom = i + 1 - progress - (i + 1 - progress <= 0 ? 1 : 0);

                final BlockPos posToGo = blockToGoTo > 0 ? worldPosition.relative(currentDirection, blockToGoTo) : worldPosition.relative(currentOutPutDirection, Math.abs(blockToGoTo));
                final BlockPos posToGoFrom = blockToGoFrom > 0 ? worldPosition.relative(currentDirection, blockToGoFrom) : worldPosition.relative(currentOutPutDirection, Math.abs(blockToGoFrom));
                if (level.isEmptyBlock(posToGo) || level.getBlockState(posToGo).getMaterial().isLiquid())
                {
                    BlockState tempState = level.getBlockState(posToGoFrom);
                    if (blockToMove.getBlock() == tempState.getBlock() && level.hasChunkAt(posToGoFrom) && level.hasChunkAt(posToGo))
                    {
                        pushEntitiesIfNecessary(posToGo, worldPosition);

                        tempState = Block.updateFromNeighbourShapes(tempState, this.level, posToGo);
                        level.setBlock(posToGo, tempState, 67);
                        if (tempState.getBlock() instanceof IBucketPickupHandler)
                        {
                            ((IBucketPickupHandler) tempState.getBlock()).takeLiquid(level, posToGo, tempState);
                        }
                        this.level.neighborChanged(posToGo, tempState.getBlock(), posToGo);

                        level.removeBlock(posToGoFrom, false);
                    }
                }
            }
            level.playSound((PlayerEntity) null,
              worldPosition,
              SoundEvents.PISTON_EXTEND,
              SoundCategory.BLOCKS,
              (float) VOLUME,
              (float) PITCH);
            progress++;
        }
    }

    private void pushEntitiesIfNecessary(final BlockPos posToGo, final BlockPos pos)
    {
        final List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AxisAlignedBB(posToGo));
        final BlockPos vector = posToGo.subtract(pos);
        final BlockPos posTo = posToGo.relative(getNearest(vector.getX(), vector.getY(), vector.getZ()));
        for (final Entity entity : entities)
        {
            entity.teleportTo(posTo.getX() + HALF_BLOCK, posTo.getY() + HALF_BLOCK, posTo.getZ() + HALF_BLOCK);
        }
    }

    @Override
    public void rotate(final Rotation rotationIn)
    {
        if (output != UP && output != DOWN)
        {
            output = rotationIn.rotate(output);
        }

        if (direction != UP && direction != DOWN)
        {
            direction = rotationIn.rotate(direction);
        }
        super.rotate(rotationIn);
    }

    @Override
    public void mirror(final Mirror mirrorIn)
    {
        if (output != UP && output != DOWN)
        {
            output = mirrorIn.mirror(output);
        }

        if (direction != UP && direction != DOWN)
        {
            direction = mirrorIn.mirror(direction);
        }

        super.mirror(mirrorIn);
    }

    /**
     * Check if the redstone is on.
     *
     * @return true if so.
     */
    public boolean isOn()
    {
        return on;
    }

    /**
     * Get the direction the block is facing.
     *
     * @return the EnumFacing.
     */
    public Direction getDirection()
    {
        return direction;
    }

    /**
     * Get the output direction the block is facing.
     *
     * @return the EnumFacing.
     */
    public Direction getOutput()
    {
        return output;
    }

    /**
     * Set the direction it should be facing.
     *
     * @param direction the direction.
     */
    public void setDirection(final Direction direction)
    {
        this.direction = direction;
    }

    /**
     * Set the direction it should output to.
     *
     * @param output the direction.
     */
    public void setOutput(final Direction output)
    {
        this.output = output;
    }

    /**
     * Get the range of blocks it should push.
     *
     * @return the range.
     */
    public int getRange()
    {
        return range;
    }

    /**
     * Set the range it should push.
     *
     * @param range the range.
     */
    public void setRange(final int range)
    {
        this.range = Math.min(range, MAX_RANGE);
        this.progress = range;
    }

    /**
     * Get the speed of the block.
     *
     * @return the speed (min 1 max 3).
     */
    public int getSpeed()
    {
        return speed;
    }

    /**
     * Setter for speed.
     *
     * @param speed the speed to set.
     */
    public void setSpeed(final int speed)
    {
        this.speed = Ints.constrainToRange(speed, MIN_SPEED, MAX_SPEED);
    }

    @Override
    public void load(final BlockState blockState, final CompoundNBT compound)
    {
        super.load(blockState, compound);

        range = compound.getInt(TAG_RANGE);
        this.progress = compound.getInt(TAG_PROGRESS);
        direction = values()[compound.getInt(TAG_DIRECTION)];
        on = compound.getBoolean(TAG_INPUT);
        if (compound.getAllKeys().contains(TAG_OUTPUT_DIRECTION))
        {
            output = values()[compound.getInt(TAG_OUTPUT_DIRECTION)];
        }
        else
        {
            output = direction.getOpposite();
        }
        speed = compound.getInt(TAG_SPEED);
    }

    @NotNull
    @Override
    public CompoundNBT save(final CompoundNBT compound)
    {
        super.save(compound);
        compound.putInt(TAG_RANGE, range);
        compound.putInt(TAG_PROGRESS, progress);
        compound.putInt(TAG_DIRECTION, direction.ordinal());
        compound.putBoolean(TAG_INPUT, on);
        if (output != null)
        {
            compound.putInt(TAG_OUTPUT_DIRECTION, output.ordinal());
        }
        compound.putInt(TAG_SPEED, speed);

        return compound;
    }

    @Override
    public void handleUpdateTag(final BlockState blockState, final CompoundNBT tag)
    {
        this.load(blockState, tag);
    }

    @Override
    public void onDataPacket(final NetworkManager net, final SUpdateTileEntityPacket pkt)
    {
        this.load(Structurize.proxy.getBlockStateFromWorld(pkt.getPos()), pkt.getTag());
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket()
    {
        CompoundNBT nbt = new CompoundNBT();
        this.save(nbt);

        return new SUpdateTileEntityPacket(this.getBlockPos(), 0, nbt);
    }

    @NotNull
    @Override
    public CompoundNBT getUpdateTag()
    {
        return save(new CompoundNBT());
    }
}
