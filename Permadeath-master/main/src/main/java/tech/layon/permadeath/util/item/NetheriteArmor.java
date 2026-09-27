package tech.layon.permadeath.util.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.util.TextUtils;
import tech.layon.permadeath.util.lib.ItemBuilder;

public final class NetheriteArmor implements Listener {

    private static final String HELMET_NAME = TextUtils.format("&5Netherite Helmet");

    private static final String CHEST_NAME = TextUtils.format("&5Netherite Chestplate");

    private static final String LEG_NAME = TextUtils.format("&5Netherite Leggings");

    private static final String BOOT_NAME = TextUtils.format("&5Netherite Boots");

    /*
     * Todas las piezas nuevas llevan esta etiqueta.
     *
     * Esto evita depender solamente del nombre visible
     * del objeto para identificar la armadura especial.
     */
    private static NamespacedKey getNetheritePieceKey() {

        return new NamespacedKey(
                Main.getInstance(),
                "negative_netherite_piece");
    }

    public static ItemStack craftNetheriteHelmet() {

        return createNetheritePiece(
                Material.NETHERITE_HELMET,
                HELMET_NAME);
    }

    public static ItemStack craftNetheriteChest() {

        return createNetheritePiece(
                Material.NETHERITE_CHESTPLATE,
                CHEST_NAME);
    }

    public static ItemStack craftNetheriteLegs() {

        return createNetheritePiece(
                Material.NETHERITE_LEGGINGS,
                LEG_NAME);
    }

    public static ItemStack craftNetheriteBoots() {

        return createNetheritePiece(
                Material.NETHERITE_BOOTS,
                BOOT_NAME);
    }

    /*
     * Crea una pieza utilizando Netherite REAL.
     *
     * Ya no es necesario simular:
     *
     * Attribute.ARMOR
     * Attribute.ARMOR_TOUGHNESS
     *
     * Minecraft proporciona automáticamente los atributos
     * correctos correspondientes a cada pieza de Netherite.
     */
    private static ItemStack createNetheritePiece(
            Material material,
            String displayName) {

        ItemStack item = new ItemBuilder(material, 1)
                .setDisplayName(displayName)
                .build();

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setUnbreakable(true);

        meta.getPersistentDataContainer().set(
                getNetheritePieceKey(),
                PersistentDataType.BYTE,
                (byte) 1);

        item.setItemMeta(meta);

        return item;
    }

    public static boolean isNetheritePiece(ItemStack stack) {

        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = stack.getItemMeta();

        if (meta == null) {
            return false;
        }

        /*
         * Sistema nuevo.
         */
        if (meta.getPersistentDataContainer().has(
                getNetheritePieceKey(),
                PersistentDataType.BYTE)) {

            return true;
        }

        /*
         * Compatibilidad con objetos creados por versiones
         * anteriores del plugin.
         *
         * Esto permite que una armadura antigua de cuero
         * llamada "Netherite ..." continúe siendo reconocida.
         */
        if (meta.isUnbreakable() && meta.hasDisplayName()) {

            String name = ChatColor.stripColor(
                    meta.getDisplayName());

            return name != null
                    && name.startsWith("Netherite");
        }

        return false;
    }

    public static boolean isInfernalPiece(ItemStack stack) {

        if (stack == null) {
            return false;
        }

        if (stack.hasItemMeta()) {

            ItemMeta meta = stack.getItemMeta();

            if (meta == null) {
                return false;
            }

            if (stack.getType() == Material.ELYTRA
                    && meta.hasCustomModelData()
                    && meta.getCustomModelData() == 1) {

                return true;
            }

            if (meta.isUnbreakable()
                    && meta.hasDisplayName()) {

                String name = ChatColor.stripColor(
                        meta.getDisplayName());

                return name != null
                        && name.startsWith("Infernal");
            }
        }

        return false;
    }

    public static void setupHealth(Player p) {

        double maxHealth = getAvailableMaxHealth(p);

        if (p.getAttribute(Attribute.MAX_HEALTH) != null) {

            p.getAttribute(Attribute.MAX_HEALTH)
                    .setBaseValue(maxHealth);
        }
    }

    public static Double getAvailableMaxHealth(Player p) {

        int currentNetheritePieces = 0;
        int currentInfernalPieces = 0;

        boolean doPlayerAteOne = p.getPersistentDataContainer().has(
                new NamespacedKey(
                        Main.getInstance(),
                        "hyper_one"),
                PersistentDataType.BYTE);

        boolean doPlayerAteTwo = p.getPersistentDataContainer().has(
                new NamespacedKey(
                        Main.getInstance(),
                        "hyper_two"),
                PersistentDataType.BYTE);

        for (ItemStack contents : p.getInventory().getArmorContents()) {

            if (isNetheritePiece(contents)) {
                currentNetheritePieces++;
            }

            if (isInfernalPiece(contents)) {
                currentInfernalPieces++;
            }
        }

        double maxHealth = 20.0D;

        if (doPlayerAteOne) {
            maxHealth += 4.0D;
        }

        if (doPlayerAteTwo) {
            maxHealth += 4.0D;
        }

        /*
         * Bonus original del set completo.
         *
         * +8 HP = +4 corazones.
         */
        if (currentNetheritePieces >= 4) {
            maxHealth += 8.0D;
        }

        /*
         * Bonus original del set Infernal completo.
         */
        if (currentInfernalPieces >= 4) {

            maxHealth += 10.0D;

            p.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.RESISTANCE,
                            20 * 3,
                            0));
        }

        if (Main.getInstance().getDay() >= 40) {

            // 12 HP = 6 corazones.
            maxHealth -= 8.0D;

            if (Main.getInstance().getDay() >= 60) {

                // 4 HP = 2 corazones.
                maxHealth -= 8.0D;

                boolean hasOrb = checkForOrb(p);

                if (!hasOrb) {
                    maxHealth -= 16.0D;
                }
            }
        }

        return Math.max(
                maxHealth,
                0.000001D);
    }

    public static boolean checkForOrb(Player p) {

        if (Main.getInstance()
                .getOrbEvent()
                .isRunning()) {

            return true;
        }

        for (ItemStack stack : p.getInventory().getContents()) {

            if (stack == null
                    || !stack.hasItemMeta()) {

                continue;
            }

            ItemMeta meta = stack.getItemMeta();

            if (meta != null
                    && stack.getType() == Material.BROWN_DYE
                    && meta.isUnbreakable()) {

                return true;
            }
        }

        return false;
    }
}