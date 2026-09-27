package tech.layon.permadeath.util;

import org.bukkit.Bukkit;
import tech.layon.permadeath.Main;

import java.util.Locale;

/**
 * Detecta la plataforma del servidor sin enlazar clases de Paper de forma directa.
 *
 * Prioridad:
 * 1) Paper (y forks compatibles con Paper)
 * 2) Spigot
 * 3) Cualquier otra implementación se considera no soportada
 */
public final class ServerPlatform {

    public enum Type {
        PAPER,
        SPIGOT,
        UNSUPPORTED
    }

    private static volatile Type detectedType;
    private static volatile boolean announced;

    private ServerPlatform() {
    }

    public static Type detect() {
        Type cached = detectedType;
        if (cached != null) {
            return cached;
        }

        synchronized (ServerPlatform.class) {
            if (detectedType != null) {
                return detectedType;
            }

            /*
             * Paper 26.3 expone io.papermc.paper.ServerBuildInfo.
             * Se comprueba primero para que Paper tenga prioridad aunque también
             * incluya clases heredadas de Spigot.
             */
            if (classExists("io.papermc.paper.ServerBuildInfo")
                    || classExists("io.papermc.paper.plugin.configuration.PluginMeta")) {
                detectedType = Type.PAPER;
                return detectedType;
            }

            String serverName = Bukkit.getName();
            String normalizedName = serverName == null
                    ? ""
                    : serverName.toLowerCase(Locale.ROOT);

            if (normalizedName.contains("paper")) {
                detectedType = Type.PAPER;
                return detectedType;
            }

            if (classExists("org.spigotmc.SpigotConfig")
                    || normalizedName.contains("spigot")) {
                detectedType = Type.SPIGOT;
                return detectedType;
            }

            detectedType = Type.UNSUPPORTED;
            return detectedType;
        }
    }

    public static void bootstrap(Main plugin) {
        Type type = detect();

        // Esta bandera ya existía en el plugin y controla los listeners de Paper.
        Main.runningPaperSpigot = type == Type.PAPER;

        if (!announced) {
            announced = true;

            switch (type) {
                case PAPER -> Bukkit.getConsoleSender().sendMessage(
                        "[NegativeStudios] Plataforma detectada: Paper. "
                                + "Se usaran las integraciones Paper prioritarias."
                );
                case SPIGOT -> Bukkit.getConsoleSender().sendMessage(
                        "[NegativeStudios] Plataforma detectada: Spigot. "
                                + "Se usaran las rutas de compatibilidad Spigot."
                );
                case UNSUPPORTED -> Bukkit.getLogger().severe(
                        "[NegativeStudios] Plataforma no soportada. "
                                + "PermadeathCoreRebirth requiere Paper o Spigot 26.3."
                );
            }
        }

        if (type == Type.UNSUPPORTED && plugin != null && plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin,
                    () -> Bukkit.getPluginManager().disablePlugin(plugin));
        }
    }

    public static boolean isPaper() {
        return detect() == Type.PAPER;
    }

    public static boolean isSpigot() {
        return detect() == Type.SPIGOT;
    }

    public static boolean isSupported() {
        return detect() != Type.UNSUPPORTED;
    }

    public static String getDisplayName() {
        return switch (detect()) {
            case PAPER -> "Paper";
            case SPIGOT -> "Spigot";
            case UNSUPPORTED -> Bukkit.getName();
        };
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className, false, ServerPlatform.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
