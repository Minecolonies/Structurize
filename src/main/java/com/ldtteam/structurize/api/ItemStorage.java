package com.ldtteam.structurize.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Used to store an stack with various informations to compare items later on.
 */
public class ItemStorage
{
    /**
     * The stack to store.
     */
    private final ItemStack stack;

    /**
     * Set this to ignore the damage value in comparisons.
     */
    private final boolean shouldIgnoreDamageValue;

    /**
     * Set this to ignore the damage value in comparisons.
     */
    private final boolean shouldIgnoreNBTValue;

    /**
     * Amount of the storage.
     */
    private int amount;

    /**
     * Creates an instance of the storage.
     *
     * @param stack             the stack.
     * @param amount            the amount.
     * @param ignoreDamageValue should the damage value be ignored?
     */
    public ItemStorage(final ItemStack stack, final int amount, final boolean ignoreDamageValue)
    {
        this.stack = stack;
        this.shouldIgnoreDamageValue = ignoreDamageValue;
        this.shouldIgnoreNBTValue = ignoreDamageValue;
        this.amount = amount;
    }

    /**
     * Creates an instance of the storage.
     *
     * @param stack                the stack.
     * @param ignoreDamageValue    should the damage value be ignored?
     * @param shouldIgnoreNBTValue should the nbt value be ignored?
     */
    public ItemStorage(final ItemStack stack, final boolean ignoreDamageValue, final boolean shouldIgnoreNBTValue)
    {
        this.stack = stack;
        this.shouldIgnoreDamageValue = ignoreDamageValue;
        this.shouldIgnoreNBTValue = shouldIgnoreNBTValue;
    }

    /**
     * Creates an instance of the storage.
     *
     * @param stack             the stack.
     * @param ignoreDamageValue should the damage value be ignored?
     */
    public ItemStorage(final ItemStack stack, final boolean ignoreDamageValue)
    {
        this.stack = stack;
        this.shouldIgnoreDamageValue = ignoreDamageValue;
        this.shouldIgnoreNBTValue = ignoreDamageValue;
        this.amount = ItemStackUtils.getSize(stack);
    }

    /**
     * Creates an instance of the storage.
     *
     * @param stack the stack.
     */
    public ItemStorage(final ItemStack stack)
    {
        this.stack = stack;
        this.shouldIgnoreDamageValue = false;
        this.shouldIgnoreNBTValue = false;
        this.amount = ItemStackUtils.getSize(stack);
    }

    /**
     * Check a list for an ItemStack matching a predicate.
     *
     * @param list      the list to check.
     * @param predicate the predicate to test.
     * @return the matching stack or null if not found.
     */
    public static ItemStorage getItemStackOfListMatchingPredicate(final List<ItemStorage> list, final Predicate<ItemStack> predicate)
    {
        for (final ItemStorage stack : list)
        {
            if (predicate.test(stack.getItemStack()))
            {
                return stack;
            }
        }
        return null;
    }

    /**
     * Get the itemStack from this itemStorage.
     *
     * @return the stack.
     */
    public ItemStack getItemStack()
    {
        return stack;
    }

    /**
     * Getter for the quantity.
     *
     * @return the amount.
     */
    public int getAmount()
    {
        return this.amount;
    }

    /**
     * Setter for the quantity.
     *
     * @param amount the amount.
     */
    public void setAmount(final int amount)
    {
        this.amount = amount;
    }

    /**
     * Getter for the ignoreDamageValue.
     *
     * @return true if should ignore.
     */
    public boolean ignoreDamageValue()
    {
        return shouldIgnoreDamageValue;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(stack.getItem())
                + (this.shouldIgnoreDamageValue ? 0 : (this.stack.getDamageValue() * 31))
                + (this.shouldIgnoreNBTValue ? 0 : this.stack.getComponents().hashCode());
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final ItemStorage that = (ItemStorage) o;
        return ItemStackUtils.compareItemStacksIgnoreStackSize(that.getItemStack(), this.getItemStack(), !(this.shouldIgnoreDamageValue || that.shouldIgnoreDamageValue), !(this.shouldIgnoreNBTValue || that.shouldIgnoreNBTValue));
    }

    /**
     * Getter for the stack.
     *
     * @return the stack.
     */
        public Item getItem()
    {
        return stack.getItem();
    }

    /**
     * Getter for the damage value.
     *
     * @return the damage value.
     */
    public int getDamageValue()
    {
        return stack.getDamageValue();
    }

    /**
     * Adder for the quantity.
     *
     * @param amount the amount to be added.
     */
    public void addAmount(final int amount)
    {
        setAmount(getAmount() + amount);
    }
}