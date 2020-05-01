package com.ldtteam.structurize.util;

import com.ldtteam.structures.blueprints.v1.Blueprint;
import com.ldtteam.structures.helpers.Structure;
import com.ldtteam.structurize.api.util.Log;
import com.ldtteam.structurize.blocks.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.extensions.IForgeBlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods related to structure placement.
 */
public class StructurePlacementUtils
{
    /**
     * Check if the structure block is equal to the world block.
     *
     * @param world         the world.
     * @param blockPosition the world block position.
     * @param state         the state in the structure.
     * @return true if so.
     */
    public static boolean isStructureBlockEqualWorldBlock(final World world, final BlockPos blockPosition, final IForgeBlockState state)
    {
        final Block structureBlock = state.getBlockState().getBlock();

        //All worldBlocks are equal the substitution block
        if (structureBlock == ModBlocks.blockSubstitution)
        {
            return true;
        }

        final BlockState worldBlockState = world.getBlockState(blockPosition);

        if (structureBlock == ModBlocks.blockSolidSubstitution && worldBlockState.getMaterial().isSolid())
        {
            return true;
        }

        final Block worldBlock = worldBlockState.getBlock();

        //list of things to only check block for.
        //For the time being any flower pot is equal to each other.
        if (structureBlock instanceof DoorBlock || structureBlock == Blocks.FLOWER_POT)
        {
            return structureBlock == worldBlock;
        }
        else if (structureBlock instanceof StairsBlock && state.getBlockState() == worldBlockState)
        {
            return true;
        }
        else if ((structureBlock == Blocks.GRASS_BLOCK && worldBlock == Blocks.DIRT)
        || worldBlock == Blocks.GRASS_BLOCK && structureBlock == Blocks.DIRT)
        {
            return true;
        }

        //had this problem in a super flat world, causes builder to sit doing nothing because placement failed
        return blockPosition.getY() <= 0 || structureBlock == worldBlock;
    }

    /**
     * Unload a structure at a certain location.
     *
     * @param world    the world.
     * @param pos      the position.
     * @param first    the name.
     * @param rotation the rotation.
     * @param mirror   the mirror.
     */
    public static void unloadStructure(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final String first, final Rotation rotation, @NotNull final Mirror mirror)
    {
        @NotNull final Structure structure = new Structure(world, first, new PlacementSettings(mirror, rotation));
        @NotNull final InstantStructurePlacer structureWrapper = new InstantStructurePlacer(structure);
        structure.setPosition(pos);
        structure.rotate(rotation, world, pos, mirror);
        structureWrapper.removeStructure(pos.subtract(structure.getOffset()));
    }

    /**
     * Load a structure into this world
     * and place it in the right position and rotation.
     *
     * @param worldObj the world to load it in
     * @param blueprint the blueprint to load and place.
     * @param pos      coordinates
     * @param rotation the rotation.
     * @param mirror   the mirror used.
     * @param player   the placing player.
     */
    public static void loadAndPlaceShapeWithRotation(
      final ServerWorld worldObj,
      final Blueprint blueprint,
      @NotNull final BlockPos pos,
      final Rotation rotation,
      @NotNull final Mirror mirror,
      final ServerPlayerEntity player)
    {
        try
        {
            final Structure structure = new Structure(worldObj);
            structure.setBluePrint(blueprint);
            structure.setPlacementSettings(new PlacementSettings(Mirror.NONE, Rotation.NONE));
            structure.setPosition(pos);
            structure.rotate(rotation, worldObj, pos, mirror);
            @NotNull final InstantStructurePlacer structureWrapper = new InstantStructurePlacer(structure);
            structureWrapper.setupStructurePlacement(pos.subtract(structure.getOffset()), false, player);
        }
        catch (final IllegalStateException e)
        {
            Log.getLogger().warn("Could not load structure!", e);
        }
    }
}
