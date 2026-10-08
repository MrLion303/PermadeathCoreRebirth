package tech.layon.permadeath.world.beginning.generator;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import tech.layon.permadeath.world.beginning.BeginningManager;

import java.util.*;

public class BeginningLootTable {

    private final List<Integer> randomLoc = new ArrayList<>();
    private final List<String> chances;
    private final Set<Material> alreadyRolled;
    private final SplittableRandom random;

    public BeginningLootTable(BeginningManager man) {

        for (int i = 0; i < 27; ++i) {
            this.randomLoc.add(i);
        }

        this.chances = new ArrayList<>();
        this.alreadyRolled = new HashSet<>();
        this.random = new SplittableRandom();

        addItem(chances, Material.GOLD_INGOT, 5, 50, 60);
        addItem(chances, Material.GOLDEN_APPLE, 60, 1, 8);
        addItem(chances, Material.DIAMOND, 60, 16, 24);
        addItem(chances, Material.ARROW, 10, 10, 16);
        addItem(chances, Material.FIREWORK_ROCKET, 20, 55, 64);
        addItem(chances, Material.TOTEM_OF_UNDYING, 5, 1, 2);
        addItem(chances, Material.STRUCTURE_VOID, 1, 1, 1);

    }

    public void populateChest(Chest chest) {

        World w = chest.getWorld();
        Inventory inv = chest.getBlockInventory();
        if (!w.getName().equalsIgnoreCase("pdc_the_beginning")) return;
        if (inv.contains(Material.DIAMOND_PICKAXE)) return;
        alreadyRolled.clear();
        roll(chest);
    }

    private void addItem(List<String> list, Material mat, int chance, int min, int max) {
        list.add(mat.toString() + ";" + chance + ";" + min + ";" + max);
    }

    private void roll(Chest c) {
        int rollTimes = random.nextInt(3) + 1;
        for (int i = 0; i < rollTimes; i++) {
            generate(c);
        }
    }

    private void generate(Chest chest) {
        Inventory inventory = chest.getBlockInventory();
        List<Integer> freeSlots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null || inventory.getItem(slot).getType().isAir()) {
                freeSlots.add(slot);
            }
        }
        if (freeSlots.isEmpty()) return;

        Collections.shuffle(freeSlots);
        for (String entry : chances) {
            String[] split = entry.split(";");
            Material material = getMaterial(split);
            if (alreadyRolled.contains(material)) continue;
            if (random.nextInt(100) + 1 > getChance(split)) continue;

            int amount = generateValue(getMin(split), getMax(split));
            int slot = freeSlots.remove(freeSlots.size() - 1);
            inventory.setItem(slot, new ItemStack(material, amount));
            alreadyRolled.add(material);

            // Los objetos especiales ocupan una sola casilla.
            if (material == Material.TOTEM_OF_UNDYING || material == Material.STRUCTURE_VOID) {
                return;
            }
            break;
        }
    }

    private boolean hasSlot(Inventory inventory) {
        boolean b = false;
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                b = true;
            }
        }

        return b;
    }

    private int getMin(String[] s) {
        return Integer.parseInt(s[2]);
    }

    private int getMax(String[] s) {
        return Integer.parseInt(s[3]);
    }

    private int getChance(String[] s) {
        return Integer.parseInt(s[1]);
    }

    private Material getMaterial(String[] s) {
        return Material.valueOf(s[0]);
    }

    private int generateValue(int min, int max) {
        if (max <= min) return Math.max(1, min);
        return random.nextInt(min, max + 1);
    }
}

