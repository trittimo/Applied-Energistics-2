package appeng.tile.inventory;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.DualityInterface;
import appeng.util.ConfigManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.Spliterator;
import java.util.function.Consumer;

public class AppEngInternalOversizedInventory extends AppEngInternalInventory {
    private final ConfigManager cm;
    private IMEInventory<IAEItemStack> network = null;
    private IActionSource networkSource = null;

    public AppEngInternalOversizedInventory(DualityInterface inventory, int numberOfStorageSlots, int maxStack, ConfigManager cm) {
        super(inventory, numberOfStorageSlots, maxStack);
        this.cm = cm;
    }

    public boolean needsNetwork() {
        return this.network == null || this.networkSource == null;
    }

    public void assignNetwork(IMEInventory<IAEItemStack> network, IActionSource networkSource) {
        this.network = network;
        this.networkSource = networkSource;
    }

    private ItemStack simulateNetworkInsert(ItemStack stack) {
        if (this.network == null || networkSource == null) {
            return stack;
        }
        final IAEItemStack out = this.network.injectItems(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(stack), Actionable.SIMULATE, networkSource);
        if (out == null) {
            return ItemStack.EMPTY;
        }
        return out.createItemStack();
    }

    private boolean shouldIgnoreNetwork() {
        return this.cm.getSetting(Settings.INTERFACE_ALWAYS_ALLOW_INSERTION) == YesNo.YES;
    }

    @Override
    @Nonnull
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (this.filter != null && !this.filter.allowInsert(this, slot, stack)) {
            return stack;
        }

        if (!simulate) {
            this.previousStack = this.getStackInSlot(slot).copy();
        }

        if (stack.isEmpty())
            return ItemStack.EMPTY;

        validateSlotIndex(slot);


        ItemStack existing = this.stacks.get(slot);

        int limit = maxStack[slot];


        if (!existing.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing)) {
                return stack;
            }

            limit -= existing.getCount();
        }

        if (limit <= 0)
            return stack;


        boolean reachedLimit = stack.getCount() > limit;
        ItemStack largestRemainder = reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;

        if (!shouldIgnoreNetwork()) {
            if (slot > 0) {
                // If we're checking anything other than slot 0, tell the caller we can't accept any items
                // Otherwise we could get into a weird scenario where it tries to fill the other slots with partial stacks
                return stack;
            }

            // We can't guarantee that if there are multiple different types of items in the interface that we will still
            // be able to insert items into the network, even if we could theoretically stack the incoming item with one
            // already existing in the interface. Easier to just tell the caller we can't accept items at all if the
            // interface has stuff in it, even if we could probably write a more complicated solve that would allow us to
            // check that the interface only has one type of item in it
            // The other option would be to create a simulated network insert that had a 'begin' and 'end' where
            // You can calculate what the network would look like after inserting multiple different kinds of stacks,
            // but that would probably be messy and not super performance friendly
            for (ItemStack currentStack : this.stacks) {
                if (currentStack != null && currentStack.getCount() > 0) {
                    return stack;
                }
            }

            ItemStack networkInsertRemainder = simulateNetworkInsert(stack);
            if (networkInsertRemainder.getCount() > largestRemainder.getCount()) { // The network can't some portion of the incoming items
                largestRemainder = networkInsertRemainder;
            }
        }

        if (largestRemainder.getCount() >= stack.getCount()) {
            // Can't insert anything - either network is full or the interface is
            return stack;
        }

        ItemStack finalStack = ItemHandlerHelper.copyStackWithSize(stack, existing.getCount() + stack.getCount() - largestRemainder.getCount());

        if (!simulate) {
            this.stacks.set(slot, finalStack);
            onContentsChanged(slot);
        }

        return finalStack.getCount() == 0 ? ItemStack.EMPTY : largestRemainder ;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (this.filter != null && !this.filter.allowExtract(this, slot, amount)) {
            return ItemStack.EMPTY;
        }

        if (!simulate) {
            this.previousStack = this.getStackInSlot(slot).copy();
        }

        if (amount == 0)
            return ItemStack.EMPTY;

        validateSlotIndex(slot);

        ItemStack existing = this.stacks.get(slot);

        if (existing.isEmpty())
            return ItemStack.EMPTY;

        if (existing.getCount() <= amount) {
            if (!simulate) {
                this.stacks.set(slot, ItemStack.EMPTY);
                onContentsChanged(slot);
            }
            return existing;
        } else {
            if (!simulate) {
                this.stacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - amount));
                onContentsChanged(slot);
            }

            return ItemHandlerHelper.copyStackWithSize(existing, amount);
        }
    }

    @Override
    public void forEach(Consumer<? super ItemStack> consumer) {
        super.forEach(consumer);
    }

    @Override
    public Spliterator<ItemStack> spliterator() {
        return super.spliterator();
    }
}
