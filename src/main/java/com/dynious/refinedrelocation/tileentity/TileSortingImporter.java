package com.dynious.refinedrelocation.tileentity;

import com.dynious.refinedrelocation.helper.GuiHelper;
import com.dynious.refinedrelocation.helper.OreDictionaryHelper;
import com.dynious.refinedrelocation.lib.Names;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

public class TileSortingImporter extends TileSortingConnector implements IInventory
{
    public ItemStack[] bufferInventory = new ItemStack[1];
    private List<ItemStack> itemList = new ArrayList<ItemStack>();
    private List<Integer> idList = new ArrayList<Integer>();
    private long lastClickTime;
    private ItemStack lastAddedStack;
    protected List<EntityPlayer> crafters = new ArrayList<EntityPlayer>();

    public void onRightClick(EntityPlayer player)
    {
        if (player.isSneaking() || player.getHeldItem() == null)
        {
            GuiHelper.openGui(player, this);
        }
        else if (bufferInventory[0] == null)
        {
            //lastAddedStack = player.getHeldItem();
            setInventorySlotContents(0, player.getHeldItem());
            player.inventory.setInventorySlotContents(player.inventory.currentItem, null);

            /*
            if (worldObj.getWorldTime() - lastClickTime < 10L)
            {
                for (int invSlot = 0; invSlot < player.inventory.getSizeInventory(); ++invSlot)
                {
                    ItemStack stack = player.inventory.getStackInSlot(invSlot);
                    if (ItemStackHelper.areItemStacksEqual(lastAddedStack, stack))
                    {
                        setInventorySlotContents(0, player.getHeldItem());
                        player.inventory.setInventorySlotContents(invSlot, null);
                    }
                    if (bufferInventory[0] != null)
                    {
                        break;
                    }
                }
            }
            lastClickTime = worldObj.getWorldTime();
            */
        }
    }

    @Override
    public int getSizeInventory()
    {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int i)
    {
        if (i == 0 )
        {
            return bufferInventory[0];
        }
        else
        {
            if (i - 1 < itemList.size())
            {
                return itemList.get(i - 1);
            }
            else
            {
                return null;
            }
        }
    }

    @Override
    public ItemStack decrStackSize(int par1, int par2)
    {
        if (this.bufferInventory[par1] != null)
        {
            ItemStack itemstack;

            if (this.bufferInventory[par1].stackSize <= par2)
            {
                itemstack = this.bufferInventory[par1];
                this.bufferInventory[par1] = null;
                this.onInventoryChanged();
                return itemstack;
            }
            else
            {
                itemstack = this.bufferInventory[par1].splitStack(par2);

                if (this.bufferInventory[par1].stackSize == 0)
                {
                    this.bufferInventory[par1] = null;
                }

                this.onInventoryChanged();
                return itemstack;
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int par1)
    {
        if (this.bufferInventory[par1] != null)
        {
            ItemStack itemstack = this.bufferInventory[par1];
            this.bufferInventory[par1] = null;
            return itemstack;
        }
        else
        {
            return null;
        }
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack)
    {
        if (i == 0)
        {
            if (worldObj.isRemote)
            {
                return;
            }
            int[] oreIDs = OreDictionaryHelper.getOreIDs(itemstack);
            int id = -1;
            for (int oreID : oreIDs)
            {
                if (idList.contains(oreID))
                {
                    id = oreID;
                    break;
                }
            }
            if (id != -1)
            {
                ItemStack stack = itemList.get(idList.indexOf(id)).copy();
                stack.stackSize = itemstack.stackSize;
                itemstack = stack;
            }
            bufferInventory[0] = getHandler().getGrid().filterStackToGroup(itemstack, this, i);
            if (bufferInventory[0] != null)
            {
                syncInventory();
            }
        }
        else
        {
            int index = i - 1;

            if (itemstack == null && index < itemList.size())
            {
                itemList.remove(index);
                idList.remove(index);
            }
            else if (itemstack != null)
            {
                int[] oreIDs = OreDictionaryHelper.getOreIDs(itemstack);
                for (int oreID : oreIDs)
                {
                    if (i - 1 < itemList.size() && (!idList.contains(oreID) || oreID == idList.get(index)))
                    {
                        itemList.set(index, itemstack);
                        idList.set(index, oreID);
                    }
                    else if (index == itemList.size() && !idList.contains(oreID))
                    {
                        itemList.add(itemstack);
                        idList.add(oreID);
                    }
                }
            }
        }
    }

    @Override
    public String getInvName()
    {
        return Names.sortingImporter;
    }

    @Override
    public boolean isInvNameLocalized()
    {
        return false;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer)
    {
        return true;
    }

    @Override
    public void openChest()
    {
    }

    @Override
    public void closeChest()
    {
    }

    public final void addCrafter(EntityPlayer player)
    {
        crafters.add(player);
    }

    public final void removeCrafter(EntityPlayer player)
    {
        crafters.remove(player);
    }

    public void syncInventory()
    {
        for (EntityPlayer crafter : crafters)
        {
            ((ICrafting)crafter).sendSlotContents(crafter.openContainer, 0, bufferInventory[0]);
        }
    }

    public int getItemListSize()
    {
        return itemList.size();
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack)
    {
        return bufferInventory[0] == null;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        NBTTagList nbttaglist = compound.getTagList("Items");
        for (int i = 0; i < nbttaglist.tagCount(); ++i)
        {
            NBTTagCompound tag = (NBTTagCompound) nbttaglist.tagAt(i);
            itemList.add(ItemStack.loadItemStackFromNBT(tag));
            idList.add(tag.getInteger("oreId"));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);

        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.itemList.size(); ++i)
        {
            NBTTagCompound tag = new NBTTagCompound();
            this.itemList.get(i).writeToNBT(tag);
            tag.setInteger("oreId", idList.get(i));
            nbttaglist.appendTag(tag);
        }
        compound.setTag("Items", nbttaglist);
    }
}
