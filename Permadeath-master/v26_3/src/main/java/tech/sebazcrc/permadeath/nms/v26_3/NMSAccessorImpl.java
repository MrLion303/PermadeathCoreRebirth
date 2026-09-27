package tech.sebazcrc.permadeath.nms.v26_3;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.attribute.CraftAttribute;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import tech.sebazcrc.permadeath.util.interfaces.NMSAccessor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class NMSAccessorImpl implements NMSAccessor {

    private final Field attributeMapField;
    private final Method attributeModifiedMethod;

    public NMSAccessorImpl() {

        try {

            attributeMapField =
                    AttributeMap.class.getDeclaredField("attributes");

            attributeMapField.setAccessible(true);

            attributeModifiedMethod =
                    AttributeMap.class.getDeclaredMethod(
                            "onAttributeModified",
                            AttributeInstance.class
                    );

            attributeModifiedMethod.setAccessible(true);

        } catch (ReflectiveOperationException e) {

            throw new IllegalStateException(
                    "No se pudo inicializar el acceso a AttributeMap de Minecraft 26.3.",
                    e
            );
        }
    }

    @Override
    public void setMaxHealth(
            LivingEntity entity,
            Double value,
            boolean setHealth
    ) {

        org.bukkit.attribute.AttributeInstance attribute =
                Objects.requireNonNull(
                        entity.getAttribute(Attribute.MAX_HEALTH),
                        "La entidad no posee el atributo MAX_HEALTH."
                );

        attribute.setBaseValue(value);

        if (setHealth) {
            entity.setHealth(value);
        }
    }

    @Override
    public double getMaxHealth(LivingEntity entity) {

        return Objects.requireNonNull(
                entity.getAttribute(Attribute.MAX_HEALTH),
                "La entidad no posee el atributo MAX_HEALTH."
        ).getValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerAttribute(
            Attribute attribute,
            double value,
            LivingEntity entity
    ) {

        net.minecraft.world.entity.LivingEntity nmsEntity =
                ((CraftLivingEntity) entity).getHandle();

        AttributeMap attributeMap =
                nmsEntity.getAttributes();

        Holder<net.minecraft.world.entity.ai.attributes.Attribute>
                minecraftAttribute =
                CraftAttribute.bukkitToMinecraftHolder(attribute);

        AttributeInstance instance =
                attributeMap.getInstance(minecraftAttribute);

        if (instance == null) {

            instance =
                    new AttributeInstance(
                            minecraftAttribute,
                            modified -> {

                                try {

                                    attributeModifiedMethod.invoke(
                                            attributeMap,
                                            modified
                                    );

                                } catch (ReflectiveOperationException e) {

                                    throw new RuntimeException(
                                            "No se pudo actualizar el atributo NMS.",
                                            e
                                    );
                                }
                            }
                    );

            try {

                Map<
                        Holder<net.minecraft.world.entity.ai.attributes.Attribute>,
                        AttributeInstance
                        > attributes =
                        (Map<
                                Holder<net.minecraft.world.entity.ai.attributes.Attribute>,
                                AttributeInstance
                                >) attributeMapField.get(attributeMap);

                attributes.put(
                        minecraftAttribute,
                        instance
                );

            } catch (IllegalAccessException e) {

                throw new RuntimeException(
                        "No se pudo registrar el atributo NMS.",
                        e
                );
            }
        }

        instance.setBaseValue(value);
    }

    @Override
    public void registerHostileMobs() {
        // Ya no es necesario registrar tipos de entidad manualmente.
    }

    @Override
    public void injectHostilePathfinders(
            LivingEntity entity
    ) {

        Entity nmsEntity =
                ((CraftEntity) entity).getHandle();

        if (!(nmsEntity instanceof PathfinderMob mob)) {
            return;
        }

        /*
         * Llamas y pandas conservan el comportamiento especial
         * que tenía el plugin original.
         */
        if (entity.getType() != EntityType.LLAMA
                && entity.getType() != EntityType.PANDA) {

            GoalSelector goalSelector =
                    mob.goalSelector;

            AtomicBoolean containsMeleeGoal =
                    new AtomicBoolean(false);

            goalSelector.removeAllGoals(goal -> {

                if (goal instanceof MeleeAttackGoal) {
                    containsMeleeGoal.set(true);
                }

                return goal instanceof AvoidEntityGoal<?>
                        || goal instanceof PanicGoal;
            });

            if (!containsMeleeGoal.get()) {

                goalSelector.addGoal(
                        0,
                        new MeleeAttackGoal(
                                mob,
                                1.0D,
                                true
                        )
                );
            }
        }

        mob.targetSelector.addGoal(
                0,
                new NearestAttackableTargetGoal<>(
                        mob,
                        net.minecraft.world.entity.player.Player.class,
                        true
                )
        );
    }

    @Override
    public void drown(
            Player player,
            double amount
    ) {

        DamageSource source =
                DamageSource
                        .builder(DamageType.DROWN)
                        .build();

        player.damage(
                amount,
                source
        );
    }

    @Override
    public void clearEntityPathfinders(
            Object goalSelector,
            Object targetSelector
    ) {

        GoalSelector goals =
                (GoalSelector) goalSelector;

        GoalSelector targets =
                (GoalSelector) targetSelector;

        goals.removeAllGoals(
                goal -> true
        );

        targets.removeAllGoals(
                goal -> true
        );
    }
}