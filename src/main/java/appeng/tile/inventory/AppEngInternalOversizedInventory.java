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
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.filter.IAEItemFilter;
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

    public void assignNetwork(IMEInventory<IAEItemStack> network, IActionSource networkSource) {
        this.network = network;
        this.networkSource = networkSource;
    }

    private boolean canInsertIntoNetwork(ItemStack stack) {
        if (this.network == null || networkSource == null) {
            return false;
        }
        final IAEItemStack out = this.network.injectItems(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(stack), Actionable.SIMULATE, networkSource);
        if (out == null) {
            return true;
        }
        return out.getStackSize() != stack.getCount();
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

        if (!shouldIgnoreNetwork() && !canInsertIntoNetwork(stack)) {
            return stack;
        }

        ItemStack existing = this.stacks.get(slot);

        int limit = maxStack[slot];

        if (!existing.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
                return stack;

            limit -= existing.getCount();
        }

        if (limit <= 0)
            return stack;

        boolean reachedLimit = stack.getCount() > limit;

        if (!simulate) {
            if (existing.isEmpty()) {
                this.stacks.set(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            onContentsChanged(slot);
        }

        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;
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
