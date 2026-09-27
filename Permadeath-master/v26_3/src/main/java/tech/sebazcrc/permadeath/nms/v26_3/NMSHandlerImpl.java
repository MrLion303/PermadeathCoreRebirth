package tech.sebazcrc.permadeath.nms.v26_3;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Ghast;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import tech.sebazcrc.permadeath.Main;
import tech.sebazcrc.permadeath.nms.v26_3.entity.CustomGhast;
import tech.sebazcrc.permadeath.util.NMS;
import tech.sebazcrc.permadeath.util.interfaces.NMSHandler;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class NMSHandlerImpl implements NMSHandler {

    @Override
    public Class<?> getNMSClass(String name) {

        try {

            return Class.forName(
                    "net.minecraft." + name
            );

        } catch (ClassNotFoundException e) {

            e.printStackTrace();
            return null;
        }
    }

    @Override
    public net.minecraft.world.entity.EntityType<?>
    convertBukkitToNMS(EntityType type) {

        return CraftEntityType
                .bukkitToMinecraft(type);
    }

    @Override
    public Entity spawnNMSEntity(
            String className,
            EntityType type,
            Location location,
            CreatureSpawnEvent.SpawnReason reason
    ) {

        /*
         * Conservamos la condición especial del plugin
         * para mobs acuáticos / murciélagos generados
         * mediante Spawn Egg.
         */
        if (
                (type != EntityType.BAT
                        && type != EntityType.COD
                        && type != EntityType.SALMON
                        && type != EntityType.SQUID
                        && type != EntityType.PUFFERFISH
                        && type != EntityType.TROPICAL_FISH)

                        || reason
                        != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG

                        || Math.random() <= 0.02004008016D
        ) {

            World world =
                    Objects.requireNonNull(
                            location.getWorld(),
                            "La ubicación no posee un mundo."
                    );

            Class<? extends Entity> entityClass =
                    type.getEntityClass();

            if (entityClass == null) {
                return null;
            }

            return ((CraftWorld) world).spawn(
                    location,
                    entityClass,
                    null,
                    reason
            );
        }

        return null;
    }

    @Override
    public Entity spawnNMSCustomEntity(
            String classPath,
            EntityType type,
            Location location,
            CreatureSpawnEvent.SpawnReason reason
    ) {

        net.minecraft.world.entity.Entity nmsEntity =
                null;

        try {

            Class<?> clazz =
                    Class.forName(
                            NMS.search(
                                    "entity." + classPath
                            )
                    );

            nmsEntity =
                    (net.minecraft.world.entity.Entity)
                            clazz
                                    .getConstructor(Location.class)
                                    .newInstance(location);

        } catch (
                NoSuchMethodException
                        | ClassNotFoundException
                        | IllegalAccessException
                        | InstantiationException
                        | InvocationTargetException ignored
        ) {
        }

        if (nmsEntity == null) {
            return null;
        }

        nmsEntity.setPos(
                location.getX(),
                location.getY(),
                location.getZ()
        );

        nmsEntity.setXRot(
                location.getPitch()
        );

        nmsEntity.setYRot(
                location.getYaw()
        );

        CraftWorld world =
                (CraftWorld)
                        Objects.requireNonNull(
                                location.getWorld(),
                                "La ubicación no posee un mundo."
                        );

        world.addEntityToWorld(
                nmsEntity,
                reason
        );

        return nmsEntity.getBukkitEntity();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Entity spawnCustomGhast(
            Location location,
            CreatureSpawnEvent.SpawnReason reason,
            boolean isEnder
    ) {

        CraftWorld craftWorld =
                (CraftWorld)
                        Objects.requireNonNull(
                                location.getWorld(),
                                "La ubicación no posee un mundo."
                        );

        ServerLevel nmsWorld =
                craftWorld.getHandle();

        net.minecraft.world.entity.EntityType<? extends Ghast>
                ghastType =
                (net.minecraft.world.entity.EntityType<? extends Ghast>)
                        CraftEntityType.bukkitToMinecraft(
                                EntityType.GHAST
                        );

        CustomGhast ghast =
                new CustomGhast(
                        ghastType,
                        nmsWorld
                );

        ghast.setPos(
                location.getX(),
                location.getY(),
                location.getZ()
        );

        ghast.setXRot(
                location.getPitch()
        );

        ghast.setYRot(
                location.getYaw()
        );

        craftWorld.addEntityToWorld(
                ghast,
                reason
        );

        if (isEnder) {

            Objects.requireNonNull(
                    ghast.getAttribute(
                            Attributes.MAX_HEALTH
                    )
            ).setBaseValue(100.0D);

            ghast.setHealth(
                    100.0F
            );

            ghast.setCustomName(
                    CraftChatMessage.fromStringOrNull(
                            "§6Ender Ghast"
                    )
            );

            ghast.setCustomNameVisible(
                    false
            );

            ghast
                    .getBukkitEntity()
                    .getPersistentDataContainer()
                    .set(
                            new NamespacedKey(
                                    Main.getInstance(),
                                    "ender_ghast"
                            ),
                            PersistentDataType.BYTE,
                            (byte) 1
                    );
        }

        return ghast.getBukkitEntity();
    }

    @Override
    public void addMushrooms() {
        // TODO original del proyecto.
    }
}