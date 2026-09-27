package tech.sebazcrc.permadeath.util;

public enum MinecraftVersion {

    /*
     * Se conservan las versiones antiguas únicamente
     * para no romper código legado que todavía las
     * pueda mencionar.
     *
     * El plugin actual solo soporta v26_3.
     */
    v1_15_R1,
    v1_16_R3,
    v1_20_R1,
    v26_3;

    private static final String REVISION_PATTERN = "_R\\d+";

    public boolean isAboveOrEqual(MinecraftVersion compare) {
        return ordinal() >= compare.ordinal();
    }

    public boolean isSubVersionOf(MinecraftVersion version) {

        if (version == null) {
            return false;
        }

        return this == version;
    }

    public String getFormattedName() {

        String formatted = name()
                .replaceAll(
                        REVISION_PATTERN,
                        "")
                .replace(
                        "_",
                        ".");

        return formatted.startsWith("v")
                ? formatted.substring(1)
                : formatted;
    }
}