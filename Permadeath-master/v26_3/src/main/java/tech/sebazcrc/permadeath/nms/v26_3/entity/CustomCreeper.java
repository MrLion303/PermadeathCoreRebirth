package tech.sebazcrc.permadeath.nms.v26_3.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class CustomCreeper extends Creeper {

    private final boolean enderCreeper;

    public CustomCreeper(
            EntityType<? extends Creeper> type,
            Level world,
            boolean enderCreeper
    ) {

        super(type, world);

        this.enderCreeper =
                enderCreeper;
    }

    @Override
    protected void registerGoals() {

        this.goalSelector.addGoal(
                1,
                new FloatGoal(this)
        );

        this.goalSelector.addGoal(
                2,
                new SwellGoal(this)
        );

        this.goalSelector.addGoal(
                4,
                new MeleeAttackGoal(
                        this,
                        1.0D,
                        false
                )
        );

        this.goalSelector.addGoal(
                5,
                new WaterAvoidingRandomStrollGoal(
                        this,
                        0.8D
                )
        );

        this.goalSelector.addGoal(
                6,
                new LookAtPlayerGoal(
                        this,
                        Player.class,
                        8.0F
                )
        );

        this.goalSelector.addGoal(
                6,
                new RandomLookAroundGoal(this)
        );

        this.targetSelector.addGoal(
                1,
                new NearestAttackableTargetGoal<>(
                        this,
                        Player.class,
                        true
                )
        );

        this.targetSelector.addGoal(
                2,
                new HurtByTargetGoal(this)
        );
    }

    @Override
    public boolean hurtServer(
            ServerLevel level,
            DamageSource damageSource,
            float amount
    ) {

        if (this.isInvulnerableTo(
                level,
                damageSource
        )) {
            return false;
        }

        if (!this.enderCreeper) {

            return super.hurtServer(
                    level,
                    damageSource,
                    amount
            );
        }

        boolean potion =
                damageSource.getDirectEntity()
                        instanceof AbstractThrownPotion;

        /*
         * Comportamiento original:
         *
         * Si NO es proyectil/poción, recibe daño normal.
         * En algunas fuentes no-vivas intenta teleportarse.
         */
        if (!damageSource.is(
                DamageTypeTags.IS_PROJECTILE
        ) && !potion) {

            boolean damaged =
                    super.hurtServer(
                            level,
                            damageSource,
                            amount
                    );

            if (!(damageSource.getEntity()
                    instanceof LivingEntity)
                    && this.random.nextInt(10) != 0) {

                teleport();
            }

            return damaged;
        }

        /*
         * Proyectiles y pociones:
         * intenta escapar hasta 64 veces.
         */
        for (int i = 0; i < 64; i++) {

            if (teleport()) {
                return true;
            }
        }

        return super.hurtServer(
                level,
                damageSource,
                amount
        );
    }

    public boolean teleport() {

        if (!this.isAlive()
                || this.level().isClientSide()) {
            return false;
        }

        double x =
                this.getX()
                        + (this.random.nextDouble() - 0.5D)
                        * 64.0D;

        double y =
                this.getY()
                        + this.random.nextInt(64)
                        - 32;

        double z =
                this.getZ()
                        + (this.random.nextDouble() - 0.5D)
                        * 64.0D;

        Vec3 oldPosition =
                this.position();

        /*
         * En 26.3 randomTeleport necesita indicar qué
         * BlockState hace inválida una ubicación.
         *
         * Conservamos la regla del código antiguo:
         * no teleportarse al agua.
         */
        boolean teleported =
                this.randomTeleport(
                        x,
                        y,
                        z,
                        true,
                        state ->
                                state
                                        .getFluidState()
                                        .is(FluidTags.WATER)
                );

        if (!teleported) {
            return false;
        }

        this.level().gameEvent(
                GameEvent.TELEPORT,
                oldPosition,
                GameEvent.Context.of(this)
        );

        if (!this.isSilent()) {

            this.level().playSound(
                    null,
                    this.xo,
                    this.yo,
                    this.zo,
                    SoundEvents.ENDERMAN_TELEPORT,
                    this.getSoundSource(),
                    1.0F,
                    1.0F
            );

            this.playSound(
                    SoundEvents.ENDERMAN_TELEPORT,
                    1.0F,
                    1.0F
            );
        }

        return true;
    }
}