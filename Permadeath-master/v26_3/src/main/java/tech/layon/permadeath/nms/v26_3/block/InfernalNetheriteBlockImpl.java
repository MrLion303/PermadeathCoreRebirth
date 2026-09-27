package tech.layon.permadeath.nms.v26_3.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.util.interfaces.InfernalNetheriteBlock;
import tech.layon.permadeath.util.item.PermadeathItems;

import java.util.Objects;

public class InfernalNetheriteBlockImpl implements InfernalNetheriteBlock {

    private NamespacedKey getKey() {
        return new NamespacedKey(
                Main.getInstance(),
                "infernal_netherite"
        );
    }

    @Override
    public void placeCustomBlock(Location pos) {

        World world = Objects.requireNonNull(
                pos.getWorld(),
                "La ubicación no tiene mundo."
        );

        Block block = pos.getBlock();
        block.setType(Material.SPAWNER);

        BlockState state = block.getState();

        if (!(state instanceof CreatureSpawner spawner)) {
            return;
        }

        /*
         * Reproduce el antiguo SpawnData:
         *
         * ArmorStand
         * - invisible
         * - marker
         * - Structure Block como casco
         *
         * El ArmorStand NO se añade al mundo.
         * Solo creamos un EntitySnapshot para la animación
         * interna del spawner.
         */
        ArmorStand armorStand =
                world.createEntity(
                        pos.clone().add(0.5D, 0.0D, 0.5D),
                        ArmorStand.class
                );

        armorStand.setInvisible(true);
        armorStand.setMarker(true);

        armorStand.getEquipment().setHelmet(
                new ItemStack(Material.STRUCTURE_BLOCK)
        );

        EntitySnapshot snapshot =
                armorStand.createSnapshot();

        if (snapshot != null) {
            spawner.setSpawnedEntity(snapshot);
        } else {
            spawner.setSpawnedType(
                    EntityType.ARMOR_STAND
            );
        }

        /*
         * El bloque solamente utiliza la visual del spawner.
         * No debe generar mobs realmente.
         */
        spawner.setSpawnRange(0);
        spawner.setSpawnCount(0);
        spawner.setRequiredPlayerRange(0);
        spawner.setMaxNearbyEntities(0);

        spawner.getPersistentDataContainer().set(
                getKey(),
                PersistentDataType.BYTE,
                (byte) 1
        );

        spawner.update(true, false);

        world.playSound(
                pos,
                Sound.BLOCK_STONE_BREAK,
                1.0F,
                1.0F
        );
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {

        if (!isInfernalNetherite(
                event.getBlock().getLocation()
        )) {
            return;
        }

        event.getBlock()
                .getWorld()
                .dropItemNaturally(
                        event.getBlock()
                                .getLocation()
                                .add(0.0D, 0.5D, 0.0D),
                        PermadeathItems
                                .craftInfernalNetheriteIngot()
                );

        event.setExpToDrop(0);
    }

    @Override
    public boolean isInfernalNetherite(Location pos) {

        Block block = pos.getBlock();

        if (block.getType() != Material.SPAWNER) {
            return false;
        }

        BlockState state = block.getState();

        if (!(state instanceof CreatureSpawner spawner)) {
            return false;
        }

        Byte value =
                spawner
                        .getPersistentDataContainer()
                        .get(
                                getKey(),
                                PersistentDataType.BYTE
                        );

        if (value != null && value == (byte) 1) {
            return true;
        }

        /*
         * Compatibilidad con bloques generados por versiones
         * anteriores del plugin.
         */
        return spawner.getSpawnedType()
                == EntityType.ARMOR_STAND;
    }
}
