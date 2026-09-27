package tech.layon.permadeath.discord;

import org.bukkit.OfflinePlayer;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.util.ServerPlatform;

/**
 * Puente del bot de Discord.
 *
 * JDA viaja empaquetado dentro de PermadeathCoreRebirth, por lo que no depende
 * de JDA-Spigot ni de otra libreria/plugin instalado por separado.
 */
public final class DiscordPortal {

    private DiscordPortal() {
    }

    public static boolean isJDAInstalled() {
        /*
         * Main consulta este metodo antes de startPlugin(). Aprovechamos ese punto
         * temprano para detectar Paper/Spigot antes de registrar listeners o
         * ejecutar logica especifica de plataforma.
         */
        Main plugin = Main.getInstance();
        if (plugin != null) {
            ServerPlatform.bootstrap(plugin);
        }

        // JDA forma parte del propio JAR de PermadeathCoreRebirth.
        return true;
    }

    public static void banPlayer(OfflinePlayer off, boolean isAFKBan) {
        DiscordManager.getInstance().banPlayer(off, isAFKBan);
    }

    public static void onDeathTrain(String msg) {
        DiscordManager.getInstance().onDeathTrain(msg);
    }

    public static void onDayChange() {
        DiscordManager.getInstance().onDayChange();
    }

    public static void onDisable() {
        DiscordManager.getInstance().onDisable();
    }

    public static void reload() {
        DiscordManager.getInstance();
    }
}
