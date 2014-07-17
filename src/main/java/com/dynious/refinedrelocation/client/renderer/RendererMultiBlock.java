package com.dynious.refinedrelocation.client.renderer;

import com.dynious.refinedrelocation.multiblock.IMultiBlock;
import com.dynious.refinedrelocation.multiblock.MultiBlockRegistry;
import com.dynious.refinedrelocation.multiblock.TileMultiBlockBase;
import com.dynious.refinedrelocation.util.BlockAndMeta;
import com.dynious.refinedrelocation.util.MultiBlockAndMeta;
import com.dynious.refinedrelocation.util.Vector3;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

public class RendererMultiBlock extends TileEntitySpecialRenderer
{
    private static RenderBlocks renderBlocks = new RenderBlocks();

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double xPos, double yPos, double zPos, float timer)
    {
        if (tileEntity != null && tileEntity instanceof TileMultiBlockBase)
        {
            TileMultiBlockBase tileMultiBlock = (TileMultiBlockBase) tileEntity;
            if (!tileMultiBlock.isFormed(false))
            {
                IMultiBlock multiBlock = MultiBlockRegistry.getMultiBlock(tileMultiBlock.getMultiBlockIdentifier());

                if (multiBlock != null)
                {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

                    GL11.glPushMatrix();
                    GL11.glTranslated(xPos + 0.5F, yPos + 0.5F, zPos + 0.5F);

                    for (int x = 0; x < multiBlock.getMultiBlockMap().getSizeX(); x++)
                    {
                        for (int y = 0; y < multiBlock.getMultiBlockMap().getSizeY(); y++)
                        {
                            for (int z = 0; z < multiBlock.getMultiBlockMap().getSizeZ(); z++)
                            {
                                GL11.glPushMatrix();
                                
                                renderBlocks(multiBlock, tileMultiBlock, x, y, z);

                                GL11.glPopMatrix();
                            }
                        }
                    }
                    GL11.glPopMatrix();
                }
            }
        }
    }

    private void renderBlocks(IMultiBlock multiBlock, TileMultiBlockBase tileMultiBlock, int x, int y, int z)
    {
        Vector3 leaderPos = multiBlock.getRelativeLeaderPos();
        Object blockInfo = multiBlock.getMultiBlockMap().getBlockAndMetaAtPos(x, y, z);
        int xOffset = tileMultiBlock.xCoord + x - leaderPos.getX();
        int yOffset = tileMultiBlock.yCoord + y - leaderPos.getY();
        int zOffset = tileMultiBlock.zCoord + z - leaderPos.getZ();

        if (!tileMultiBlock.getWorldObj().isAirBlock(xOffset, yOffset, zOffset))
        {
            Block block = tileMultiBlock.getWorldObj().getBlock(xOffset, yOffset, tileMultiBlock.zCoord + z - leaderPos.getZ());
            BlockAndMeta blockAndMeta = null;
            if (blockInfo instanceof MultiBlockAndMeta)
            {
                blockAndMeta = getBlockAndMeta((MultiBlockAndMeta) blockInfo, tileMultiBlock);
            }
            else if (blockInfo instanceof BlockAndMeta)
            {
                blockAndMeta = (BlockAndMeta) blockInfo;
            }

            if (blockAndMeta != null && !tileMultiBlock.getWorldObj().getBlock(xOffset, yOffset, zOffset).isOpaqueCube())
            {
                renderBlock(blockInfo, multiBlock, tileMultiBlock, x, y, z);
            }

            if (blockAndMeta != null && blockAndMeta.getBlock() == block && blockAndMeta.getMeta() == tileMultiBlock.getWorldObj().getBlockMetadata(xOffset, yOffset, zOffset))
            {
                // No need for any rendering, the right block is at these coordinates
            }
            else
            {
                renderIncorrectBlock(x, y, z);
            }
        }
        else
        {
            renderBlock(blockInfo, multiBlock, tileMultiBlock, x, y, z);
        }
    }

    private void renderBlock(Object blockInfo, IMultiBlock multiBlock, TileMultiBlockBase tileMultiBlock, int x, int y, int z)
    {
        Vector3 leaderPos = multiBlock.getRelativeLeaderPos();
        BlockAndMeta blockAndMeta = null;
        int relativeX = 0;
        int relativeY = 0;
        int relativeZ = 0;

        if (blockInfo instanceof MultiBlockAndMeta)
        {
            MultiBlockAndMeta multiBlockAndMeta = (MultiBlockAndMeta) blockInfo;

            blockAndMeta = getBlockAndMeta(multiBlockAndMeta, tileMultiBlock);
            relativeX = x - leaderPos.getX();
            relativeY = y - leaderPos.getY();
            relativeZ = z - leaderPos.getZ();
        }
        else if (blockInfo instanceof BlockAndMeta)
        {
            blockAndMeta = (BlockAndMeta) blockInfo;
            relativeX = x - leaderPos.getX();
            relativeY = y - leaderPos.getY();
            relativeZ = z - leaderPos.getZ();
        }

        if (blockAndMeta != null && blockAndMeta.getBlock() != null)
        {
            float scale = 0.5F;
            GL11.glTranslatef(relativeX, relativeY, relativeZ);
            GL11.glScalef(scale, scale, scale);
            renderBlocks.renderBlockAsItem(blockAndMeta.getBlock(), blockAndMeta.getMeta(), 255F);
        }
    }

    private void renderIncorrectBlock(int x, int y, int z)
    {
        // TODO: Add incorrect block rendering
    }

    private BlockAndMeta getBlockAndMeta(MultiBlockAndMeta multiBlockAndMeta, TileMultiBlockBase tileMultiBlock)
    {
        int timePerBlock = 20 / multiBlockAndMeta.getBlockAndMetas().size();
        return multiBlockAndMeta.getBlockAndMetas().get((tileMultiBlock.timer % 20) / timePerBlock);
    }
}