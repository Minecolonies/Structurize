package com.ldtteam.structurize.management;

import com.ldtteam.structurize.Structurize;
import com.ldtteam.structurize.api.util.Log;
import com.ldtteam.structurize.api.util.MathUtils;
import com.ldtteam.structurize.proxy.ClientProxy;
import com.ldtteam.structurize.util.StructureLoadingUtils;
import com.ldtteam.structurize.util.StructureUtils;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.Tuple;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static com.ldtteam.structurize.api.util.constant.Constants.SECONDS_A_MINUTE;
import static com.ldtteam.structurize.api.util.constant.Suppression.EXCEPTION_HANDLERS_SHOULD_PRESERVE_THE_ORIGINAL_EXCEPTIONS;

/**
 * StructureProxy class.
 */
public final class Structures
{
    /**
     * Extension used by the schematic files.
     */
    public static final String SCHEMATIC_EXTENSION_NEW = ".blueprint";

    /**
     * Storage location for the "normal" schematics.
     * In the jar file or on the local hard drive
     */
    public static final String SCHEMATICS_PREFIX = "schematics";

    /**
     * Storage location for the cached schematics.
     */
    public static final String SCHEMATICS_CACHE = "cache";

    /**
     * Storage location for the player's schematics.
     */
    public static final String SCHEMATICS_SCAN = "scans";

    /**
     * Schematic's path in the jar file.
     */
    public static final String SCHEMATICS_ASSET_PATH = "assets";

    /**
     * Schematic's path separator.
     */
    public static final String SCHEMATICS_SEPARATOR = "/";

    /**
     * Hashmap of schematic pieces by UUID.
     */
    private static final Map<UUID, Tuple<Long, Map<Integer, byte[]>>> schematicPieces = new HashMap<>();

    /**
     * Hut/Decoration, Styles, Levels.
     * This is populated on the client side only
     * Examples:
     * - schematics/stone/Builder1 => Builder -> stone -> Level 1 , huts/stone/Builder1
     * - schematics/walls/stone/Gate => decorations -> walls/stone -> Gate , decorations/walls/stone/Gate
     * - scans/458764687564687654 => scans -> <none> -> 458764687564687654 , scan/458764687564687654
     */
    @NotNull
    private static final Map<String, Map<String, Map<String, String>>> schematicsMap = new HashMap<>();

    /**
     * md5 hash for the schematics.
     * format is:
     * schematics/stone/builder1 -> hash
     * schematics/decoration/Well -> hash
     * scans/test/buidling -> hash
     * cache/458764687564687654 => 458764687564687654
     */
    @NotNull
    private static final Map<String, String> md5Map = new HashMap<>();

    /**
     * file extension for the schematics
     */
    @NotNull
    private static final Map<String, String> fileMap = new HashMap<>();

    /**
     * Whether or not the schematics list have changed.
     */
    private static boolean dirty = false;

    /**
     * Private constructor so Structures objects can't be made.
     */
    private Structures()
    {
        // Hide implicit public constructor.
    }

    /**
     * Calls {@link #loadStyleMaps()}.
     */
    public static void init()
    {
        loadStyleMaps();
    }

    /**
     * Loads all styles saved in ["/assets/structurize/schematics/"].
     * Puts these in {@link #md5Map}, with key being the fullname of the structure (schematics/stone/Builder1).
     */
    // The same exception will be triggered in the 2nd catch with logging this time.
    @SuppressWarnings(EXCEPTION_HANDLERS_SHOULD_PRESERVE_THE_ORIGINAL_EXCEPTIONS)
    private static void loadStyleMaps()
    {
        if (!Structurize.getConfig().getServer().ignoreSchematicsFromJar.get())
        {
            for (final Map.Entry<String, ModFileInfo> origin : StructureLoadingUtils.getOriginMods().entrySet())
            {
                final Path path = origin.getValue().getFile().getLocator().findPath(origin.getValue().getFile(), SCHEMATICS_ASSET_PATH, origin.getKey());
                Log.getLogger().info("Trying jar discover: {}", path.toString());
                loadSchematicsForPrefix(path, SCHEMATICS_PREFIX);
            }
        }

        final File schematicsFolder = Structurize.proxy.getSchematicsFolder();
        if (schematicsFolder != null)
        {
            Log.getLogger().info("Load additional huts or decorations from " + schematicsFolder + SCHEMATICS_SEPARATOR + SCHEMATICS_PREFIX);
            checkDirectory(schematicsFolder.toPath().resolve(SCHEMATICS_PREFIX).toFile());
            loadSchematicsForPrefix(schematicsFolder.toPath(), SCHEMATICS_PREFIX);
        }

        for (final File cachedSchems : StructureLoadingUtils.getCachedSchematicsFolders())
        {
            if (cachedSchems != null)
            {
                checkDirectory(cachedSchems);
                Log.getLogger().info("Load cached schematic from " + cachedSchems + SCHEMATICS_SEPARATOR + SCHEMATICS_CACHE);
                checkDirectory(cachedSchems.toPath().resolve(SCHEMATICS_CACHE).toFile());
                loadSchematicsForPrefix(cachedSchems.toPath(), SCHEMATICS_CACHE);
            }
        }

        if (md5Map.size() == 0)
        {
            Log.getLogger().warn("No file found during schematic discover. Things may break!");
        }
    }

    /**
     * Load all schematics from the scan folder.
     */
    @OnlyIn(Dist.CLIENT)
    public static void loadScannedStyleMaps()
    {
        if (!Structurize.getConfig().getServer().allowPlayerSchematics.get() && ServerLifecycleHooks.getCurrentServer() == null)
        {
            return;
        }

        schematicsMap.remove(SCHEMATICS_SCAN);

        for (final File clientSchems : StructureLoadingUtils.getClientSchematicsFolders())
        {
            checkDirectory(clientSchems.toPath().resolve(SCHEMATICS_SCAN).toFile());
            loadSchematicsForPrefix(clientSchems.toPath(), SCHEMATICS_SCAN);
        }
    }

    /**
     * check/create a directory and its parents.
     *
     * @param directory to be created
     */
    private static void checkDirectory(@NotNull final File directory)
    {
        if (!directory.exists() && !directory.mkdirs())
        {
            Log.getLogger().error("Directory doesn't exist and failed to be created: " + directory.toString());
        }
    }

    /**
     * Load all style maps from a certain path.
     * load all the schematics inside the folder path/prefix
     * and add them in the md5Map
     *
     * @param base     the base path.
     * @param prefix   either schematics, scans, cache
     */
    private static void loadSchematicsForPrefix(@NotNull final Path base, @NotNull final String prefix)
    {
        final Path basePath = base.toAbsolutePath();
        if (!Files.exists(basePath.resolve(prefix)))
        {
            return;
        }

        try (Stream<Path> walk = Files.walk(basePath.resolve(prefix)))
        {
            final Iterator<Path> it = walk.iterator();
            while (it.hasNext())
            {
                final Path path = it.next();
                if (path.toString().endsWith(SCHEMATIC_EXTENSION_NEW))
                {
                    String relativePath = basePath.relativize(path).toString();
                    relativePath = relativePath.substring(0, relativePath.length() - SCHEMATIC_EXTENSION_NEW.length());
                    if (!SCHEMATICS_SEPARATOR.equals(path.getFileSystem().getSeparator()))
                    {
                        relativePath = relativePath.replace(path.getFileSystem().getSeparator(), SCHEMATICS_SEPARATOR);
                    }
                    if (relativePath.startsWith(SCHEMATICS_SEPARATOR))
                    {
                        relativePath = relativePath.substring(1);
                    }

                    try
                    {
                        final StructureName structureName = new StructureName(relativePath);
                        fileMap.put(structureName.toString(), SCHEMATIC_EXTENSION_NEW);
                        final byte[] structureBytes = StructureLoadingUtils.getByteArray(relativePath);
                        final String md5 = StructureUtils.calculateMD5(structureBytes);
                        if (md5 == null)
                        {
                            fileMap.remove(structureName.toString());
                            Log.getLogger().error("Structures: " + structureName + " with md5 null.");
                        }
                        else if (isSchematicSizeValid(structureBytes))
                        {
                            md5Map.put(structureName.toString(), md5);
                            if (Structurize.proxy instanceof ClientProxy)
                            {
                                addSchematic(structureName);
                            }
                        }
                    }
                    catch (final ResourceLocationException e)
                    {
                        Log.getLogger()
                            .warn("Structure failed Loading because of invalid resource name (probably capitalization issue)", e);
                        Log.getLogger().warn(relativePath);
                    }
                }
            }
        }
        catch (@NotNull final IOException e)
        {
            Log.getLogger().warn("loadSchematicsForPrefix: Could not load schematics from " + basePath.resolve(prefix), e);
        }
    }

    /**
     * check that a schematic is not too big to be sent.
     *
     * @param structureData data of the structure to check for.
     * @return True when the schematic is not too big.
     */
    private static boolean isSchematicSizeValid(@NotNull final byte[] structureData)
    {
        final byte[] compressed = StructureUtils.compress(structureData);

        if (compressed == null)
        {
            Log.getLogger().warn("Compressed structure returned null, please retry, this shouldn't happen, ever.");
            return false;
        }
        return true;
    }

    /**
     * add a schematic in the schematicsMap.
     *
     * @param structureName the structure to add
     */
    @OnlyIn(Dist.CLIENT)
    private static void addSchematic(@NotNull final StructureName structureName)
    {
        if (structureName.getPrefix().equals(SCHEMATICS_CACHE))
        {
            return;
        }

        if (!schematicsMap.containsKey(structureName.getSection()))
        {
            schematicsMap.put(structureName.getSection(), new HashMap<>());
        }

        final Map<String, Map<String, String>> sectionMap = schematicsMap.get(structureName.getSection());
        if (!sectionMap.containsKey(structureName.getStyle()))
        {
            sectionMap.put(structureName.getStyle(), new TreeMap<>());
        }

        final Map<String, String> styleMap = sectionMap.get(structureName.getStyle());
        styleMap.put(structureName.getSchematic(), structureName.toString());
    }

    /**
     * return true if the schematics list have changed.
     *
     * @return True if dirty, otherwise false
     */
    public static boolean isDirty()
    {
        return dirty;
    }

    /**
     * mark Structures as not dirty.
     */
    public static void clearDirty()
    {
        dirty = false;
    }

    /**
     * Whether ot not the server allow players schematics.
     *
     * @return True if the server accept schematics otherwise False
     */
    @Deprecated // use config reference
    public static boolean isPlayerSchematicsAllowed()
    {
        return Structurize.getConfig().getServer().allowPlayerSchematics.get();
    }

    /**
     * rename a scanned structure.
     * rename the file and the md5 entry
     *
     * @param structureName the structure to rename
     * @param name          New name for the schematic as in style/schematicname
     * @return the new structureName
     */
    @OnlyIn(Dist.CLIENT)
    public static StructureName renameScannedStructure(@NotNull final StructureName structureName, @NotNull final String name)
    {
        if (!SCHEMATICS_SCAN.equals(structureName.getPrefix()))
        {
            Log.getLogger().warn("Renamed failed: Invalid name " + structureName);
            return null;
        }

        if (!hasMD5(structureName))
        {
            Log.getLogger().warn("Renamed failed: No MD5 hash found for " + structureName);
            return null;
        }

        final StructureName newStructureName = new StructureName(SCHEMATICS_SCAN + SCHEMATICS_SEPARATOR + name);

        if (!hasMD5(structureName))
        {
            Log.getLogger().warn("Renamed failed: File already exist " + newStructureName);
            return null;
        }

        for (final File clientSchems : StructureLoadingUtils.getClientSchematicsFolders())
        {
            final File structureFile = clientSchems.toPath()
                .resolve(structureName.toString() + getFileExtension(structureName.toString()))
                .toFile();
            final File newStructureFile = clientSchems.toPath()
                .resolve(newStructureName.toString() + getFileExtension(structureName.toString()))
                .toFile();
            checkDirectory(newStructureFile.getParentFile());
            if (structureFile.renameTo(newStructureFile))
            {
                final String md5 = getMD5(structureName.toString());
                md5Map.put(newStructureName.toString(), md5);
                md5Map.remove(structureName.toString());
                fileMap.put(newStructureName.toString(), fileMap.get(structureName.toString()));
                fileMap.remove(structureName.toString());
                Log.getLogger().info("Structure " + structureName + " have been renamed " + newStructureName);
                return newStructureName;
            }
            else
            {
                Log.getLogger().warn("Failed to rename structure from " + structureName + " to " + newStructureName);
                Log.getLogger().warn("Failed to rename structure from " + structureFile + " to " + newStructureFile);
            }
        }
        return null;
    }

    /**
     * check if a structure exist.
     *
     * @param structureName name of the structure as 'hut/wooden/Builder1'
     * @return the md5 hash or and empty String if not found
     */
    public static boolean hasMD5(@NotNull final StructureName structureName)
    {
        return hasMD5(structureName.toString());
    }

    /**
     * get the md5 hash for a structure name.
     *
     * @param structureName name of the structure as 'hut/wooden/Builder1'
     * @return the md5 hash String or null if not found
     */
    public static String getMD5(@NotNull final String structureName)
    {
        if (!md5Map.containsKey(structureName))
        {
            return null;
        }

        return md5Map.get(structureName);
    }

    public static boolean hasMD5(@NotNull final String structureName)
    {
        return md5Map.containsKey(structureName);
    }

    /**
     * delete a scanned structure.
     * delete the file and the md5 entry
     *
     * @param structureName the structure to delete
     * @return True if the structure have been deleted, False otherwise
     */
    @OnlyIn(Dist.CLIENT)
    public static boolean deleteScannedStructure(@NotNull final StructureName structureName)
    {
        if (!SCHEMATICS_SCAN.equals(structureName.getPrefix()))
        {
            Log.getLogger().warn("Delete failed: Invalid name " + structureName);
            return false;
        }

        if (!hasMD5(structureName))
        {
            Log.getLogger().warn("Delete failed: No MD5 hash found for " + structureName);
            return false;
        }

        for (final File clientSchems : StructureLoadingUtils.getClientSchematicsFolders())
        {
            final File structureFile = clientSchems.toPath().resolve(structureName.toString() + SCHEMATIC_EXTENSION_NEW).toFile();
            if (structureFile.delete())
            {
                md5Map.remove(structureName.toString());
                Log.getLogger().info("Structures: " + structureName + " deleted successfully");
                return true;
            }
            else
            {
                Log.getLogger().warn("Failed to delete structure " + structureName);
            }
        }
        return false;
    }

    /**
     * Get the list of Sections.
     * Builder, Citizen, Farmer ... + decorations and scans.
     *
     * @return list of sections.
     */
    @NotNull
    @OnlyIn(Dist.CLIENT)
    public static List<String> getSections()
    {
        final ArrayList<String> list = new ArrayList<>(schematicsMap.keySet());
        Collections.sort(list);
        return list;
    }

    /**
     * Get the list of styles for a given section.
     *
     * @param section such as decorations, Builder ...
     * @return the list of style for that section.
     */
    @NotNull
    @OnlyIn(Dist.CLIENT)
    public static List<String> getStylesFor(final String section)
    {
        if (schematicsMap.containsKey(section))
        {
            final Map<String, Map<String, String>> sectionMap = schematicsMap.get(section);
            return sectionMap.keySet().stream().filter(str -> !str.endsWith("/miner")).sorted().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Get a list of schematics for this section and style.
     *
     * @param section such as Builder, schematics. scans ...
     * @param style   limit the list for schematics to this style.
     * @return the list of schematics
     */
    @NotNull
    @OnlyIn(Dist.CLIENT)
    public static List<String> getSchematicsFor(final String section, final String style)
    {
        if (schematicsMap.containsKey(section))
        {
            final Map<String, Map<String, String>> sectionMap = schematicsMap.get(section);
            if (sectionMap.containsKey(style))
            {
                final ArrayList<String> list = new ArrayList<>(sectionMap.get(style).values());
                Collections.sort(list);
                return list;
            }
        }
        return Collections.emptyList();
    }

    /**
     * get a structure name for a give md5 hash.
     *
     * @param md5 hash identifying the schematic
     * @return the structure name as 'schematics/wooden/Builder1' or an empty String if not found
     */
    public static StructureName getStructureNameByMD5(final String md5)
    {
        if (md5 != null)
        {
            for (final Map.Entry<String, String> md5Entry : md5Map.entrySet())
            {
                if (md5Entry.getValue().equals(md5))
                {
                    return new StructureName(md5Entry.getKey());
                }
            }
        }

        return null;
    }

    /**
     * Get the file type of a corresponding structure name
     *
     * @param structureName identifying the structure
     * @return the file extension ('.nbt' or '.blueprint')
     */
    public static String getFileExtension(final String structureName)
    {
        if (!fileMap.containsKey(structureName))
        {
            return null;
        }

        return fileMap.get(structureName);
    }

    /**
     * Returns a map of all the structures.
     *
     * @return List of structure with their md5 hash.
     */
    public static Map<String, String> getMD5s()
    {
        return Structures.md5Map;
    }

    /**
     * For use on client side by the StructurizeStylesMessage.
     *
     * @param md5s new md5Map.
     */
    @OnlyIn(Dist.CLIENT)
    public static void setMD5s(final Map<String, String> md5s)
    {
        // First clear all section except scans
        schematicsMap.entrySet().removeIf(entry -> !entry.getKey().equals(SCHEMATICS_SCAN));

        // Then we update all mdp hash and fill the schematicsMap
        for (final Map.Entry<String, String> md5 : md5s.entrySet())
        {
            final StructureName sn = new StructureName(md5.getKey());
            if (!sn.getSection().equals(SCHEMATICS_SCAN))
            {
                md5Map.put(md5.getKey(), md5.getValue());
                addSchematic(sn);
            }
        }
    }

    /**
     * Handle a schematic which has been cut into pieces.
     * This method is valid on the server
     * The schematic will be gathered until all pieces have been put together and then handled like on the client.
     *
     * @param bytes  representing the schematic.
     * @param id     UUID.
     * @param piece  the piece.
     * @param pieces the amount of pieces.
     * @return true if successful.
     */
    public static boolean handleSaveSchematicMessage(final byte[] bytes, final UUID id, final int pieces, final int piece)
    {
        for (final Map.Entry<UUID, Tuple<Long, Map<Integer, byte[]>>> entry : new HashSet<>(schematicPieces.entrySet()))
        {
            if (MathUtils.nanoSecondsToSeconds(System.nanoTime() - entry.getValue().getA()) > SECONDS_A_MINUTE)
            {
                schematicPieces.remove(entry.getKey());
                Log.getLogger().warn("Waiting too long for piece of structure, discarding it");
            }
        }

        if (pieces == 1)
        {
            return Structures.handleSaveSchematicMessage(bytes);
        }
        else
        {
            if (!canStoreNewSchematic())
            {
                Log.getLogger().warn("Could not store schematic in cache");
                return false;
            }
            Log.getLogger()
                .info("Recieved piece: " + piece + " of: " + pieces + " with the size: " + bytes.length + " and ID: " + id.toString());
            final Map<Integer, byte[]> schemPieces;
            if (schematicPieces.containsKey(id))
            {
                final Tuple<Long, Map<Integer, byte[]>> schemTuple = schematicPieces.remove(id);
                schemPieces = schemTuple.getB();

                if (MathUtils.nanoSecondsToSeconds(System.nanoTime() - schemTuple.getA()) > SECONDS_A_MINUTE)
                {
                    Log.getLogger().warn("Waiting too long for piece: " + piece);
                    return false;
                }

                if (schemPieces.containsKey(piece))
                {
                    Log.getLogger().warn("Already had piece: " + piece);
                    return false;
                }

                schemPieces.put(piece, bytes);

                if (schemPieces.size() == pieces)
                {
                    try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
                    {
                        schemPieces.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                            try
                            {
                                outputStream.write(entry.getValue());
                            }
                            catch (final IOException e)
                            {
                                Log.getLogger().error("Error combining byte arrays of schematic pieces.", e);
                            }
                        });

                        return Structures.handleSaveSchematicMessage(outputStream.toByteArray());
                    }
                    catch (final IOException e)
                    {
                        Log.getLogger().error("Error combining byte arrays of schematic pieces.", e);
                        return false;
                    }
                }
            }
            else
            {
                schemPieces = new HashMap<>();
                schemPieces.put(piece, bytes);
            }
            schematicPieces.put(id, new Tuple<>(System.nanoTime(), schemPieces));
            return true;
        }
    }

    /**
     * Save a schematic in the cache.
     * This method is valid on the client and server
     * The schematic will be save under the cache directory using is md5 hash as a name.
     *
     * @param bytes representing the schematic.
     * @return True is the schematic have been saved successfully.
     */
    public static boolean handleSaveSchematicMessage(final byte[] bytes)
    {
        if (!canStoreNewSchematic())
        {
            Log.getLogger().warn("Could not store schematic in cache");
            return false;
        }

        final String md5 = StructureUtils.calculateMD5(bytes);
        if (md5 != null)
        {
            Log.getLogger().info("Structures.handleSaveSchematicMessage: received new schematic md5:" + md5);
            for (final File cachedSchems : StructureLoadingUtils.getCachedSchematicsFolders())
            {
                final File schematicFile = cachedSchems.toPath()
                    .resolve(SCHEMATICS_CACHE + SCHEMATICS_SEPARATOR + md5 + SCHEMATIC_EXTENSION_NEW)
                    .toFile();
                checkDirectory(schematicFile.getParentFile());
                try (OutputStream outputstream = new FileOutputStream(schematicFile))
                {
                    outputstream.write(bytes);
                    Structures.addMD5ToCache(md5);
                    Manager.setSchematicDownloaded(true);
                    fileMap.put(SCHEMATICS_CACHE + SCHEMATICS_SEPARATOR + md5, SCHEMATIC_EXTENSION_NEW);
                    return true;
                }
                catch (@NotNull final IOException e)
                {
                    Log.getLogger().warn("Exception while trying to save a schematic.", e);
                }
            }
        }
        else
        {
            Log.getLogger().info("Structures.handleSaveSchematicMessage: Could not calculate the MD5 hash");
            return false;
        }

        return false;
    }

    /**
     * check that we can store the schematic.
     * According to the total number of schematic allowed on the server
     *
     * @return true if we can store more schematics
     */
    private static boolean canStoreNewSchematic()
    {
        if (Structurize.proxy instanceof ClientProxy)
        {
            return true;
        }
        if (!Structurize.getConfig().getServer().allowPlayerSchematics.get())
        {
            return false;
        }

        final int maxCachedSchematics = Structurize.getConfig().getServer().maxCachedSchematics.get();

        final Set<String> md5Set = getCachedMD5s();
        if (md5Set.size() < maxCachedSchematics)
        {
            return true;
        }

        // md5Set contain only the unused one
        final Iterator<String> iterator = md5Set.iterator();
        while (iterator.hasNext() && md5Set.size() >= maxCachedSchematics)
        {
            final StructureName sn = new StructureName(iterator.next());
            if (deleteCachedStructure(sn))
            {
                iterator.remove();
            }
        }

        return md5Set.size() < maxCachedSchematics;
    }

    /**
     * add the md5 as a known structure in cache.
     *
     * @param md5 hash of the structure
     */
    public static void addMD5ToCache(@NotNull final String md5)
    {
        markDirty();
        md5Map.put(Structures.SCHEMATICS_CACHE + SCHEMATICS_SEPARATOR + md5, md5);
    }

    /**
     * get the set of cached schematic.
     */
    private static Set<String> getCachedMD5s()
    {
        final Set<String> md5Set = new HashSet<>();
        for (final Map.Entry<String, String> md5 : md5Map.entrySet())
        {
            final StructureName sn = new StructureName(md5.getKey());
            if (sn.getSection().equals(SCHEMATICS_CACHE))
            {
                md5Set.add(md5.getKey());
            }
        }
        return md5Set;
    }

    /**
     * delete a cached structure.
     * delete the file and the md5 entry
     *
     * @param structureName the structure to delete
     * @return True if the structure have been deleted, False otherwise
     */
    private static boolean deleteCachedStructure(@NotNull final StructureName structureName)
    {
        if (!SCHEMATICS_CACHE.equals(structureName.getPrefix()))
        {
            Log.getLogger().warn("Delete failed: Invalid name " + structureName);
            return false;
        }

        if (!hasMD5(structureName))
        {
            Log.getLogger().warn("Delete failed: No MD5 hash found for " + structureName);
            return false;
        }

        final File structureFileBlueprint = Structurize.proxy.getSchematicsFolder()
            .toPath()
            .resolve(structureName.toString() + SCHEMATIC_EXTENSION_NEW)
            .toFile();
        if (structureFileBlueprint.delete())
        {
            md5Map.remove(structureName.toString());
            fileMap.remove(structureName.toString());
            return true;
        }
        else
        {
            Log.getLogger().warn("Failed to delete structure " + structureName);
        }

        return false;
    }

    /**
     * mark Structures as dirty.
     */
    private static void markDirty()
    {
        dirty = true;
    }
}
