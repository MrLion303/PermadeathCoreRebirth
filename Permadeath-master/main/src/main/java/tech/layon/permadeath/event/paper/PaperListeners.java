package tech.layon.permadeath.event.paper;

import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent;
import com.destroystokyo.paper.event.entity.EntityTeleportEndGatewayEvent;
import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.EndGateway;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.end.demon.DemonPhase;
import tech.layon.permadeath.util.TextUtils;

import java.util.ArrayList;
import java.util.SplittableRandom;

/**
 * Integraciones especificas de Paper.
 *
 * Esta clase solo debe registrarse cuando ServerPlatform detecta Paper. Los
 * servidores Spigot usan SpigotCompatibilityListener como fallback.
 */
public final class PaperListeners implements Listener {

    private final Main main;
    private final SplittableRandom random = new SplittableRandom();

    public PaperListeners(Main main) {
        this.main = main;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(EnderDragonFireballHitEvent e) {
        AreaEffectCloud cloud = e.getAreaEffectCloud();
        if (cloud == null || main.getTask() == null || main.endWorld == null) return;

        ArrayList<Block> toChange = new ArrayList<>();

        Block base = main.endWorld.getHighestBlockAt(cloud.getLocation());
        Location highest = main.endWorld.getHighestBlockAt(cloud.getLocation()).getLocation();

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
            cloud.setParticle(Particle.SMOKE);
            cloud.addCustomEffect(
                    new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 20, 1),
                    false
            );
        } else {
            placeBedrockPattern(toChange, highest);
        }
    }

    private void placeBedrockPattern(ArrayList<Block> blocks, Location highest) {
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGatewayTeleport(EntityTeleportEndGatewayEvent e) {
        if (main.getDay() < 40) return;
        if (!beginningReady()) return;

        if (main.getDay() >= 50) {
            if (main.getBeginningManager().isClosed()) {
                e.setCancelled(true);
                return;
            }

            Entity entity = e.getEntity();
            if (entity instanceof Player) return;

            Location from = e.getFrom();
            World world = from.getWorld();
            World beginningWorld = main.getBeginningManager().getBeginningWorld();

            e.setCancelled(true);

            final Vector direction = entity.getLocation().getDirection().clone();
            final Vector velocity = entity.getVelocity().clone();
            final float pitch = entity.getLocation().getPitch();
            final float yaw = entity.getLocation().getYaw();

            if (world.equals(main.world)) {
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

            if (world.equals(beginningWorld)) {
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
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGatewayTeleport(PlayerTeleportEndGatewayEvent e) {
        if (main.getDay() < 40) return;
        if (!beginningReady()) return;

        Player player = e.getPlayer();
        World fromWorld = e.getFrom().getWorld();
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
            e.setCancelled(true);
            return;
        }

        EndGateway gateway = e.getGateway();
        gateway.setExitLocation(gateway.getLocation());
        gateway.update();
        e.setCancelled(true);

        final Vector direction = player.getLocation().getDirection().clone();
        final Vector velocity = player.getVelocity().clone();

        if (fromWorld.equals(main.world)) {
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
            Bukkit.getScheduler().runTask(main, () -> {
                Location target = main.world.getSpawnLocation().clone();
                target.setDirection(direction);
                player.teleport(target, PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.setVelocity(velocity);
            });
        }
    }

    private boolean beginningReady() {
        return main.world != null
                && main.getBeginningManager() != null
                && main.getBeginningManager().getBeginningWorld() != null
                && main.getBeData() != null;
    }
}
