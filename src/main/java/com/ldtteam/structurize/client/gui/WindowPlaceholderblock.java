package com.ldtteam.structurize.client.gui;

import com.google.common.collect.ImmutableList;
import com.ldtteam.blockout.Color;
import com.ldtteam.blockout.Pane;
import com.ldtteam.blockout.controls.*;
import com.ldtteam.blockout.views.ScrollingList;
import com.ldtteam.blockout.views.Window;
import com.ldtteam.structurize.Network;
import com.ldtteam.structurize.api.util.ItemStackUtils;
import com.ldtteam.structurize.api.util.constant.Constants;
import com.ldtteam.structurize.blocks.interfaces.IBlueprintDataProvider;
import com.ldtteam.structurize.network.messages.UpdatePlaceholderBlockMessage;
import com.ldtteam.structurize.tileentities.TileEntityPlaceholder;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.ldtteam.structurize.api.util.constant.WindowConstants.*;

/**
 * Window for the replace block GUI.
 */
public class WindowPlaceholderblock extends Window implements ButtonHandler
{
    private static final String BUTTON_DONE          = "done";
    private static final String BUTTON_CANCEL        = "cancel";
    private static final String BUTTON_REMOVE_TAG    = "removeTag";
    private static final String INPUT_NAME           = "name";
    private static final String WINDOW_REPLACE_BLOCK = ":gui/windowreplaceblock.xml";
    private static final String BUTTON_TAG           = "addTag";
    private static final String TAG_LABEL_NAME       = "tagname";

    /**
     * The stack to replace.
     */
    private ItemStack from;

    /**
     * The position.
     */
    private final BlockPos pos;

    /**
     * White color.
     */
    private static final int WHITE     = Color.getByName("white", 0);

    /**
     * List of all item stacks in the game.
     */
    private final List<ItemStack> allItems = new ArrayList<>();

    /**
     * Resource scrolling list.
     */
    private final ScrollingList resourceList;

    /**
     * Resource tag list.
     */
    private final ScrollingList tagList;

    /**
     * The list of tag strings
     */
    private List<String> tagStringList = new ArrayList<>();

    /**
     * The filter for the resource list.
     */
    private String filter = "";

    /**
     * Create the replacement GUI.
     * @param pos the pos.
     */
    public WindowPlaceholderblock(final BlockPos pos)
    {
        super(Constants.MOD_ID + WINDOW_REPLACE_BLOCK);

        final TileEntityPlaceholder te = (TileEntityPlaceholder) Minecraft.getInstance().world.getTileEntity(pos);
        if (te != null)
        {
            this.from = te.getStack();
            this.tagStringList = ((IBlueprintDataProvider) te).getPositionedTags().get(pos);
        }

        this.pos = pos;
        resourceList = findPaneOfTypeByID(LIST_RESOURCES, ScrollingList.class);
        tagList = findPaneOfTypeByID(LIST_TAGS, ScrollingList.class);
    }

    @Override
    public void onOpened()
    {
        if (this.from == null)
        {
            close();
        }

        findPaneOfTypeByID("resourceIconFrom", ItemIcon.class).setItem(from);
        findPaneOfTypeByID("resourceNameFrom", Label.class).setLabelText((IFormattableTextComponent) from.getDisplayName());
        findPaneOfTypeByID("resourceIconTo", ItemIcon.class).setItem(new ItemStack(Blocks.AIR));
        findPaneOfTypeByID("resourceNameTo", Label.class).setLabelText((IFormattableTextComponent) new ItemStack(Blocks.AIR).getDisplayName());
        findPaneOfTypeByID("taglistname", Label.class).setLabelText("Tags:");
        updateResources();
        updateTags();
    }

    private void updateResources()
    {
        allItems.clear();
        allItems.addAll(ImmutableList.copyOf(StreamSupport.stream(Spliterators.spliteratorUnknownSize(ForgeRegistries.ITEMS.iterator(), Spliterator.ORDERED), false)
            .map(ItemStack::new)
            .filter(stack -> (stack.getItem() instanceof BlockItem) && (filter.isEmpty() || stack.getTranslationKey().toLowerCase(Locale.US).contains(filter.toLowerCase(Locale.US)))).collect(Collectors.toList())));

        final List<ItemStack> specialBlockList = new ArrayList<>();
        specialBlockList.add(new ItemStack(Items.WATER_BUCKET));
        specialBlockList.add(new ItemStack(Items.LAVA_BUCKET));
        specialBlockList.add(new ItemStack(Items.MILK_BUCKET));

        allItems.addAll(specialBlockList.stream().filter(
                stack -> filter.isEmpty()
                        || stack.getTranslationKey().toLowerCase(Locale.US).contains(filter.toLowerCase(Locale.US))
                        || stack.getDisplayName().getString().toLowerCase(Locale.US).contains(filter.toLowerCase(Locale.US)))
                .collect(Collectors.toList()));
        updateResourceList();
    }

    @Override
    public boolean onKeyTyped(final char ch, final int key)
    {
        final boolean result = super.onKeyTyped(ch, key);
        final String name = findPaneOfTypeByID(INPUT_NAME, TextField.class).getText();
        if (!name.isEmpty())
        {
            filter = name;
        }
        updateResources();
        return result;
    }

    @Override
    public void onButtonClicked(@NotNull final Button button)
    {
        switch (button.getID())
        {
            case BUTTON_DONE:
            {
                ItemStack to = findPaneOfTypeByID("resourceIconTo", ItemIcon.class).getItem();
                if (ItemStackUtils.isEmpty(to))
                {
                    to = from;
                }

                    final TileEntityPlaceholder te = (TileEntityPlaceholder) Minecraft.getInstance().world.getTileEntity(pos);
                    if (te != null)
                    {
                        te.setStack(to);
                        ((IBlueprintDataProvider) te).getPositionedTags().put(pos, tagStringList);
                        Network.getNetwork().sendToServer(new UpdatePlaceholderBlockMessage(pos, to, tagStringList));
                        close();
                    }
                break;
            }
            case BUTTON_CANCEL:
                close();
                break;
            case BUTTON_SELECT:
            {
                final int row = resourceList.getListElementIndexByPane(button);
                final ItemStack to = allItems.get(row);
                findPaneOfTypeByID("resourceIconTo", ItemIcon.class).setItem(to);
                findPaneOfTypeByID("resourceNameTo", Label.class).setLabelText((IFormattableTextComponent) to.getDisplayName());
                break;
            }
            case BUTTON_TAG:
            {
                new WindowAddTag(this).open();
                break;
            }
            case BUTTON_REMOVE_TAG:
            {
                final int row = tagList.getListElementIndexByPane(button);
                tagStringList.remove(row);
                break;
            }
        }
    }

    public void updateResourceList()
    {
        resourceList.enable();
        resourceList.show();
        final List<ItemStack> tempRes = new ArrayList<>(allItems);

        //Creates a dataProvider for the unemployed resourceList.
        resourceList.setDataProvider(new ScrollingList.DataProvider()
        {
            /**
             * The number of rows of the list.
             * @return the number.
             */
            @Override
            public int getElementCount()
            {
                return tempRes.size();
            }

            /**
             * Inserts the elements into each row.
             * @param index the index of the row/list element.
             * @param rowPane the parent Pane for the row, containing the elements to update.
             */
            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final ItemStack resource = tempRes.get(index);
                final Label resourceLabel = rowPane.findPaneOfTypeByID(RESOURCE_NAME, Label.class);
                resourceLabel.setLabelText((IFormattableTextComponent) resource.getDisplayName());
                resourceLabel.setColor(WHITE, WHITE);
                rowPane.findPaneOfTypeByID(RESOURCE_ICON, ItemIcon.class).setItem(resource);
            }
        });
    }

    /**
     * Updates the tag list shown with data
     */
    public void updateTags()
    {
        tagList.enable();
        tagList.show();

        //Creates a dataProvider for the unemployed resourceList.
        tagList.setDataProvider(new ScrollingList.DataProvider()
        {
            /**
             * The number of rows of the list.
             * @return the number.
             */
            @Override
            public int getElementCount()
            {
                return tagStringList.size();
            }

            /**
             * Inserts the elements into each row.
             * @param index the index of the row/list element.
             * @param rowPane the parent Pane for the row, containing the elements to update.
             */
            @Override
            public void updateElement(final int index, @NotNull final Pane rowPane)
            {
                final Label tagLabel = rowPane.findPaneOfTypeByID(TAG_LABEL_NAME, Label.class);
                tagLabel.setLabelText(tagStringList.get(index));
                tagLabel.setColor(WHITE, WHITE);
            }
        });
    }

    /**
     * Adds a string nbt tag to the window
     *
     * @param tag String to add
     */
    public void addTag(final String tag)
    {
        tagStringList.add(tag);
    }
}
