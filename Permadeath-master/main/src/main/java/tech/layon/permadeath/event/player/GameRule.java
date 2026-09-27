package tech.layon.permadeath.event.player;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

/**
 * Puente de compatibilidad de gamerules para Paper y Spigot 26.3.
 *
 * Paper 26.3 movio los campos vanilla modernos a org.bukkit.GameRules,
 * mientras Spigot 26.3 todavia expone varios de ellos directamente desde
 * org.bukkit.GameRule. Para evitar enlazar el plugin a una de esas dos
 * representaciones, resolvemos la gamerule por su NamespacedKey comun.
 */
public final class GameRule {

    public static final org.bukkit.GameRule<Boolean> NATURAL_HEALTH_REGENERATION =
            resolveBooleanRule("natural_health_regeneration");

    private GameRule() {
    }

    @SuppressWarnings("unchecked")
    private static org.bukkit.GameRule<Boolean> resolveBooleanRule(String key) {
        org.bukkit.GameRule<?> rule = Registry.GAME_RULE.get(
                NamespacedKey.minecraft(key)
        );

        if (rule == null) {
            throw new IllegalStateException(
                    "No se encontro la gamerule minecraft:" + key
            );
        }

        return (org.bukkit.GameRule<Boolean>) rule;
    }
}
