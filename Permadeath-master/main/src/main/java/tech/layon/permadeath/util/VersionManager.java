package tech.layon.permadeath.util;

import lombok.Getter;

public final class VersionManager {

    private static final String REVISION = "26_3";

    @Getter
    private static final String version = REVISION;

    @Getter
    private static final MinecraftVersion minecraftVersion = MinecraftVersion.v26_3;

    private VersionManager() {
    }

    public static String getRev() {
        return REVISION;
    }

    public static boolean isValidVersionSet() {
        return true;
    }

    public static String getFormattedVersion() {
        return minecraftVersion.getFormattedName();
    }

    public static boolean isRunningPostNetherUpdate() {
        return true;
    }
}
