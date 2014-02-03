package ic2.api.energy.event;

import ic2.api.energy.tile.IEnergyTile;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.world.WorldEvent;

/**
 * Base class for energy net events, don't use it directly.
 * <p/>
 * See ic2/api/energy/usage.txt for an overall description of the energy net api.
 */
public class EnergyTileEvent extends WorldEvent
{
    public final IEnergyTile energyTile;

    public EnergyTileEvent(IEnergyTile energyTile)
    {
        super(((TileEntity) energyTile).worldObj);

        this.energyTile = energyTile;
    }
}
