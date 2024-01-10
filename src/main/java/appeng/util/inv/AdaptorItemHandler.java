/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.util.inv;


import appeng.api.config.FuzzyMode;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Iterator;


public class AdaptorItemHandler extends InventoryAdaptor {
    protected final IItemHandler itemHandler;

    public AdaptorItemHandler(IItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    @Override
    public boolean hasSlots() {
        return this.itemHandler.getSlots() > 0;
    }

    private ItemStack removeItems(int amount, @Nullable ItemStack filter, @Nullable IInventoryDestination destination, boolean findFirstAcceptableItem, boolean simulateOnly) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        int slots = this.itemHandler.getSlots();

        ItemStack extracted = ItemStack.EMPTY;
        for (int slot = 0; slot < slots && amount > 0; slot++) {
            final ItemStack stack = this.itemHandler.getStackInSlot(slot);
            final int stackSize = stack.getCount();
            if (stack.isEmpty() || (filter != null && !filter.isEmpty() && !Platform.itemComparisons().isSameItem(stack, filter))) {
                continue;
            }

            int targetRemainder = destination == null ? stackSize : (int)destination.canInsertWithRemainder(stack);

            // If we have a destination inventory and we aren't able to insert into it
            if (targetRemainder == stackSize) {
                // Keep looking if the boolean indicator tells us we should
                if (findFirstAcceptableItem) {
                    continue;
                }
                // Otherwise just stop looking on this item
                break;
            }

            // Simulate the extraction first so we don't attempt to extract something that can't be extracted
            // If we have too many items, subtract off the stuff we can't send over, otherwise just send the amount
            final int amountToExtract = amount > targetRemainder ? amount - targetRemainder : amount;

            ItemStack simulatedExtract = this.itemHandler.extractItem(slot, amountToExtract, true);
            if (simulatedExtract.isEmpty()) {
                // Item handler tells us we can't extract the current item, so keep moving along
                continue;
            }

            ItemStack currentExtraction = simulatedExtract;
            if (!simulateOnly) {
                // If we're not simulating, go ahead and actually extract the item
                currentExtraction = this.itemHandler.extractItem(slot, amountToExtract, false);
                if (currentExtraction.isEmpty()) {
                    continue;
                }
            }


            if (extracted.isEmpty()) {
                // Use the first stack as a template for the result
                extracted = currentExtraction;
                filter = currentExtraction;
            } else {
                // Subsequent stacks will just increase the extracted size
                extracted.grow(currentExtraction.getCount());
            }
            amount -= currentExtraction.getCount();
        }

        return extracted;
    }

    @Override
    public ItemStack removeItems(int amount, @Nullable ItemStack filter, @Nullable IInventoryDestination destination, boolean findFirstAcceptableItem) {
        return removeItems(amount, filter, destination, findFirstAcceptableItem, false);
    }

    @Override
    public ItemStack simulateRemove(int amount, @Nullable ItemStack filter, IInventoryDestination destination, boolean findFirstAcceptableItem) {
        return removeItems(amount, filter, destination, findFirstAcceptableItem, true);
    }

    /**
     * For fuzzy extract, we will only ever extract one slot, since we're afraid of merging two item stacks with
     * different damage values.
     */
    @Override
    public ItemStack removeSimilarItems(int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination) {
        int slots = this.itemHandler.getSlots();
        ItemStack extracted = ItemStack.EMPTY;

        for (int slot = 0; slot < slots && extracted.isEmpty(); slot++) {
            final ItemStack is = this.itemHandler.getStackInSlot(slot);
            if (is.isEmpty() || (!filter.isEmpty() && !Platform.itemComparisons().isFuzzyEqualItem(is, filter, fuzzyMode))) {
                continue;
            }

            if (destination != null) {
                if (!destination.canInsert(is)) {
                    continue;
                }

                ItemStack simulated = this.itemHandler.extractItem(slot, amount, true);
                if (simulated.isEmpty()) {
                    continue;
                }
            }

            // Attempt extracting it
            extracted = this.itemHandler.extractItem(slot, amount, false);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }

        return extracted;
    }

    @Override
    public ItemStack simulateSimilarRemove(int amount, ItemStack filter, FuzzyMode fuzzyMode, IInventoryDestination destination) {
        int slots = this.itemHandler.getSlots();
        ItemStack extracted = ItemStack.EMPTY;

        for (int slot = 0; slot < slots && extracted.isEmpty(); slot++) {
            final ItemStack is = this.itemHandler.getStackInSlot(slot);
            if (is.isEmpty() || (!filter.isEmpty() && !Platform.itemComparisons().isFuzzyEqualItem(is, filter, fuzzyMode))) {
                continue;
            }

            if (destination != null && !destination.canInsert(is)) {
                continue;
            }

            // Attempt extracting it
            extracted = this.itemHandler.extractItem(slot, amount, true);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }

        return extracted;
    }

    @Override
    public ItemStack addItems(ItemStack toBeAdded) {
        return this.addItems(toBeAdded, false);
    }

    @Override
    public ItemStack simulateAdd(ItemStack toBeSimulated) {
        return this.addItems(toBeSimulated, true);
    }

    protected ItemStack addItems(ItemStack itemsToAdd, final boolean simulate) {
        if (itemsToAdd.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (int slot = 0; slot < this.itemHandler.getSlots(); slot++) {
            if (!simulate) {
                itemsToAdd = itemsToAdd.copy();
            }
            itemsToAdd = this.itemHandler.insertItem(slot, itemsToAdd, simulate);

            if (itemsToAdd.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        return itemsToAdd;
    }

    @Override
    public boolean containsItems() {
        int slots = this.itemHandler.getSlots();
        for (int slot = 0; slot < slots; slot++) {
            if (!this.itemHandler.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<ItemSlot> iterator() {
        return new ItemHandlerIterator(this.itemHandler);
    }

}
