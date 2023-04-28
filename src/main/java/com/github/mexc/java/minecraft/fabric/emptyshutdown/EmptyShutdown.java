package com.github.mexc.java.minecraft.fabric.emptyshutdown;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmptyShutdown implements ModInitializer {
    public static final Logger ModLogger = LoggerFactory.getLogger("EmptyShutdownTrigger");
    @Override
    public void onInitialize() {
        ModLogger.info("General mod hook launched");

    }
}
