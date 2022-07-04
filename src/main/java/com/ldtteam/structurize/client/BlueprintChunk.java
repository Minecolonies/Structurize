package com.ldtteam.structurize.client;

import com.ldtteam.structurize.storage.rendering.RenderingCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData.BlockEntityTagOutput;
import net.minecraft.server.level.ChunkHolder.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.levelgen.Aquifer.FluidPicker;
import net.minecraft.world.level.levelgen.DensityFunctions.BeardifierOrMarker;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Blueprint simulated chunk.
 */
public class BlueprintChunk extends LevelChunk
{
    /**
     * The block access it gets.
     */
    private final BlueprintBlockAccess access;

    /**
     * Construct the element.
     * 
     * @param worldIn the blockAccess.
     * @param x       the chunk x.
     * @param z       the chunk z.
     */
    public BlueprintChunk(final BlueprintBlockAccess worldIn, final int x, final int z)
    {
        super(worldIn, new ChunkPos(x, z));
        this.access = worldIn;
    }

    @Override
    public BlockState getBlockState(final BlockPos pos)
    {
        return access.getBlockState(pos);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(final BlockPos pos, final EntityCreationType creationMode)
    {
        return access.getBlockEntity(pos);
    }

    @Override
    public FluidState getFluidState(final BlockPos pos)
    {
        return access.getFluidState(pos);
    }

    @Override
    public FluidState getFluidState(int bx, int by, int bz)
    {
        return getFluidState(new BlockPos(bx, by, bz));
    }

    @Override
    public BlueprintBlockAccess getLevel()
    {
        return access;
    }

    @Override
    public void addEntity(Entity entityIn)
    {
        // Noop
    }

    @Override
    public void setBlockEntityNbt(CompoundTag nbt)
    {
        // Noop
    }

    @Nullable
    @Override
    public StructureStart getStartForFeature(final ConfiguredStructureFeature<?, ?> feature)
    {
        // Noop
        return StructureStart.INVALID_START;
    }

    @NotNull
    @Override
    public LongSet getReferencesForFeature(final ConfiguredStructureFeature<?, ?> feature)
    {
        // Noop
        return new LongOpenHashSet();
    }

    @Override
    public void addReferenceForFeature(final ConfiguredStructureFeature<?, ?> feature, final long p_207947_)
    {
        // Noop
    }

    @Override
    public void setStartForFeature(final ConfiguredStructureFeature<?, ?> feature, final StructureStart start)
    {
        // Noop
    }

    @Override
    public CompoundTag getBlockEntityNbt(BlockPos pos)
    {
        // Noop - this is for pending BEs
        return null;
    }

    @Override
    public Heightmap getOrCreateHeightmapUnprimed(Types typeIn)
    {
        // Noop
        return null;
    }

    @Override
    public Collection<Entry<Types, Heightmap>> getHeightmaps()
    {
        // Noop
        return null;
    }

    @Override
    public long getInhabitedTime()
    {
        // Noop
        return 0;
    }

    @Override
    public FullChunkStatus getFullStatus()
    {
        // Noop (mostly related to loading and ticking - we do NOT want both)
        return FullChunkStatus.INACCESSIBLE;
    }

    @Override
    public LevelChunkSection[] getSections()
    {
        // Noop (we dont section)
        return null;
    }

    @NotNull
    @Override
    public ChunkStatus getStatus()
    {
        return ChunkStatus.FULL;
    }

    @NotNull
    @Override
    public Map<ConfiguredStructureFeature<?, ?>, LongSet> getAllReferences()
    {
        return Collections.emptyMap();
    }

    @NotNull
    @Override
    public Map<ConfiguredStructureFeature<?, ?>, StructureStart> getAllStarts()
    {
        return Collections.emptyMap();
    }

    @NotNull
    @Override
    public Set<BlockPos> getBlockEntitiesPos()
    {
        // Noop (to lazy to construct that - we build BEs lazily)
        // used just in chunk serializer
        return null;
    }

    @NotNull
    @Override
    public Map<BlockPos, BlockEntity> getBlockEntities()
    {
        // Noop (to lazy to construct that - we build BEs lazily)
        return Collections.emptyMap();
    }

    @Override
    public int getHeight(Types heightmapType, int x, int z)
    {
        // Noop
        return 0;
    }

    @Override
    public UpgradeData getUpgradeData()
    {
        // Noop
        return UpgradeData.EMPTY;
    }

    @Override
    public boolean isLightCorrect()
    {
        return true;
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean isUnsaved()
    {
        return false;
    }

    @Override
    public void runPostLoad()
    {
        // Noop
    }

    @Override
    public void postProcessGeneration()
    {
        // Noop
    }

    @Override
    public void removeBlockEntity(BlockPos pos)
    {
        // Noop
    }

    @Override
    public void unpackTicks(final long p_187986_)
    {
        // Noop
    }

    @Override
    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving)
    {
        // Noop
        return null;
    }

    @Override
    public void setHeightmap(Types type, long[] data)
    {
        // Noop
    }

    @Override
    public void setInhabitedTime(long newInhabitedTime)
    {
        // Noop
    }

    @Override
    public void setLightCorrect(boolean lightCorrectIn)
    {
        // Noop
    }

    @Override
    public void setLoaded(boolean loaded)
    {
        // Noop
    }

    @Override
    public void setFullStatus(Supplier<FullChunkStatus> locationTypeIn)
    {
        // Noop
    }

    @Override
    public void setUnsaved(boolean modified)
    {
        // Noop
    }

    @Override
    public void setAllReferences(final Map<ConfiguredStructureFeature<?, ?>, LongSet> p_201606_1_)
    {
        // Noop
    }

    @Override
    public void setAllStarts(final Map<ConfiguredStructureFeature<?, ?>, StructureStart> structureStartsIn)
    {
        // Noop
    }

    @Override
    public void invalidateCaps()
    {
        // Noop
    }

    @Override
    public void reviveCaps()
    {
        // Noop
    }

    @Override
    public void addPackedPostProcess(short packedPosition, int index)
    {
        // Noop
    }

    @Override
    public LevelChunkSection getHighestSection()
    {
        // Noop
        return null;
    }

    @Override
    public boolean isYSpaceEmpty(int startY, int endY)
    {
        return false;
    }

    @Override
    public void markPosForPostprocessing(BlockPos pos)
    {
        // Noop
    }

    @Override
    public int getLightEmission(BlockPos pos)
    {
        return RenderingCache.forceLightLevel() ? RenderingCache.getOurLightLevel() : super.getLightEmission(pos);
    }

    @Override
    public void addAndRegisterBlockEntity(BlockEntity p_156391_)
    {
        // Noop
    }

    @Override
    public boolean areCapsCompatible(CapabilityProvider<LevelChunk> other)
    {
        // Noop
        return false;
    }

    @Override
    public boolean areCapsCompatible(@Nullable CapabilityDispatcher other)
    {
        // Noop
        return false;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        // Noop
        return LazyOptional.empty();
    }

    @Override
    public GameEventDispatcher getEventDispatcher(int p_156372_)
    {
        // Noop
        return GameEventDispatcher.NOOP;
    }

    @Override
    public boolean isClientLightReady()
    {
        return true;
    }

    @Override
    public void registerAllBlockEntitiesAfterLevelLoad()
    {
        // Noop
    }

    @Override
    public void registerTickContainerInLevel(ServerLevel p_187959_)
    {
        // Noop
    }

    @Override
    public void replaceWithPacketData(FriendlyByteBuf p_187972_, CompoundTag p_187973_, Consumer<BlockEntityTagOutput> p_187974_)
    {
        // Noop
    }

    @Override
    public void setBlockEntity(BlockEntity p_156374_)
    {
        // Noop
    }

    @Override
    public void setClientLightReady(boolean p_196865_)
    {
        // Noop
    }

    @Override
    public void unregisterTickContainerFromLevel(ServerLevel p_187980_)
    {
        // Noop
    }

    @Override
    public void fillBiomesFromNoise(BiomeResolver p_187638_, Sampler p_187639_)
    {
        // Noop
    }

    @Override
    public Holder<Biome> getNoiseBiome(int p_204347_, int p_204348_, int p_204349_)
    {
        // Noop
        return null;
    }

    @Override
    public NoiseChunk getOrCreateNoiseChunk(NoiseRouter p_207938_,
        Supplier<BeardifierOrMarker> p_207939_,
        NoiseGeneratorSettings p_207940_,
        FluidPicker p_207941_,
        Blender p_207942_)
    {
        // Noop
        return null;
    }

    @Override
    public LevelChunkSection getSection(int p_187657_)
    {
        // Noop (we dont section)
        return null;
    }

    @Override
    public void incrementInhabitedTime(long p_187633_)
    {
        // Noop
    }

    @Override
    public boolean isOldNoiseGeneration()
    {
        // Noop
        return false;
    }

    @Override
    public boolean isUpgrading()
    {
        // Noop
        return false;
    }

    @Override
    public void setBlendingData(BlendingData p_187646_)
    {
        // Noop
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap)
    {
        // Noop
        return LazyOptional.empty();
    }

    @Override
    public BlockHitResult clip(ClipContext p_45548_)
    {
        // copied "on miss" code from super
        final Vec3 vec3 = p_45548_.getFrom().subtract(p_45548_.getTo());
        return BlockHitResult.miss(p_45548_.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), new BlockPos(p_45548_.getTo()));
    }

    @Override
    public BlockHitResult isBlockInLine(ClipBlockStateContext p_151354_)
    {
        // copied "on miss" code from super
        final Vec3 vec3 = p_151354_.getFrom().subtract(p_151354_.getTo());
        return BlockHitResult.miss(p_151354_.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), new BlockPos(p_151354_.getTo()));
    }

    @Override
    public int getSectionsCount()
    {
        // Noop (we dont section)
        return 0;
    }

    @Override
    public boolean isOutsideBuildHeight(int p_151563_)
    {
        // false cuz we're infinite world
        return false;
    }

    @Override
    public @Nullable BlockEntity getExistingBlockEntity(BlockPos pos)
    {
        // weird forge method
        return getBlockEntity(pos);
    }
}
