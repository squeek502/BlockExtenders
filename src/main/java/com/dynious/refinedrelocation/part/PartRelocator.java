package com.dynious.refinedrelocation.part;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.*;
import com.dynious.refinedrelocation.api.filter.IRelocatorModule;
import com.dynious.refinedrelocation.api.tileentity.IRelocator;
import com.dynious.refinedrelocation.grid.relocator.TravellingItem;
import com.dynious.refinedrelocation.lib.Names;
import com.dynious.refinedrelocation.lib.RelocatorData;
import com.dynious.refinedrelocation.renderer.RendererRelocator;
import com.dynious.refinedrelocation.tileentity.TileRelocator;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;

public class PartRelocator extends JCuboidPart implements IRelocator, ISidedInventory, JNormalOcclusion, TSlottedPart
{
    private TileRelocator relocator;

    public PartRelocator(TileRelocator tile)
    {
        relocator = tile;
    }

    public PartRelocator()
    {
        relocator = new TileRelocator();
    }

    @Override
    public void onWorldJoin()
    {
        super.onWorldJoin();
        initRelocator();
    }

    @Override
    public Cuboid6 getBounds()
    {
        return RelocatorData.middleCuboid;
    }

    public Iterable<IndexedCuboid6> getSubParts()
    {
        List<IndexedCuboid6> list = new ArrayList<IndexedCuboid6>();
        for (int i = 0; i < ForgeDirection.VALID_DIRECTIONS.length; i++)
        {
            if (relocator.connectsToSide(i))
            {
                list.add(new IndexedCuboid6(i, RelocatorData.sideCuboids[i]));
            }
        }
        list.add(new IndexedCuboid6(6, RelocatorData.middleCuboid));
        return list;
    }

    @Override
    public String getType()
    {
        return Names.relocator;
    }

    @Override
    public void onNeighborChanged()
    {
        super.onNeighborChanged();
        relocator.blocksChanged = true;
    }

    @Override
    public void onPartChanged(TMultiPart part)
    {
        relocator.blocksChanged = true;
    }

    @Override
    public Iterable<ItemStack> getDrops()
    {
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (TravellingItem travellingItem : relocator.getItems(true))
        {
            items.add(travellingItem.getItemStack());
        }
        return items;
    }

    @Override
    public void update()
    {
        super.update();
        if (relocator.blocksChanged)
        {
            relocator.updateEntity();
            if (!world().isRemote)
                sendDescUpdate();
        }
        else
        {
            relocator.updateEntity();
        }
    }

    public void initRelocator()
    {
        if (relocator.getWorldObj() == null)
        {
            relocator.setWorldObj(world());
            relocator.xCoord = x();
            relocator.yCoord = y();
            relocator.zCoord = z();
            relocator.validate();
        }
    }

    @Override
    public void renderDynamic(Vector3 pos, float frame, int pass)
    {
        RendererRelocator.instance.renderTileEntityAt(relocator, pos.x, pos.y, pos.z, frame);
    }

    @Override
    public boolean activate(EntityPlayer player, MovingObjectPosition hit, ItemStack item)
    {
        return relocator.onActivated(player, hit, item);
    }

    @Override
    public Iterable<Cuboid6> getOcclusionBoxes()
    {
        List<Cuboid6> list = new ArrayList<Cuboid6>();
        list.add(RelocatorData.middleCuboid);
        return list;
    }

    @Override
    public boolean occlusionTest(TMultiPart part)
    {
        return NormalOcclusionTest.apply(this, part);
    }

    @Override
    public int getSlotMask()
    {
        return 0;
    }

    /*
    NBT Handling
     */

    @Override
    public void save(NBTTagCompound tag)
    {
        super.save(tag);
        NBTTagCompound compound = new NBTTagCompound();
        relocator.writeToNBT(compound);
        tag.setTag("relocator", compound);
    }

    @Override
    public void load(NBTTagCompound tag)
    {
        super.load(tag);
        relocator.readFromNBT(tag.getCompoundTag("relocator"));
    }

    @Override
    public void writeDesc(MCDataOutput packet)
    {
        packet.writeNBTTagCompound(((S35PacketUpdateTileEntity) relocator.getDescriptionPacket()).func_148857_g());
    }

    @Override
    public void readDesc(MCDataInput packet)
    {
        relocator.onDataPacket(null, new S35PacketUpdateTileEntity(0, 0, 0, 0, packet.readNBTTagCompound()));
    }

    /*
    Relocator implementation
     */

    @Override
    public TileEntity[] getConnectedInventories()
    {
        return relocator.getConnectedInventories();
    }

    @Override
    public IRelocator[] getConnectedRelocators()
    {
        return relocator.getConnectedRelocators();
    }

    @Override
    public boolean canConnectOnSide(int side)
    {
        return tile().canAddPart(new NormallyOccludedPart(RelocatorData.sideCuboids[side]));
    }

    @Override
    public boolean connectsToSide(int side)
    {
        return relocator.connectsToSide(side);
    }

    @Override
    public IRelocatorModule getRelocatorModule(int side)
    {
        return relocator.getRelocatorModule(side);
    }

    @Override
    public boolean passesFilter(ItemStack itemStack, int side, boolean input)
    {
        return relocator.passesFilter(itemStack, side, input);
    }

    @Override
    public ItemStack insert(ItemStack itemStack, int side, boolean simulate)
    {
        return relocator.insert(itemStack, side, simulate);
    }

    @Override
    public void receiveTravellingItem(TravellingItem item)
    {
        relocator.receiveTravellingItem(item);
    }

    @Override
    public List<TravellingItem> getItems(boolean includeItemsToAdd)
    {
        return relocator.getItems(includeItemsToAdd);
    }

    @Override
    public TileEntity getTileEntity()
    {
        return relocator;
    }

    @Override
    public GuiScreen getGUI(int side)
    {
        return relocator.getGUI(side);
    }

    @Override
    public Container getContainer(int side)
    {
        return relocator.getContainer(side);
    }

    /*
    ISidedInventory implementation
     */

    @Override
    public int[] getAccessibleSlotsFromSide(int var1)
    {
        return relocator.getAccessibleSlotsFromSide(var1);
    }

    @Override
    public boolean canInsertItem(int i, ItemStack itemstack, int j)
    {
        return relocator.canInsertItem(i, itemstack, j);
    }

    @Override
    public boolean canExtractItem(int i, ItemStack itemstack, int j)
    {
        return relocator.canExtractItem(i, itemstack, j);
    }

    @Override
    public int getSizeInventory()
    {
        return relocator.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int i)
    {
        return relocator.getStackInSlot(i);
    }

    @Override
    public ItemStack decrStackSize(int i, int j)
    {
        return relocator.decrStackSize(i, j);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i)
    {
        return relocator.getStackInSlotOnClosing(i);
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemstack)
    {
        relocator.setInventorySlotContents(i, itemstack);
    }

    @Override
    public String getInventoryName()
    {
        return relocator.getInventoryName();
    }

    @Override
    public boolean hasCustomInventoryName()
    {
        return relocator.hasCustomInventoryName();
    }

    @Override
    public int getInventoryStackLimit()
    {
        return relocator.getInventoryStackLimit();
    }

    @Override
    public void markDirty()
    {
        relocator.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityplayer)
    {
        return relocator.isUseableByPlayer(entityplayer);
    }

    @Override
    public void openInventory()
    {
        relocator.openInventory();
    }

    @Override
    public void closeInventory()
    {
        relocator.closeInventory();
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack)
    {
        return relocator.isItemValidForSlot(i, itemstack);
    }
}
