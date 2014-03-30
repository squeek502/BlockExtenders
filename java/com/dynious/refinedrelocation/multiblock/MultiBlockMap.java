package com.dynious.refinedrelocation.multiblock;

import com.dynious.refinedrelocation.until.Vector2;

public class MultiBlockMap
{
    private Vector2[][][] map;

    public MultiBlockMap(int sizeX, int sizeY, int sizeZ)
    {
        map = new Vector2[sizeX][sizeY][sizeZ];
    }

    public void addBlockAtPos(Vector2 blockIdMeta, int x, int y, int z)
    {
        if (x >= 0 && x < map.length && y >= 0 && y < map[0].length && z >= 0 && z < map[0][0].length)
        {
            map[x][y][z] = blockIdMeta;
        }
    }

    public void addBlockAtPos(int blockID, int x, int y, int z)
    {
        addBlockAtPos(new Vector2(blockID, -1), x, y, z);
    }

    public void addBlockAtPos(int blockID, int blockMeta, int x, int y, int z)
    {
        addBlockAtPos(new Vector2(blockID, blockMeta), x, y, z);
    }

    public Vector2 getBlockAtPos(int x, int y, int z)
    {
        if (x >= 0 && x < map.length && y >= 0 && y < map[0].length && z >= 0 && z < map[0][0].length)
        {
            return map[x][y][z];
        }
        return null;
    }

    public int getSizeX()
    {
        return map.length;
    }

    public int getSizeY()
    {
        return map[0].length;
    }

    public int getSizeZ()
    {
        return map[0][0].length;
    }
}
