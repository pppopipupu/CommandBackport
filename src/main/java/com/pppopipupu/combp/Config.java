package com.pppopipupu.combp;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static String command_prefix = "cb";

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        command_prefix = configuration.getString(
            "command_prefix",
            Configuration.CATEGORY_GENERAL,
            command_prefix,
            "The prefix of commands added by this mod");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
