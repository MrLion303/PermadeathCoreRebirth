package tech.layon.permadeath.event;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Camel;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sniffer;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.util.item.NetheriteArmor;

import java.util.List;
import java.util.SplittableRandom;

/**
 * Cambios de dificultad permanentes solicitados para NegativeStudios.
 * Cada cambio queda activo desde el día indicado en adelante.
 */
public final class DifficultyChanges implements Listener {

    private static final long THIRTY_MINUTES_MS = 30L * 60L * 1000L;

    private final Main plugin;
    private final SplittableRandom random = new SplittableRandom();

    private final NamespacedKey saturationGrantedKey;
    private final NamespacedKey wardenLastRollKey;
    private final NamespacedKey demonBuffedKey;
    private final NamespacedKey xpBottleRecipeKey;
    private final NamespacedKey batAttackOriginalKey;
    private final NamespacedKey dragonHealthOriginalKey;

    private boolean xpBottleRecipeRegistered = false;

    public DifficultyChanges(Main plugin) {
        this.plugin = plugin;

        this.saturationGrantedKey = new NamespacedKey(plugin, "day45_saturation_granted");
        this.wardenLastRollKey = new NamespacedKey(plugin, "day60_warden_last_roll");
        this.demonBuffedKey = new NamespacedKey(plugin, "day35_demon_buffed");
        this.xpBottleRecipeKey = new NamespacedKey(plugin, "day15_xp_bottles");
        this.batAttackOriginalKey = new NamespacedKey(plugin, "difficulty_bat_attack_original");
        this.dragonHealthOriginalKey = new NamespacedKey(plugin, "difficulty_dragon_health_original");

        // Daños por inventario/bloques, Darkness, agua, Elytras, Saturación y Warden de día 60.
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickPlayers, 20L, 20L);

        // Reaplica los cambios a mobs que ya estaban cargados al cambiar de día.
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshLoadedMobs, 40L, 200L);
    }

    private void tickPlayers() {
        registerXpBottleRecipeIfNeeded();

        long day = plugin.getDay();
        if (day < 15) {
            xpBottleRecipeRegistered = false;
            Bukkit.removeRecipe(xpBottleRecipeKey);
        }

        if (day < 25) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.isDead() || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            if (day < 45 && player.getPersistentDataContainer().has(saturationGrantedKey, PersistentDataType.BYTE)) {
                player.removePotionEffect(PotionEffectType.SATURATION);
                player.getPersistentDataContainer().remove(saturationGrantedKey);
            }

            // Día 25: herramientas antiguas y armadura de Netherite vanilla arden.
            if (hasBurningDay25Item(player)) {
                applyInstantDamage(player, 0);
            }

            // Día 35: bloques peligrosos y Darkness aleatorio durante la noche.
            if (day >= 35) {
                Material floor = getFloorMaterial(player);
                if (isProgressiveDamageFloor(floor, day)) {
                    applyInstantDamage(player, 1);
                }

                long time = player.getWorld().getTime();
                if (time >= 13000L && time <= 23000L && random.nextInt(300) == 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 10, 0));
                }
            }

            // Día 45: Saturación se entrega exactamente una vez por jugador.
            if (day >= 45 && !player.getPersistentDataContainer().has(
                    saturationGrantedKey, PersistentDataType.BYTE)) {

                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SATURATION,
                        PotionEffect.INFINITE_DURATION,
                        0,
                        false,
                        false,
                        true));

                player.getPersistentDataContainer().set(
                        saturationGrantedKey,
                        PersistentDataType.BYTE,
                        (byte) 1);
            }

            if (day >= 60) {
                // Herramientas de Netherite vanilla, excepto espada, hacen daño de nivel II.
                if (hasVanillaNetheriteTool(player)) {
                    applyInstantDamage(player, 1);
                }

                // Agua: Wither II renovado mientras el jugador la toca.
                if (isTouchingWater(player)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 1));
                }

                // Cualquier Elytra, incluyendo la Infernal.
                ItemStack chest = player.getInventory().getChestplate();
                if (chest != null && chest.getType() == Material.ELYTRA) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 1));
                }

                processDay60WardenRoll(player);
            }
        }
    }

    private void refreshLoadedMobs() {
        if (plugin.getDay() < 5) {
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity instanceof Player || !entity.isValid() || entity.isDead()) {
                    continue;
                }
                removeOutdatedChanges(entity);
                applyMobChanges(entity);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        applyMobChanges(event.getEntity());
    }

    private void applyMobChanges(LivingEntity entity) {
        long day = plugin.getDay();
        removeOutdatedChanges(entity);

        if (day >= 5) {
            if (entity instanceof Warden) {
                addPermanentEffect(entity, PotionEffectType.SPEED, 3);
                addPermanentEffect(entity, PotionEffectType.RESISTANCE, 2);
            }

            if (entity instanceof PiglinBrute brute) {
                EntityEquipment equipment = brute.getEquipment();
                if (equipment != null) {
                    ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
                    ItemMeta meta = axe.getItemMeta();
                    if (meta != null) {
                        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                        axe.setItemMeta(meta);
                    }
                    equipment.setItemInMainHand(axe);
                    equipment.setItemInMainHandDropChance(0.0F);
                }
            }

            if (entity instanceof Piglin) {
                addPermanentEffect(entity, PotionEffectType.SPEED, 2);
                addPermanentEffect(entity, PotionEffectType.STRENGTH, 2);
                addPermanentEffect(entity, PotionEffectType.FIRE_RESISTANCE, 1);
            }
        }

        if (day >= 10) {
            if (entity instanceof Frog) {
                addPermanentEffect(entity, PotionEffectType.STRENGTH, 50);
            }

            if (entity instanceof Warden) {
                addPermanentEffect(entity, PotionEffectType.INVISIBILITY, 1);
            }
        }

        if (day >= 15) {
            if (entity instanceof Allay allay) {
                EntityEquipment equipment = allay.getEquipment();
                if (equipment != null) {
                    ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
                    ItemMeta meta = sword.getItemMeta();
                    if (meta != null) {
                        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                        sword.setItemMeta(meta);
                    }
                    equipment.setItemInMainHand(sword);
                    equipment.setItemInMainHandDropChance(0.0F);
                }
            }

            if (entity instanceof Camel) {
                addPermanentEffect(entity, PotionEffectType.SPEED, 3);
                addPermanentEffect(entity, PotionEffectType.RESISTANCE, 2);
            }

            if (entity instanceof Bat bat) {
                addPermanentEffect(entity, PotionEffectType.STRENGTH, 4);
                addPermanentEffect(entity, PotionEffectType.RESISTANCE, 3);

                AttributeInstance attackDamage = bat.getAttribute(Attribute.ATTACK_DAMAGE);
                if (attackDamage == null) {
                    plugin.getNmsAccessor().registerAttribute(Attribute.ATTACK_DAMAGE, 20.0D, bat);
                    attackDamage = bat.getAttribute(Attribute.ATTACK_DAMAGE);
                }
                if (attackDamage != null) {
                    rememberAttribute(attackDamage, bat, batAttackOriginalKey);
                    attackDamage.setBaseValue(20.0D);
                }
            }

            if (entity instanceof Illusioner) {
                addPermanentEffect(entity, PotionEffectType.SPEED, 1);
                addPermanentEffect(entity, PotionEffectType.STRENGTH, 1);
                addPermanentEffect(entity, PotionEffectType.RESISTANCE, 1);
            }
        }

        if (day >= 35 && entity instanceof EnderDragon dragon) {
            buffPermadeathDemon(dragon);
        }
    }

    private void removeOutdatedChanges(LivingEntity entity) {
        long day = plugin.getDay();

        if (day < 5) {
            if (entity instanceof Warden || entity instanceof Piglin || entity instanceof PiglinBrute) {
                removeEffect(entity, PotionEffectType.SPEED);
                removeEffect(entity, PotionEffectType.RESISTANCE);
            }
            if (entity instanceof Piglin) {
                removeEffect(entity, PotionEffectType.STRENGTH);
                removeEffect(entity, PotionEffectType.FIRE_RESISTANCE);
            }
            removeTaggedEffect(entity, PotionEffectType.SPEED);
            removeTaggedEffect(entity, PotionEffectType.RESISTANCE);
            removeTaggedEffect(entity, PotionEffectType.FIRE_RESISTANCE);
        }
        if (day < 10) {
            if (entity instanceof Warden) removeEffect(entity, PotionEffectType.INVISIBILITY);
            removeTaggedEffect(entity, PotionEffectType.INVISIBILITY);
        }
        if (day < 15) {
            if (entity instanceof Camel || entity instanceof Bat || entity instanceof Illusioner) {
                removeEffect(entity, PotionEffectType.STRENGTH);
                removeEffect(entity, PotionEffectType.SPEED);
                removeEffect(entity, PotionEffectType.RESISTANCE);
            }
        }
        if (day < 15) {
            restoreAttribute(entity, batAttackOriginalKey);
        }
        if (day < 35) {
            restoreAttribute(entity, dragonHealthOriginalKey);
        }
        if (entity.getPersistentDataContainer().has(saturationGrantedKey, PersistentDataType.BYTE) && day < 45) {
            entity.removePotionEffect(PotionEffectType.SATURATION);
            entity.getPersistentDataContainer().remove(saturationGrantedKey);
        }
    }

    private void removeEffect(LivingEntity entity, PotionEffectType type) {\n        entity.removePotionEffect(type);\n    }\n\n    private void removeTaggedEffect(LivingEntity entity, PotionEffectType type) {
        String keyName = "difficulty_effect_" + type.getName().toLowerCase();
        NamespacedKey key = new NamespacedKey(plugin, keyName);
        if (entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            entity.removePotionEffect(type);
            entity.getPersistentDataContainer().remove(key);
        }
    }

    private void rememberAttribute(AttributeInstance attribute, LivingEntity entity, NamespacedKey key) {
        if (!entity.getPersistentDataContainer().has(key, PersistentDataType.DOUBLE)) {
            entity.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, attribute.getBaseValue());
        }
    }

    private void restoreAttribute(LivingEntity entity, NamespacedKey key) {
        Double original = entity.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
        if (original != null) {
            Attribute attribute = key.equals(batAttackOriginalKey) ? Attribute.ATTACK_DAMAGE : Attribute.MAX_HEALTH;
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                instance.setBaseValue(original);
                if (attribute == Attribute.MAX_HEALTH) {
                    entity.setHealth(Math.min(entity.getHealth(), original));
                }
            }
            entity.getPersistentDataContainer().remove(key);
            entity.getPersistentDataContainer().remove(demonBuffedKey);
        }
    }

    private void addPermanentEffect(LivingEntity entity, PotionEffectType type, int displayedLevel) {
        int amplifier = Math.max(0, displayedLevel - 1);
        PotionEffect current = entity.getPotionEffect(type);

        if (current != null
                && current.getAmplifier() >= amplifier
                && current.getDuration() == PotionEffect.INFINITE_DURATION) {
            return;
        }

        entity.addPotionEffect(new PotionEffect(
                type,
                PotionEffect.INFINITE_DURATION,
                amplifier,
                false,
                true,
                true));
        entity.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "difficulty_effect_" + type.getName().toLowerCase()),
                PersistentDataType.BYTE,
                (byte) 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRaidWave(RaidSpawnWaveEvent event) {
        if (plugin.getDay() < 5 || event.getRaid().getBadOmenLevel() < 3) {
            return;
        }

        LocationHolder holder = getRaidSpawnLocation(event);
        if (holder == null) {
            return;
        }

        Illusioner illusioner = holder.world.spawn(holder.location, Illusioner.class);
        illusioner.setRemoveWhenFarAway(false);
        applyMobChanges(illusioner);
    }

    private LocationHolder getRaidSpawnLocation(RaidSpawnWaveEvent event) {
        if (!event.getRaiders().isEmpty()) {
            Entity raider = event.getRaiders().get(0);
            return new LocationHolder(raider.getWorld(), raider.getLocation());
        }

        if (event.getRaid().getLocation() != null) {
            return new LocationHolder(event.getWorld(), event.getRaid().getLocation());
        }

        return null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        long day = plugin.getDay();

        if (day >= 15 && event.getDamager() instanceof Spider && event.getEntity() instanceof Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 3, 254));

            Block feet = player.getLocation().getBlock();
            if (feet.getType().isAir()) {
                feet.setType(Material.COBWEB);
            }
        }

        if (day >= 15 && event.getDamager() instanceof Bat) {
            event.setDamage(20.0D);
        }

        if (day >= 35
                && event.getDamager() instanceof EnderDragon dragon
                && plugin.endWorld != null
                && dragon.getWorld().equals(plugin.endWorld)) {
            event.setDamage(event.getDamage() * 1.25D);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (plugin.getDay() < 35 || !(event.getEntity() instanceof Sniffer)) {
            return;
        }

        // Drops buenos vanilla para el Sniffer.
        event.getDrops().add(new ItemStack(Material.DIAMOND, random.nextInt(2, 5)));
        event.getDrops().add(new ItemStack(Material.EMERALD, random.nextInt(4, 11)));
        event.getDrops().add(new ItemStack(Material.GOLDEN_APPLE, 1));

        if (random.nextInt(100) < 25) {
            event.getDrops().add(new ItemStack(Material.NETHERITE_SCRAP, random.nextInt(1, 3)));
        }

        if (random.nextInt(100) < 5) {
            event.getDrops().add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (plugin.getDay() >= 45
                && event.getEntity() instanceof Player
                && event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            event.setDamage(2048.0D);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        long day = plugin.getDay();
        if (day < 55) {
            return;
        }

        Player player = event.getPlayer();
        Material material = event.getClickedBlock().getType();

        // Día 55: puertas, vallas, trampillas y botones son instakill.
        if (isDay55InstantKillBlock(material)) {
            player.damage(2048.0D);
            return;
        }

        // Día 55: cualquier cofre o Shulker Box hace daño equivalente a Instant Damage II.
        if (isChestOrShulker(material)) {
            applyInstantDamage(player, 1);
            return;
        }

        // Día 60: cualquier otro bloque interactuable hace Instant Damage I.
        if (day >= 60 && material.isInteractable()) {
            applyInstantDamage(player, 0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        long day = plugin.getDay();
        if (day < 30) {
            return;
        }

        // Se hace un tick después para que se apliquen primero los efectos vanilla del tótem.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || player.isDead()) {
                return;
            }

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.MINING_FATIGUE,
                    20 * 60 * 2,
                    0));

            if (plugin.getDay() >= 60) {
                teleportToNearestMob(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (plugin.getDay() < 32
                || event.getModifiedType() != PotionEffectType.INVISIBILITY
                || event.getNewEffect() == null) {
            return;
        }

        EntityPotionEffectEvent.Cause cause = event.getCause();
        if (cause == EntityPotionEffectEvent.Cause.POTION_DRINK
                || cause == EntityPotionEffectEvent.Cause.POTION_SPLASH
                || cause == EntityPotionEffectEvent.Cause.AREA_EFFECT_CLOUD) {
            event.setCancelled(true);
        }
    }

    private void registerXpBottleRecipeIfNeeded() {
        if (xpBottleRecipeRegistered || plugin.getDay() < 15) {
            return;
        }

        ItemStack result = new ItemStack(Material.EXPERIENCE_BOTTLE, 8);
        ShapedRecipe recipe = new ShapedRecipe(xpBottleRecipeKey, result);
        recipe.shape(" L ", "LDL", " L ");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('L', Material.LAPIS_LAZULI);

        try {
            Bukkit.addRecipe(recipe);
        } catch (IllegalStateException ignored) {
            // Ya existía en el registro actual.
        }

        xpBottleRecipeRegistered = true;
    }

    private void buffPermadeathDemon(EnderDragon dragon) {
        if (plugin.endWorld == null || !dragon.getWorld().equals(plugin.endWorld)) {
            return;
        }

        if (dragon.getPersistentDataContainer().has(demonBuffedKey, PersistentDataType.BYTE)) {
            return;
        }

        AttributeInstance maxHealth = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        rememberAttribute(maxHealth, dragon, dragonHealthOriginalKey);
        double oldMax = Math.max(1.0D, maxHealth.getBaseValue());
        double configuredHealth = plugin.getConfig().getDouble("Toggles.End.PermadeathDemon.Health");
        if (configuredHealth <= 0.0D) {
            configuredHealth = oldMax;
        }

        double targetMax = Math.max(oldMax, configuredHealth * 1.50D);
        double healthRatio = Math.max(0.0D, Math.min(1.0D, dragon.getHealth() / oldMax));

        maxHealth.setBaseValue(targetMax);
        dragon.setHealth(Math.max(1.0D, Math.min(targetMax, targetMax * healthRatio)));
        dragon.getPersistentDataContainer().set(demonBuffedKey, PersistentDataType.BYTE, (byte) 1);
    }

    private boolean hasBurningDay25Item(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            Material type = item.getType();
            if (isLegacyTool(type)) {
                return true;
            }

            if (isVanillaNetheriteArmor(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLegacyTool(Material material) {
        String name = material.name();
        boolean validTier = name.startsWith("WOODEN_")
                || name.startsWith("STONE_")
                || name.startsWith("IRON_")
                || name.startsWith("GOLDEN_")
                || name.startsWith("DIAMOND_");

        if (!validTier) {
            return false;
        }

        return name.endsWith("_SWORD")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE");
    }

    private boolean isVanillaNetheriteArmor(ItemStack item) {
        Material material = item.getType();
        boolean netheriteArmor = material == Material.NETHERITE_HELMET
                || material == Material.NETHERITE_CHESTPLATE
                || material == Material.NETHERITE_LEGGINGS
                || material == Material.NETHERITE_BOOTS;

        return netheriteArmor && !NetheriteArmor.isNetheritePiece(item);
    }

    private boolean hasVanillaNetheriteTool(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            Material material = item.getType();
            boolean dangerous = material == Material.NETHERITE_PICKAXE
                    || material == Material.NETHERITE_AXE
                    || material == Material.NETHERITE_SHOVEL
                    || material == Material.NETHERITE_HOE;

            if (dangerous && !isPluginNetheriteTool(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPluginNetheriteTool(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.isUnbreakable() || !meta.hasDisplayName()) {
            return false;
        }

        String name = ChatColor.stripColor(meta.getDisplayName());
        if (name == null) {
            return false;
        }

        return name.equals("Pico de Netherite")
                || name.equals("Hacha de Netherite")
                || name.equals("Pala de Netherite")
                || name.equals("Azada de Netherite")
                || name.equals("Espada de Netherite");
    }

    private Material getFloorMaterial(Player player) {
        return player.getLocation().clone().subtract(0.0D, 0.1D, 0.0D).getBlock().getType();
    }

    private boolean isProgressiveDamageFloor(Material material, long day) {
        if (material == Material.STONECUTTER) {
            return day >= 35;
        }

        if (day >= 57 && (material == Material.GRASS_BLOCK || material.name().endsWith("_SLAB"))) {
            return true;
        }

        return day >= 60 && (material == Material.PURPUR_BLOCK || material == Material.MAGMA_BLOCK);
    }

    private boolean isTouchingWater(Player player) {
        Material feet = player.getLocation().getBlock().getType();
        Material eyes = player.getEyeLocation().getBlock().getType();
        return feet == Material.WATER || eyes == Material.WATER;
    }

    private boolean isChestOrShulker(Material material) {
        return material == Material.CHEST
                || material == Material.TRAPPED_CHEST
                || material == Material.ENDER_CHEST
                || material.name().endsWith("SHULKER_BOX");
    }

    private boolean isDay55InstantKillBlock(Material material) {
        String name = material.name();
        return name.endsWith("_DOOR")
                || name.endsWith("_TRAPDOOR")
                || name.endsWith("_BUTTON")
                || name.endsWith("_FENCE")
                || name.endsWith("_FENCE_GATE");
    }

    private void applyInstantDamage(Player player, int amplifier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, amplifier));
    }

    private void processDay60WardenRoll(Player player) {
        long now = System.currentTimeMillis();
        Long lastRoll = player.getPersistentDataContainer().get(
                wardenLastRollKey,
                PersistentDataType.LONG);

        if (lastRoll == null) {
            player.getPersistentDataContainer().set(
                    wardenLastRollKey,
                    PersistentDataType.LONG,
                    now);
            return;
        }

        if (now - lastRoll < THIRTY_MINUTES_MS) {
            return;
        }

        player.getPersistentDataContainer().set(
                wardenLastRollKey,
                PersistentDataType.LONG,
                now);

        if (random.nextInt(100) == 0) {
            Warden warden = player.getWorld().spawn(player.getLocation(), Warden.class);
            applyMobChanges(warden);
        }
    }

    private void teleportToNearestMob(Player player) {
        Mob nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        List<LivingEntity> entities = player.getWorld().getLivingEntities();
        for (LivingEntity living : entities) {
            if (!(living instanceof Mob mob) || living.equals(player) || living.isDead() || !living.isValid()) {
                continue;
            }

            double distance = living.getLocation().distanceSquared(player.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = mob;
            }
        }

        if (nearest != null) {
            player.teleport(nearest.getLocation());
        }
    }

    private record LocationHolder(World world, org.bukkit.Location location) {
    }
}
