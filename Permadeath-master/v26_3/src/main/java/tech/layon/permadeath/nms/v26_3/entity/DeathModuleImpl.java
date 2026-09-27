package tech.layon.permadeath.nms.v26_3.entity;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.util.interfaces.DeathModule;

public class DeathModuleImpl implements DeathModule {

    private static final int MODULE_HARM_EFFECT_LEVEL = 3;

    @Override
    public void spawn(Location where) {

        World world = where.getWorld();

        if (world == null) {
            return;
        }

        /*
         * Creamos el Minecart con Spawner.
         */
        SpawnerMinecart spawnerMinecart =
                world.spawn(
                        where,
                        SpawnerMinecart.class
                );

        /*
         * Configuración equivalente al antiguo BaseSpawner NMS.
         */
        spawnerMinecart.setMaxSpawnDelay(150);
        spawnerMinecart.setDelay(0);
        spawnerMinecart.setSpawnRange(5);
        spawnerMinecart.setMinSpawnDelay(60);
        spawnerMinecart.setRequiredPlayerRange(32);
        spawnerMinecart.setSpawnCount(4);

        /*
         * Creamos el ItemStack de la poción que debe generar
         * el spawner.
         *
         * El antiguo código NMS utilizaba:
         *
         * id: minecraft:splash_potion
         * CustomPotionEffects:
         *   Id: 7
         *   Amplifier: 3
         *   Duration: 1
         *
         * Es decir, daño instantáneo con amplifier 3.
         */
        ItemStack potionItem =
                new ItemStack(
                        Material.SPLASH_POTION
                );

        PotionMeta potionMeta =
                (PotionMeta) potionItem.getItemMeta();

        potionMeta.addCustomEffect(
                new PotionEffect(
                        PotionEffectType.INSTANT_DAMAGE,
                        1,
                        MODULE_HARM_EFFECT_LEVEL
                ),
                true
        );

        potionItem.setItemMeta(
                potionMeta
        );

        /*
         * En Bukkit 26.3 podemos crear una entidad sin
         * agregarla al mundo.
         *
         * Esto evita depender del NBT y BaseSpawner internos
         * de Minecraft.
         */
        SplashPotion splashPotion =
                world.createEntity(
                        where,
                        SplashPotion.class
                );

        splashPotion.setItem(
                potionItem
        );

        EntitySnapshot potionSnapshot =
                splashPotion.createSnapshot();

        if (potionSnapshot == null) {

            throw new IllegalStateException(
                    "No se pudo crear el EntitySnapshot de la poción del DeathModule."
            );
        }

        /*
         * El SpawnerMinecart generará copias de esta poción.
         */
        spawnerMinecart.setSpawnedEntity(
                potionSnapshot
        );

        /*
         * Mantenemos el mensaje de depuración que hacía el
         * código original con BaseSpawner#save.
         */
        Bukkit.broadcastMessage(
                potionSnapshot.getAsString()
        );

        /*
         * Marca utilizada por el resto de PermadeathCore.
         */
        spawnerMinecart
                .getPersistentDataContainer()
                .set(
                        new NamespacedKey(
                                Main.getInstance(),
                                "module_minecart"
                        ),
                        PersistentDataType.BYTE,
                        (byte) 1
                );

        /*
         * Estructura original:
         *
         * Cave Spider
         *   └── Shulker rojo
         *         └── Spawner Minecart
         */
        CaveSpider spider =
                world.spawn(
                        where,
                        CaveSpider.class
                );

        Shulker shulker =
                world.spawn(
                        where,
                        Shulker.class
                );

        shulker.setColor(
                DyeColor.RED
        );

        shulker.addPassenger(
                spawnerMinecart
        );

        spider.addPassenger(
                shulker
        );
    }
}
