package tech.layon.permadeath;

import lombok.Getter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.logging.log4j.LogManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tech.layon.permadeath.data.Messages;
import tech.layon.permadeath.util.*;
import tech.layon.permadeath.util.events.LifeOrbEvent;
import tech.layon.permadeath.util.events.ShellEvent;
import tech.layon.permadeath.util.item.NetheriteArmor;
import tech.layon.permadeath.util.item.PermadeathItems;
import tech.layon.permadeath.util.lib.FileAPI;
import tech.layon.permadeath.util.lib.UpdateChecker;
import tech.layon.permadeath.data.BeginningDataManager;
import tech.layon.permadeath.data.DateManager;
import tech.layon.permadeath.data.EndDataManager;
import tech.layon.permadeath.data.PlayerDataManager;
import tech.layon.permadeath.util.log.Log4JFilter;
import tech.layon.permadeath.util.log.PDCLog;
import tech.layon.permadeath.util.item.RecipeManager;
import tech.layon.permadeath.discord.DiscordPortal;
import tech.layon.permadeath.end.EndManager;
import tech.layon.permadeath.event.block.BlockListener;
import tech.layon.permadeath.event.DifficultyChanges;
import tech.layon.permadeath.event.entity.EntityEvents;
import tech.layon.permadeath.util.mob.CustomSkeletons;
import tech.layon.permadeath.event.entity.SpawnListener;
import tech.layon.permadeath.event.entity.TotemListener;
import tech.layon.permadeath.event.HostileEntityListener;
import tech.layon.permadeath.event.paper.PaperListeners;
import tech.layon.permadeath.event.player.AnvilListener;
import tech.layon.permadeath.event.player.PlayerListener;
import tech.layon.permadeath.event.player.SlotBlockListener;
import tech.layon.permadeath.event.raid.RaidEvents;
import tech.layon.permadeath.event.world.WorldEvents;
import tech.layon.permadeath.util.interfaces.InfernalNetheriteBlock;
import tech.layon.permadeath.util.interfaces.NMSAccessor;
import tech.layon.permadeath.util.interfaces.NMSHandler;
import tech.layon.permadeath.task.EndTask;
import tech.layon.permadeath.world.beginning.BeginningManager;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Filter;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Main extends JavaPlugin implements Listener {

    private static final int CURRENT_CONFIG_VERSION = 3;
    public static boolean DEBUG = false;
    public static boolean DISABLED_LINGERING = false;

    public static boolean SPEED_RUN_MODE = false;
    @Getter
    public static Main instance;
    public static String prefix = "";
    @Getter
    public static boolean runningPaperSpigot = false;
    public static boolean worldEditFound;
    private final SplittableRandom random = new SplittableRandom();
    public World world = null;
    public World endWorld = null;
    private int playTime = 0;
    private HostileEntityListener hostile;
    private RecipeManager recipes;
    @Getter
    private Messages messages;
    @Getter
    private EndTask task = null;
    private EndManager endManager;
    private MobFactory factory;
    private BeginningManager begginingManager;
    @Getter
    private BeginningDataManager beData;
    @Getter
    private EndDataManager endData;
    private Map<Integer, Boolean> registeredDays = new HashMap<>();
    private ArrayList<Player> doneEffectPlayers = new ArrayList<>();
    private boolean loaded = false;
    private boolean alreadyRegisteredChanges = false;
    private ShellEvent shulkerEvent;
    private LifeOrbEvent orbEvent;
    private SpawnListener spawnListener;
    private DifficultyChanges difficultyChanges;
    private PlayerListener playerListener;
    private Listener slotBlockListener;
    private Listener paperListeners;
    private boolean beginningWarningShown = false;
    private int witherTimer = 0;
    private long lastGlobalMobDay = -1L;
    private int eventTickAccumulator = 0;
    private org.apache.logging.log4j.core.Filter rootLogFilter;
    private Filter legacyLogFilter;

    public static boolean optifineItemsEnabled() {
        if (instance == null) return false;
        return instance.getConfig().getBoolean("Toggles.OptifineItems");
    }

    @Override
    public void onEnable() {
        instance = this;

        this.saveDefaultConfig();
        setupConsoleFilter();

        prefix = TextUtils.format((getConfig().contains("Prefix") ? getConfig().getString("Prefix") : "&cPermadeath &7➤ &f"));

        tickAll();

        this.playTime = getConfig().getInt("DontTouch.PlayTime");
    }

    @Override
    public void onLoad() {
        instance = this;
        try {
            NMS.loadInfernalNetheriteBlock();
            NMS.loadNMSAccessor();
            NMS.loadNMSHandler();

            getNmsAccessor().registerHostileMobs();
        } catch (Exception ex) {
            ex.printStackTrace();
            Bukkit.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {

        getConfig().set("DontTouch.PlayTime", this.playTime);
        saveConfig();
        reloadConfig();

        DiscordPortal.onDisable();
        DateManager.reset();
        removeConsoleFilter();

        Bukkit.getConsoleSender().sendMessage(TextUtils.format("&f&m------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("             &c&lPERMADEATH"));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("     &7- Desactivando el Plugin."));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("&f&m------------------------------------------"));

        this.instance = null;
    }

    private void tickAll() {
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {

                if (!getFile().exists()) {
                    saveDefaultConfig();
                }

                if (!loaded) {

                    if (DiscordPortal.isJDAInstalled()) {
                        Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&aSe ha encontrado la librería JDA, cargando bot de Discord."));
                        DiscordPortal.reload();
                    } else {
                        Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&cNo se ha encontrado una librería JDA, deberás instalar una en caso de querer utilizar un bot de Discord."));
                        Bukkit.getConsoleSender().sendMessage(TextUtils.format("&eDescarga aquí: &f" + Utils.SPIGOT_LINK));
                        Bukkit.getConsoleSender().sendMessage(TextUtils.format("&eSi no puedes descargarlo allí, únete a este Discord y te daremos acceso al enlace: &ehttps://discord.gg/nKxBA7qzAU"));
                    }

                    // Valida la versión antes de completar claves para no convertir una config antigua en "válida" accidentalmente.
                    reloadConfig();
                    boolean resetConfig = !getConfig().contains("config-version");
                    if (!resetConfig) {
                        try {
                            resetConfig = getConfig().getInt("config-version") != CURRENT_CONFIG_VERSION;
                        } catch (Exception ignored) {
                            resetConfig = true;
                        }
                    }

                    if (resetConfig) {
                        Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&eEstamos eliminando config.yml debido a que está desactualizado."));
                        PDCLog.getInstance().log("Eliminando config.yml por versión antigua.");
                        getFile().delete();
                        saveDefaultConfig();
                        reloadConfig();
                    }

                    // Completa las claves que falten antes de arrancar los sistemas que leen la configuración.
                    setupConfig();
                    reloadConfig();
                    startPlugin();

                    // Las transformaciones globales se sincronizan después de iniciar; los cambios exclusivos del spawn no se reaplican.
                    loaded = true;
                }

                DateManager.getInstance().tick();
                registerListeners();

                long currentGlobalMobDay = getDay();
                if (lastGlobalMobDay != currentGlobalMobDay) {
                    applyGlobalMobChanges();
                    if (currentGlobalMobDay < 50) {
                        enforceBeginningAccess();
                    }
                    lastGlobalMobDay = currentGlobalMobDay;
                }

                if (Bukkit.getOnlinePlayers().size() >= 1 && SPEED_RUN_MODE) {
                    playTime++;

                    if (playTime % (3600) == 0) {
                        Bukkit.broadcastMessage(prefix + TextUtils.format("&cFelicitaciones, han avanzado a la hora número: " + getDay()));
                        if (getDay() >= 50 && getBeData() != null && getBeginningManager() != null) {
            BeginningDataManager data = getBeData();
            World beginningWorld = getBeginningManager().getBeginningWorld();
            if (beginningWorld != null && !data.killedED()) {
                Chunk c = beginningWorld.getBlockAt(0, 100, 0).getChunk();
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = beginningWorld.getMaxHeight() - 1; y > 0; y--) {
                            Block b = c.getBlock(x, y, z);
                            if (b.getType() == Material.END_GATEWAY || b.getType() == Material.BEDROCK) {
                                b.setType(Material.AIR);
                            }
                        }
                    }
                }
                for (EnderDragon dragon : beginningWorld.getEntitiesByClass(EnderDragon.class)) {
                    dragon.remove();
                }
                if (!beginningWorld.getEntitiesByClass(EnderDragon.class).isEmpty()) {
                    data.setKilledED();
                } else {
                    data.setKilledED();
                }
            }
        }

        if (getDay() >= 60) {
            witherTimer++;
            if (witherTimer >= 2400) {
                Player target = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.getGameMode() == GameMode.SURVIVAL && p.getWorld() != null)
                        .findFirst().orElse(null);
                if (target != null) {
                    Wither wither = target.getWorld().spawn(target.getLocation().clone().add(0, 5, 0), Wither.class);
                    try {
                        Object nmsw = wither.getClass().getDeclaredMethod("getHandle").invoke(wither);
                        nmsw.getClass().getDeclaredMethod("r", int.class).invoke(nmsw, 100);
                    } catch (Exception ignored) {
                    }
                }
                witherTimer = 0;
            }
        } else {
            witherTimer = 0;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 100.0F, 100.0F);
                        }
                    }
                }

                if (getDay() >= 60 && !getConfig().getBoolean("DontTouch.Event.LifeOrbEnded") && !getOrbEvent().isRunning()) {
                    if (SPEED_RUN_MODE) orbEvent.setTimeLeft(60 * 8);
                    orbEvent.setRunning(true);
                }

                tickEvents();
                tickPlayers();
                tickWorlds();
            }
        }, 0, 30L);
    }

    /**
     * Reprocesa las entidades cargadas cuando cambia el día.
     *
     * Solo se ejecutan aquí transformaciones globales e irreversibles. No se
     * vuelven a aplicar las modificaciones de spawn (equipamiento, tiradas,
     * variantes aleatorias, etc.), evitando convertir un cambio de día en un
     * buff retroactivo de todo el servidor.
     */
    private void applyGlobalMobChanges() {
        if (spawnListener == null && hostile == null) return;

        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : new ArrayList<>(world.getLivingEntities())) {
                if (entity.isDead()) continue;
                if (spawnListener != null) {
                    spawnListener.applyDayChanges(entity);
                }
                if (hostile != null) {
                    hostile.applyToExisting(entity);
                }
            }
        }
    }

    private void tickWorlds() {
        if (this.getDay() >= 40) {
            for (World w : Bukkit.getWorlds().stream().filter(world1 -> world1.getEnvironment() != World.Environment.THE_END).collect(Collectors.toList())) {
                for (Ravager ravager : w.getEntitiesByClass(Ravager.class)) {
                    if (ravager.getPersistentDataContainer().has(new NamespacedKey(instance, "ultra_ravager"), PersistentDataType.BYTE)) {
                        List<Block> b = ravager.getLineOfSight(null, 5);

                        for (Block block : b) {
                            for (int i = -1; i < 1; i++) {
                                for (int j = -1; j < 1; j++) {
                                    for (int k = -1; k < 1; k++) {
                                        Block r = block.getRelative(i, j, k);
                                        if (r.getType() == Material.NETHERRACK) {
                                            r.setType(Material.AIR);
                                            r.getWorld().playSound(r.getLocation(), Sound.BLOCK_STONE_BREAK, 2.0F, 1.0F);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void tickPlayers() {

        if (Bukkit.getOnlinePlayers().size() < 1) return;

        long segundosbrutos = world.getWeatherDuration() / 20;

        long hours = segundosbrutos % 86400 / 3600;
        long minutes = (segundosbrutos % 3600) / 60;
        long seconds = segundosbrutos % 60;
        long days = segundosbrutos / 86400;

        final String time = String.format((days >= 1 ? String.format("%02d día(s) ", days) : "") + "%02d:%02d:%02d", hours, minutes, seconds);

        this.doneEffectPlayers.removeIf(player -> player == null || !player.isOnline());

        for (Player player : Bukkit.getOnlinePlayers()) {

            World w = player.getWorld();

            if (this.shulkerEvent.isRunning()) {
                if (!this.shulkerEvent.getBossBar().getPlayers().contains(player)) {
                    this.shulkerEvent.getBossBar().addPlayer(player);
                }
            }

            if (this.orbEvent.isRunning()) {
                if (!this.orbEvent.getBossBar().getPlayers().contains(player)) {
                    this.orbEvent.getBossBar().addPlayer(player);
                }
            }

            NetheriteArmor.setupHealth(player);
            PermadeathItems.slotBlock(player);

            if (SPEED_RUN_MODE) {
                String actionBar = "";

                if (world.hasStorm()) {
                    actionBar = getMessages().getMessageByPlayer("Server-Messages.ActionBarMessage", player.getName()).replace("%tiempo%", time) + " - ";
                }
                actionBar = actionBar + ChatColor.GRAY + "Tiempo total: " + TextUtils.formatInterval(playTime);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBar));

            } else {
                if (world.hasStorm()) {
                    String msg = getMessages().getMessageByPlayer("Server-Messages.ActionBarMessage", player.getName()).replace("%tiempo%", time);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
                }
            }

            if (player.getWorld().getEnvironment() == World.Environment.THE_END && getDay() >= 30) {
                if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.BEDROCK) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 10 * 20, 9));
                }
                BeginningManager beginningManager = getBeginningManager();
                World beginningWorld = beginningManager != null ? beginningManager.getBeginningWorld() : null;
                if (beginningWorld != null && player.getWorld().equals(beginningWorld)
                        && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                }
            }

            if (getDay() >= 40) {

                if (player.getWorld().hasStorm() && player.getGameMode() != GameMode.SPECTATOR) {
                    Location block = player.getWorld().getHighestBlockAt(player.getLocation().clone()).getLocation();
                    int highestY = block.getBlockY();

                    if (highestY < player.getLocation().getY()) {

                        int probability = random.nextInt(10000) + 1;

                        int blind = (getDay() < 50 ? 1 : 300);
                        if (probability <= blind) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 60, 0));
                        }
                        if (getDay() >= 50) {
                            if (probability == 301) {
                                int duration = random.nextInt(17);
                                duration = duration + 3;
                                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration * 20, 0));
                            }
                        }
                    }
                }
            }

            if (getDay() >= 50) {

                if (player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
                    PotionEffect e = player.getPotionEffect(PotionEffectType.MINING_FATIGUE);
                    if (e.getDuration() >= 4 * 60 * 20 && !getDoneEffectPlayers().contains(player)) {
                        int min = 10 * 60;
                        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, min * 20, 2));
                        getDoneEffectPlayers().add(player);
                    }

                    if (e.getDuration() <= 4 * 60 * 20 && getDoneEffectPlayers().contains(player)) {
                        getDoneEffectPlayers().remove(player);
                    }
                }

                if (player.getWorld().getEnvironment() == World.Environment.NETHER && getDay() < 60) {

                    int random = this.random.nextInt(4500) + 1;

                    if (random <= 10 && player.getWorld().getLivingEntities().size() < 110) {

                        Location ploc = player.getLocation().clone();

                        ArrayList<Location> spawns = new ArrayList<>();
                        spawns.add(ploc.clone().add(10, 25, -5));
                        spawns.add(ploc.clone().add(5, 25, 5));
                        spawns.add(ploc.clone().add(-5, 25, 5));

                        for (Location l : spawns) {
                            if (w.getBlockAt(l).getType() == Material.AIR && w.getBlockAt(l.clone().add(0, 1, 0)).getType() == Material.AIR) {
                                int randomEntities = this.random.nextInt(3) + 1;
                                for (int i = 0; i < randomEntities; i++) {
                                    getNmsHandler().spawnNMSEntity("PigZombie", EntityType.valueOf(VersionManager.isRunningPostNetherUpdate() ? "ZOMBIFIED_PIGLIN" : "PIG_ZOMBIE"), l, CreatureSpawnEvent.SpawnReason.CUSTOM);
                                }
                            }
                        }
                    }
                }
            }

            if (getDay() >= 60) {
                if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.SOUL_SAND) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30 * 20, 2));
                }
                if (getConfig().getBoolean("Toggles.Mike-Creeper-Spawn")) {

                    Location l = player.getLocation().clone();

                    if (random.nextInt(30) == 0 && player.getNearbyEntities(30, 30, 30)
                            .stream()
                            .filter(entity -> entity instanceof Creeper)
                            .map(Creeper.class::cast)
                            .collect(Collectors.toList()).size() < 10) {
                        int pX = (random.nextBoolean() ? -1 : 1) * (random.nextInt(15)) + 15;
                        int pZ = (random.nextBoolean() ? -1 : 1) * (random.nextInt(15)) + 15;
                        int y = (int) l.getY();

                        Block block = l.getWorld().getBlockAt(l.getBlockX() + pX, y, l.getBlockZ() + pZ);
                        Block up = block.getRelative(BlockFace.UP);

                        if (block.getType() != Material.AIR && up.getType() == Material.AIR) {
                            getFactory().spawnEnderQuantumCreeper(up.getLocation(), null);
                        }
                    }
                }
            }
        }
    }

    private void tickEvents() {

        // tickAll se ejecuta cada 30 ticks (~1.5 s). Los eventos guardan segundos,
        // así que acumulamos ticks para que sus relojes avancen a tiempo real.
        eventTickAccumulator += 30;
        int elapsedSeconds = eventTickAccumulator / 20;
        eventTickAccumulator %= 20;
        if (elapsedSeconds <= 0) return;

        if (this.orbEvent.isRunning()) {
            if (this.orbEvent.getTimeLeft() > 0) {

                this.orbEvent.setTimeLeft(Math.max(0, this.orbEvent.getTimeLeft() - elapsedSeconds));

                int res = this.orbEvent.getTimeLeft();

                int hrs = res / 3600;
                int minAndSec = res % 3600;
                int min = minAndSec / 60;
                int sec = minAndSec % 60;

                String tiempo = String.format("%02d:%02d:%02d", hrs, min, sec);

                this.orbEvent.getBossBar().setColor(BarColor.values()[random.nextInt(BarColor.values().length)]);
                this.orbEvent.setTitle(TextUtils.format("&6&l" + tiempo + " para obtener el Life Orb"));
            } else {

                Bukkit.broadcastMessage(TextUtils.format(instance.prefix + "&cSe ha acabado el tiempo para obtener el Life Orb, ¡sufrid! ahora tendreís 8 contenedores de vida menos."));
                this.orbEvent.setRunning(false);
                this.orbEvent.clearPlayers();
                this.orbEvent.setTimeLeft((SPEED_RUN_MODE ? 60 * 8 : 60 * 60 * 8));
                this.orbEvent.getBossBar().removeAll();

                getConfig().set("DontTouch.Event.LifeOrbEnded", true);
                saveConfig();
                reloadConfig();
            }
        }

        if (this.shulkerEvent.isRunning()) {

            if (this.shulkerEvent.getTimeLeft() > 0) {

                this.shulkerEvent.setTimeLeft(Math.max(0, this.shulkerEvent.getTimeLeft() - elapsedSeconds));

                int res = this.shulkerEvent.getTimeLeft();

                int hrs = res / 3600;
                int minAndSec = res % 3600;
                int min = minAndSec / 60;
                int sec = minAndSec % 60;

                String tiempo = String.format("%02d:%02d:%02d", hrs, min, sec);

                this.shulkerEvent.setTitle(TextUtils.format("&e&lX2 Shulker Shells: &b&n" + tiempo));
            } else {

                Bukkit.broadcastMessage(TextUtils.format(instance.prefix + "&eEl evento de &c&lX2 Shulker Shells &eha acabado."));
                this.shulkerEvent.setRunning(false);
                this.shulkerEvent.clearPlayers();
                this.shulkerEvent.setTimeLeft(60 * 60 * 4);
                this.shulkerEvent.getBossBar().removeAll();
            }
        }
    }

    private void startPlugin() {
        this.messages = new Messages(this);
        this.shulkerEvent = new ShellEvent(this);
        this.orbEvent = new LifeOrbEvent(this);
        this.factory = new MobFactory(this);

        if (VersionManager.getMinecraftVersion().isAboveOrEqual(MinecraftVersion.v1_20_R1)) {
            // Schematics nuevas
            new FileAPI.FileOut(instance, "updated_schematics/beginning_portal.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "updated_schematics/ytic.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "updated_schematics/island1.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "updated_schematics/island2.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "updated_schematics/island3.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "updated_schematics/island4.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "updated_schematics/island5.schem", "schematics/", true);
        } else {
            new FileAPI.FileOut(instance, "original_schematics/beginning_portal.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "original_schematics/ytic.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "original_schematics/island1.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "original_schematics/island2.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "original_schematics/island3.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "original_schematics/island4.schem", "schematics/", true);
            new FileAPI.FileOut(instance, "original_schematics/island5.schem", "schematics/", true);
        }

        int HelmetValue = instance.getConfig().getInt("Toggles.Netherite.Helmet", 10);
        int ChestplateValue = instance.getConfig().getInt("Toggles.Netherite.Chestplate", 10);
        int LeggingsValue = instance.getConfig().getInt("Toggles.Netherite.Leggings", 10);
        int BootsValue = instance.getConfig().getInt("Toggles.Netherite.Boots", 10);
        if (HelmetValue > 100 || HelmetValue < 1) {
            PDCLog.getInstance().log("[ERROR] Error al cargar la probabilidad de 'Helmet' en 'config.yml', asegurate de introducir un numero valido del 1 al 100.", true);
            PDCLog.getInstance().log("[ERROR] Ha ocurrido un error al cargar el archivo config.yml, si este error persiste avisanos por discord.", true);
        }
        if (ChestplateValue > 100 || ChestplateValue < 1) {
            PDCLog.getInstance().log("[ERROR] Error al cargar la probabilidad de 'Chestplate' en 'config.yml', asegurate de introducir un numero valido del 1 al 100.", true);
            PDCLog.getInstance().log("[ERROR] Ha ocurrido un error al cargar el archivo config.yml, si este error persiste avisanos por discord.", true);
        }
        if (LeggingsValue > 100 || LeggingsValue < 1) {
            PDCLog.getInstance().log("[ERROR] Error al cargar la probabilidad de 'Leggings' en 'config.yml', asegurate de introducir un numero valido del 1 al 100.", true);
            PDCLog.getInstance().log("[ERROR] Ha ocurrido un error al cargar el archivo config.yml, si este error persiste avisanos por discord.", true);
        }
        if (BootsValue > 100 || BootsValue < 1) {
            PDCLog.getInstance().log("[ERROR] Error al cargar la probabilidad de 'BootsValue' en 'config.yml', asegurate de introducir un numero valido del 1 al 100.", true);
            PDCLog.getInstance().log("[ERROR] Ha ocurrido un error al cargar el archivo config.yml, si este error persiste avisanos por discord.", true);
        }
        String compatibleVersion = VersionManager.getVersion() != null ? ("&aCompatible") : "&cIncompatible";

        String software = "";
        try {
            if (Class.forName("org.spigotmc.SpigotConfig") != null) {
                software = "SpigotMC (Compatible)";
            }
        } catch (ClassNotFoundException e) {
            software = "Bukkit";
        }

        try {
            if (Class.forName("com.destroystokyo.paper.PaperConfig") != null) {
                software = "PaperMC (Compatible)";
                runningPaperSpigot = true;
            }
        } catch (ClassNotFoundException e) {
        }

        String worldState = setupWorld();
        enforceBeginningAccess();
        if (this.world == null || this.endWorld == null) {
            Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&cNo se pudieron resolver los mundos requeridos. El plugin será desactivado."));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getConsoleSender().sendMessage(TextUtils.format("&f&m------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("             &c&lPERMADEATH"));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("     &7- Versión: &e" + this.getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("     &7- Versión del Servidor: &e" + VersionManager.getFormattedVersion()));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("&f&m------------------------------------------"));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format(" &7>> &e&lINFORME DE ESTADO"));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("&7> &bEstado de mundos: " + worldState));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("    &7> &eEnd&7: &8" + this.endWorld.getName()));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("    &7> &aOverworld&7: &8" + this.world.getName()));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("&7> &bEstado de Compatibilidad: " + compatibleVersion));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("&7> &bSoftware: " + software));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("&7> &b&lCambios:"));
        Bukkit.getConsoleSender().sendMessage(TextUtils.format("&7>   &aDías disponibles: &71-60"));

        worldEditFound = (Bukkit.getPluginManager().getPlugin("WorldEdit") != null || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null);
        if (!worldEditFound) {
            Bukkit.getConsoleSender().sendMessage(TextUtils.format("&7> &4&lADVERTENCIA: &7No se ha encontrado el plugin &7World Edit"));
            Bukkit.getConsoleSender().sendMessage(TextUtils.format("&7> &7Algunas funciones pueden no funcionar correctamente. Ten en cuenta que las estructuras de The Beginning no podrán ser generadas en días avanzados."));
            PDCLog.getInstance().log("No se encontró WorldEdit");
        }

        if (software.contains("Bukkit")) {
            Bukkit.broadcastMessage(TextUtils.format(prefix + "&7> &4&lADVERTENCIA&7: &eEl plugin NO es compatible con CraftBukkit, cambia a SpigotMC o PaperSpigot"));
            PDCLog.getInstance().disable("No es compatible con Bukkit.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        setupListeners();
        setupCommands();

        new UpdateChecker(this).getVersion(version -> {
            if (this.getDescription().getVersion().equalsIgnoreCase(version)) {
                Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&bVersión del plugin: &aVersión más reciente instalada."));
            } else {
                Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&eNueva versión detectada."));
                Bukkit.getConsoleSender().sendMessage(TextUtils.format("&7> &aDescarga: &7" + Utils.SPIGOT_LINK));
            }
        });

        registerChanges();
        generateOfflinePlayerData();

        PDCLog.getInstance().log("Se ha activado el plugin.");
    }

    private void registerListeners() {
        worldEditFound = Bukkit.getPluginManager().getPlugin("WorldEdit") != null
                || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
        String prefix = "&e[PermaDeath] &7> ";

        if (!registeredDays.get(1)) {
            registeredDays.replace(1, true);

            this.getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
        }

        if (DateManager.getInstance().getDay() >= 20 && !registeredDays.get(20)) {
            registeredDays.replace(20, true);

            this.hostile = new HostileEntityListener(this);
            getServer().getPluginManager().registerEvents(hostile, instance);
            Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&eSe han registrado los cambios de Mobs pacíficos hostiles."));
        }

        if (DateManager.getInstance().getDay() >= 30) {
            if (endManager == null || endData == null) {
                this.endData = new EndDataManager(instance);
                this.endManager = new EndManager(instance);
                getServer().getPluginManager().registerEvents(this.endManager, instance);
                registeredDays.replace(30, true);

                if (runningPaperSpigot) {
                    this.paperListeners = new PaperListeners(instance);
                    getServer().getPluginManager().registerEvents(this.paperListeners, instance);
                    Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&eSe han registrado cambios especiales para &c&lPaperMC&e."));
                }
            }

            // Si WorldEdit/FAWE se instaló después de arrancar, la preparación pendiente
            // se reintentará sin reiniciar el plugin.
            worldEditFound = Bukkit.getPluginManager().getPlugin("WorldEdit") != null
                    || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
            if (!endData.isReplacedObsidian() || !endData.isCreatedRegenZone()) {
                this.endManager.prepareEnd(this.endData);
            }
        }

        if (DateManager.getInstance().getDay() >= 40 && !registeredDays.get(40)) {

            if (!worldEditFound) {
                if (!beginningWarningShown) {
                    beginningWarningShown = true;
                    Bukkit.broadcastMessage(TextUtils.format(prefix + "&4&lNo se pudo registrar TheBeginning ya que no se ha encontrado WorldEdit/FAWE."));
                    Bukkit.broadcastMessage(TextUtils.format(prefix + "&7Instala WorldEdit o FastAsyncWorldEdit y reinicia el servidor para habilitar TheBeginning."));
                }
            } else {
                if (this.recipes == null) this.recipes = new RecipeManager(this);
                this.recipes.registerRecipes();
                this.getNmsHandler().addMushrooms();
                this.slotBlockListener = new SlotBlockListener(instance);
                getServer().getPluginManager().registerEvents(this.slotBlockListener, instance);

                this.beData = new BeginningDataManager(this);
                this.begginingManager = new BeginningManager(this);
                registeredDays.replace(40, true);

                Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&eSe han registrado cambios para el día &b40"));
                Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&eSe han registrado cambios de TheBeginning"));
            }
        }

        if (DateManager.getInstance().getDay() >= 50 && !registeredDays.get(50)) {

            if (this.recipes == null) {
                this.recipes = new RecipeManager(this);
                this.recipes.registerRecipes();
            }

            Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&eSe han registrado cambios para el día &b50"));
            this.recipes.registerD50Recipes();
            registeredDays.replace(50, true);
        }

        if (DateManager.getInstance().getDay() >= 60 && !registeredDays.get(60)) {

            if (this.recipes == null) {
                this.recipes = new RecipeManager(this);
                this.recipes.registerRecipes();
                this.recipes.registerD50Recipes();
            }

            this.recipes.registerD60Recipes();
            Bukkit.getConsoleSender().sendMessage(TextUtils.format(prefix + "&eSe han registrado cambios para el día &b60"));
            registeredDays.replace(60, true);
        }
    }

    protected void registerChanges() {
        if (alreadyRegisteredChanges) return;
        alreadyRegisteredChanges = true;
    }

    public void generateOfflinePlayerData() {

        for (OfflinePlayer off : Bukkit.getOfflinePlayers()) {

            if (off == null) return;

            PlayerDataManager manager = new PlayerDataManager(off.getName(), this);
            manager.generateDayData();
        }
    }

    protected String setupWorld() {

        String configuredMain = instance.getConfig().getString("Worlds.MainWorld", "world");
        String configuredEnd = instance.getConfig().getString("Worlds.EndWorld", "world_the_end");

        this.world = Bukkit.getWorld(configuredMain);
        if (this.world == null) {
            PDCLog.getInstance().log("[ERROR] No se encontró el mundo principal configurado: " + configuredMain, true);
            PDCLog.getInstance().log("[ERROR] No se usará otro mundo automáticamente para evitar operar sobre un mundo equivocado.", true);
        }

        this.endWorld = Bukkit.getWorld(configuredEnd);
        if (this.endWorld == null) {
            PDCLog.getInstance().log("[ERROR] No se encontró el mundo del End configurado: " + configuredEnd, true);
            PDCLog.getInstance().log("[ERROR] No se usará otro mundo automáticamente para evitar operar sobre un End equivocado.", true);
        }

        if (this.world == null) {
            PDCLog.getInstance().log("[ERROR] No se pudo resolver ningún mundo principal.");
            return "&cOverworld no encontrado";
        }
        if (this.endWorld == null) {
            PDCLog.getInstance().log("[ERROR] No se pudo resolver ningún mundo del End.");
            return "&cEnd no encontrado";
        }

        boolean dobleCap = getConfig().getBoolean("Toggles.Doble-Mob-Cap") && getDay() >= 10;
        if (dobleCap) Bukkit.getConsoleSender().sendMessage(prefix + "&eDoblando la mob-cap en todos los mundos.");

        for (World w : Bukkit.getWorlds()) {
            if (dobleCap) {
                w.setMonsterSpawnLimit(140);
            }

            if (isRunningPaperSpigot()) {
                try {
                    Object nmsW = w.getClass().getDeclaredMethod("getHandle").invoke(w);

                    final Field f = nmsW.getClass().getDeclaredField("paperConfig");
                    f.setAccessible(true);
                    Object paperConfig = f.get(nmsW);

                    final Field creepers = paperConfig.getClass().getDeclaredField("disableCreeperLingeringEffect");
                    creepers.setAccessible(true);
                    creepers.set(paperConfig, true);

                    Bukkit.getConsoleSender().sendMessage(prefix + "&eDeshabilitando Creeper-Lingering-Effect...");

                } catch (Exception ignored) {
                }
            }
        }

        return "&aOverworld: &b" + this.world.getName() + " &eEnd: &b" + this.endWorld.getName();
    }

    public void reload(CommandSender sender) {

        this.setupConfig();
        reloadConfig();
        this.messages.reloadFiles();
        DateManager.getInstance().reloadDate();
        setupWorld();
        if (this.world == null || this.endWorld == null) {
            sender.sendMessage(TextUtils.format("&cNo se pudo recargar el plugin porque faltan los mundos requeridos. Revisa Worlds.MainWorld y Worlds.EndWorld."));
            PDCLog.getInstance().log("[ERROR] /pdc reload cancelado: no se pudieron resolver los mundos requeridos.");
            return;
        }
        resetDaySystems();
        registerListeners();
        if (this.recipes != null) {
            this.recipes.unregisterRecipesAboveDay(getDay());
        }
        this.eventTickAccumulator = 0;

        sender.sendMessage(TextUtils.format("&aSe ha recargado el archivo de configuración y los mensajes."));
        sender.sendMessage(TextUtils.format("&eAlgunos cambios pueden requerir un reinicio para funcionar correctamente."));
        sender.sendMessage(TextUtils.format("&c&lNota importante: &7Algunos cambios pueden requerir un reinicio y la fecha puede no ser exacta."));
        prefix = TextUtils.format((getConfig().contains("Prefix") ? getConfig().getString("Prefix") : "&c&lPERMADEATH&4&l &7➤ &f"));

        PDCLog.getInstance().log("Se ha recargado el plugin");
        DiscordPortal.reload();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void blockBeginningBeforeDay50(PlayerTeleportEvent event) {
        if (getDay() >= 50 || event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }

        World beginningWorld = Bukkit.getWorld("pdc_the_beginning");
        if (beginningWorld == null || !event.getTo().getWorld().equals(beginningWorld)) {
            return;
        }

        Player player = event.getPlayer();
        Location spawn = world.getSpawnLocation().clone();
        event.setTo(spawn);
        Bukkit.broadcastMessage(TextUtils.format("&c&lEl jugador &4&l" + player.getName()
                + " &c&lintentó entrar a TheBeginning antes del día 50 y fue enviado al spawn."));
    }

    private void enforceBeginningAccess() {
        if (getDay() >= 50 || world == null) {
            return;
        }

        World beginningWorld = Bukkit.getWorld("pdc_the_beginning");
        if (beginningWorld == null) {
            return;
        }

        for (Player player : new ArrayList<>(beginningWorld.getPlayers())) {
            player.teleport(world.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }

    private void resetDaySystems() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (hostile != null) {
            HandlerList.unregisterAll(hostile);
            hostile = null;
        }
        if (endManager != null) {
            HandlerList.unregisterAll(endManager);
            endManager = null;
        }
        if (paperListeners != null) {
            HandlerList.unregisterAll(paperListeners);
            paperListeners = null;
        }
        if (slotBlockListener != null) {
            HandlerList.unregisterAll(slotBlockListener);
            slotBlockListener = null;
        }
        if (begginingManager != null) {
            HandlerList.unregisterAll(begginingManager);
            begginingManager = null;
        }

        // The Beginning permanece en el mundo aunque bajemos de día, pero no
        // puede quedar ningún jugador dentro cuando el acceso está bloqueado.
        enforceBeginningAccess();
        if (playerListener != null) {
            HandlerList.unregisterAll(playerListener);
            playerListener = new PlayerListener();
            getServer().getPluginManager().registerEvents(playerListener, instance);
        }

        endData = null;
        beginningWarningShown = false;
        registeredDays.replace(20, false);
        registeredDays.replace(30, false);
        registeredDays.replace(40, false);
        registeredDays.replace(50, false);
        registeredDays.replace(60, false);
    }

    private void setupListeners() {
        getServer().getPluginManager().registerEvents(this, this);

        this.spawnListener = new SpawnListener(this);
        getServer().getPluginManager().registerEvents(spawnListener, instance);
        getServer().getPluginManager().registerEvents(new CustomSkeletons(instance), instance);
        this.playerListener = new PlayerListener();
        getServer().getPluginManager().registerEvents(this.playerListener, instance);
        getServer().getPluginManager().registerEvents(new BlockListener(), instance);
        getServer().getPluginManager().registerEvents(new EntityEvents(), instance);
        getServer().getPluginManager().registerEvents(new TotemListener(), instance);
        getServer().getPluginManager().registerEvents(new RaidEvents(), instance);
        getServer().getPluginManager().registerEvents(new WorldEvents(), instance);
        this.difficultyChanges = new DifficultyChanges(this);
        getServer().getPluginManager().registerEvents(this.difficultyChanges, instance);
        registeredDays.put(1, false);
        registeredDays.put(20, false);
        registeredDays.put(30, false);
        registeredDays.put(40, false);
        registeredDays.put(50, false);
        registeredDays.put(60, false);
    }

    private void setupConsoleFilter() {
        try {
            Class.forName("org.apache.logging.log4j.core.filter.AbstractFilter");
            org.apache.logging.log4j.core.Logger logger =
                    (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
            removeConsoleFilter();
            rootLogFilter = new Log4JFilter();
            logger.addFilter(rootLogFilter);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            removeConsoleFilter();
            legacyLogFilter = record -> {
                String message = record == null ? "" : String.valueOf(record.getMessage());
                return !message.contains("Ignoring unknown attribute")
                        && !message.contains("Summoned new Wither");
            };
            Bukkit.getLogger().setFilter(legacyLogFilter);
            Logger.getLogger("Minecraft").setFilter(legacyLogFilter);
        }
    }

    private void removeConsoleFilter() {
        try {
            if (rootLogFilter != null) {
                ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false)).removeFilter(rootLogFilter);
                rootLogFilter = null;
            }
        } catch (Throwable ignored) {
        }

        if (legacyLogFilter != null) {
            Bukkit.getLogger().setFilter(null);
            Logger.getLogger("Minecraft").setFilter(null);
            legacyLogFilter = null;
        }
    }

    private void setupCommands() {
        Objects.requireNonNull(getCommand("pdc")).setExecutor(new PDCCommand(instance));
        Objects.requireNonNull(getCommand("pdc")).setTabCompleter(new PDCCommandCompleter());
    }

    private void setupConfig() {

        File f = new File(getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(f);

        FileAPI.UtilFile c = FileAPI.select(instance, f, config);

        c.set("config-version", 3);
        c.set("Prefix", "&cPermadeath &7➤ &f");
        c.set("ban-enabled", true);
        c.set("anti-afk-enabled", false);
        c.set("AntiAFK.DaysForBan", 7);
        c.set("Toggles.OptifineItems", false);
        c.set("Toggles.DefaultDeathSoundsEnabled", true);
        c.set("Toggles.Netherite.Helmet", 10);
        c.set("Toggles.Netherite.Chestplate", 10);
        c.set("Toggles.Netherite.Leggings", 10);
        c.set("Toggles.Netherite.Boots", 10);
        c.set("Toggles.End.Mob-Spawn-Limit", 70);
        c.set("Toggles.End.Ender-Ghast-Count", 170);
        c.set("Toggles.End.Ender-Creeper-Count", 20);
        c.set("Toggles.End.Protect-End-Spawn", false);
        c.set("Toggles.End.Protect-Radius", 10);
        c.set("Toggles.End.Small-Islands", true);
        c.set("Toggles.End.PermadeathDemon.DisplayName", "&6&lPERMADEATH DEMON");
        c.set("Toggles.End.PermadeathDemon.DisplayNameEnraged", "&6&lENRAGED PERMADEATH DEMON");
        c.set("Toggles.End.PermadeathDemon.Health", 1350);
        c.set("Toggles.End.PermadeathDemon.EnragedHealth", 1350);
        c.set("Toggles.End.PermadeathDemon.Optimizar-TNT", false);
        c.set("Toggles.TheBeginning.YticGenerateChance", 100000);
        c.set("Toggles.Spider-Effect", true);
        c.set("Toggles.OP-Ban", true);
        c.set("Toggles.Doble-Mob-Cap", false);
        c.set("Toggles.Replace-Mobs-On-Chunk-Load", true);
        c.set("Toggles.Quantum-Explosion-Power", 60);
        c.set("Toggles.Mike-Creeper-Spawn", true);
        c.set("Toggles.Optimizar-Mob-Spawns", false);
        c.set("Toggles.Gatos-Supernova.Destruir-Bloques", true);
        c.set("Toggles.Gatos-Supernova.Fuego", true);
        c.set("Toggles.Gatos-Supernova.Explosion-Power", 200);
        c.set("Server-Messages.coords-msg-enable", true);
        c.set("TotemFail.Enable", true);
        c.set("TotemFail.Medalla", "&7¡El jugador %player% ha usado su medalla de superviviente!");
        c.set("TotemFail.ChatMessage", "&7¡El tótem de &c%player% &7ha fallado!");
        c.set("TotemFail.ChatMessageTotems", "&7¡Los tótems de &c%player% &7han fallado!");
        c.set("TotemFail.NotEnoughTotems", "&7¡%player% no tenía suficientes tótems en el inventario!");
        c.set("TotemFail.PlayerUsedTotemMessage", "&7El jugador %player% ha consumido un tótem (Probabilidad: %totem_fail% %porcent% %number%)");
        c.set("TotemFail.PlayerUsedTotemsMessage", "&7El jugador %player% ha consumido {ammount} tótems (Probabilidad: %totem_fail% %porcent% %number%)");
        c.set("Worlds.MainWorld", "world");
        c.set("Worlds.EndWorld", "world_the_end");
        c.set("DontTouch.PlayTime", 0);

        c.save();
        c.load();
    }

    public MobFactory getFactory() {
        return factory;
    }

    public NMSHandler getNmsHandler() {
        return NMS.getHandler();
    }

    public BeginningManager getBeginningManager() {
        return begginingManager;
    }

    public void setBeginningDataForDebug() {
        if (this.beData == null) {
            this.beData = new BeginningDataManager(this);
        }
    }

    public void createBeginningForDebug() {
        if (this.beData == null) {
            this.beData = new BeginningDataManager(this);
        }
        if (this.begginingManager == null) {
            this.begginingManager = new BeginningManager(this);
        }
        if (this.recipes == null) {
            this.recipes = new RecipeManager(this);
        }
        this.recipes.registerRecipes();
    }

    public void setTask(EndTask task) {
        this.task = task;
    }

    public NMSAccessor getNmsAccessor() {
        return NMS.getAccessor();
    }

    public long getDay() {
        return DateManager.getInstance().getDay();
    }

    public int getWitherTimer() {
        return witherTimer;
    }

    public HostileEntityListener getHostile() {
        return hostile;
    }

    public ArrayList<Player> getDoneEffectPlayers() {
        return doneEffectPlayers;
    }

    public ShellEvent getShulkerEvent() {
        return shulkerEvent;
    }

    public LifeOrbEvent getOrbEvent() {
        return orbEvent;
    }

    public InfernalNetheriteBlock getNetheriteBlock() {
        return NMS.getNetheriteBlock();
    }

    public boolean isSmallIslandsEnabled() {
        return getConfig().getBoolean("Toggles.End.Small-Islands", true);
    }

    public void deathTrainEffects(LivingEntity entity) {
        if (entity instanceof Player) return;

        if (getDay() >= 25) {

            int lvl = (getDay() >= 50 ? 1 : 0);

            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, lvl));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, lvl));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, lvl));

            if (getDay() >= 50 && getDay() < 60) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
            }
        }
    }

    public SpawnListener getSpawnListener() {
        return spawnListener;
    }

    public int getPlayTime() {
        return playTime;
    }

    public void setPlayTime(int playTime) {
        this.playTime = playTime;
    }
}
