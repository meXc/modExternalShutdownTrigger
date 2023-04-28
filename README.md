# modExternalShutdownTrigger

Creates a file, after the last person leaves the server, and a certain threshold has passed.

Install:
* build gradle tasks assemble
* Needs the Fabric-API: https://www.curseforge.com/minecraft/mc-mods/fabric-api
* copy build/lib/*.jar to server side mod folder
* copy fabric-api to server mod folder

Fabric in general:
* download minecraft server: https://www.minecraft.net/en-us/download/server
* download fabric server wrapper: https://fabricmc.net/use/server/
* place both in the same folder
* run fabric i.e.: java -Xmx2G -jar fabric-server-mc.1.19.2-loader.0.14.10-launcher.0.11.1.jar nogui
* stop server (stop)
* edit configuration if needed 
* confirm eula if needed (not done already)
* start up again