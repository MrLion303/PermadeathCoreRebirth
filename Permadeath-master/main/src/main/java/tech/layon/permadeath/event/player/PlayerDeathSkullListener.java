package tech.layon.permadeath.event.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import tech.layon.permadeath.Main;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacion robusta del memorial de muerte:
 *
 *   PLAYER_HEAD
 *   NETHER_BRICK_FENCE
 *   BEDROCK
 *
 * La ubicacion de muerte se captura inmediatamente para que un teletransporte,
 * respawn, cambio a espectador o kick posterior no mueva el memorial.
 *
 * Si SkinsRestorer esta instalado, se consulta su API publica de forma opcional
 * mediante reflexion y se usa la textura realmente aplicada al jugador. De esta
 * forma tambien funciona correctamente en servidores offline/no-premium.
 */
public final class PlayerDeathSkullListener implements Listener {

    private static final String TOGGLE_PATH = "Toggles.Player-Skulls";
    private static final long PLACE_DELAY_TICKS = 11L;

    private final Main plugin;

    public PlayerDeathSkullListener(Main plugin) {
        this.plugin = plugin;

        /*
         * Versiones antiguas del config podian no contener esta clave. Bukkit
         * devuelve false para una clave inexistente, desactivando silenciosamente
         * las cabezas. La restauramos como activa por defecto.
         */
        if (!plugin.getConfig().contains(TOGGLE_PATH)) {
            plugin.getConfig().set(TOGGLE_PATH, true);
            plugin.saveConfig();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean(TOGGLE_PATH)) {
            return;
        }

        Player player = event.getEntity();

        // Capturamos todo AHORA. No dependemos de la posicion del jugador 10 ticks despues.
        Location deathLocation = player.getLocation().clone();
        float deathYaw = player.getLocation().getYaw();
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        PlayerProfile capturedProfile = player.getPlayerProfile().clone();
        boolean capturedProfileHasSkin = hasSkin(capturedProfile);
        boolean skinRestorerAvailable = Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer");
        boolean onlineMode = Bukkit.getOnlineMode();

        /*
         * El codigo legacy de PlayerListener intentaba crear la cabeza a los 10
         * ticks. Este listener actua un tick despues y deja el resultado final en
         * una ubicacion estable y con el perfil correcto.
         */
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            placeMemorial(deathLocation, deathYaw, capturedProfileHasSkin ? capturedProfile : null);
        }, PLACE_DELAY_TICKS);

        if (skinRestorerAvailable) {
            /*
             * getSkinForPlayer puede consultar almacenamiento/red. Nunca lo
             * ejecutamos en el hilo principal.
             */
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                SkinData skinData = resolveSkinRestorerSkin(playerId, playerName, onlineMode);

                if (skinData != null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            applySkinToMemorial(deathLocation, playerId, playerName, skinData));
                    return;
                }

                requestOfficialProfileFallback(capturedProfile, deathLocation);
            }, PLACE_DELAY_TICKS);
        } else if (!capturedProfileHasSkin) {
            requestOfficialProfileFallback(capturedProfile, deathLocation);
        }
    }

    /**
     * Construye siempre la estructura usando el bloque de los pies como centro:
     * cabeza arriba, valla en el punto de muerte y bedrock debajo.
     */
    private void placeMemorial(Location deathLocation, float yaw, PlayerProfile profile) {
        World world = deathLocation.getWorld();
        if (world == null) return;

        int x = deathLocation.getBlockX();
        int z = deathLocation.getBlockZ();

        int minFenceY = world.getMinHeight() + 1;
        int maxFenceY = world.getMaxHeight() - 2;
        int fenceY = Math.max(minFenceY, Math.min(deathLocation.getBlockY(), maxFenceY));

        Block bedrock = world.getBlockAt(x, fenceY - 1, z);
        Block fence = world.getBlockAt(x, fenceY, z);
        Block head = world.getBlockAt(x, fenceY + 1, z);

        bedrock.setType(Material.BEDROCK, false);
        fence.setType(Material.NETHER_BRICK_FENCE, false);
        head.setType(Material.PLAYER_HEAD, false);

        if (!(head.getState() instanceof Skull skull)) {
            return;
        }

        if (profile != null && hasSkin(profile)) {
            try {
                setSkullProfile(skull, profile);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning(
                        "No se pudo aplicar inmediatamente la skin al memorial de muerte: "
                                + ex.getMessage());
            }
        }

        skull.update(true, false);

        if (head.getBlockData() instanceof Rotatable rotatable) {
            rotatable.setRotation(rotationFromYaw(yaw));
            head.setBlockData(rotatable, false);
        }
    }

    private void applySkinToMemorial(
            Location deathLocation,
            UUID playerId,
            String playerName,
            SkinData skinData
    ) {
        World world = deathLocation.getWorld();
        if (world == null) return;

        int x = deathLocation.getBlockX();
        int z = deathLocation.getBlockZ();
        int minFenceY = world.getMinHeight() + 1;
        int maxFenceY = world.getMaxHeight() - 2;
        int fenceY = Math.max(minFenceY, Math.min(deathLocation.getBlockY(), maxFenceY));

        Block head = world.getBlockAt(x, fenceY + 1, z);
        if (head.getType() != Material.PLAYER_HEAD || !(head.getState() instanceof Skull skull)) {
            return;
        }

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(playerId, playerName);
            PlayerTextures textures = profile.getTextures();

            URL skinUrl = URI.create(skinData.textureUrl()).toURL();
            PlayerTextures.SkinModel model = "SLIM".equalsIgnoreCase(skinData.model())
                    ? PlayerTextures.SkinModel.SLIM
                    : PlayerTextures.SkinModel.CLASSIC;

            textures.setSkin(skinUrl, model);
            profile.setTextures(textures);

            setSkullProfile(skull, profile);
            skull.update(true, false);
        } catch (Exception ex) {
            plugin.getLogger().warning(
                    "No se pudo aplicar la skin de " + playerName
                            + " al memorial: " + ex.getMessage());
        }
    }

    /**
     * Fallback para servidores sin SkinsRestorer. PlayerProfile#update realiza la
     * consulta de perfil de manera asincrona; no bloqueamos nunca el hilo principal.
     */
    private void requestOfficialProfileFallback(PlayerProfile capturedProfile, Location deathLocation) {
        try {
            capturedProfile.update().thenAccept(updatedProfile -> {
                if (!hasSkin(updatedProfile)) return;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    World world = deathLocation.getWorld();
                    if (world == null) return;

                    int x = deathLocation.getBlockX();
                    int z = deathLocation.getBlockZ();
                    int minFenceY = world.getMinHeight() + 1;
                    int maxFenceY = world.getMaxHeight() - 2;
                    int fenceY = Math.max(minFenceY, Math.min(deathLocation.getBlockY(), maxFenceY));

                    Block head = world.getBlockAt(x, fenceY + 1, z);
                    if (head.getType() != Material.PLAYER_HEAD
                            || !(head.getState() instanceof Skull skull)) {
                        return;
                    }

                    try {
                        setSkullProfile(skull, updatedProfile);
                        skull.update(true, false);
                    } catch (RuntimeException ex) {
                        plugin.getLogger().warning(
                                "No se pudo aplicar el perfil actualizado al memorial: "
                                        + ex.getMessage());
                    }
                });
            });
        } catch (RuntimeException ex) {
            plugin.getLogger().warning(
                    "No se pudo solicitar el perfil de skin como fallback: " + ex.getMessage());
        }
    }

    /**
     * Integracion opcional con SkinsRestorer sin dependencia obligatoria.
     *
     * API usada (v15):
     * SkinsRestorerProvider.get()
     *   -> getPlayerStorage()
     *   -> getSkinForPlayer(UUID, String, boolean)
     * PropertyUtils.getSkinTextureUrl(...)
     * PropertyUtils.getSkinVariant(...)
     */
    private SkinData resolveSkinRestorerSkin(UUID playerId, String playerName, boolean onlineMode) {
        try {
            Class<?> providerClass = Class.forName(
                    "net.skinsrestorer.api.SkinsRestorerProvider",
                    true,
                    Bukkit.getPluginManager().getPlugin("SkinsRestorer").getClass().getClassLoader()
            );

            Object api = providerClass.getMethod("get").invoke(null);
            Object playerStorage = api.getClass().getMethod("getPlayerStorage").invoke(api);

            ClassLoader skinRestorerLoader = providerClass.getClassLoader();
            Class<?> playerStorageClass = Class.forName(
                    "net.skinsrestorer.api.storage.PlayerStorage",
                    true,
                    skinRestorerLoader
            );

            Method getSkinForPlayer = playerStorageClass.getMethod(
                    "getSkinForPlayer",
                    UUID.class,
                    String.class,
                    boolean.class
            );

            Object optionalResult = getSkinForPlayer.invoke(
                    playerStorage,
                    playerId,
                    playerName,
                    onlineMode
            );

            if (!(optionalResult instanceof Optional<?> optional) || optional.isEmpty()) {
                return null;
            }

            Object skinProperty = optional.get();
            Class<?> skinPropertyClass = Class.forName(
                    "net.skinsrestorer.api.property.SkinProperty",
                    true,
                    skinRestorerLoader
            );
            Class<?> propertyUtilsClass = Class.forName(
                    "net.skinsrestorer.api.PropertyUtils",
                    true,
                    skinRestorerLoader
            );

            String textureUrl = (String) propertyUtilsClass
                    .getMethod("getSkinTextureUrl", skinPropertyClass)
                    .invoke(null, skinProperty);

            Object variant = propertyUtilsClass
                    .getMethod("getSkinVariant", skinPropertyClass)
                    .invoke(null, skinProperty);

            String model = variant instanceof Enum<?> enumValue
                    ? enumValue.name()
                    : "CLASSIC";

            if (textureUrl == null || textureUrl.isBlank()) {
                return null;
            }

            return new SkinData(textureUrl, model);
        } catch (Throwable ex) {
            plugin.getLogger().warning(
                    "SkinsRestorer esta instalado, pero no se pudo obtener la skin de "
                            + playerName + ": " + ex.getClass().getSimpleName()
                            + (ex.getMessage() == null ? "" : " - " + ex.getMessage()));
            return null;
        }
    }

    private boolean hasSkin(PlayerProfile profile) {
        if (profile == null) return false;

        try {
            return profile.getTextures() != null
                    && !profile.getTextures().isEmpty()
                    && profile.getTextures().getSkin() != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * setOwnerProfile pertenece a la API compartida por Spigot/Paper 26.3.
     * Paper la marca deprecated a favor de ResolvableProfile, pero usar el metodo
     * comun evita enlazar clases exclusivas de Paper y mantiene un unico JAR.
     */
    @SuppressWarnings("deprecation")
    private void setSkullProfile(Skull skull, PlayerProfile profile) {
        skull.setOwnerProfile(profile);
    }

    private BlockFace rotationFromYaw(float yaw) {
        float normalized = yaw % 360.0F;
        if (normalized < 0.0F) {
            normalized += 360.0F;
        }

        if (normalized < 22.5F || normalized >= 337.5F) return BlockFace.SOUTH;
        if (normalized < 67.5F) return BlockFace.SOUTH_WEST;
        if (normalized < 112.5F) return BlockFace.WEST;
        if (normalized < 157.5F) return BlockFace.NORTH_WEST;
        if (normalized < 202.5F) return BlockFace.NORTH;
        if (normalized < 247.5F) return BlockFace.NORTH_EAST;
        if (normalized < 292.5F) return BlockFace.EAST;
        return BlockFace.SOUTH_EAST;
    }

    private record SkinData(String textureUrl, String model) {
    }
}
