package tech.layon.permadeath.event;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.util.Utils;
import tech.layon.permadeath.util.VersionManager;

public class HostileEntityListener implements Listener {
    private final Main instance;
    private final NamespacedKey hostileInjectedKey;

    public HostileEntityListener(Main instance) {
        this.instance = instance;
        this.hostileInjectedKey = new NamespacedKey(instance, "hostile_behavior_injected");
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {
        if (e.isCancelled()) return;

        if (instance.getDay() >= 20 && !Utils.isHostileMob(e.getEntityType()) && e.getEntityType() != EntityType.ARMOR_STAND && e.getEntityType() != EntityType.ENDERMAN) {
            injectHostileBehavior(e.getEntity());
        }
    }

    private void injectHostileBehavior(LivingEntity entity) {
        if (entity.getPersistentDataContainer().has(hostileInjectedKey, PersistentDataType.BYTE)) {
            return;
        }
        instance.getNmsAccessor().injectHostilePathfinders(entity);
        entity.getPersistentDataContainer().set(hostileInjectedKey, PersistentDataType.BYTE, (byte) 1);
        if (entity.getAttribute(Attribute.ATTACK_DAMAGE) == null) {
            instance.getNmsAccessor().registerAttribute(Attribute.ATTACK_DAMAGE, 8.0D, entity);
        }
    }

    /** Aplica la conversión hostil a entidades que ya existían cuando cambió el día. */
    public void applyToExisting(LivingEntity entity) {
        if (entity == null || entity.isDead() || instance.getDay() < 20) return;
        if (Utils.isHostileMob(entity.getType())
                || entity.getType() == EntityType.ARMOR_STAND
                || entity.getType() == EntityType.ENDERMAN) return;
        injectHostileBehavior(entity);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (instance.getDay() < 20) return;
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity living) {
                applyToExisting(living);
            }
        }
    }
}

