package com.ldtteam.structurize.management;

import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.Structurize;
import com.ldtteam.structurize.operations.RedoOperation;
import com.ldtteam.structurize.operations.UndoOperation;
import com.ldtteam.structurize.placement.StructurePlacementUtils;
import com.ldtteam.structurize.util.BlockUtils;
import com.ldtteam.structurize.util.ChangeStorage;
import com.ldtteam.structurize.util.ITickedWorldOperation;
import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.api.Shape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

import static com.ldtteam.structurize.operations.UndoOperation.UNDO_PREFIX;

/**
 * Singleton class that links colonies to minecraft.
 */
public final class Manager
{
    /**
     * Indicate if a schematic have just been downloaded.
     * Client only
     */
    private static boolean schematicDownloaded = false;

    /**
     * List of the last changes to the world.
     */
    private static final Map<UUID, List<ChangeStorage>> changeQueue = new HashMap<>();

    /**
     * List of scanTool operations.
     */
    private static final LinkedList<ITickedWorldOperation> scanToolOperationPool = new LinkedList<>();

    private Manager()
    {
        //Hides default constructor.
    }

    /**
     * Method called on world tick to run cached operations.
     *
     * @param world the world which is ticking.
     */
    public static void onWorldTick(final ServerLevel world)
    {
        int count = 0;
        if (!scanToolOperationPool.isEmpty())
        {
            while (count++ <= Structurize.getConfig().getServer().maxOperationsPerTick.get())
            {
                final ITickedWorldOperation operation = scanToolOperationPool.peek();
                if (operation != null && operation.apply(world))
                {
                    scanToolOperationPool.pop();
                    if (!(operation instanceof UndoOperation || operation instanceof RedoOperation))
                    {
                        addToUndoRedoCache(operation.getChangeStorage());
                    }
                }
            }
        }
    }

    /**
     * Add a new item to the scanTool operation queue.
     *
     * @param operation the operation to add.
     */
    public static void addToQueue(final ITickedWorldOperation operation)
    {
        scanToolOperationPool.addLast(operation);
    }

    /**
     * Add a new item to the queue.
     *
     * @param storage the storage to add.
     */
    public static void addToUndoRedoCache(final ChangeStorage storage)
    {
        final List<ChangeStorage> storages = changeQueue.computeIfAbsent(storage.getPlayerID(), key -> new ArrayList<>());
        if (!storages.contains(storage))
        {
            storages.add(0, storage);
            if (storages.size() >= Structurize.getConfig().getServer().maxCachedChanges.get())
            {
                storages.remove(storages.size() - 1);
            }
        }
    }

    /**
     * Returns a list of the recently cached operations for this player
     *
     * @param player
     * @return
     */
    public static List<ChangeStorage> getChangeStoragesForPlayer(final UUID player)
    {
        return changeQueue.getOrDefault(player, new ArrayList<>());
    }

    /**
     * Paste a structure into the world.
     *
     * @param server         the server world.
     * @param pos            the position.
     * @param width          the width.
     * @param length         the length.
     * @param height         the height.
     * @param frequency      the frequency.
     * @param equation       the equation.
     * @param shape          the shape.
     * @param inputBlock     the input block.
     * @param inputFillBlock the fill block.
     * @param hollow         if hollow or not.
     * @param player         the player.
     * @param rotMir         the mirror and the rotation.
     */
    public static void pasteStructure(
      final ServerLevel server,
      final BlockPos pos,
      final int width,
      final int length,
      final int height,
      final int frequency,
      final String equation,
      final Shape shape,
      final ItemStack inputBlock,
      final ItemStack inputFillBlock,
      final boolean hollow,
      final ServerPlayer player,
      final RotationMirror rotMir,
      final HolderLookup.Provider provider)
    {
        final Blueprint blueprint = Manager.getStructureFromFormula(width, length, height, frequency, equation, shape, inputBlock, inputFillBlock, hollow, provider);
        StructurePlacementUtils.loadAndPlaceStructureWithRotation(server, blueprint, pos, rotMir, true, player);
    }

    /**
     * Just returns a cube for now, I can tinker this later.
     *
     * @param width          the width.
     * @param length         the length.
     * @param height         the height.
     * @param frequency      the frequency.
     * @param equation       the equation.
     * @param shape          the shape.
     * @param inputBlock     the input block.
     * @param inputFillBlock the fill block.
     * @param hollow         if hollow or not.
     * @return the new blueprint.
     */
    public static Blueprint getStructureFromFormula(
      final int width,
      final int length,
      final int height,
      final int frequency,
      final String equation,
      final Shape shape,
      final ItemStack inputBlock,
      final ItemStack inputFillBlock,
      final boolean hollow,
      final HolderLookup.Provider provider)
    {
        final Blueprint blueprint;
        final BlockState mainBlock = BlockUtils.getBlockStateFromStack(inputBlock, Blocks.GOLD_BLOCK.defaultBlockState());
        final BlockState fillBlock = BlockUtils.getBlockStateFromStack(inputFillBlock, Blocks.GOLD_BLOCK.defaultBlockState());

        if (shape == Shape.SPHERE || shape == Shape.HALF_SPHERE || shape == Shape.BOWL)
        {
            blueprint = generateSphere(height / 2, mainBlock, fillBlock, hollow, shape, provider);
        }
        else if (shape == Shape.CUBE)
        {
            blueprint = generateCube(height, width, length, mainBlock, fillBlock, hollow, provider);
        }
        else if (shape == Shape.WAVE)
        {
            blueprint = generateWave(height, width, length, frequency, mainBlock, true, provider);
        }
        else if (shape == Shape.WAVE_3D)
        {
            blueprint = generateWave(height, width, length, frequency, mainBlock, false, provider);
        }
        else if (shape == Shape.CYLINDER)
        {
            blueprint = generateCylinder(height, width, mainBlock, fillBlock, hollow, provider);
        }
        else if (shape == Shape.PYRAMID || shape == Shape.UPSIDE_DOWN_PYRAMID || shape == Shape.DIAMOND)
        {
            blueprint = generatePyramid(height, mainBlock, fillBlock, hollow, shape, provider);
        }
        else if (shape == Shape.CONE)
        {
            blueprint = generateCone(height, width, mainBlock, fillBlock, hollow, shape, provider);
        }
        else
        {
            blueprint = generateRandomShape(height, width, length, equation, mainBlock, provider);
        }
        return blueprint;
    }

    private static Blueprint generatePyramid(
      final int inputHeight,
      final BlockState block,
      final BlockState fillBlock,
      final boolean hollow,
      final Shape shape,
      final HolderLookup.Provider provider)
    {
        final int height = shape == Shape.DIAMOND ? inputHeight : inputHeight * 2;
        final int hHeight = height / 2;

        final Map<BlockPos, BlockState> posList = new HashMap<>();
        for (int y = 0; y < hHeight; y++)
        {
            for (int x = 0; x < hHeight; x++)
            {
                for (int z = 0; z < hHeight; z++)
                {
                    if (((x == z && x >= y) || (x == y && x >= z) || ((hollow ? y == z : y >= z) && y >= x)) && x * z <= y * y)
                    {
                        final BlockState blockToUse = x == z && x >= y || x == y || y == z ? block : fillBlock;
                        if (shape == Shape.UPSIDE_DOWN_PYRAMID || shape == Shape.DIAMOND)
                        {
                            addPosToList(new BlockPos(hHeight + x, y, hHeight + z), blockToUse, posList);
                            addPosToList(new BlockPos(hHeight + x, y, hHeight - z), blockToUse, posList);
                            addPosToList(new BlockPos(hHeight - x, y, hHeight + z), blockToUse, posList);
                            addPosToList(new BlockPos(hHeight - x, y, hHeight - z), blockToUse, posList);
                        }

                        if (shape == Shape.PYRAMID || shape == Shape.DIAMOND)
                        {
                            addPosToList(new BlockPos(hHeight + x, -y + height - (shape == Shape.DIAMOND ? 2 : inputHeight), hHeight + z), blockToUse, posList);
                            addPosToList(new BlockPos(hHeight + x, -y + height - (shape == Shape.DIAMOND ? 2 : inputHeight), hHeight - z), blockToUse, posList);
                            addPosToList(new BlockPos(hHeight - x, -y + height - (shape == Shape.DIAMOND ? 2 : inputHeight), hHeight + z), blockToUse, posList);
                            addPosToList(new BlockPos(hHeight - x, -y + height - (shape == Shape.DIAMOND ? 2 : inputHeight), hHeight - z), blockToUse, posList);
                        }
                    }
                }
            }
        }

        final Blueprint blueprint = new Blueprint((short) height, (short) (shape == Shape.DIAMOND ? height : inputHeight + 2), (short) height, provider);
        posList.forEach(blueprint::addBlockState);
        return blueprint;
    }

    private static Blueprint generateCone(
      final int inputHeight,
      final int width,
      final BlockState block,
      final BlockState fillBlock,
      final boolean hollow,
      final Shape shape,
      final HolderLookup.Provider provider)
    {
        final int height = shape == Shape.DIAMOND ? inputHeight : inputHeight * 2;
        final Map<BlockPos, BlockState> posList = new HashMap<>();
        for (int x = 0; x < width; x++)
        {
            for (int z = 0; z < width; z++)
            {
                for (int y = 0; y < height; y++)
                {
                    final int consideredWidth = (int) (width - y);
                    int sum = x * x + z * z;
                    final boolean shouldBeEmpty = sum > (consideredWidth * consideredWidth) / 4 - consideredWidth;
                    if (sum < (consideredWidth * consideredWidth) / 4 && (!hollow || shouldBeEmpty) && consideredWidth > 0)
                    {
                        final BlockState blockToUse = shouldBeEmpty ? block : fillBlock;
                        addPosToList(new BlockPos(width + x, y, width + z), blockToUse, posList);
                        addPosToList(new BlockPos(width + x, y, width - z), blockToUse, posList);
                        addPosToList(new BlockPos(width - x, y, width + z), blockToUse, posList);
                        addPosToList(new BlockPos(width - x, y, width - z), blockToUse, posList);
                    }
                }
            }
        }

        final Blueprint blueprint = new Blueprint((short) (width * 2), (short) height, (short) (width * 2), provider);
        posList.forEach(blueprint::addBlockState);
        return blueprint;
    }

    /**
     * Generates a cube with the specific size and adds it to the blueprint provided.
     *
     * @param height    the height.
     * @param width     the width.
     * @param length    the length
     * @param block     the block to use.
     * @param fillBlock the fill block.
     * @param hollow    if full.
     */
    private static Blueprint generateCube(
      final int height,
      final int width,
      final int length,
      final BlockState block,
      final BlockState fillBlock,
      final boolean hollow,
      final HolderLookup.Provider provider)
    {
        final Map<BlockPos, BlockState> posList = new HashMap<>();
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                for (int z = 0; z < length; z++)
                {
                    if ((x == 0 || x == width - 1) || (y == 0 || y == height - 1) || (z == 0 || z == length - 1))
                    {
                        posList.put(new BlockPos(x, y, z), block);
                    }
                    else if (!hollow)
                    {
                        posList.put(new BlockPos(x, y, z), fillBlock);
                    }
                }
            }
        }
        final Blueprint blueprint = new Blueprint((short) width, (short) height, (short) length, provider);
        posList.forEach(blueprint::addBlockState);
        return blueprint;
    }

    /**
     * Generates a hollow sphere with the specific size and adds it to the blueprint provided.
     *
     * @param height    the height.
     * @param block     the block to use.
     * @param fillBlock the fill block.
     * @param hollow    if hollow.
     * @param shape     the type of shape.
     */
    private static Blueprint generateSphere(
      final int height,
      final BlockState block,
      final BlockState fillBlock,
      final boolean hollow,
      final Shape shape,
      final HolderLookup.Provider provider)
    {
        final Map<BlockPos, BlockState> posList = new HashMap<>();
        for (int y = 0; y <= height + 1; y++)
        {
            for (int x = 0; x <= height + 1; x++)
            {
                for (int z = 0; z <= height + 1; z++)
                {
                    int sum = x * x + z * z + y * y;
                    if (sum < height * height && (!hollow || sum > height * height - 2 * height))
                    {
                        final BlockState blockToUse = (sum > height * height - 2 * height) ? block : fillBlock;
                        if (shape == Shape.HALF_SPHERE || shape == Shape.SPHERE)
                        {
                            addPosToList(new BlockPos(height + x, height + y, height + z), blockToUse, posList);
                            addPosToList(new BlockPos(height + x, height + y, height - z), blockToUse, posList);
                            addPosToList(new BlockPos(height - x, height + y, height + z), blockToUse, posList);
                            addPosToList(new BlockPos(height - x, height + y, height - z), blockToUse, posList);
                        }
                        if (shape == Shape.BOWL || shape == Shape.SPHERE)
                        {
                            addPosToList(new BlockPos(height + x, height - y, height + z), blockToUse, posList);
                            addPosToList(new BlockPos(height + x, height - y, height - z), blockToUse, posList);
                            addPosToList(new BlockPos(height - x, height - y, height + z), blockToUse, posList);
                            addPosToList(new BlockPos(height - x, height - y, height - z), blockToUse, posList);
                        }
                    }
                }
            }
        }

        final Blueprint blueprint = new Blueprint((short) ((height + 2) * 2), (short) ((height + 2) * 2), (short) ((height + 2) * 2), provider);
        posList.forEach(blueprint::addBlockState);
        return blueprint;
    }

    /**
     * Generates a cube with the specific size and adds it to the blueprint provided.
     *
     * @param height    the height.
     * @param width     the width.
     * @param block     the block to use.
     * @param fillBlock the fill block.
     * @param hollow    if full.
     */
    private static Blueprint generateCylinder(
      final int height,
      final int width,
      final BlockState block,
      final BlockState fillBlock,
      final boolean hollow,
      final HolderLookup.Provider provider)
    {
        final Map<BlockPos, BlockState> posList = new HashMap<>();
        for (int x = 0; x < width; x++)
        {
            for (int z = 0; z < width; z++)
            {
                for (int y = 0; y < height; y++)
                {
                    int sum = x * x + z * z;
                    final boolean shouldBeEmpty = sum > (width * width) / 4 - width;
                    if (sum < (width * width) / 4 && (!hollow || shouldBeEmpty))
                    {
                        final BlockState blockToUse = shouldBeEmpty ? block : fillBlock;
                        addPosToList(new BlockPos(width + x, y, width + z), blockToUse, posList);
                        addPosToList(new BlockPos(width + x, y, width - z), blockToUse, posList);
                        addPosToList(new BlockPos(width - x, y, width + z), blockToUse, posList);
                        addPosToList(new BlockPos(width - x, y, width - z), blockToUse, posList);
                    }
                }
            }
        }

        final Blueprint blueprint = new Blueprint((short) (width * 2), (short) height, (short) (width * 2), provider);
        posList.forEach(blueprint::addBlockState);
        return blueprint;
    }

    /**
     * Generates a wave with the specific size and adds it to the blueprint provided.
     *
     * @param height the height.
     * @param width  the width.
     * @param length the length.
     * @param block  the block to use.
     */
    private static Blueprint generateWave(
      final int height,
      final int width,
      final int length,
      final int frequency,
      final BlockState block,
      final boolean flat,
      final HolderLookup.Provider provider)
    {
        final Map<BlockPos, BlockState> posList = new HashMap<>();
        for (int x = 0; x < length; x++)
        {
            for (int z = 0; z < width; z++)
            {
                final double yVal = (flat ? 0 : z) + (double) frequency * Math.sin(x / (double) height);
                addPosToList(BlockPos.containing(x, yVal + frequency, (flat ? 0 : width) + z), block, posList);
                if (!flat)
                {
                    addPosToList(BlockPos.containing(x, yVal + frequency, width - z), block, posList);
                    addPosToList(BlockPos.containing(x, yVal + width - 1 + frequency, width + z - width + 1), block, posList);
                    addPosToList(BlockPos.containing(x, yVal + width - 1 + frequency, width - z + width - 1), block, posList);
                }
            }
        }

        final Blueprint blueprint = new Blueprint((short) length, (short) (frequency * 2 + 1 + (!flat ? width * 2 : 0)), (short) (width * 2 + 1), provider);
        posList.forEach(blueprint::addBlockState);
        return blueprint;
    }

    /**
     * Randomly generates shape based on an equation.
     *
     * @param height   the height.
     * @param width    the width.
     * @param length   the length.
     * @param equation the equation.
     * @param block    the block.
     * @return the created blueprint
     */
    public static Blueprint generateRandomShape(final int height, final int width, final int length, final String equation, final BlockState block, final HolderLookup.Provider provider)
    {
        /*Expression e = new Expression(equation);
        final Argument argumentX = new Argument("x = 0");
        final Argument argumentY = new Argument("y = 0");
        final Argument argumentZ = new Argument("z = 0");
        final Argument argumentH = new Argument("h = " + height);
        final Argument argumentW = new Argument("w = " + width);
        final Argument argumentL = new Argument("l = " + length);

        e.addArguments(argumentX, argumentY, argumentZ, argumentH, argumentW, argumentL);

        final Map<BlockPos, BlockState> posList = new HashMap<>();
        for (double x = -length / 2.0; x <= length / 2f; x++)
        {
            for (double y = -height / 2.0; y <= height / 2f; y++)
            {
                for (double z = -width / 2.0; z <= width / 2f; z++)
                {
                    argumentX.setArgumentValue(x);
                    argumentY.setArgumentValue(y);
                    argumentZ.setArgumentValue(z);
                    if (e.calculate() == 1)
                    {
                        addPosToList(new BlockPos(x + length / 2.0, y + height / 2.0, z + width / 2.0), block, posList);
                    }
                }
            }
        }
        final Blueprint blueprint = new Blueprint((short) (length + 1), (short) (height + 1), (short) (width + 1));
        posList.forEach(blueprint::addBlockState);
        return blueprint;*/
        return null;
    }

    /**
     * Add the position to list if not already.
     *
     * @param blockPos   the pos to add.
     * @param blockToUse the block to use.
     * @param posList    the list to add it to.
     */
    private static void addPosToList(final BlockPos blockPos, final BlockState blockToUse, final Map<BlockPos, BlockState> posList)
    {
        if (!posList.containsKey(blockPos))
        {
            posList.put(blockPos, blockToUse);
        }
    }

    /**
     * Undo a change to the world made by a player.
     *
     * @param player      the player who made it.
     * @param operationID
     */
    public static void undo(final Player player, final int operationID)
    {
        final List<ChangeStorage> list = changeQueue.get(player.getUUID());
        if (list == null || list.isEmpty())
        {
            player.displayClientMessage(Component.translatable("structurize.gui.undoredo.undo.notfound"), false);
            return;
        }

        for (final Iterator<ChangeStorage> iterator = list.iterator(); iterator.hasNext(); )
        {
            final ChangeStorage storage = iterator.next();
            if (storage.getID() == operationID)
            {
                if (!storage.isDone())
                {
                    player.displayClientMessage(Component.translatable("structurize.gui.undoredo.undo.inprogress", storage.getOperation()), false);
                    return;
                }

                player.displayClientMessage(Component.translatable("structurize.gui.undoredo.undo.add", storage.getOperation()), false);
                addToQueue(new UndoOperation(player, storage));
                if (storage.getOperation().toString().indexOf(UNDO_PREFIX) == 0)
                {
                    iterator.remove();
                }
                return;
            }
        }

        player.displayClientMessage(Component.translatable("structurize.gui.undoredo.undo.notfound"), false);
    }

    /**
     * Undo a change to the world made by a player.
     *
     * @param player      the player who made it.
     * @param operationID
     */
    public static void redo(final Player player, final int operationID)
    {
        final List<ChangeStorage> list = changeQueue.get(player.getUUID());
        if (list == null || list.isEmpty())
        {
            player.displayClientMessage(Component.translatable("structurize.gui.undoredo.redo.notfound"), false);
            return;
        }

        for (final ChangeStorage storage : list)
        {
            if (storage.getID() == operationID)
            {
                if (!storage.isDone())
                {
                    player.displayClientMessage(Component.translatable("structurize.gui.undoredo.redo.inprogress", storage.getOperation()), false);
                    return;
                }

                player.displayClientMessage(Component.translatable("structurize.gui.undoredo.redo.add", storage.getOperation()), false);
                addToQueue(new RedoOperation(player, storage));
                return;
            }
        }

        player.displayClientMessage(Component.translatable("structurize.gui.undoredo.redo.notfound"), false);
    }

    /**
     * Whether or not a new schematic have been downloaded.
     *
     * @return True if a new schematic have been received.
     */
    public static boolean isSchematicDownloaded()
    {
        return schematicDownloaded;
    }

    /**
     * Set the schematic downloaded
     *
     * @param downloaded True if a new schematic have been received.
     */
    public static void setSchematicDownloaded(final boolean downloaded)
    {
        schematicDownloaded = downloaded;
    }
}
