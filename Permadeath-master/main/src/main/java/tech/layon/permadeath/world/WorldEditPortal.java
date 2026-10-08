package tech.layon.permadeath.world;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import tech.layon.permadeath.Main;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.SplittableRandom;

public class WorldEditPortal {

    public static boolean generateIsland(World world, int x, int z, int height, SplittableRandom random) {
        Clipboard clipboard;
        File file;

        switch (random.nextInt(5)) {
            case 0:
                file = new File(Main.getInstance().getDataFolder(), "schematics/island1.schem");
                break;
            case 1:
                file = new File(Main.getInstance().getDataFolder(), "schematics/island2.schem");
                break;
            case 2:
                file = new File(Main.getInstance().getDataFolder(), "schematics/island3.schem");
                break;
            case 3:
                file = new File(Main.getInstance().getDataFolder(), "schematics/island4.schem");
                break;
            default:
                file = new File(Main.getInstance().getDataFolder(), "schematics/island5.schem");
                break;
        }

        try {
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            if (format != null && file.isFile()) {
                try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                    clipboard = reader.read();
                }
            } else {
                int schematic = Integer.parseInt(file.getName().replace("island", "").replace(".schem", ""));
                String resourcePath = "updated_schematics/island" + schematic + ".schem";
                InputStream resource = Main.getInstance().getResource(resourcePath);
                if (resource == null) {
                    resourcePath = "original_schematics/island" + schematic + ".schem";
                    resource = Main.getInstance().getResource(resourcePath);
                }
                if (resource == null) {
                    Main.getInstance().getLogger().warning("No se encontró el schematic " + resourcePath + " para la isla del End.");
                    return false;
                }
                ClipboardFormat resourceFormat = ClipboardFormats.findByFile(new File("island" + schematic + ".schem"));
                if (resourceFormat == null) {
                    Main.getInstance().getLogger().warning("WorldEdit no reconoce el formato del schematic de la isla " + schematic + ".");
                    resource.close();
                    return false;
                }
                try (InputStream stream = resource; ClipboardReader reader = resourceFormat.getReader(stream)) {
                    clipboard = reader.read();
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return false;
        }

        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(world), -1)) {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(x, height + 20, z))
                    .ignoreAirBlocks(true)
                    .build();
            Operations.complete(operation);
            return true;
        } catch (WorldEditException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean generateYtic(World world, int x, int z, int height) {
        Clipboard clipboard;
        File file = new File(Main.getInstance().getDataFolder(), "schematics/ytic.schem");

        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null || !file.isFile()) {
            InputStream resource = Main.getInstance().getResource("updated_schematics/ytic.schem");
            if (resource == null) resource = Main.getInstance().getResource("original_schematics/ytic.schem");
            if (resource == null) {
                Main.getInstance().getLogger().warning("No se encontró el schematic ytic.schem.");
                return false;
            }
            try (InputStream stream = resource; ClipboardReader reader = ClipboardFormats.findByFile(new File("ytic.schem")).getReader(stream)) {
                clipboard = reader.read();
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
                return false;
            }
        } else try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            clipboard = reader.read();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(world), -1)) {
            ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);

            Operation operation = clipboardHolder
                    .createPaste(editSession)
                    .to(BlockVector3.at(x, height + 34, z))
                    .ignoreAirBlocks(true)
                    .copyEntities(true)
                    .build();

            Operations.complete(operation);
            //editSession.replaceBlocks(
            return true;
        } catch (WorldEditException e) {
            e.printStackTrace();
            return false;
        }
    }
    public static void generatePortal(boolean overworld, Location to) {

        if (!Main.getInstance().getBeData().generatedOverWorldBeginningPortal() && overworld) {

            int x = Math.max(1, Main.getInstance().getConfig().getInt("TheBeginning.X-Limit"));
            int z = Math.max(1, Main.getInstance().getConfig().getInt("TheBeginning.Z-Limit"));
            Random random = new Random();

            int ranX = random.nextInt(x);
            int ranZ = random.nextInt(z);

            if (random.nextBoolean()) {
                ranX = ranX * -1;
            }
            if (random.nextBoolean()) {

                ranZ = ranZ * -1;
            }
            Location loc = new Location(Main.getInstance().world, ranX, 0, ranZ);

            int highestBlockAt = Main.getInstance().world.getHighestBlockAt(loc).getY();
            if (highestBlockAt == -1) {
                highestBlockAt = 50;
            }

            highestBlockAt = highestBlockAt + 15;
            loc.setY(highestBlockAt);
            if (pasteSchematic(loc, new File(Main.getInstance().getDataFolder().getAbsolutePath() + "/schematics/beginning_portal.schem"))) {
                Main.getInstance().getBeData().setOverWorldPortal(loc);
            }
        }

        if (!Main.getInstance().getBeData().generatedBeginningPortal() && !overworld) {
            World beginning = Main.getInstance().getBeginningManager() != null
                    ? Main.getInstance().getBeginningManager().getBeginningWorld()
                    : Bukkit.getWorld("pdc_the_beginning");
            if (beginning == null) return;
            beginning.loadChunk(to.getChunk());
            if (pasteSchematic(to, new File(Main.getInstance().getDataFolder().getAbsolutePath() + "/schematics/beginning_portal.schem"))) {
                Main.getInstance().getBeData().setBeginningPortal(to);
            }
        }
    }

    public static boolean pasteSchematic(Location loc, File schematic) {
        com.sk89q.worldedit.world.World adaptedWorld = BukkitAdapter.adapt(loc.getWorld());
        ClipboardFormat format = ClipboardFormats.findByFile(schematic);
        try {
            Clipboard clipboard;
            if (format != null && schematic.isFile()) {
                try (ClipboardReader reader = format.getReader(new FileInputStream(schematic))) {
                    clipboard = reader.read();
                }
            } else {
                String name = schematic.getName();
                InputStream resource = Main.getInstance().getResource("updated_schematics/" + name);
                if (resource == null) resource = Main.getInstance().getResource("original_schematics/" + name);
                if (resource == null) {
                    Main.getInstance().getLogger().warning("No se encontró el schematic " + name + ".");
                    return false;
                }
                ClipboardFormat resourceFormat = ClipboardFormats.findByFile(new File(name));
                if (resourceFormat == null) {
                    resource.close();
                    Main.getInstance().getLogger().warning("WorldEdit no reconoce el formato de " + name + ".");
                    return false;
                }
                try (InputStream stream = resource; ClipboardReader reader = resourceFormat.getReader(stream)) {
                    clipboard = reader.read();
                }
            }

            try (EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(adaptedWorld, -1)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()))
                        .ignoreAirBlocks(true)
                        .build();
                Operations.complete(operation);
                editSession.flushSession();
                return true;
            }
        } catch (IOException | WorldEditException e) {
            e.printStackTrace();
            return false;
        }
    }
}
