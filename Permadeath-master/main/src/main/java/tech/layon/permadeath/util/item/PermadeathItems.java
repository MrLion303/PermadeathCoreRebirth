package tech.layon.permadeath.util.item;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.util.TextUtils;
import tech.layon.permadeath.util.lib.HiddenStringUtils;
import tech.layon.permadeath.util.lib.ItemBuilder;

import java.util.Arrays;
import java.util.UUID;

public class PermadeathItems {

    private static final int[] beginningRelicLockedSlots = {
            40, 34, 33, 32, 30, 29, 28, 27, 26, 25, 24, 23,
            21, 20, 19, 18, 17, 16, 15, 14, 12, 11, 10, 9, 8, 7
    };

    public static ItemStack crearReliquia() {

        ItemStack s = new ItemBuilder(Material.LIGHT_BLUE_DYE)
                .setCustomModelData(1, !Main.optifineItemsEnabled())
                .setDisplayName(TextUtils.format("&6Reliquia Del Fin"))
                .build();

        ItemMeta meta = s.getItemMeta();

        if (meta != null) {
            meta.setUnbreakable(true);
            meta.setLore(Arrays.asList(
                    HiddenStringUtils.encodeString(
                            "{" + UUID.randomUUID().toString() + ": 0}")));

            s.setItemMeta(meta);
        }

        return s;
    }

    public static ItemStack createLifeOrb() {

        return new ItemBuilder(Material.BROWN_DYE)
                .setCustomModelData(1, !Main.optifineItemsEnabled())
                .setUnbrekeable(true)
                .setDisplayName(TextUtils.format("&6Orbe de Vida"))
                .build();
    }

    public static ItemStack createBeginningRelic() {

        return new ItemBuilder(Material.CYAN_DYE)
                .setCustomModelData(1, !Main.optifineItemsEnabled())
                .setUnbrekeable(true)
                .setDisplayName(TextUtils.format("&6Reliquia del Comienzo"))
                .build();
    }

    public static ItemStack craftInfernalElytra() {

        ItemStack s = new ItemBuilder(Material.ELYTRA)
                .setCustomModelData(1)
                .setDisplayName(TextUtils.format("&6Elytras de Netherite Infernal"))
                .build();

        ItemMeta meta = s.getItemMeta();

        if (meta != null) {
            meta.setUnbreakable(true);
            s.setItemMeta(meta);
        }

        return s;
    }

    /*
     * ============================================================
     * HERRAMIENTAS DE NETHERITE
     * ============================================================
     *
     * Antes estas herramientas utilizaban DIAMOND_* como base
     * y simulaban Netherite mediante atributos manuales.
     *
     * Desde Minecraft 26.3 utilizamos los objetos NETHERITE_*
     * reales para conservar automáticamente:
     *
     * - daño vanilla correcto
     * - velocidad de ataque correcta
     * - durabilidad/tier correcto
     * - comportamiento correcto de Netherite
     *
     * Además reciben un pequeño bonus.
     */

    public static ItemStack craftNetheriteSword() {

        ItemStack s = new ItemBuilder(Material.NETHERITE_SWORD)
                .setCustomModelData(1, !Main.optifineItemsEnabled())
                .setDisplayName(TextUtils.format("&6Espada de Netherite"))
                .build();

        ItemMeta meta = s.getItemMeta();

        if (meta != null) {

            // Pequeño bonus sobre una espada de Netherite vanilla.
            meta.addEnchant(
                    Enchantment.SHARPNESS,
                    1,
                    true);

            meta.setUnbreakable(true);

            s.setItemMeta(meta);
        }

        return s;
    }

    public static ItemStack craftNetheritePickaxe() {

        ItemStack s = new ItemBuilder(Material.NETHERITE_PICKAXE)
                .setCustomModelData(1, !Main.optifineItemsEnabled())
                .setDisplayName(TextUtils.format("&6Pico de Netherite"))
                .build();

        ItemMeta meta = s.getItemMeta();

        if (meta != null) {

            // Pequeño bonus sobre un pico de Netherite vanilla.
            meta.addEnchant(
                    Enchantment.EFFICIENCY,
                    1,
                    true);

            meta.setUnbreakable(true);

            s.setItemMeta(meta);
        }

        return s;
    }

    public static ItemStack craftNetheriteHoe() {

        ItemStack s = new ItemBuilder(Material.NETHERITE_HOE)
                .setCustomModelData(1, !Main.optifineItemsEnabled())
                .setDisplayName(TextUtils.format("&6Azada de Netherite"))
                .build();

        ItemMeta meta = s.getItemMeta();

        if (meta != null) {

            // Pequeño bonus sobre una azada de Netherite vanilla.
            meta.addEnchant(
                    Enchantment.EFFICIENCY,
                    1,
                    true);

            meta.setUnbreakable(true);

            s.setItemMeta(meta);
        }

        return s;
    }

    public static ItemStack craftNetheriteAxe() {

        ItemStack s = new ItemBuilder(Material.NETHERITE_AXE)
                .setCustomModelData(1, !Main.optifineItemsEnabled())
                .setDisplayName(TextUtils.format("&6Hacha de Netherite"))
                .build();

        ItemMeta meta = s.getItemMeta();

        if (meta != null) {

            // Pequeño bonus sobre un hacha de Netherite vanilla.
            meta.addEnchant(
                    Enchantment.EFFICIENCY,
                    1,
                    true);

            meta.setUnbreakable(true);

            s.setItemMeta(meta);
        }

        return s;
    }

    public static ItemStack craftNetheriteShovel() {

        ItemStack s = new ItemBuilder(Material.NETHERITE_SHOVEL)
                .setCustomModelData(1, !Main.optifineItemsEnabled())
                .setDisplayName(TextUtils.format("&6Pala de Netherite"))
                .build();

        ItemMeta meta = s.getItemMeta();

        if (meta != null) {

            // Pequeño bonus sobre una pala de Netherite vanilla.
            meta.addEnchant(
                    Enchantment.EFFICIENCY,
                    1,
                    true);

            meta.setUnbreakable(true);

            s.setItemMeta(meta);
        }

        return s;
    }

    public static ItemStack craftInfernalNetheriteIngot() {

        ItemStack s = new ItemBuilder(Material.DIAMOND)
                .setCustomModelData(1, !Main.optifineItemsEnabled())
                .setDisplayName(TextUtils.format("&6Infernal Netherite Block"))
                .build();

        ItemMeta meta = s.getItemMeta();

        if (meta != null) {

            meta.setUnbreakable(true);

            meta.setLore(Arrays.asList(
                    HiddenStringUtils.encodeString(
                            "{" + UUID.randomUUID() + ": 0}")));

            s.setItemMeta(meta);
        }

        return s;
    }

    public static void slotBlock(Player p) {

        if (Main.getInstance().getDay() < 40) {
            return;
        }

        if (p.getGameMode() == GameMode.SPECTATOR
                || p.isDead()
                || !p.isOnline()) {
            return;
        }

        boolean hasEndRelic = false;
        boolean hasBeginningRelic = false;

        int[] endRelicLockedSlots;

        if (Main.getInstance().getDay() < 60) {

            endRelicLockedSlots = new int[] {
                    40, 13, 22, 31, 4
            };

        } else {

            endRelicLockedSlots = new int[] {
                    13, 22, 31, 4
            };
        }

        for (ItemStack contents : p.getInventory().getContents()) {

            if (!hasBeginningRelic && isBeginningRelic(contents)) {

                hasBeginningRelic = true;
                hasEndRelic = true;

            } else if (!hasEndRelic && isEndRelic(contents)) {

                hasEndRelic = true;
            }
        }

        int slot;

        if (Main.getInstance().getDay() >= 40) {

            for (int endRelicLockedSlot : endRelicLockedSlots) {

                slot = endRelicLockedSlot;

                if (hasEndRelic) {
                    unlockSlot(p, slot);
                } else {
                    lockSlot(p, slot);
                }
            }
        }

        if (Main.getInstance().getDay() >= 60) {

            for (int beginningRelicLockedSlot : beginningRelicLockedSlots) {

                slot = beginningRelicLockedSlot;

                if (hasBeginningRelic) {
                    unlockSlot(p, slot);
                } else {
                    lockSlot(p, slot);
                }
            }
        }
    }

    private static void lockSlot(Player p, int slot) {

        ItemStack item = p.getInventory().getItem(slot);

        if (item != null) {

            if (item.getType() != Material.AIR
                    && item.getType() != Material.STRUCTURE_VOID) {

                p.getWorld().dropItem(
                        p.getLocation(),
                        item.clone());
            }

            item.setType(Material.STRUCTURE_VOID);
            item.setAmount(1);

        } else {

            p.getInventory().setItem(
                    slot,
                    new ItemStack(Material.STRUCTURE_VOID));
        }
    }

    private static void unlockSlot(Player p, int slot) {

        ItemStack item = p.getInventory().getItem(slot);

        if (item != null
                && item.getType() == Material.STRUCTURE_VOID) {

            p.getInventory().clear(slot);
        }
    }

    public static boolean isEndRelic(ItemStack stack) {

        if (stack == null) {
            return false;
        }

        if (!stack.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = stack.getItemMeta();

        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        return stack.getType() == Material.LIGHT_BLUE_DYE
                && meta.getDisplayName()
                        .endsWith(
                                TextUtils.format("&6Reliquia Del Fin"));
    }

    public static boolean isBeginningRelic(ItemStack stack) {

        if (stack == null) {
            return false;
        }

        if (!stack.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = stack.getItemMeta();

        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        return stack.getType() == Material.CYAN_DYE
                && meta.getDisplayName()
                        .endsWith(
                                TextUtils.format("&6Reliquia del Comienzo"));
    }
}