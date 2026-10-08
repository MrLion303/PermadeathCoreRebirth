package tech.layon.permadeath.event;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.util.Utils;
import tech.layon.permadeath.util.VersionManager;

public class HostileEntityListener implements Listener {
    private final Main instance;

    public HostileEntityListener(Main instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {
        if (e.isCancelled()) return;

        if (instance.getDay() >= 20 && !Utils.isHostileMob(e.getEntityType()) && e.getEntityType() != EntityType.ARMOR_STAND && e.getEntityType() != EntityType.ENDERMAN) {
            injectHostileBehavior(e.getEntity());
        }
    }

    private void injectHostileBehavior(LivingEntity entity) {
        instance.getNmsAccessor().injectHostilePathfinders(entity);
        if (entity.getAttribute(Attribute.ATTACK_DAMAGE) == null) {
            instance.getNmsAccessor().registerAttribute(Attribute.ATTACK_DAMAGE, 8.0D, entity);
        }
    }

    // Los mobs existentes no se reprocesan al cargar chunks.
    // Solo CreatureSpawnEvent aplica la dificultad al mob recién creado.
}

