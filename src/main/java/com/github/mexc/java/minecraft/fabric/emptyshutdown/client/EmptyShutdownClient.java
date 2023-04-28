package com.github.mexc.java.minecraft.fabric.emptyshutdown.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class EmptyShutdownClient implements ClientModInitializer {
    public static final Logger ModLogger = LoggerFactory.getLogger("EmptyShutdownTrigger");
    @Override
    public void onInitializeClient() {
        ModLogger.info("Client only hook launched");
    }
}
