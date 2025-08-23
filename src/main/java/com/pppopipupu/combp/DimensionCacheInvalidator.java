package com.pppopipupu.combp;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class DimensionCacheInvalidator {

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // CommandLocate.StructureFinder.clearCache();
    }
}
