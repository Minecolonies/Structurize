package com.ldtteam.structurize.blueprints.v1;

import com.ldtteam.structurize.api.Log;
import com.ldtteam.common.fakelevel.IFakeLevelBlockGetter;
import com.ldtteam.structurize.api.BlockPosUtil;
import com.ldtteam.structurize.api.ItemStackUtils;
import com.ldtteam.structurize.blockentities.BlockEntityTagSubstitution;
import com.ldtteam.structurize.blockentities.ModBlockEntities;
import com.ldtteam.structurize.blocks.ModBlocks;
import com.ldtteam.structurize.blocks.interfaces.IAnchorBlock;
import com.ldtteam.structurize.component.CapturedBlock;
import com.ldtteam.structurize.blockentities.interfaces.IBlueprintDataProviderBE;
import com.ldtteam.structurize.util.BlockInfo;
import com.ldtteam.structurize.util.BlockUtils;
import com.ldtteam.structurize.util.BlueprintPositionInfo;
import com.mojang.serialization.DynamicOps;
import com.ldtteam.structurize.api.RotationMirror;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.ldtteam.structurize.blockentities.interfaces.IBlueprintDataProviderBE.*;

/**
 * The blueprint class which contains the file format for the schematics.
 * 
 * WARNING: hashcode/equals does NOT take rotation/mirror into account
 */
public class Blueprint implements IFakeLevelBlockGetter
{
    /**
     * Entity pos constant.
     */
    private static final String ENTITY_POS = "Pos";

    /**
     * The list of required mods.
     */
    private final List<String> requiredMods;

    /**
     * The size of the blueprint.
     */
    private short sizeX, sizeY, sizeZ;

    /**
     * The size of the pallete.
     */
    private short palleteSize;

    /**
     * The file name of the blueprint.
     */
    private String fileName;

    /**
     * The full file path.
     */
    private Path filePath;

    /**
     * The pack name.
     */
    private String packName;

    /**
     * The palette of different blocks.
     */
    private List<BlockState> palette;

    /**
     * The name of the blueprint.
     */
    private String name;

    /**
     * The name of the builders.
     */
    private String[] architects;

    /**
     * A list of missing modids that were missing while this schematic was loaded
     */
    private String[] missingMods;

    /**
     * The Schematic Data, each short represents an entry in the {@link Blueprint#palette}
     */
    private short[][][] structure;

    /**
     * The tileentities.
     */
    private CompoundTag[][][] tileEntities;

    /**
     * The entities.
     */
    private CompoundTag[] entities = new CompoundTag[0];

    /**
     * Various caches for storing block data in prepared structures
     */
    private List<BlockInfo>              cacheBlockInfo    = null;
    private Map<BlockPos, BlockInfo>     cacheBlockInfoMap = null;
    private Map<BlockPos, CompoundTag[]> cacheEntitiesMap  = null;

    /**
     * Cache for storing rotate/mirror anchor
     */
    private BlockPos cachePrimaryOffset = null;

    /**
     * The rotation and mirror setting of the blueprint.
     */
    private RotationMirror rotationMirror = RotationMirror.NONE;

    private final HolderLookup.Provider registryAccess;

    /**
     * Constructor of a new Blueprint.
     *
     * @param sizeX        the x size.
     * @param sizeY        the y size.
     * @param sizeZ        the z size.
     * @param palleteSize  the size of the pallete.
     * @param pallete      the palette.
     * @param structure    the structure data.
     * @param tileEntities the tileEntities.
     * @param requiredMods the required mods.
     */
    public Blueprint(
      short sizeX,
      short sizeY,
      short sizeZ,
      short palleteSize,
      List<BlockState> pallete,
      short[][][] structure,
      CompoundTag[] tileEntities,
      List<String> requiredMods,
      HolderLookup.Provider registryAccess)
    {
        this.registryAccess = registryAccess;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palleteSize = palleteSize;
        this.palette = pallete;
        this.structure = structure;
        this.tileEntities = new CompoundTag[sizeY][sizeZ][sizeX];

        for (final CompoundTag te : tileEntities)
        {
            if (te != null)
            {
                this.tileEntities[te.getShort("y")][te.getShort("z")][te.getShort("x")] = te;
            }
        }
        this.requiredMods = requiredMods;
    }

    /**
     * Constructor of a new Blueprint.
     *
     * @param sizeX the x size.
     * @param sizeY the y size.
     * @param sizeZ the z size.
     */
    public Blueprint(short sizeX, short sizeY, short sizeZ, HolderLookup.Provider registryAccess)
    {
        this.registryAccess = registryAccess;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.structure = new short[sizeY][sizeZ][sizeX];
        this.tileEntities = new CompoundTag[sizeY][sizeZ][sizeX];

        this.requiredMods = new ArrayList<>();
        this.palette = new ArrayList<>();
        this.palette.add(0, ModBlocks.blockSubstitution.get().defaultBlockState());
        this.palleteSize = 1;
    }

    /**
     * @return the Size of the Structure on the X-Axis (without rotation and/or mirroring)
     */
    public int getSizeX()
    {
        return this.sizeX;
    }

    /**
     * @return the Size of the Structure on the Y-Axis (without rotation and/or mirroring)
     */
    public short getSizeY()
    {
        return this.sizeY;
    }

    /**
     * @return the Size of the Structure on the Z-Axis (without rotation and/or mirroring)
     */
    public int getSizeZ()
    {
        return this.sizeZ;
    }

    /**
     * @return the amount of Blockstates within the pallete
     */
    public short getPalleteSize()
    {
        return this.palleteSize;
    }

    /**
     * @return the pallete (without rotation and/or mirroring)
     */
    public BlockState[] getPalette()
    {
        return this.palette.toArray(new BlockState[0]);
    }

    /**
     * Add a blockstate to the structure.
     *
     * @param pos   the position to add it to.
     * @param state the state to add.
     */
    public void addBlockState(final BlockPos pos, final BlockState state)
    {
        int index = -1;
        for (int i = 0; i < this.palette.size(); i++)
        {
            if (this.palette.get(i).equals(state))
            {
                index = i;
                break;
            }
        }

        if (index == -1)
        {
            index = this.palleteSize;
            this.palleteSize++;
            this.palette.add(state);
        }

        this.structure[pos.getY()][pos.getZ()][pos.getX()] = (short) index;
        cacheReset(true);
    }

    /**
     * @return the structure (without rotation and/or mirroring) The Coordinate order is: y, z, x
     */
    public short[][][] getStructure()
    {
        return this.structure;
    }

    /**
     * @return an array of serialized TileEntities (posX, posY and posZ tags have been localized to coordinates within the structure)
     */
    public CompoundTag[][][] getTileEntities()
    {
        return this.tileEntities;
    }

    /**
     * @return an array of serialized TileEntities (the Pos tag has been localized to coordinates within the structure)
     */
    public CompoundTag[] getEntities()
    {
        return this.entities;
    }

    /**
     * @param entities an array of serialized TileEntities (the Pos tag need to be localized to coordinates within the structure)
     */
    public void setEntities(CompoundTag[] entities)
    {
        this.entities = entities;
    }

    /**
     * @return a list of all required mods as modid's
     */
    public List<String> getRequiredMods()
    {
        return this.requiredMods;
    }

    /**
     * @return the Name of the Structure
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Sets the name of the Structure
     *
     * @param name the name to set.
     * @return this object.
     */
    public Blueprint setName(final String name)
    {
        this.name = name;
        return this;
    }

    /**
     * Sets the file name of the Structure
     *
     * @param name the file name to set.
     * @return this object.
     */
    public Blueprint setFileName(final String name)
    {
        this.fileName = name;
        return this;
    }

    /**
     * Getter for the filename.
     * @return the unique file name of the structure.
     */
    public String getFileName()
    {
        return this.fileName;
    }

    /**
     * Sets the file path of the Structure
     *
     * @param path the file path to set.
     * @return this object.
     */
    public Blueprint setFilePath(final Path path)
    {
        this.filePath = path;
        return this;
    }

    /**
     * Set the pack this blueprint is in.
     * @param packName the id of the pack.
     * @return this object.
     */
    public Blueprint setPackName(final String packName)
    {
        this.packName = packName;
        return this;
    }

    /**
     * Getter for the file path (does not contain the file name).
     * @return the file path of the structure.
     */
    public Path getFilePath()
    {
        return this.filePath;
    }

    /**
     * Get the pack name this belongs to.
     * @return the name of the pack.
     */
    public String getPackName()
    {
        return this.packName;
    }

    /**
     * @return an Array of all architects for this structure
     */
    public String[] getArchitects()
    {
        return this.architects;
    }

    /**
     * Sets an Array of all architects for this structure
     *
     * @param architects an array of architects.
     */
    public void setArchitects(final String[] architects)
    {
        this.architects = architects;
    }

    /**
     * @return An Array of all missing mods that are required to generate this structure (only works if structure was loaded from file)
     */
    public String[] getMissingMods()
    {
        return this.missingMods;
    }

    /**
     * Sets the missing mods
     *
     * @param missingMods the missing mods list.
     * @return this object.
     */
    public Blueprint setMissingMods(final String... missingMods)
    {
        this.missingMods = missingMods;
        return this;
    }

    /**
     * @return current rot/mir as relative to freshly loaded file
     */
    public RotationMirror getRotationMirror()
    {
        return rotationMirror;
    }

    /**
     * Get a list of all entities in the blueprint as a list.
     *
     * @return the list of CompoundNBTs.
     */
    public final List<CompoundTag> getEntitiesAsList()
    {
        return Arrays.stream(entities).collect(Collectors.toList());
    }

    /**
     * Get a list of all blockInfo objects in the blueprint.
     *
     * @return a list of all blockinfo (position, blockState, tileEntityData).
     */
    public final List<BlockInfo> getBlockInfoAsList()
    {
        if (cacheBlockInfo == null)
        {
            buildBlockInfoCaches();
        }
        return cacheBlockInfo;
    }

    /**
     * Get a map of all blockpos->blockInfo objects in the blueprint.
     *
     * @return a map of all blockpos->blockInfo (position, blockState, tileEntityData).
     */
    public final Map<BlockPos, BlockInfo> getBlockInfoAsMap()
    {
        if (cacheBlockInfoMap == null)
        {
            buildBlockInfoCaches();
        }
        return cacheBlockInfoMap;
    }

    /**
     * Get a map of all entities by approx position.
     *
     * @return the cached map of these.
     */
    public final Map<BlockPos, CompoundTag[]> getCachedEntitiesAsMap()
    {
        if (cacheEntitiesMap == null)
        {
            buildBlockInfoCaches();
        }
        return cacheEntitiesMap;
    }

    /**
     * Getter of the EntityInfo at a certain position.
     *
     * @param worldPos     the world position.
     * @param structurePos the position it will have in the structure.
     * @return the TE compound with real world coords.
     */
    @Nullable
    public CompoundTag getTileEntityData(final BlockPos worldPos, final BlockPos structurePos)
    {
        if (!getBlockInfoAsMap().containsKey(structurePos) || !getBlockInfoAsMap().get(structurePos).hasTileEntityData())
        {
            return null;
        }

        final CompoundTag te = getBlockInfoAsMap().get(structurePos).getTileEntityData().copy();
        final BlockPos tePos = structurePos.offset(worldPos);
        te.putInt("x", tePos.getX());
        te.putInt("y", tePos.getY());
        te.putInt("z", tePos.getZ());
        return te;
    }

    /**
     * Calculate the item needed to place the current block in the structure.
     * 
     * @param pos the pos its at.
     * @return an item or null if not initialized.
     */
    @Nullable
    public Item getItem(final BlockPos pos)
    {
        @Nullable
        final BlockInfo info = this.getBlockInfoAsMap().getOrDefault(pos, null);
        if (info == null || info.getState() == null || info.getState().isAir()
            || BlockUtils.isLiquidOnlyBlock(info.getState().getBlock()))
        {
            return null;
        }

        final ItemStack stack = BlockUtils.getItemStackFromBlockState(info.getState());

        if (!ItemStackUtils.isEmpty(stack))
        {
            return stack.getItem();
        }

        return null;
    }

    /**
     * Build the caches.
     */
    private void buildBlockInfoCaches()
    {
        cacheBlockInfo = new ArrayList<>(getVolume());
        cacheBlockInfoMap = new HashMap<>(getVolume());
        cacheEntitiesMap = new HashMap<>(getEntities().length);
        for (short y = 0; y < this.sizeY; y++)
        {
            for (short z = 0; z < this.sizeZ; z++)
            {
                for (short x = 0; x < this.sizeX; x++)
                {
                    final BlockPos tempPos = new BlockPos(x, y, z);
                    final BlockInfo blockInfo = new BlockInfo(tempPos, palette.get(structure[y][z][x] & 0xFFFF), tileEntities[y][z][x]);
                    cacheBlockInfo.add(blockInfo);
                    cacheBlockInfoMap.put(tempPos, blockInfo);
                    cacheEntitiesMap.put(tempPos,
                        Arrays.stream(this.getEntities())
                            .filter(data -> data != null && isAtPos(data, tempPos))
                            .toArray(CompoundTag[]::new));
                }
            }
        }
    }

    /**
     * Sets the primary offset for the blueprint
     *
     * @param cachePrimaryOffset the primary offset
     */
    public void setCachePrimaryOffset(final BlockPos cachePrimaryOffset)
    {
        this.cachePrimaryOffset = cachePrimaryOffset;
    }

    /**
     * Get the primary block offset.
     * 
     * @return the cached offset or a freshly calculated one.
     */
    public final BlockPos getPrimaryBlockOffset()
    {
        if (cachePrimaryOffset == null)
        {
            cachePrimaryOffset = findPrimaryBlockOffset();
        }
        return cachePrimaryOffset;
    }

    /**
     * Find the primary block offset and return it.
     * 
     * @return the offset.
     */
    private BlockPos findPrimaryBlockOffset()
    {
        final List<BlockInfo> list = getBlockInfoAsList().stream()
            .filter(blockInfo -> blockInfo.getState().getBlock() instanceof IAnchorBlock ||
                (blockInfo.hasTileEntityData() && blockInfo.getTileEntityData().contains(TAG_BLUEPRINTDATA)))
            .toList();

        if (list.size() != 1)
        {
            return new BlockPos(getSizeX() / 2, 0, getSizeZ() / 2);
        }
        return list.get(0).getPos();
    }

    /**
     * Reset the cache
     *
     * @param resetPrimaryOffset Reset the primary offset as well or not.
     */
    private void cacheReset(final boolean resetPrimaryOffset)
    {
        cacheBlockInfo = null;
        if (resetPrimaryOffset)
        {
            cachePrimaryOffset = null;
        }
        cacheBlockInfoMap = null;
        cacheEntitiesMap = null;
    }

    /**
     * Rotates and mirrors entire exactly to given param
     * 
     * @param rotationMirror exact rot/mir
     * @param level world for entity construction
     * @see #setRotationMirrorRelative(RotationMirror, Level) setRotationMirrorRelative for relative addition
     */
    public void setRotationMirror(final RotationMirror rotationMirror, final Level level)
    {
        setRotationMirrorRelative(this.rotationMirror.calcDifferenceTowards(rotationMirror), level);
    }

    /**
     * Rotates and mirrors entire content additively, formula:
     * current state + transformBy = target state
     * 
     * @param transformBy rot/mir to add
     * @param level world for entity construction
     * @see #setRotationMirror(RotationMirror, Level) setRotationMirror for exact setter
     */
    public void setRotationMirrorRelative(final RotationMirror transformBy, final Level level)
    {
        if (level.registryAccess() != registryAccess)
        {
            throw new IllegalStateException("World mismatch");
        }
        if (transformBy == RotationMirror.NONE)
        {
            return;
        }

        final DynamicOps<Tag> dynamicNbtOps = registryAccess.createSerializationContext(NbtOps.INSTANCE);
        final BlockPos primaryOffset = getPrimaryBlockOffset();
        final short newSizeX, newSizeZ, newSizeY = sizeY;

        switch (transformBy.rotation())
        {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                newSizeX = sizeZ;
                newSizeZ = sizeX;
                break;

            default:
                newSizeX = sizeX;
                newSizeZ = sizeZ;
                break;
        }

        final short[][][] newStructure = new short[newSizeY][newSizeZ][newSizeX];
        final CompoundTag[] newEntities = new CompoundTag[entities.length];
        final CompoundTag[][][] newTileEntities = new CompoundTag[newSizeY][newSizeZ][newSizeX];

        final List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < this.palette.size(); i++)
        {
            palette.add(i, transformBy.applyToBlockState(this.palette.get(i)));
        }

        final BlockPos extremes = transformBy.applyToPos(new BlockPos(sizeX, sizeY, sizeZ));
        int minX = extremes.getX() < 0 ? -extremes.getX() - 1 : 0;
        int minY = extremes.getY() < 0 ? -extremes.getY() - 1 : 0;
        int minZ = extremes.getZ() < 0 ? -extremes.getZ() - 1 : 0;

        this.palette = palette;

        for (short x = 0; x < this.sizeX; x++)
        {
            for (short y = 0; y < this.sizeY; y++)
            {
                for (short z = 0; z < this.sizeZ; z++)
                {
                    final short value = structure[y][z][x];
                    final BlockState state = palette.get(value & 0xFFFF);
                    if (state.getBlock() == Blocks.STRUCTURE_VOID)
                    {
                        continue;
                    }
                    final BlockPos tempPos = transformBy.applyToPos(new BlockPos(x, y, z)).offset(minX, minY, minZ);
                    newStructure[tempPos.getY()][tempPos.getZ()][tempPos.getX()] = value;

                    final CompoundTag compound = tileEntities[y][z][x];
                    if (compound != null)
                    {
                        compound.putInt("x", tempPos.getX());
                        compound.putInt("y", tempPos.getY());
                        compound.putInt("z", tempPos.getZ());

                        // TODO: ideally this would be generalised to any IRotatableBlockEntity or we would instead
                        //       reinflate the entity and use the Forge rotation method, but the latter requires a
                        //       Level with blockstate and entity and the former requires reinflating everything
                        //       before we can test whether it's rotatable or not, neither of which is ideal.  So
                        //       for now this is the minimal requirement.
                        if (compound.getString("id").equals(ModBlockEntities.TAG_SUBSTITUTION.getId().toString()))
                        {
                            CapturedBlock replacement = BlockEntityTagSubstitution.deserializeReplacement(compound, dynamicNbtOps);
                            replacement = replacement.applyRotationMirror(transformBy, level);
                            BlockEntityTagSubstitution.serializeReplacement(compound, dynamicNbtOps, replacement);
                        }

                        if (compound.contains(TAG_BLUEPRINTDATA))
                        {
                            CompoundTag dataCompound = compound.getCompound(TAG_BLUEPRINTDATA);

                            // Rotate tag map
                            final Map<BlockPos, List<String>> tagPosMap = IBlueprintDataProviderBE.readTagPosMapFrom(dataCompound);
                            final Map<BlockPos, List<String>> newTagPosMap = new HashMap<>();

                            for (Map.Entry<BlockPos, List<String>> entry : tagPosMap.entrySet())
                            {
                                newTagPosMap.put(transformBy.applyToPos(entry.getKey()), entry.getValue());
                            }

                            IBlueprintDataProviderBE.writeMapToCompound(dataCompound, newTagPosMap);

                            // Rotate corners
                            BlockPosUtil.writeToNBT(dataCompound, TAG_CORNER_ONE, transformBy.applyToPos(BlockPosUtil.readFromNBT(dataCompound, TAG_CORNER_ONE)));
                            BlockPosUtil.writeToNBT(dataCompound, TAG_CORNER_TWO, transformBy.applyToPos(BlockPosUtil.readFromNBT(dataCompound, TAG_CORNER_TWO)));
                        }
                    }
                    newTileEntities[tempPos.getY()][tempPos.getZ()][tempPos.getX()] = compound;
                }
            }
        }

        for (int i = 0; i < entities.length; i++)
        {
            final CompoundTag entitiesCompound = entities[i];
            if (entitiesCompound != null)
            {
                newEntities[i] = transformEntityInfoWithSettings(entitiesCompound, level, new BlockPos(minX, minY, minZ), transformBy);
            }
        }

        setCachePrimaryOffset(transformBy.applyToPos(primaryOffset).offset(minX, minY, minZ));

        sizeX = newSizeX;
        sizeY = newSizeY;
        sizeZ = newSizeZ;

        this.structure = newStructure;
        this.entities = newEntities;
        this.tileEntities = newTileEntities;
        this.rotationMirror = this.rotationMirror.add(transformBy);

        cacheReset(false);
    }

    /**
     * Transform an entity and rotate it.
     *
     * @param entityInfo the entity nbt.
     * @param world      the world.
     * @param pos        the position.
     * @param rotationMirror   the wanted rotation/mirror.
     * @return the updated nbt.
     */
    private static CompoundTag transformEntityInfoWithSettings(final CompoundTag entityInfo,
        final Level world,
        final BlockPos pos,
        final RotationMirror rotationMirror)
    {
        final Optional<EntityType<?>> type = EntityType.by(entityInfo);
        if (type.isPresent())
        {
            final Entity finalEntity = type.get().create(world);

            if (finalEntity != null)
            {
                try
                {
                    finalEntity.load(entityInfo);

                    final Vec3 entityVec = rotationMirror
                        .applyToPos(
                            finalEntity instanceof HangingEntity hang ? Vec3.atCenterOf(hang.getPos()) : finalEntity.position())
                        .add(Vec3.atLowerCornerOf(pos));
                    finalEntity.setYRot(finalEntity.mirror(rotationMirror.mirror()));
                    finalEntity.setYRot(finalEntity.rotate(rotationMirror.rotation()));
                    finalEntity.moveTo(entityVec.x, entityVec.y, entityVec.z, finalEntity.getYRot(), finalEntity.getXRot());

                    final CompoundTag newEntityInfo = new CompoundTag();
                    finalEntity.save(newEntityInfo);
                    return newEntityInfo;
                }
                catch (final Exception ex)
                {
                    Log.getLogger().error("Entity: " + type.get().getDescriptionId() + " failed to load. ", ex);
                    return null;
                }
            }
        }
        return null;
    }

    private int getVolume()
    {
        return (int) sizeX * sizeY * sizeZ;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
        result = prime * result + ((filePath == null) ? 0 : filePath.hashCode());
        result = prime * result + ((packName == null) ? 0 : packName.hashCode());
        result = prime * result + palleteSize;
        result = prime * result + entities.length;
        result = prime * result + tileEntities.length;
        result = prime * result + getVolume();
        // rot/mir intentionally not incluced
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (!(obj instanceof final Blueprint other))
        {
            return false;
        }
        return Objects.equals(name, other.name)
                 && Objects.equals(fileName, other.fileName)
                 && Objects.equals(filePath, other.filePath)
                 && Objects.equals(packName, other.packName)
                 && palleteSize == other.palleteSize
                 && entities.length == other.entities.length
                 && tileEntities.length == other.tileEntities.length
                 && getVolume() == other.getVolume();
        // rot/mir intentionally not incluced
    }

    /**
     * Get blueprint info at position.
     * 
     * @param pos             the position
     * @param includeEntities if entities should be included.
     * @return the info object.
     */
    public BlueprintPositionInfo getBluePrintPositionInfo(final BlockPos pos, final boolean includeEntities)
    {
        return new BlueprintPositionInfo(pos,
            getBlockInfoAsMap().get(pos),
            includeEntities ? getCachedEntitiesAsMap().getOrDefault(pos, new CompoundTag[0]) : new CompoundTag[0]);
    }

    /**
     * Check if an entityData object is at the local position.
     * 
     * @param entityData the data object to check.
     * @param pos        the pos to check.
     * @return true if so.
     */
    private static boolean isAtPos(final CompoundTag entityData, final BlockPos pos)
    {
        final ListTag list = entityData.getList(ENTITY_POS, 6);
        final int x = (int) list.getDouble(0);
        final int y = (int) list.getDouble(1);
        final int z = (int) list.getDouble(2);
        return new BlockPos(x, y, z).equals(pos);
    }

    /**
     * Get the blockstate at a pos.
     * 
     * @param pos the pos.
     * @return the blockstate.
     */
    public BlockState getBlockState(final BlockPos pos)
    {
        return getBlockInfoAsMap().get(pos).getState();
    }

    @Override
    public String toString()
    {
        return "Blueprint [size=[" + sizeX +
            ", " +
            sizeY +
            ", " +
            sizeZ +
            "], fileName=" +
            fileName +
            ", filePath=" +
            filePath +
            ", packName=" +
            packName +
            ", name=" +
            name +
            ", rotMir=" +
            rotationMirror +
            "]";
    }

    /**
     * @return blockEntity without world
     */
    @Override
    @javax.annotation.Nullable
    public BlockEntity getBlockEntity(final BlockPos pos)
    {
        return BlueprintUtils.constructTileEntity(getBlockInfoAsMap().get(pos), null, registryAccess);
    }

    @Override
    public int getHeight()
    {
        return getSizeY();
    }

    @Override
    public void describeSelfInCrashReport(final CrashReportCategory category)
    {
        // this should roughly match toString
        category.setDetail("Blueprint name", () -> name);
        category.setDetail("Blueprint pack name", () -> packName);
        category.setDetail("Blueprint file name", () -> fileName);
        category.setDetail("Blueprint file path", () -> filePath.toString());
        category.setDetail("Blueprint size", () -> "%d %d %d".formatted(sizeX, sizeY, sizeZ));
        category.setDetail("Blueprint rotation mirror", () -> rotationMirror.name());
    }

    public HolderLookup.Provider getRegistryAccess()
    {
        return registryAccess;
    }
}
