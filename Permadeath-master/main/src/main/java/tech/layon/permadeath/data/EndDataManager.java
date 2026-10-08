package tech.layon.permadeath.data;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tech.layon.permadeath.Main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class EndDataManager {

    private File endFile;
    private FileConfiguration config;

    private Main instance;

    private ArrayList<Integer> timeList;

    public EndDataManager(Main instance) {
        this.instance = instance;

        this.endFile = new File(instance.getDataFolder(), "endConfig.yml");
        this.config = YamlConfiguration.loadConfiguration(endFile);

        if (!endFile.exists()) {

            try {
                endFile.createNewFile();
            } catch (IOException e) {
                System.out.println("[ERROR] Ha ocurrido un error al crear el archivo 'endConfig.yml'");
            }
        }

        if (!config.contains("EnderCrystalRegenTime")) {

            config.set("EnderCrystalRegenTimeINFO", "La siguiente es una lista de números en segundos del tiempo que toma regenerar un End Crystal.");
            config.set("EnderCrystalRegenTime", Arrays.asList(60, 90, 120, 30, 240, 150));
        }

        if (!config.contains("PlacedObsidian")) {

            config.set("PlacedObsidian", new ArrayList<>());
        }

        if (!config.contains("ReplacedObsidian")) {

            config.set("ReplacedObsidian", false);
        }

        if (!config.contains("CreatedRegenZone")) {

            config.set("CreatedRegenZone", false);
        }

        if (!config.contains("DecoratedEndSpawn")) {

            config.set("DecoratedEndSpawn", true);
        }

        saveFile();
        reloadFile();

        loadSettings();
    }

    public File getEndFile() {
        return endFile;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void loadSettings() {

        this.timeList = (ArrayList<Integer>) config.getIntegerList("EnderCrystalRegenTime");
    }

    public ArrayList<Integer> getTimeList() {
        return timeList;
    }

    public boolean isCreatedRegenZone() {
        return config.getBoolean("CreatedRegenZone", false);
    }

    public boolean isReplacedObsidian() {
        return config.getBoolean("ReplacedObsidian", false);
    }

    public void setCreatedRegenZone(boolean value) {
        config.set("CreatedRegenZone", value);
        saveFile();
    }

    public void setReplacedObsidian(boolean value) {
        config.set("ReplacedObsidian", value);
        saveFile();
    }

    public void saveFile() {

        try {
            config.save(endFile);
        } catch (IOException e) {
            System.out.println("[ERROR] Ha ocurrido un error al guardar el archivo 'endConfig.yml'");
        }
    }

    public void reloadFile() {

        try {
            config.load(endFile);
        } catch (IOException e) {
            System.out.println("[ERROR] Ha ocurrido un error al guardar el archivo 'endConfig.yml'");
        } catch (InvalidConfigurationException e) {
            System.out.println("[ERROR] Ha ocurrido un error al guardar el archivo 'endConfig.yml'");
        }

        loadSettings();
    }
}

