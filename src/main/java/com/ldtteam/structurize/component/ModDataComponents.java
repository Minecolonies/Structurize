package com.ldtteam.structurize.component;

import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.items.AbstractItemWithPosSelector.PosSelection;
import com.ldtteam.structurize.items.ItemTagTool.TagData;
import com.ldtteam.structurize.util.ScanToolData;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents
{
    public static final DeferredRegister.DataComponents REGISTRY = DeferredRegister.createDataComponents(Constants.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PosSelection>> POS_SELECTION =
        savedSynced("pos_selection", PosSelection.CODEC, PosSelection.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TagData>> TAGS_DATA =
        savedSynced("tags", TagData.CODEC, TagData.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ScanToolData>> SCAN_TOOL =
        savedSynced("scan_tool", ScanToolData.CODEC, ScanToolData.STREAM_CODEC);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CapturedBlock>> CAPTURED_BLOCK =
        savedSynced("captured_block", CapturedBlock.CODEC, CapturedBlock.STREAM_CODEC);

    private static <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> savedSynced(final String name,
        final Codec<D> codec,
        final StreamCodec<RegistryFriendlyByteBuf, D> streamCodec)
    {
        return REGISTRY.register(name,
            () -> DataComponentType.<D>builder().persistent(codec).networkSynchronized(streamCodec).build());
    }
}
