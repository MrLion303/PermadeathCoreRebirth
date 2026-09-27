package tech.layon.permadeath.event;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import tech.layon.permadeath.Main;

import java.util.UUID;

/**
 * Limpieza de mecánicas legacy y compatibilidad estricta con Minecraft 26.3.
 */
public final class LegacyCleanupListener implements Listener {

    private static final double POLAR_BEAR_TRIGGER_DISTANCE_SQUARED = 3.5D * 3.5D;

    private final Main plugin;
    private final NamespacedKey primedBearKey;

    public LegacyCleanupListener(Main plugin) {
        this.plugin = plugin;
        this.primedBearKey = new NamespacedKey(plugin, "proximity_primed_polar_bear");

        Bukkit.getScheduler().runTask(plugin, this::verifyMinecraftVersion);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickPolarBears, 20L, 2L);
    }

    private void verifyMinecraftVersion() {
        String runningVersion = Bukkit.getMinecraftVersion();

        if (!"26.3".equals(runningVersion)) {
            Bukkit.getLogger().severe("[NegativeStudios] PermadeathCoreRebirth solo es compatible con Minecraft 26.3.");
            Bukkit.getLogger().severe("[NegativeStudios] Version detectada: " + runningVersion);
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    /**
     * El cambio antiguo del Nether generaba Zombified Piglins 25 bloques por encima
     * del jugador en tres offsets fijos. Se cancelan únicamente esos spawns para no
     * interferir con otros Zombified Piglins CUSTOM usados por el plugin.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLegacyNetherMobRain(CreatureSpawnEvent event) {
        long day = plugin.getDay();

        if (day < 50 || day >= 60) return;
        if (event.getEntityType() != EntityType.ZOMBIFIED_PIGLIN) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        if (event.getLocation().getWorld() == null
                || event.getLocation().getWorld().getEnvironment() != World.Environment.NETHER) return;

        if (matchesLegacyNetherRainOffset(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean matchesLegacyNetherRainOffset(Location spawn) {
        int[][] offsets = {
                {10, 25, -5},
                {5, 25, 5},
                {-5, 25, 5}
        };

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(spawn.getWorld())) continue;

            Location base = player.getLocation();

            for (int[] offset : offsets) {
                double expectedX = base.getX() + offset[0];
                double expectedY = base.getY() + offset[1];
                double expectedZ = base.getZ() + offset[2];

                if (Math.abs(spawn.getX() - expectedX) <= 1.0D
                        && Math.abs(spawn.getY() - expectedY) <= 1.0D
                        && Math.abs(spawn.getZ() - expectedZ) <= 1.0D) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Desde día 50 los osos polares funcionan como creepers de proximidad:
     * al acercarse un jugador a 3.5 bloques se ceban, suena el hiss y explotan
     * 1.5 segundos después aunque no hayan golpeado al jugador.
     */
    private void tickPolarBears() {
        if (plugin.getDay() < 50) return;

        for (World world : Bukkit.getWorlds()) {
            for (PolarBear bear : world.getEntitiesByClass(PolarBear.class)) {
                if (!bear.isValid() || bear.isDead()) continue;

                if (bear.getPersistentDataContainer().has(
                        primedBearKey,
                        PersistentDataType.BYTE)) {
                    continue;
                }

                Player target = findNearbyPlayer(bear);
                if (target != null) {
                    primePolarBear(bear);
                }
            }
        }
    }

    private Player findNearbyPlayer(PolarBear bear) {
        Player nearest = null;
        double bestDistance = POLAR_BEAR_TRIGGER_DISTANCE_SQUARED;

        for (Player player : bear.getWorld().getPlayers()) {
            if (!player.isOnline() || player.isDead()) continue;
            if (player.getGameMode() != GameMode.SURVIVAL
                    && player.getGameMode() != GameMode.ADVENTURE) continue;

            double distance = bear.getLocation().distanceSquared(player.getLocation());
            if (distance <= bestDistance) {
                bestDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    private void primePolarBear(PolarBear bear) {
        bear.getPersistentDataContainer().set(
                primedBearKey,
                PersistentDataType.BYTE,
                (byte) 1);

        bear.setAI(false);
        bear.getWorld().playSound(
                bear.getLocation(),
                Sound.ENTITY_CREEPER_PRIMED,
                1.0F,
                1.0F);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!bear.isValid() || bear.isDead()) return;

            Location location = bear.getLocation().clone();
            World world = location.getWorld();
            if (world == null) return;

            world.createExplosion(
                    location,
                    1.5F,
                    true,
                    false,
                    bear);

            bear.remove();
        }, 30L);
    }

    /**
     * El código legacy manda dos títulos de bienvenida a los 15 y 20 segundos.
     * Los limpiamos inmediatamente después para administradores y primeras entradas.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean firstJoin = !player.hasPlayedBefore();

        if (!firstJoin && !player.isOp()) return;

        suppressTitle(player, 20L * 15L);
        suppressTitle(player, 20L * 15L + 1L);
        suppressTitle(player, 20L * 20L);
        suppressTitle(player, 20L * 20L + 1L);
    }

    private void suppressTitle(Player player, long delay) {
        UUID playerId = player.getUniqueId();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online != null && online.isOnline()) {
                online.resetTitle();
            }
        }, delay);
    }
}
