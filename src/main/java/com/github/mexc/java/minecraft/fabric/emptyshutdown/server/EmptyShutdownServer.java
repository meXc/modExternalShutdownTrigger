package com.github.mexc.java.minecraft.fabric.emptyshutdown.server;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

import static java.nio.file.StandardOpenOption.*;
import static net.minecraft.server.command.CommandManager.literal;

@Environment(EnvType.SERVER)
public class EmptyShutdownServer implements DedicatedServerModInitializer {
    public static final Logger ModLogger = LoggerFactory.getLogger("EmptyShutdownTrigger");
    public static final String MOD_ID = "emptyshutdownflag";

    public static final String CommandPause = "pause";
    public static final String CommandResume = "resume";
    public static final String CommandCreateFlag = "createFlag";
    public static final Integer CurrentConfigVersion = 1;

    private Integer FlagTimer = 15;
    private Integer CheckInterval = 20;
    private String FlagFileName = "ShutMeDown.flag";
    private Boolean DoStopCommand = true;
    private Boolean NeedsFlagAtStartup = false;
    private Integer RequiredOpLevel = 4;
    private Integer ConfigVersion = 0;

    public Instant PlayerWasThere = Instant.now();
    public boolean NeedFlag = false;
    public boolean Paused = false;
    public Path flagPath;

    @Override
    public void onInitializeServer() {
        ModLogger.info("Dedicated server hook launched");

        var configPath = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("general" + ".config");
        ModLogger.info(String.format("Config path is: %s / %s", configPath, configPath.toAbsolutePath()));

        flagPath = FabricLoader.getInstance().getGameDir().resolve(FlagFileName);
        ModLogger.info(String.format("Flag path is: %s / %s", flagPath, flagPath.toAbsolutePath()));

        if (Files.exists(flagPath)) {
            try {
                Files.delete(flagPath);
            } catch (SecurityException e) {
                ModLogger.error(String.format("Can not delete old flag. Flag path is: %s --> %s, SecurityException: %s ", flagPath, flagPath.toAbsolutePath(), e.getMessage()));
            } catch (IOException e) {
                ModLogger.error(String.format("Can not delete old flag. Flag path is: %s --> %s, IOException: %s", flagPath, flagPath.toAbsolutePath(), e.getMessage()));
            }

        }

        try {
            Files.createDirectories(configPath.getParent());
            if (Files.exists(configPath)) {
                readConfig(configPath);
            }

            if (ConfigVersion < CurrentConfigVersion)
                writeConfig(configPath);

        } catch (SecurityException e) {
            ModLogger.error(String.format("Can not read config. Config path is: %s --> %s, SecurityException: %s ", configPath, configPath.toAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            ModLogger.error(String.format("Can not read config. Config path is: %s --> %s, IOException: %s", configPath, configPath.toAbsolutePath(), e.getMessage()));
        }

        NeedFlag = NeedsFlagAtStartup;

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
        {
            ModLogger.info(String.format("Player logout detected: %d", server.getCurrentPlayerCount()));
            if (server.getCurrentPlayerCount() > 1)
                return;
            ModLogger.info("Server Empty");
            PlayerWasThere = Instant.now();
            NeedFlag = true;
        });

        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            ModLogger.info("Player login detected");
            NeedFlag = false;
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {

            if (Paused)
                return;

            if (!NeedFlag)
                return;

            if (server.getTicks() % CheckInterval > 0)
                return;

            if (Duration.between(PlayerWasThere, Instant.now()).toMinutes() > FlagTimer) {
                ModLogger.info(String.format("Server empty more then %d Minutes", FlagTimer));
                NeedFlag = Boolean.FALSE;
                CreateExternalFlag(server);
            }

        });

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal(MOD_ID).executes(context -> {
                context.getSource().sendMessage(Text.literal("Called without arguments"));
                context.getSource().sendMessage(Text.literal("Arguments: pause resume createFlag"));
                return 1;
            }));
            dispatcher.register(literal(MOD_ID)
                    .then(literal(CommandPause)
                            .requires(source -> source.hasPermissionLevel(RequiredOpLevel))
                            .executes(context -> {
                                context.getSource().sendMessage(Text.literal("Pausing Flag Creation"));
                                Paused = true;
                                return 0;
                            })));
            dispatcher.register(literal(MOD_ID)
                    .then(literal(CommandResume)
                            .requires(source -> source.hasPermissionLevel(RequiredOpLevel))
                            .executes(context -> {
                                context.getSource().sendMessage(Text.literal("Resuming Flag Creation"));
                                Paused = false;
                                return 0;
                            })));

            dispatcher.register(literal(MOD_ID)
                    .then(literal(CommandCreateFlag)
                            .requires(source -> source.hasPermissionLevel(RequiredOpLevel))
                            .executes(context -> {
                                context.getSource().sendMessage(Text.literal("Creating Flag"));
                                CreateExternalFlag(context.getSource().getServer());
                                return 0;
                            })));
        }
        ));
    }

    private void readConfig(Path configPath) throws IOException, SecurityException {
        if (Files.exists(configPath) == false)
            return;

        Properties prop = new Properties();

        try (InputStream configInput = Files.newInputStream(configPath, READ)) {

            prop.load(configInput);

            FlagTimer = Integer.valueOf(prop.getProperty("general.FlagTimerInMinutes", String.valueOf(FlagTimer)));
            CheckInterval = Integer.valueOf(prop.getProperty("general.CheckIntervalInTicks", String.valueOf(CheckInterval)));
            FlagFileName = prop.getProperty("general.RelativeFlagFileName", FlagFileName);
            DoStopCommand = Boolean.valueOf(prop.getProperty("general.AutoShutdown", String.valueOf(DoStopCommand)));
            NeedsFlagAtStartup = Boolean.valueOf(prop.getProperty("general.NeedsFlagAtStartup", String.valueOf(NeedsFlagAtStartup)));
            RequiredOpLevel = Integer.valueOf(prop.getProperty("general.RequiredOpLevel", String.valueOf(RequiredOpLevel)));
            ConfigVersion = Integer.valueOf(prop.getProperty("general.ConfigVersion", String.valueOf(ConfigVersion)));

            ModLogger.info(String.format("Finished reading config from : %s", configPath));

        }
    }

    private void writeConfig(Path configPath) throws IOException, SecurityException {

        try (OutputStream configOutput = Files.newOutputStream(configPath, CREATE, WRITE, TRUNCATE_EXISTING)) {

            Properties prop = new Properties();
            prop.setProperty("general.FlagTimerInMinutes", String.valueOf(FlagTimer));
            prop.setProperty("general.CheckIntervalInTicks", String.valueOf(CheckInterval));
            prop.setProperty("general.RelativeFlagFileName", FlagFileName);
            prop.setProperty("general.AutoShutdown", String.valueOf(DoStopCommand));
            prop.setProperty("general.NeedsFlagAtStartup", String.valueOf(NeedsFlagAtStartup));
            prop.setProperty("general.RequiredOpLevel", String.valueOf(RequiredOpLevel));
            prop.setProperty("general.ConfigVersion", String.valueOf(CurrentConfigVersion));

            //Files.createFile(configPath);
            ModLogger.info(String.format("Writing potential new config or config options to : %s", configPath));
            prop.store(configOutput, "Configuration");

        }

    }

    /**
     * Creates an external flag file
     */
    private void CreateExternalFlag(MinecraftServer server) {
        File FlagFile;
        if (Files.isWritable(flagPath.getParent()) && Files.isDirectory(flagPath.getParent())) {
            try {
                Files.createFile(flagPath);
                if (DoStopCommand)
                    server.stop(false);

            } catch (SecurityException e) {
                ModLogger.error(String.format("Can not write flag. Flag path is: %s --> %s, SecurityException: %s ", flagPath, flagPath.toAbsolutePath(), e.getMessage()));
            } catch (IOException e) {
                ModLogger.error(String.format("Can not write flag. Flag path is: %s --> %s, IOException: %s", flagPath, flagPath.toAbsolutePath(), e.getMessage()));
            }
        }
    }
}
