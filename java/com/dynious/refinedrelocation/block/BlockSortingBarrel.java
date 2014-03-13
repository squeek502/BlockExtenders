package com.dynious.refinedrelocation.block;

import com.dynious.refinedrelocation.tileentity.TileSortingBarrel;
import mcp.mobius.betterbarrels.common.blocks.BlockBarrel;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockSortingBarrel extends BlockBarrel
{
    public BlockSortingBarrel(int id)
    {
        super(id);
    }

    @Override
    public TileEntity createNewTileEntity(World world)
    {
        return new TileSortingBarrel();
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, int par5, int par6)
    {
        TileSortingBarrel tile = (TileSortingBarrel )world.getBlockTileEntity(x, y, z);
        tile.getSortingInventoryHandler().onTileDestroyed();
        super.breakBlock(world, x, y, z, par5, par6);
    }
}