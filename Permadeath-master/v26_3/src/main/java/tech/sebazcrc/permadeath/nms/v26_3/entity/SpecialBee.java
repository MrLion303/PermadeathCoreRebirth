package tech.sebazcrc.permadeath.nms.v26_3.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntityType;

import java.util.Objects;

public class SpecialBee extends Bee {

    public SpecialBee(Location loc) {

        super(
                getBeeType(),
                ((CraftWorld) Objects.requireNonNull(
                        loc.getWorld()
                )).getHandle()
        );

        this.setPos(
                loc.getX(),
                loc.getY(),
                loc.getZ()
        );

        Objects.requireNonNull(
                this.getAttribute(
                        Attributes.MAX_HEALTH
                )
        ).setBaseValue(100.0D);

        Objects.requireNonNull(
                this.getAttribute(
                        Attributes.ATTACK_DAMAGE
                )
        ).setBaseValue(12.0D);

        this.setHealth(100.0F);

        /*
         * En 1.20 se usaba:
         *
         * setRemainingPersistentAngerTime(1)
         *
         * En 26.3 el sistema almacena el momento en que
         * termina el enojo.
         */
        this.setPersistentAngerEndTime(
                this.level().getGameTime() + 1L
        );

        this.targetSelector.addGoal(
                0,
                new NearestAttackableTargetGoal<>(
                        this,
                        Player.class,
                        true
                )
        );
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Bee> getBeeType() {

        return (EntityType<? extends Bee>)
                CraftEntityType.bukkitToMinecraft(
                        org.bukkit.entity.EntityType.BEE
                );
    }

    @Override
    public boolean isPersistenceRequired() {
        return false;
    }
}