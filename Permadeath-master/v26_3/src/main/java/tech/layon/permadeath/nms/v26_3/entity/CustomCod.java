package tech.layon.permadeath.nms.v26_3.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.entity.LivingEntity;
import tech.layon.permadeath.util.NMS;

import java.util.Objects;

public class CustomCod extends Cod {

    public CustomCod(Location loc) {

        super(
                getCodType(),
                ((CraftWorld) Objects.requireNonNull(
                        loc.getWorld()
                )).getHandle()
        );

        this.setPos(
                loc.getX(),
                loc.getY(),
                loc.getZ()
        );

        NMS.getAccessor().registerAttribute(
                Attribute.ATTACK_DAMAGE,
                30.0D,
                (LivingEntity) this.getBukkitEntity()
        );

        NMS.getAccessor().registerAttribute(
                Attribute.ATTACK_KNOCKBACK,
                1500.0D,
                (LivingEntity) this.getBukkitEntity()
        );
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Cod> getCodType() {

        return (EntityType<? extends Cod>)
                CraftEntityType.bukkitToMinecraft(
                        org.bukkit.entity.EntityType.COD
                );
    }

    @Override
    public boolean isPersistenceRequired() {
        return false;
    }

    @Override
    public void registerGoals() {

        super.registerGoals();

        this.goalSelector.addGoal(
                0,
                new MeleeAttackGoal(
                        this,
                        1.0D,
                        true
                )
        );

        this.targetSelector.addGoal(
                1,
                new NearestAttackableTargetGoal<>(
                        this,
                        Player.class,
                        true
                )
        );
    }
}

