package com.ldtteam.structurize.api;

import com.ldtteam.common.codec.Codecs;
import com.ldtteam.structurize.blueprints.FacingFixer;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

/**
 * Represents all possible states of structure when rorating or mirroring
 */
public enum RotationMirror
{
    NONE(Rotation.NONE, Mirror.NONE),
    R90(Rotation.CLOCKWISE_90, Mirror.NONE),
    R180(Rotation.CLOCKWISE_180, Mirror.NONE),
    R270(Rotation.COUNTERCLOCKWISE_90, Mirror.NONE),
    MIR_NONE(Rotation.NONE, Mirror.FRONT_BACK),
    MIR_R90(Rotation.CLOCKWISE_90, Mirror.FRONT_BACK),
    MIR_R180(Rotation.CLOCKWISE_180, Mirror.FRONT_BACK),
    MIR_R270(Rotation.COUNTERCLOCKWISE_90, Mirror.FRONT_BACK);

    public static final Codec<RotationMirror> CODEC = Codecs.forEnum(RotationMirror.class);
    public static final StreamCodec<ByteBuf, RotationMirror> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(ordinal -> RotationMirror.values()[ordinal], RotationMirror::ordinal);

    public static final RotationMirror[] MIRRORED = {MIR_NONE, MIR_R90, MIR_R180, MIR_R270};
    public static final RotationMirror[] NOT_MIRRORED = {NONE, R90, R180, R270};

    static
    {
        NONE.rotateCW90 = R90;
        R90.rotateCW90 = R180;
        R180.rotateCW90 = R270;
        R270.rotateCW90 = NONE;
        MIR_NONE.rotateCW90 = MIR_R90;
        MIR_R90.rotateCW90 = MIR_R180;
        MIR_R180.rotateCW90 = MIR_R270;
        MIR_R270.rotateCW90 = MIR_NONE;

        NONE.rotateCW180 = R180;
        R90.rotateCW180 = R270;
        R180.rotateCW180 = NONE;
        R270.rotateCW180 = R90;
        MIR_NONE.rotateCW180 = MIR_R180;
        MIR_R90.rotateCW180 = MIR_R270;
        MIR_R180.rotateCW180 = MIR_NONE;
        MIR_R270.rotateCW180 = MIR_R90;

        NONE.rotateCCW90 = R270;
        R90.rotateCCW90 = NONE;
        R180.rotateCCW90 = R90;
        R270.rotateCCW90 = R180;
        MIR_NONE.rotateCCW90 = MIR_R270;
        MIR_R90.rotateCCW90 = MIR_NONE;
        MIR_R180.rotateCCW90 = MIR_R90;
        MIR_R270.rotateCCW90 = MIR_R180;

        NONE.mirrorFB = MIR_NONE;
        R90.mirrorFB = MIR_R270;
        R180.mirrorFB = MIR_R180;
        R270.mirrorFB = MIR_R90;
        MIR_NONE.mirrorFB = NONE;
        MIR_R90.mirrorFB = R270;
        MIR_R180.mirrorFB = R180;
        MIR_R270.mirrorFB = R90;

        NONE.mirrorLR = MIR_R180;
        R90.mirrorLR = MIR_R90;
        R180.mirrorLR = MIR_NONE;
        R270.mirrorLR = MIR_R270;
        MIR_NONE.mirrorLR = R180;
        MIR_R90.mirrorLR = R90;
        MIR_R180.mirrorLR = NONE;
        MIR_R270.mirrorLR = R270;
    }

    private final Rotation rotation;
    private final Mirror mirror;

    private RotationMirror rotateCW90;
    private RotationMirror rotateCW180;
    private RotationMirror rotateCCW90;
    private RotationMirror mirrorFB;
    private RotationMirror mirrorLR;

    private RotationMirror(final Rotation rotation, final Mirror mirror)
    {
        this.rotation = rotation;
        this.mirror = mirror;
    }

    public Rotation rotation()
    {
        return rotation;
    }

    public boolean isMirrored()
    {
        return mirror != Mirror.NONE;
    }

    public Mirror mirror()
    {
        return mirror;
    }

    public RotationMirror rotate(final Rotation offset)
    {
        return switch (offset)
        {
            case CLOCKWISE_90 -> rotateCW90;
            case CLOCKWISE_180 -> rotateCW180;
            case COUNTERCLOCKWISE_90 -> rotateCCW90;
            case NONE -> this;
        };
    }

    public RotationMirror mirrorate()
    {
        return mirrorate(Mirror.FRONT_BACK);
    }

    /**
     * @see #mirrorate()
     */
    public RotationMirror mirrorate(final Mirror mirrorIn)
    {
        return switch (mirrorIn)
        {
            case LEFT_RIGHT -> mirrorLR;
            case FRONT_BACK -> mirrorFB;
            case NONE -> this;
        };
    }

    public static RotationMirror of(final Rotation rotation, final Mirror mirror)
    {
        return NONE.mirrorate(mirror).rotate(rotation);
    }

    /**
     * @param pos position to transform
     * @return transformed position using this rot+mir around zero pivot
     */
    public BlockPos applyToPos(final BlockPos pos)
    {
        return applyToPos(pos, BlockPos.ZERO);
    }

    /**
     * @param pos position to transform
     * @param pivot center of transformation
     * @return transformed position using this rot+mir around given pivot
     */
    public BlockPos applyToPos(final BlockPos pos, final BlockPos pivot)
    {
        return StructureTemplate.transform(pos, mirror, rotation, pivot);
    }

    /**
     * @param pos position to transform
     * @return transformed position using this rot+mir around zero pivot
     */
    public Vec3 applyToPos(final Vec3 pos)
    {
        return applyToPos(pos, BlockPos.ZERO);
    }

    /**
     * @param pos position to transform
     * @param pivot center of transformation
     * @return transformed position using this rot+mir around given pivot
     */
    public Vec3 applyToPos(final Vec3 pos, final BlockPos pivot)
    {
        return StructureTemplate.transform(pos, mirror, rotation, pivot);
    }

    /**
     * @param blockState blockState to transform
     * @return transformed blockState using this rot+mir
     * @deprecated use {@link #applyToBlockState(BlockState, Level, BlockPos)}, see vanilla methods for more info
     */
    @Deprecated
    public BlockState applyToBlockState(BlockState blockState)
    {
        if (isMirrored())
        {
            blockState = FacingFixer.fixMirroredFacing(blockState.mirror(mirror), blockState);
        }
        return blockState.rotate(rotation);
    }

    /**
     * @param blockState blockState to transform
     * @param level      in which given blockState lives
     * @param pos        where the given blockState is in given level
     * @return transformed blockState using this rot+mir
     */
    public BlockState applyToBlockState(BlockState blockState, final Level level, final BlockPos pos)
    {
        if (isMirrored())
        {
            blockState = FacingFixer.fixMirroredFacing(blockState.mirror(mirror), blockState);
        }
        return blockState.rotate(level, pos, rotation);
    }

    /**
     * @param  end in which state we should end
     * @return     end - this = what it takes from this to end
     */
    public RotationMirror calcDifferenceTowards(final RotationMirror end)
    {
        for (final RotationMirror rotMir : (this.mirror == end.mirror ? NOT_MIRRORED : MIRRORED))
        {
            if (add(rotMir) == end)
            {
                return rotMir;
            }
        }
        throw new IllegalStateException("Missing RotationMirror path");
    }

    /**
     * @see #calcDifferenceTowards(RotationMirror)
     */
    public RotationMirror add(final RotationMirror other)
    {
        return this.mirrorate(other.mirror).rotate(other.rotation);
    }
}
