package tech.layon.permadeath.event.spigot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.end.demon.DemonPhase;
import tech.layon.permadeath.util.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Fallbacks para Spigot de mecanicas que en Paper usan eventos mas especificos.
 *
 * Paper siempre tiene prioridad. Esta clase solo se registra cuando el servidor
 * detectado es Spigot.
 */
public final class SpigotCompatibilityListener implements Listener {

    private final Main main;
    private final SplittableRandom random = new SplittableRandom();

    public SpigotCompatibilityListener(Main main) {
        this.main = main;
    }

    /**
     * Sustituto Spigot de EnderDragonFireballHitEvent de Paper.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonFireballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof DragonFireball)) return;
        if (main.getTask() == null || main.endWorld == null) return;

        Location impact;
        if (event.getHitBlock() != null) {
            impact = event.getHitBlock().getLocation().add(0.5D, 0.5D, 0.5D);
        } else if (event.getHitEntity() != null) {
            impact = event.getHitEntity().getLocation().clone();
        } else {
            impact = event.getEntity().getLocation().clone();
        }

        if (!impact.getWorld().equals(main.endWorld)) return;

        applyDragonFireballTerrainEffect(impact);
    }

    private void applyDragonFireballTerrainEffect(Location impact) {
        Block base = main.endWorld.getHighestBlockAt(impact);
        Location highest = base.getLocation();

        List<Block> toChange = new ArrayList<>();
        int structure = random.nextInt(5);

        if (structure == 0) {
            toChange.add(base.getRelative(BlockFace.NORTH));
            toChange.add(base.getRelative(BlockFace.NORTH).getRelative(BlockFace.WEST));
            toChange.add(base.getRelative(BlockFace.SOUTH));
            toChange.add(base.getRelative(BlockFace.SOUTH_EAST));
            toChange.add(base.getRelative(BlockFace.SOUTH_WEST));
            toChange.add(base.getRelative(BlockFace.SOUTH_EAST).getRelative(BlockFace.SOUTH));
            toChange.add(base.getRelative(BlockFace.SOUTH_EAST).getRelative(BlockFace.NORTH));
            toChange.add(base.getRelative(BlockFace.NORTH).getRelative(BlockFace.NORTH));
        } else if (structure == 1) {
            toChange.add(base.getRelative(BlockFace.NORTH));
            toChange.add(base.getRelative(BlockFace.NORTH_EAST));
            toChange.add(base);
        } else if (structure == 2) {
            toChange.add(base.getRelative(BlockFace.SOUTH));
            toChange.add(base.getRelative(BlockFace.SOUTH_WEST));
            toChange.add(base);
        } else if (structure == 3) {
            toChange.add(base.getRelative(BlockFace.NORTH));
            toChange.add(base.getRelative(BlockFace.NORTH_EAST));
            toChange.add(base);
            toChange.add(base.getRelative(BlockFace.SOUTH));
            toChange.add(base.getRelative(BlockFace.EAST));
        } else {
            toChange.add(base.getRelative(BlockFace.SOUTH));
            toChange.add(base.getRelative(BlockFace.NORTH_WEST));
            toChange.add(base);
            toChange.add(base.getRelative(BlockFace.NORTH));
            toChange.add(base.getRelative(BlockFace.WEST));
        }

        if (main.getTask().getCurrentDemonPhase() == DemonPhase.NORMAL) {
            placeBedrockPattern(toChange, highest);
            return;
        }

        if (random.nextBoolean()) {
            /*
             * En Spigot ProjectileHitEvent no entrega directamente el cloud del
             * DragonFireball. Lo buscamos un tick despues alrededor del impacto.
             */
            Bukkit.getScheduler().runTaskLater(main, () -> {
                AreaEffectCloud cloud = findNearestCloud(impact, 4.0D);
                if (cloud == null) return;

                cloud.setParticle(Particle.SMOKE);
                cloud.addCustomEffect(
                        new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 20, 1),
                        false
                );
            }, 1L);
        } else {
            placeBedrockPattern(toChange, highest);
        }
    }

    private void placeBedrockPattern(List<Block> blocks, Location highest) {
        if (highest.getY() <= main.endWorld.getMinHeight()) return;

        for (Block block : blocks) {
            Location column = new Location(
                    main.endWorld,
                    block.getX(),
                    block.getY(),
                    block.getZ()
            );

            Location used = main.endWorld.getHighestBlockAt(column).getLocation();
            Block target = main.endWorld.getBlockAt(used);

            if (target.getType() != Material.AIR) {
                target.setType(Material.BEDROCK);
            }
        }
    }

    private AreaEffectCloud findNearestCloud(Location center, double radius) {
        AreaEffectCloud nearest = null;
        double nearestDistance = radius * radius;

        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof AreaEffectCloud cloud)) continue;

            double distance = cloud.getLocation().distanceSquared(center);
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = cloud;
            }
        }

        return nearest;
    }

    /**
     * Spigot si expone END_GATEWAY como TeleportCause, por lo que podemos
     * reproducir el comportamiento de PlayerTeleportEndGatewayEvent de Paper.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerGatewayTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_GATEWAY) return;
        if (main.getDay() < 40) return;
        if (!beginningReady()) return;

        Player player = event.getPlayer();
        World fromWorld = event.getFrom().getWorld();
        World beginningWorld = main.getBeginningManager().getBeginningWorld();

        if (main.getDay() < 50) {
            if (fromWorld.equals(main.world) || fromWorld.equals(beginningWorld)) {
                player.setNoDamageTicks(player.getMaximumNoDamageTicks());
                player.damage(player.getHealth() + 1.0D);
                player.setNoDamageTicks(0);

                Bukkit.broadcastMessage(TextUtils.format(
                        "&c&lEl jugador &4&l" + player.getName()
                                + " &c&lentro a TheBeginning antes de tiempo."
                ));
            }
            return;
        }

        if (main.getBeginningManager().isClosed()) {
            event.setCancelled(true);
            return;
        }

        Vector direction = player.getLocation().getDirection().clone();
        Vector velocity = player.getVelocity().clone();

        if (fromWorld.equals(main.world)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(main, () -> {
                Location destination = main.getBeData().getBeginningPortal();
                if (destination == null) return;

                Location target = destination.clone();
                target.setDirection(direction);
                player.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.setVelocity(velocity);
            });
            return;
        }

        if (fromWorld.equals(beginningWorld)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(main, () -> {
                Location target = main.world.getSpawnLocation().clone();
                target.setDirection(direction);
                player.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.setVelocity(velocity);
            });
        }
    }

    /**
     * Spigot no tiene el evento Paper EntityTeleportEndGatewayEvent. Para mobs
     * identificamos el gateway por el bloque de origen de EntityTeleportEvent.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityGatewayTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof Player) return;
        if (main.getDay() < 50) return;
        if (!beginningReady()) return;
        if (!isEndGatewayOrigin(event.getFrom())) return;

        if (main.getBeginningManager().isClosed()) {
            event.setCancelled(true);
            return;
        }

        Entity entity = event.getEntity();
        World fromWorld = event.getFrom().getWorld();
        World beginningWorld = main.getBeginningManager().getBeginningWorld();

        Vector direction = entity.getLocation().getDirection().clone();
        Vector velocity = entity.getVelocity().clone();
        float pitch = entity.getLocation().getPitch();
        float yaw = entity.getLocation().getYaw();

        if (fromWorld.equals(main.world)) {
            event.setCancelled(true);

            Location destination = main.getBeData().getBeginningPortal();
            if (destination == null) return;

            Location target = destination.clone();
            target.setDirection(direction);
            target.setPitch(pitch);
            target.setYaw(yaw);

            Bukkit.getScheduler().runTask(main, () -> {
                entity.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
                entity.setVelocity(velocity);
            });
            return;
        }

        if (fromWorld.equals(beginningWorld)) {
            event.setCancelled(true);

            Location target = main.world.getSpawnLocation().clone();
            target.setDirection(direction);
            target.setPitch(pitch);
            target.setYaw(yaw);

            Bukkit.getScheduler().runTask(main, () -> {
                entity.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
                entity.setVelocity(velocity);
            });
        }
    }

    private boolean beginningReady() {
        return main.world != null
                && main.getBeginningManager() != null
                && main.getBeginningManager().getBeginningWorld() != null
                && main.getBeData() != null;
    }

    private boolean isEndGatewayOrigin(Location location) {
        Block block = location.getBlock();
        if (block.getType() == Material.END_GATEWAY) return true;

        return block.getRelative(BlockFace.UP).getType() == Material.END_GATEWAY
                || block.getRelative(BlockFace.DOWN).getType() == Material.END_GATEWAY;
    }
}
