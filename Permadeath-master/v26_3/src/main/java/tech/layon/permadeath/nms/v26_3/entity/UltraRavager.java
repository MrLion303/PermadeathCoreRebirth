package tech.layon.permadeath.nms.v26_3.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ravager;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntityType;

import java.util.Objects;

public class UltraRavager extends Ravager {

    public UltraRavager(Location loc) {

        super(
                getRavagerType(),
                ((CraftWorld) Objects.requireNonNull(
                        loc.getWorld()
                )).getHandle()
        );

        this.setPos(
                loc.getX(),
                loc.getY(),
                loc.getZ()
        );
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Ravager>
    getRavagerType() {

        return (EntityType<? extends Ravager>)
                CraftEntityType.bukkitToMinecraft(
                        org.bukkit.entity.EntityType.RAVAGER
                );
    }

    @Override
    public boolean isPersistenceRequired() {
        return false;
    }
}
