package tech.layon.permadeath.discord;

import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.data.PlayerDataManager;
import tech.layon.permadeath.util.log.PDCLog;

import java.awt.Color;
import java.io.File;
import java.time.LocalDate;
import java.util.Objects;

public class DiscordManager {

    private static DiscordManager discordManager;
    private final Main instance;

    @Getter
    private final File file;
    @Getter
    private final FileConfiguration configuration;

    @Getter
    private JDA bot;

    public DiscordManager() {
        this.instance = Main.getInstance();
        this.file = new File(instance.getDataFolder(), "discord.yml");

        // El archivo debe existir antes de cargarlo. Así el bot funciona desde
        // PermadeathCoreRebirth sin depender de JDA-Spigot u otro plugin externo.
        if (!file.exists()) {
            this.instance.saveResource("discord.yml", false);
        }

        this.configuration = YamlConfiguration.loadConfiguration(this.file);

        if (!configuration.getBoolean("Enable")) {
            log("El bot de discord no está activado en la config");
            return;
        }

        log("Intentando cargar la aplicación de Discord integrada.");

        String token = configuration.getString("Token", "").trim();
        if (token.isEmpty()) {
            log("No se ha proporcionado un token por el usuario");
            return;
        }

        try {
            JDABuilder builder = JDABuilder.createDefault(token);
            builder.setActivity(Activity.watching(
                    Objects.requireNonNullElse(configuration.getString("Status"), "¡Permadeath!")));

            this.bot = builder.build();
            this.bot.awaitReady();

            String channelId = configuration.getString("Channels.Anuncios");
            if (channelId != null && !channelId.isBlank()) {
                TextChannel channel = bot.getTextChannelById(channelId);
                if (channel != null) {
                    sendEmbed(channel, buildEmbed(
                            "Permadeath",
                            Color.GREEN,
                            null,
                            null,
                            null,
                            ":gear: Plugin encendido."));
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log("Se interrumpió el inicio de sesión con la aplicación de Discord.");
            this.bot = null;
        } catch (Exception ex) {
            log("Ha ocurrido un error al iniciar sesión con la aplicación de Discord, revisa tu token.");
            ex.printStackTrace();
            this.bot = null;
        }
    }

    public static DiscordManager getInstance() {
        if (discordManager == null) {
            discordManager = new DiscordManager();
        }
        return discordManager;
    }

    public void onDisable() {
        if (this.bot == null) return;

        try {
            String channelId = configuration.getString("Channels.Anuncios");
            if (channelId != null && !channelId.isBlank()) {
                TextChannel channel = bot.getTextChannelById(channelId);
                if (channel != null) {
                    sendEmbed(channel, buildEmbed(
                            "Permadeath",
                            Color.RED,
                            null,
                            null,
                            null,
                            ":gear: Plugin desactivado."));
                }
            }
        } finally {
            // Ahora que JDA pertenece al propio plugin, también administramos
            // su cierre para no dejar hilos de Discord vivos al apagar el servidor.
            bot.shutdown();
            bot = null;
        }
    }

    public void onDeathTrain(String msg) {
        if (this.bot == null) return;

        String channelId = configuration.getString("Channels.Anuncios");
        if (channelId == null || channelId.isBlank()) return;

        TextChannel channel = bot.getTextChannelById(channelId);
        if (channel == null) return;

        sendEmbed(channel, buildEmbed(
                "Permadeath",
                Color.RED,
                null,
                null,
                null,
                ":fire: " + ChatColor.stripColor(msg)));
    }

    public void onDayChange() {
        if (this.bot == null) return;

        String channelId = configuration.getString("Channels.Anuncios");
        if (channelId == null || channelId.isBlank()) return;

        TextChannel channel = bot.getTextChannelById(channelId);
        if (channel == null) return;

        sendEmbed(channel, buildEmbed(
                "Permadeath",
                Color.GREEN,
                null,
                null,
                null,
                ":alarm_clock: Han avanzado al día " + instance.getDay()));
    }

    public void banPlayer(OfflinePlayer off, boolean isAFKBan) {
        if (this.bot == null || off == null) return;

        Player onlinePlayer = off.getPlayer();
        PlayerDataManager data = new PlayerDataManager(off.getName(), instance);

        String playerLoc = "";
        if (!isAFKBan && onlinePlayer != null) {
            playerLoc = onlinePlayer.getLocation().getBlockX()
                    + " " + onlinePlayer.getLocation().getBlockY()
                    + " " + onlinePlayer.getLocation().getBlockZ();
        }

        String serverName = configuration.getString("ServerName", "Mi Permadeath");
        LocalDate now = LocalDate.now();
        String date = String.format("%02d/%02d/%02d",
                now.getDayOfMonth(),
                now.getMonthValue(),
                now.getYear());
        String cause = isAFKBan ? "AFK" : data.getBanCause();

        EmbedBuilder embed = buildEmbed(
                off.getName() + " ha sido PERMABANEADO en " + serverName + "\n",
                new Color(0xF40C0C),
                null,
                null,
                "https://mineskin.eu/headhelm/" + off.getName() + "/100.png");

        embed.setAuthor(
                "Permadeath",
                "https://twitter.com/layon",
                "https://www.spigotmc.org/data/avatars/l/429/429856.jpg?1692799382");
        embed.addField("📅 Fecha", date, true);
        embed.addField("💀 Razón", cause, true);
        if (!isAFKBan && !playerLoc.isEmpty()) {
            embed.addField("🧭 Coordenadas", playerLoc, true);
        }

        String channelId = configuration.getString("Channels.DeathChannel");
        if (channelId == null || channelId.isBlank()) {
            log("No se ha configurado el canal de muertes.");
            return;
        }

        TextChannel channel = bot.getTextChannelById(channelId);
        if (channel == null) {
            log("No pudimos encontrar el canal de muertes.");
            return;
        }

        channel.sendMessageEmbeds(embed.build()).queue(message ->
                message.addReaction(Emoji.fromFormatted("☠")).queue());

        log("Enviando mensaje de muerte a discord");
    }

    private void log(String message) {
        PDCLog.getInstance().log("[DISCORD] " + message);
    }

    private EmbedBuilder buildEmbed(
            String title,
            Color color,
            String footer,
            String image,
            String thumbnail,
            String... description) {

        EmbedBuilder embed = new EmbedBuilder();

        if (title != null) embed.setTitle(title);
        if (color != null) embed.setColor(color);
        if (footer != null) embed.setFooter(footer);
        if (image != null) embed.setImage(image);
        if (thumbnail != null) embed.setThumbnail(thumbnail);

        for (String line : description) {
            embed.addField("", line, false);
        }

        return embed;
    }

    private void sendEmbed(MessageChannel channel, EmbedBuilder embed, String... reactions) {
        channel.sendMessageEmbeds(embed.build()).queue(message -> {
            for (String reaction : reactions) {
                message.addReaction(Emoji.fromFormatted(reaction)).queue();
            }
        });
    }
}
