package tech.sebazcrc.permadeath.util;

import lombok.Getter;
import org.bukkit.Location;
import tech.sebazcrc.permadeath.util.interfaces.DeathModule;
import tech.sebazcrc.permadeath.util.interfaces.InfernalNetheriteBlock;
import tech.sebazcrc.permadeath.util.interfaces.NMSAccessor;
import tech.sebazcrc.permadeath.util.interfaces.NMSHandler;

import java.lang.reflect.InvocationTargetException;

public final class NMS {

    private static final String CURRENT_REVISION = "26_3";

    private static final String BASE_PACKAGE = "tech.sebazcrc.permadeath.nms.v26_3";

    @Getter
    private static NMSAccessor accessor;

    @Getter
    private static NMSHandler handler;

    @Getter
    private static InfernalNetheriteBlock netheriteBlock;

    private static Class<?> deathModuleClass;

    static {

        try {

            deathModuleClass = Class.forName(
                    search(
                            "entity.DeathModuleImpl"));

        } catch (ClassNotFoundException e) {

            throw new ExceptionInInitializerError(
                    "No se encontró DeathModuleImpl para Minecraft 26.3: "
                            + e.getMessage());
        }
    }

    private NMS() {
    }

    public static void loadNMSAccessor()
            throws ClassNotFoundException,
            NoSuchMethodException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException {

        accessor = (NMSAccessor) Class.forName(
                search(
                        "NMSAccessorImpl"))
                .getConstructor()
                .newInstance();
    }

    public static void loadNMSHandler()
            throws ClassNotFoundException,
            NoSuchMethodException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException {

        handler = (NMSHandler) Class.forName(
                search(
                        "NMSHandlerImpl"))
                .getConstructor()
                .newInstance();
    }

    public static void loadInfernalNetheriteBlock()
            throws ClassNotFoundException,
            NoSuchMethodException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException {

        netheriteBlock = (InfernalNetheriteBlock) Class.forName(
                search(
                        "block.InfernalNetheriteBlockImpl"))
                .getConstructor()
                .newInstance();
    }

    public static String search(String classPath) {

        return BASE_PACKAGE
                + "."
                + classPath;
    }

    /*
     * Se mantiene esta sobrecarga por compatibilidad
     * con código antiguo del plugin.
     */
    public static String search(
            String revision,
            String classPath) {

        String normalizedRevision = revision == null
                || revision.isBlank()
                        ? CURRENT_REVISION
                        : revision;

        if (normalizedRevision.startsWith("v")) {

            normalizedRevision = normalizedRevision.substring(1);
        }

        return "tech.sebazcrc.permadeath.nms.v"
                + normalizedRevision
                + "."
                + classPath;
    }

    public static void spawnDeathModule(
            Location location) {

        if (deathModuleClass == null) {

            throw new IllegalStateException(
                    "DeathModuleImpl de Minecraft 26.3 no está cargado.");
        }

        try {

            DeathModule module = (DeathModule) deathModuleClass
                    .getConstructor()
                    .newInstance();

            module.spawn(location);

        } catch (ReflectiveOperationException e) {

            throw new RuntimeException(
                    "No se pudo crear el Death Module.",
                    e);
        }
    }
}