package crazypants.enderio.machine.gui;

import java.awt.Point;

import javax.annotation.Nonnull;

import com.enderio.core.api.common.util.IProgressTile;
import com.enderio.core.common.ContainerEnder;
import com.enderio.core.common.util.Util;

import crazypants.enderio.machine.base.container.SlotRangeHelper;
import crazypants.enderio.machine.base.container.SlotRangeHelper.IRangeProvider;
import crazypants.enderio.machine.base.container.SlotRangeHelper.SlotRange;
import crazypants.enderio.machine.baselegacy.AbstractInventoryMachineEntity;
import crazypants.enderio.machine.baselegacy.SlotDefinition;
import crazypants.util.Prep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public abstract class AbstractMachineContainer<E extends AbstractInventoryMachineEntity> extends ContainerEnder<IInventory> implements IRangeProvider {

  protected Slot upgradeSlot;
  protected final @Nonnull E te;
  
  protected SlotRangeHelper<? extends AbstractMachineContainer<E>> rangeHelper = new SlotRangeHelper.Legacy<>(this);

  public AbstractMachineContainer(@Nonnull InventoryPlayer playerInv, @Nonnull E te) {
    super(playerInv, te.getAsInventory());
    this.te = te;
  }

  public E getTe() {
    return te;
}

@Override
  protected void addSlots(@Nonnull InventoryPlayer playerInv) {
    addMachineSlots(playerInv);

    if (te.getSlotDefinition().getNumUpgradeSlots() == 1) {
      addSlotToContainer(upgradeSlot = new Slot(getInv(), te.getSlotDefinition().getMinUpgradeSlot(), getUpgradeOffset().x, getUpgradeOffset().y) {

        @Override
        public int getSlotStackLimit() {
          return 1;
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack itemStack) {
          return te.isItemValidForSlot(te.getSlotDefinition().getMinUpgradeSlot(), itemStack);
        }
      });
    }
  }

  @Override
  public @Nonnull Point getPlayerInventoryOffset() {
    return new Point(8, 84);
  }

  @Override
  public @Nonnull Point getUpgradeOffset() {
    return new Point(12, 60);
  }

  public Slot getUpgradeSlot() {
    return upgradeSlot;
  }

  /**
   * ATTN: Do not access any non-static field from this method. Your object has not yet been constructed when it is called!
   */
  protected abstract void addMachineSlots(InventoryPlayer playerInv);

  @Override
  public @Nonnull ItemStack transferStackInSlot(@Nonnull EntityPlayer entityPlayer, int slotNumber) {
    hasAlreadyJustSuccessfullyTransferedAStack = false;
    SlotDefinition slotDef = te.getSlotDefinition();

    ItemStack copystack = Prep.getEmpty();
    Slot slot = inventorySlots.get(slotNumber);
    if (slot != null && slot.getHasStack()) {
      ItemStack origStack = slot.getStack();
      if (Prep.isValid(origStack)) {
        copystack = origStack.copy();

        boolean merged = false;
        for (SlotRange range : rangeHelper.getTargetSlotsForTransfer(slotNumber, slot)) {
          if (mergeItemStack(origStack, range.getStart(), range.getEnd(), range.isReverse())) {
            while (mergeItemStack(origStack, range.getStart(), range.getEnd(), range.isReverse())) {
            }
            merged = true;
            break;
          }
        }

        if (!merged) {
          return Prep.getEmpty();
        }

        if (slotDef.isOutputSlot(slot.getSlotIndex())) {
          slot.onSlotChange(origStack, copystack);
        }

        if (Prep.isInvalid(origStack)) {
          slot.putStack(Prep.getEmpty());
        } else {
          slot.onSlotChanged();
        }

        if (origStack.getCount() == copystack.getCount()) {
          return Prep.getEmpty();
        }

        slot.onTake(entityPlayer, origStack);
      }
    }

    hasAlreadyJustSuccessfullyTransferedAStack = true;
    return copystack;
  }

  private boolean hasAlreadyJustSuccessfullyTransferedAStack = false;

  @Override
  protected void retrySlotClick(int slotId, int clickedButton, boolean mode, @Nonnull EntityPlayer playerIn) {
    if (!hasAlreadyJustSuccessfullyTransferedAStack) {
      this.slotClick(slotId, clickedButton, ClickType.QUICK_MOVE, playerIn);
    } else {
      hasAlreadyJustSuccessfullyTransferedAStack = false;
    }
  }

  protected int getIndexOfFirstPlayerInvSlot(@Nonnull SlotDefinition slotDef) {
    return slotDef.getNumSlots();
  }

  @Override
  public SlotRange getPlayerInventorySlotRange(boolean reverse) {
    return new SlotRange(startPlayerSlot, endHotBarSlot, reverse);
  }

  @Override
  public SlotRange getPlayerInventoryWithoutHotbarSlotRange() {
    return new SlotRange(startPlayerSlot, endPlayerSlot, false);
  }

  @Override
  public SlotRange getPlayerHotbarSlotRange() {
    return new SlotRange(startHotBarSlot, endHotBarSlot, false);
  }

  protected int getProgressScaled(int scale) {
    if (te instanceof IProgressTile) {
      Util.getProgressScaled(scale, (IProgressTile) te);
    }
    return 0;
  }



  private int guiID = -1;

  public void setGuiID(int id) {
    guiID = id;
  }

  public int getGuiID() {
    return guiID;
  }

}
