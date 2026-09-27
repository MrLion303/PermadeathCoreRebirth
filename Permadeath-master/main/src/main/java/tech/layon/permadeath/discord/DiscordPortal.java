package tech.layon.permadeath.discord;

import org.bukkit.OfflinePlayer;

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
