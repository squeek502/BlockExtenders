package com.dynious.blex.tileentity;

import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler;
import cofh.api.energy.IEnergyHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import static cpw.mods.fml.common.Optional.*;

@InterfaceList(value = {
    @Interface(iface = "buildcraft.api.power.IPowerReceptor", modid = "BuildCraft|Energy"),
    @Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2"),
    @Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHCore"),
    @Interface(iface = "dan200.computer.api.IPeripheral", modid = "ComputerCraft")})
public class TileBlockExtender extends TileEntity implements ISidedInventory, IFluidHandler, IPowerReceptor, IEnergySink, IEnergyHandler, IPeripheral

{
    protected ForgeDirection connectedDirection = ForgeDirection.UNKNOWN;
    protected IInventory inventory;
    protected int[] accessibleSlots;
    protected IFluidHandler fluidHandler;
    protected IPowerReceptor powerReceptor;
    protected IEnergySink energySink;
    protected IEnergyHandler energyHandler;
    protected TileEntity[] tiles = new TileEntity[ForgeDirection.values().length];
    public boolean blocksChanged = true;
    public boolean isInitialConnectionCheck = true;
    protected float lightAmount = 0F;
    protected int recheckTiles = 0;
    protected boolean isRedstonePowered = false;
    protected boolean isRedstoneEnabled = true;
    @SideOnly(Side.CLIENT)
    public boolean hasConnection = false;

    public TileBlockExtender()
    {
        super();
    }

    public void setConnectedSide(int connectedSide)
    {
        this.connectedDirection = ForgeDirection.getOrientation(connectedSide);
        this.blocksChanged = true;
        if (worldObj != null)
        {
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, worldObj.getBlockId(this.xCoord, this.yCoord, this.zCoord));
        }
    }

    public ForgeDirection getConnectedDirection()
    {
        return connectedDirection;
    }

    public void setInventory(IInventory inventory)
    {
        this.inventory = inventory;
        if (inventory != null)
        {
            accessibleSlots = new int[inventory.getSizeInventory()];
            for (int i = 0; i < inventory.getSizeInventory(); i++)
            {
                accessibleSlots[i] = i;
            }
        }
    }

    public void setFluidHandler(IFluidHandler fluidHandler)
    {
        this.fluidHandler = fluidHandler;
    }

    public void setPowerReceptor(IPowerReceptor powerReceptor)
    {
        this.powerReceptor = powerReceptor;
    }

    public void setEnergyHandler(IEnergyHandler energyHandler)
    {
        this.energyHandler = energyHandler;
    }

    public void setEnergySink(IEnergySink energySink)
    {
        if (this.energySink == null && energySink != null)
        {
            this.energySink = energySink;
            if (!worldObj.isRemote)
            {
                MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            }
        }
        else if (this.energySink != null)
        {
            if (energySink == null && !worldObj.isRemote)
            {
                MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            }
            this.energySink = energySink;
        }
    }

    public TileEntity[] getTiles()
    {
        return tiles;
    }

    @Override
    public void invalidate()
    {
        if (this.energySink != null && !worldObj.isRemote)
        {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
        }
        super.invalidate();
    }

    @Override
    public void onChunkUnload()
    {
        if (this.energySink != null && !worldObj.isRemote)
        {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
        }
        super.onChunkUnload();
    }

    @Override
    public void updateEntity()
    {
        super.updateEntity();
        if (blocksChanged)
        {
            for (ForgeDirection direction : ForgeDirection.values())
            {
                if (direction != connectedDirection)
                {
                    tiles[direction.ordinal()] = worldObj.getBlockTileEntity(this.xCoord + direction.offsetX, this.yCoord + direction.offsetY, this.zCoord + direction.offsetZ);
                }
            }
            this.checkRedstonePower();
            TileEntity tile = getConnectedTile();
            boolean needRecheck = checkConnectedDirection(tile);
            if (!needRecheck)
                blocksChanged = false;
        }
        /*
        recheckTiles++;
        if (recheckTiles >= 20)
        {
            TileEntity tile = getConnectedTile();
            checkConnectedDirection(tile);
            recheckTiles = 0;
            worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, worldObj.getBlockId(this.xCoord, this.yCoord, this.zCoord));
        }
        */
        if (lightAmount > 0F)
        {
            lightAmount = lightAmount - 0.01F;
        }
    }

    protected boolean checkConnectedDirection(TileEntity tile)
    {
        if (tile != null && canConnect() && !isLooping(tile))
        {
            IInventory previousInventory = inventory;
            IFluidHandler previousFluidHandler = fluidHandler;
            IPowerReceptor previousPowerReceptor = powerReceptor;
            IEnergySink previousEnergySink = energySink;
            IEnergyHandler previousEnergyHandler = energyHandler;

            if (tile instanceof TileBlockExtender)
            {
                if (isInitialConnectionCheck)
                {
                    // wait until non-BlockExtender connections are made and then recheck
                    isInitialConnectionCheck = false;
                    return true;
                }
                TileBlockExtender connectedBlockExtender = (TileBlockExtender) tile;
                setInventory(connectedBlockExtender.inventory);
                setFluidHandler(connectedBlockExtender.fluidHandler);
                setPowerReceptor(connectedBlockExtender.powerReceptor);
                setEnergySink(connectedBlockExtender.energySink);
                setEnergyHandler(connectedBlockExtender.energyHandler);
            }
            else
            {
                if (tile instanceof IInventory)
                    setInventory((IInventory) tile);
                else
                    setInventory(null);

                if (tile instanceof IFluidHandler)
                    setFluidHandler((IFluidHandler) tile);
                else
                    setFluidHandler(null);

                if (Loader.isModLoaded("BuildCraft|Energy") && tile instanceof IPowerReceptor)
                    setPowerReceptor((IPowerReceptor) tile);
                else
                    setPowerReceptor(null);

                if (Loader.isModLoaded("IC2") && tile instanceof IEnergySink)
                    setEnergySink((IEnergySink) tile);
                else
                    setEnergySink(null);

                if (Loader.isModLoaded("CoFHCore") && tile instanceof IEnergyHandler)
                    setEnergyHandler((IEnergyHandler) tile);
                else
                    setEnergyHandler(null);
            }

            if (previousInventory != inventory || previousFluidHandler != fluidHandler ||
                    previousPowerReceptor != powerReceptor || previousEnergySink != energySink ||
                    previousEnergyHandler != energyHandler)
            {
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, worldObj.getBlockId(this.xCoord, this.yCoord, this.zCoord), connectedDirection.ordinal());
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
        else
        {
            resetConnections();
        }

        recheckTiles = 0;
        isInitialConnectionCheck = false;
        return false;
    }

    protected boolean resetConnections()
    {
        boolean hadConnections = hasConnection();

        setInventory(null);
        setFluidHandler(null);
        setPowerReceptor(null);
        setEnergySink(null);
        setEnergyHandler(null);

        if (hadConnections)
        {
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, worldObj.getBlockId(this.xCoord, this.yCoord, this.zCoord), connectedDirection.ordinal());
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }

        return hadConnections;
    }

    public boolean hasConnection()
    {
        if (inventory != null || fluidHandler != null)
        {
            return true;
        }
        if (Loader.isModLoaded("BuildCraft|Energy") && powerReceptor != null)
        {
            return true;
        }
        if (Loader.isModLoaded("IC2") && energySink != null)
        {
            return true;
        }
        if (Loader.isModLoaded("CoFHCore") && energyHandler != null)
        {
            return true;
        }
        return false;
    }
    
    public List<String> getConnectionTypes()
    {
        List<String> connections = new ArrayList<String>();
        
        if (inventory != null)
            connections.add("Inventory");
        if (fluidHandler != null)
            connections.add("Fluid Transmission");
        if (Loader.isModLoaded("BuildCraft|Energy") && powerReceptor != null)
            connections.add("Buildcraft Energy");
        if (Loader.isModLoaded("IC2") && energySink != null)
            connections.add("IC2 Energy");
        if (Loader.isModLoaded("CoFHCore") && energyHandler != null)
            connections.add("Thermal Expansion Energy");
        
        return connections;
    }

    public void objectTransported()
    {
        lightAmount = 0.15F;
    }

    public float getLightAmount()
    {
        return lightAmount;
    }

    public ForgeDirection getInputSide(ForgeDirection side)
    {
        return connectedDirection.getOpposite();
    }

    public boolean canConnect()
    {
        return connectedDirection != ForgeDirection.UNKNOWN;
    }

    public TileEntity getConnectedTile()
    {
        return worldObj.getBlockTileEntity(this.xCoord + connectedDirection.offsetX, this.yCoord + connectedDirection.offsetY, this.zCoord + connectedDirection.offsetZ);
    }

    public void checkRedstonePower()
    {
        boolean wasRedstonePowered = isRedstoneTransmissionActive();

        setRedstoneTransmissionActive(false);
        if (isRedstoneTransmissionEnabled())
        {
            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS)
            {
                // facing direction is output only
                if (direction == connectedDirection)
                    continue;

                int indirectPowerLevelFromDirection = worldObj.getIndirectPowerLevelTo(this.xCoord + direction.offsetX, this.yCoord + direction.offsetY, this.zCoord + direction.offsetZ, direction.ordinal());
                if (indirectPowerLevelFromDirection > 0)
                {
                    setRedstoneTransmissionActive(true);
                    break;
                }
            }
        }

        if (isRedstoneTransmissionActive() != wasRedstonePowered)
        {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            worldObj.notifyBlockOfNeighborChange(xCoord + connectedDirection.offsetX, yCoord + connectedDirection.offsetY, zCoord + connectedDirection.offsetZ, worldObj.getBlockId(this.xCoord, this.yCoord, this.zCoord));
        }
    }

    public int isPoweringTo(int side)
    {
        ForgeDirection realDir = ForgeDirection.getOrientation(side).getOpposite();

        if (isRedstoneTransmissionActive() && connectedDirection == realDir)
            return 15;

        return 0;
    }

    /*
    * Side:
    *  -1: UP
    *   0: NORTH
    *   1: EAST
    *   2: SOUTH
    *   3: WEST
    *   */
    public boolean canConnectRedstone(int side)
    {
        if (!this.isRedstoneTransmissionEnabled())
            return false;

        ForgeDirection realDirection = ForgeDirection.UNKNOWN;

        switch (side)
        {
            case -1:
                realDirection = ForgeDirection.UP;
                break;
            case 0:
                realDirection = ForgeDirection.NORTH;
                break;
            case 1:
                realDirection = ForgeDirection.EAST;
                break;
            case 2:
                realDirection = ForgeDirection.SOUTH;
                break;
            case 3:
                realDirection = ForgeDirection.WEST;
                break;
        }

        return realDirection != ForgeDirection.UNKNOWN && realDirection == connectedDirection;
    }

    private boolean isLooping(TileEntity tile)
    {
        return tile != null && tile instanceof TileBlockExtender && isTileConnectedToThis((TileBlockExtender) tile, new ArrayList<TileBlockExtender>());
    }

    private boolean isTileConnectedToThis(TileBlockExtender blockExtender, List<TileBlockExtender> visited)
    {
        boolean isLooping;
        TileEntity tile = blockExtender.getConnectedTile();
        if (tile == this || visited.contains(tile))
        {
            return true;
        }
        if (tile != null && tile instanceof TileBlockExtender)
        {
            visited.add((TileBlockExtender) tile);
            isLooping = isTileConnectedToThis((TileBlockExtender) tile, visited);
        }
        else
        {
            return false;
        }
        return isLooping;
    }

    /*
    ComputerCraft interaction
     */
    HashSet<IComputerAccess> computers = new HashSet<IComputerAccess>();

    @Method(modid = "ComputerCraft")
    @Override
    public String getType()
    {
        return "block_extender";
    }

    @Method(modid = "ComputerCraft")
    @Override
    public String[] getMethodNames()
    {
        return new String[]{"getConnectedDirection", "setConnectedDirection"};
    }

    @Method(modid = "ComputerCraft")
    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception
    {
        switch (method)
        {
            case 0:
                return new String[]{connectedDirection.toString()};
            case 1:
                if (arguments.length > 0 && arguments[0] instanceof String)
                {
                    ForgeDirection direction = ForgeDirection.valueOf(((String) arguments[0]).toUpperCase());
                    if (direction != null && direction != ForgeDirection.UNKNOWN)
                    {
                        setConnectedSide(direction.ordinal());
                        return new Boolean[]{true};
                    }
                }
                return new Boolean[]{false};
        }
        return null;
    }

    @Method(modid = "ComputerCraft")
    @Override
    public boolean canAttachToSide(int side)
    {
        return true;
    }

    @Method(modid = "ComputerCraft")
    @Override
    public void attach(IComputerAccess computer)
    {
        computers.add(computer);
    }

    @Method(modid = "ComputerCraft")
    @Override
    public void detach(IComputerAccess computer)
    {
        computers.remove(computer);
    }

    /*
    Item/Fluid/Power interaction
     */

    @Override
    public int[] getAccessibleSlotsFromSide(int i)
    {
        if (inventory != null)
        {
            if (inventory instanceof ISidedInventory)
            {
                return ((ISidedInventory) inventory).getAccessibleSlotsFromSide(getInputSide(ForgeDirection.getOrientation(i)).ordinal());
            }
            return accessibleSlots;
        }
        return new int[0];
    }

    @Override
    public boolean canInsertItem(int i, ItemStack itemStack, int i2)
    {
        if (inventory != null)
        {
            if (inventory instanceof ISidedInventory)
            {
                if (((ISidedInventory) inventory).canInsertItem(i, itemStack, getInputSide(ForgeDirection.getOrientation(i2)).ordinal()))
                {
                    objectTransported();
                    return true;
                }
                return false;
            }
            objectTransported();
            return true;
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int i, ItemStack itemStack, int i2)
    {
        if (inventory != null)
        {
            if (inventory instanceof ISidedInventory)
            {
                if (((ISidedInventory) inventory).canExtractItem(i, itemStack, getInputSide(ForgeDirection.getOrientation(i2)).ordinal()))
                {
                    objectTransported();
                    return true;
                }
                return false;
            }
            objectTransported();
            return true;
        }
        return false;
    }

    @Override
    public int getSizeInventory()
    {
        if (inventory != null)
        {
            return inventory.getSizeInventory();
        }
        return 0;
    }

    @Override
    public ItemStack getStackInSlot(int i)
    {
        if (inventory != null)
        {
            return inventory.getStackInSlot(i);
        }
        return null;
    }

    @Override
    public ItemStack decrStackSize(int i, int i2)
    {
        if (inventory != null)
        {
            return inventory.decrStackSize(i, i2);
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int i)
    {
        if (inventory != null)
        {
            return inventory.getStackInSlotOnClosing(i);
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack itemStack)
    {
        if (inventory != null)
        {
            inventory.setInventorySlotContents(i, itemStack);
        }
    }

    @Override
    public String getInvName()
    {
        if (inventory != null)
        {
            return inventory.getInvName();
        }
        return null;
    }

    @Override
    public boolean isInvNameLocalized()
    {
        return inventory != null && inventory.isInvNameLocalized();
    }

    @Override
    public int getInventoryStackLimit()
    {
        if (inventory != null)
        {
            return inventory.getInventoryStackLimit();
        }
        return 0;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entityPlayer)
    {
        return inventory != null && inventory.isUseableByPlayer(entityPlayer);
    }

    @Override
    public void openChest()
    {
        if (inventory != null)
        {
            inventory.openChest();
        }
    }

    @Override
    public void closeChest()
    {
        if (inventory != null)
        {
            inventory.closeChest();
        }
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack)
    {
        return inventory != null && inventory.isItemValidForSlot(i, itemStack);
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
    {
        if (fluidHandler != null)
        {
            int amount = fluidHandler.fill(getInputSide(from), resource, doFill);
            if (amount > 0 && doFill)
            {
                objectTransported();
            }
            return amount;
        }
        return 0;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
    {
        if (fluidHandler != null)
        {
            FluidStack amount = fluidHandler.drain(getInputSide(from), resource, doDrain);
            if (amount != null && amount.amount > 0 && doDrain)
            {
                objectTransported();
            }
            return amount;
        }
        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
    {
        if (fluidHandler != null)
        {
            FluidStack amount = fluidHandler.drain(getInputSide(from), maxDrain, doDrain);
            if (amount != null && amount.amount > 0 && doDrain)
            {
                objectTransported();
            }
            return amount;
        }
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid)
    {
        return fluidHandler != null && fluidHandler.canFill(getInputSide(from), fluid);
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid)
    {
        return fluidHandler != null && fluidHandler.canDrain(getInputSide(from), fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from)
    {
        if (fluidHandler != null)
        {
            return fluidHandler.getTankInfo(getInputSide(from));
        }
        return new FluidTankInfo[0];
    }

    @Method(modid = "BuildCraft|Energy")
    @Override
    public PowerHandler.PowerReceiver getPowerReceiver(ForgeDirection forgeDirection)
    {
        if (powerReceptor != null)
        {
            return powerReceptor.getPowerReceiver(getInputSide(forgeDirection));
        }
        return null;
    }

    @Method(modid = "BuildCraft|Energy")
    @Override
    public void doWork(PowerHandler powerHandler)
    {
        if (powerReceptor != null)
        {
            powerReceptor.doWork(powerHandler);
            objectTransported();
        }
    }

    @Method(modid = "BuildCraft|Energy")
    @Override
    public World getWorld()
    {
        if (powerReceptor != null)
        {
            return powerReceptor.getWorld();
        }
        return null;
    }

    @Method(modid = "IC2")
    @Override
    public double demandedEnergyUnits()
    {
        if (energySink != null)
        {
            return energySink.demandedEnergyUnits();
        }
        return 0;
    }

    @Method(modid = "IC2")
    @Override
    public double injectEnergyUnits(ForgeDirection forgeDirection, double v)
    {
        if (energySink != null)
        {
            double amount = energySink.injectEnergyUnits(getInputSide(forgeDirection), v);
            if (amount > 0)
            {
                objectTransported();
            }
            return amount;
        }
        return 0;
    }

    @Method(modid = "IC2")
    @Override
    public int getMaxSafeInput()
    {
        if (energySink != null)
        {
            return energySink.getMaxSafeInput();
        }
        return 0;
    }

    @Method(modid = "IC2")
    @Override
    public boolean acceptsEnergyFrom(TileEntity tileEntity, ForgeDirection forgeDirection)
    {
        return energySink != null && energySink.acceptsEnergyFrom(tileEntity, getInputSide(forgeDirection));
    }

    @Method(modid = "CoFHCore")
    @Override
    public int receiveEnergy(ForgeDirection forgeDirection, int i, boolean b)
    {
        if (energyHandler != null)
        {
            int amount = energyHandler.receiveEnergy(getInputSide(forgeDirection), i, b);
            if (amount > 0 && b)
            {
                objectTransported();
            }
            return amount;
        }
        return 0;
    }

    @Method(modid = "CoFHCore")
    @Override
    public int extractEnergy(ForgeDirection forgeDirection, int i, boolean b)
    {
        if (energyHandler != null)
        {
            int amount = energyHandler.extractEnergy(getInputSide(forgeDirection), i, b);
            if (amount > 0 && b)
            {
                objectTransported();
            }
            return amount;
        }
        return 0;
    }

    @Method(modid = "CoFHCore")
    @Override
    public boolean canInterface(ForgeDirection forgeDirection)
    {
        return energyHandler != null && energyHandler.canInterface(getInputSide(forgeDirection));
    }

    @Method(modid = "CoFHCore")
    @Override
    public int getEnergyStored(ForgeDirection forgeDirection)
    {
        if (energyHandler != null)
        {
            return energyHandler.getEnergyStored(getInputSide(forgeDirection));
        }
        return 0;
    }

    @Method(modid = "CoFHCore")
    @Override
    public int getMaxEnergyStored(ForgeDirection forgeDirection)
    {
        if (energyHandler != null)
        {
            return energyHandler.getMaxEnergyStored(getInputSide(forgeDirection));
        }
        return 0;
    }

    /*
    NBT stuffs
     */

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        setConnectedSide(compound.getByte("side"));
        setRedstoneTransmissionEnabled(compound.getBoolean("redstoneEnabled"));
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setByte("side", (byte) connectedDirection.ordinal());
        compound.setBoolean("redstoneEnabled", this.isRedstoneTransmissionEnabled());
    }

    @Override
    public void onDataPacket(INetworkManager net, Packet132TileEntityData pkt)
    {
        setConnectedSide(pkt.data.getByte("side"));
        setRedstoneTransmissionActive(pkt.data.getBoolean("redstone"));
        setRedstoneTransmissionEnabled(pkt.data.getBoolean("redstoneEnabled"));
        hasConnection = (pkt.data.getBoolean("hasConnection"));    }

    @Override
    public Packet getDescriptionPacket()
    {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setByte("side", (byte) connectedDirection.ordinal());
        compound.setBoolean("redstone", this.isRedstoneTransmissionActive());
        compound.setBoolean("redstoneEnabled", this.isRedstoneTransmissionEnabled());
        compound.setBoolean("hasConnection", this.hasConnection());        return new Packet132TileEntityData(xCoord, yCoord, zCoord, 1, compound);
    }

    public boolean rotateBlock()
    {
        setConnectedSide((getConnectedDirection().ordinal() + 1) % ForgeDirection.VALID_DIRECTIONS.length);
        return true;
    }

    public boolean isRedstoneTransmissionEnabled()
    {
        return isRedstoneEnabled;
    }

    public void setRedstoneTransmissionEnabled(boolean state)
    {
        boolean wasRedstoneEnabled = isRedstoneTransmissionEnabled();
        isRedstoneEnabled = state;

        if (worldObj != null && isRedstoneTransmissionEnabled() != wasRedstoneEnabled)
        {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            this.checkRedstonePower();
        }
    }

    public boolean isRedstoneTransmissionActive()
    {
        return isRedstonePowered;
    }

    public void setRedstoneTransmissionActive(boolean state)
    {
        isRedstonePowered = state;
    }
}
