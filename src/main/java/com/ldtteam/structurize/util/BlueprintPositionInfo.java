package com.ldtteam.structurize.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Contains all information we need for placement at a given position.
 */
public class BlueprintPositionInfo
{
    /**
     * The position in the structure (relative pos not world pos).
     */
    private final BlockPos pos;

    /**
     * The block info.
     */
    @Nullable
    private final BlockInfo info;

    /**
     * List of entities at position.
     */
        public final CompoundTag[] entities;

    /**
     * Constructor of a blueprint position info.
     *
     * @param pos            the position.
     * @param info          the block info.
     * @param entities      the entity data.
     */
    public BlueprintPositionInfo(final BlockPos pos, @Nullable final BlockInfo info, @Nullable final CompoundTag[] entities)
    {
        this.pos = pos;
        this.info = info;
        this.entities = entities;
    }

    /**
     * Get the relative position in the structure.
     * @return the relative pos.
     */
    public BlockPos getPos()
    {
        return this.pos;
    }

    /**
     * Get the blockinfo at the position.
     * @return the info.
     */
    @Nullable
    public BlockInfo getBlockInfo()
    {
        return this.info;
    }

    /**
     * Get the list of entities at the position.
     * @return their RAW nbt data.
     */
    @Nullable
    public CompoundTag[] getEntities()
    {
        return this.entities;
    }
}
