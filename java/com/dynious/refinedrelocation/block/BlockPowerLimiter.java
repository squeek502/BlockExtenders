package com.dynious.refinedrelocation.block;

import cofh.api.block.IDismantleable;
import com.dynious.refinedrelocation.RefinedRelocation;
import com.dynious.refinedrelocation.lib.Names;
import com.dynious.refinedrelocation.tileentity.TileBlockExtender;
import com.dynious.refinedrelocation.tileentity.TilePowerLimiter;
import cpw.mods.fml.common.Optional;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

import java.util.ArrayList;

@Optional.InterfaceList(value = {@Optional.Interface(iface = "cofh.api.block.IDismantleable", modid = "CoFHCore")})
public class BlockPowerLimiter extends BlockContainer implements IDismantleable
{
    protected BlockPowerLimiter(int id)
    {
        super(id, Material.rock);
        this.setUnlocalizedName(Names.powerLimiter);
        this.setHardness(3.0F);
        this.setCreativeTab(RefinedRelocation.tabRefinedRelocation);
    }

    @Override
    public TileEntity createNewTileEntity(World world)
    {
        return new TilePowerLimiter();
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int par5)
    {
        super.onNeighborBlockChange(world, x, y, z, par5);
        TileEntity tile = world.getBlockTileEntity(x, y, z);
        if (tile != null && tile instanceof TilePowerLimiter)
        {
            ((TilePowerLimiter) tile).blocksChanged = true;
        }
    }

    @Optional.Method(modid = "CoFHCore")
    @Override
    public ItemStack dismantleBlock(EntityPlayer player, World world, int x,
                                    int y, int z, boolean returnBlock)
    {
        int meta = world.getBlockMetadata(x, y, z);

        ArrayList<ItemStack> items = this.getBlockDropped(world, x, y, z, meta, 0);

        for (ItemStack item : items)
        {

            EntityItem entityitem = new EntityItem(world, x + 0.5F, y + 0.5F, z + 0.5F, item);

            entityitem.delayBeforeCanPickup = 10;

            world.spawnEntityInWorld(entityitem);

            entityitem.motionX *= 0.25F;
            entityitem.motionZ *= 0.25F;
        }

        world.setBlockToAir(x, y, z);
        return null;
    }

    @Optional.Method(modid = "CoFHCore")
    @Override
    public boolean canDismantle(EntityPlayer player, World world, int x, int y, int z)
    {
        return true;
    }

    @Override
    public boolean rotateBlock(World worldObj, int x, int y, int z, ForgeDirection axis)
    {
        TileBlockExtender tile = (TileBlockExtender) worldObj.getBlockTileEntity(x, y, z);
        return tile.rotateBlock();
    }
}