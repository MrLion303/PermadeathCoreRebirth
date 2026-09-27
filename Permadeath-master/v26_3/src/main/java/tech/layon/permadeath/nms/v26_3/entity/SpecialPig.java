package tech.layon.permadeath.nms.v26_3.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tech.layon.permadeath.util.NMS;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class SpecialPig
        extends net.minecraft.world.entity.animal.pig.Pig {

    public SpecialPig(Location loc) {

        super(
                getPigType(),
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
                40.0D,
                (LivingEntity) getBukkitEntity()
        );

        ArrayList<String> effectList =
                new ArrayList<>();

        Pig pig =
                (Pig) getBukkitEntity();

        effectList.add("SPEED");
        effectList.add("REGENERATION");
        effectList.add("STRENGTH");
        effectList.add("INVISIBILITY");
        effectList.add("JUMP_BOOST");
        effectList.add("SLOW_FALLING");
        effectList.add("GLOWING");
        effectList.add("RESISTANCE");

        Random random =
                new Random();

        for (int i = 0; i < 5; i++) {

            int randomIndex =
                    random.nextInt(
                            effectList.size()
                    );

            String effect =
                    effectList.get(
                            randomIndex
                    );

            if (effect.equals("SPEED")) {

                pig.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.SPEED,
                                9_999_999,
                                2
                        )
                );
            }

            if (effect.equals("REGENERATION")) {

                pig.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.REGENERATION,
                                9_999_999,
                                3
                        )
                );
            }

            if (effect.equals("STRENGTH")) {

                pig.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.STRENGTH,
                                9_999_999,
                                3
                        )
                );
            }

            if (effect.equals("INVISIBILITY")) {

                pig.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.INVISIBILITY,
                                9_999_999,
                                0
                        )
                );
            }

            if (effect.equals("JUMP_BOOST")) {

                pig.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.JUMP_BOOST,
                                9_999_999,
                                4
                        )
                );
            }

            if (effect.equals("SLOW_FALLING")) {

                pig.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.SLOW_FALLING,
                                9_999_999,
                                0
                        )
                );
            }

            if (effect.equals("GLOWING")) {

                pig.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.GLOWING,
                                9_999_999,
                                0
                        )
                );
            }

            if (effect.equals("RESISTANCE")) {

                pig.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.RESISTANCE,
                                9_999_999,
                                2
                        )
                );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends net.minecraft.world.entity.animal.pig.Pig>
    getPigType() {

        return (EntityType<? extends net.minecraft.world.entity.animal.pig.Pig>)
                CraftEntityType.bukkitToMinecraft(
                        org.bukkit.entity.EntityType.PIG
                );
    }

    @Override
    public boolean isPersistenceRequired() {
        return false;
    }

    @Override
    public void registerGoals() {

        this.goalSelector.addGoal(
                0,
                new FloatGoal(this)
        );

        this.goalSelector.addGoal(
                1,
                new RandomStrollGoal(
                        this,
                        1.0D
                )
        );

        this.goalSelector.addGoal(
                2,
                new LookAtPlayerGoal(
                        this,
                        Player.class,
                        6.0F
                )
        );

        this.goalSelector.addGoal(
                3,
                new RandomLookAroundGoal(this)
        );

        this.goalSelector.addGoal(
                0,
                new MeleeAttackGoal(
                        this,
                        1.0D,
                        true
                )
        );

        this.targetSelector.addGoal(
                4,
                new NearestAttackableTargetGoal<>(
                        this,
                        Player.class,
                        true
                )
        );
    }
}
