package com.pppopipupu.combp;

import com.pppopipupu.combp.command.*;
import com.pppopipupu.combp.network.S2CTickRatePacket;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class CommonProxy {

    public static SimpleNetworkWrapper network;

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(CommandBackport.MODID);
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        network.registerMessage(S2CTickRatePacket.Handler.class, S2CTickRatePacket.class, 0, Side.CLIENT);

    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {}

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {}

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandFill());
        event.registerServerCommand(new CommandLocate());
        event.registerServerCommand(new CommandClone());
        event.registerServerCommand(new CommandTick());
        event.registerServerCommand(new CommandKillDrops());

    }
}
