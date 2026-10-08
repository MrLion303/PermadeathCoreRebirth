package tech.layon.permadeath.event.entity;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import tech.layon.permadeath.Main;
import tech.layon.permadeath.util.TextUtils;

import java.util.Objects;

public class TotemListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void totemNerf(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;

        ItemStack mainHand = p.getInventory().getItemInMainHand();
        ItemStack offHand = p.getInventory().getItemInOffHand();
        if ((mainHand == null || mainHand.getType() != Material.TOTEM_OF_UNDYING)
                && (offHand == null || offHand.getType() != Material.TOTEM_OF_UNDYING)) {
            return;
        }

        Main plugin = Main.getInstance();
        if (!plugin.getConfig().getBoolean("TotemFail.Enable")) return;

        int day = plugin.getDay();
        String sectionPath = "TotemFail.FailProbs." + day;
        if (!plugin.getConfig().contains(sectionPath)) return;

        int failProb = Math.max(0, Math.min(100, plugin.getConfig().getInt(sectionPath)));
        String player = p.getName();
        String totemFail = plugin.getConfig().getString("TotemFail.ChatMessage", "&7¡El tótem de &c%player% &7ha fallado!");
        String totemMessage = plugin.getConfig().getString("TotemFail.PlayerUsedTotemMessage", "&7El jugador %player% ha consumido un tótem.");
        String medalMessage = plugin.getConfig().getString("TotemFail.Medalla", "&7¡El jugador %player% ha usado su medalla de superviviente!");

        boolean specialTotem = doPlayerHaveSpecialTotem(p);
        int neededTotems = day >= 60 ? 3 : (day >= 40 ? 2 : 1);

        if (specialTotem) {
            Bukkit.broadcastMessage(TextUtils.format(medalMessage.replace("%player%", player)));
            return;
        }

        int availableTotems = countTotems(p);
        if (availableTotems < neededTotems) {
            String notEnough = plugin.getConfig().getString(
                    "TotemFail.NotEnoughTotems",
                    "&7¡%player% no tenía suficientes tótems en el inventario!");
            Bukkit.broadcastMessage(TextUtils.format(notEnough.replace("%player%", player)));
            event.setCancelled(true);
            return;
        }

        int roll = (int) (Math.random() * 100) + 1;
        boolean failed = failProb >= 100 || roll > (100 - failProb);

        String comparison = failed ? "=" : "!=";
        int shown = failed ? failProb : roll;
        Bukkit.broadcastMessage(TextUtils.format(
                totemMessage.replace("%player%", player)
                        .replace("%porcent%", comparison)
                        .replace("%totem_fail%", String.valueOf(shown))
                        .replace("%number%", String.valueOf(failProb))));

        if (failed) {
            removeTotems(p, neededTotems);
            Bukkit.broadcastMessage(TextUtils.format(totemFail.replace("%player%", player)));
            event.setCancelled(true);
            return;
        }

        // Bukkit consumirá el tótem que activa el evento; retiramos solo los adicionales.
        removeTotems(p, Math.max(0, neededTotems - 1));
        if (neededTotems > 1) {
            String multi = plugin.getConfig().getString(
                    "TotemFail.ChatMessageTotems",
                    "&7¡Los tótems de &c%player% &7han sido consumidos!");
            Bukkit.broadcastMessage(TextUtils.format(multi.replace("%player%", player)));
        }
    }

    private int countTotems(Player p) {
        int total = 0;
        for (ItemStack item : p.getInventory().getStorageContents()) {
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING) {
                total += item.getAmount();
            }
        }
        ItemStack off = p.getInventory().getItemInOffHand();
        if (off != null && off.getType() == Material.TOTEM_OF_UNDYING) {
            total += off.getAmount();
        }
        return total;
    }

    private void removeTotems(Player p, int amount) {
        int remaining = amount;
        if (remaining <= 0) return;

        ItemStack off = p.getInventory().getItemInOffHand();
        if (off != null && off.getType() == Material.TOTEM_OF_UNDYING && remaining > 0) {
            int take = Math.min(remaining, off.getAmount());
            off.setAmount(off.getAmount() - take);
            if (off.getAmount() <= 0) p.getInventory().setItemInOffHand(null);
            remaining -= take;
        }

        if (remaining <= 0) return;
        for (int slot = 0; slot < p.getInventory().getStorageContents().length && remaining > 0; slot++) {
            ItemStack item = p.getInventory().getItem(slot);
            if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) continue;
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            if (item.getAmount() <= 0) p.getInventory().setItem(slot, null);
            remaining -= take;
        }
    }

    private ItemStack getTotem(Player p) {
        return getSpecialTotem(p) == EnumPlayerTotemSlot.MAIN_HAND ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand();
    }

    private EnumPlayerTotemSlot getSpecialTotem(Player p) {
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off = p.getInventory().getItemInOffHand();

        if (isSpecial(main)) {
            return EnumPlayerTotemSlot.MAIN_HAND;
        } else if (isSpecial(off)) {
            return EnumPlayerTotemSlot.OFF_HAND;
        } else {
            return null;
        }
    }

    private boolean doPlayerHaveSpecialTotem(Player p) {
        boolean tieneMedalla = false;

        if (p.getInventory().getItemInMainHand() != null) {
            if (isSpecial(p.getInventory().getItemInMainHand())) {
                tieneMedalla = true;
            }
        }

        if (p.getInventory().getItemInOffHand() != null) {
            if (isSpecial(p.getInventory().getItemInOffHand())) {
                tieneMedalla = true;
            }
        }

        return tieneMedalla;
    }

    private boolean isSpecial(ItemStack off) {
        return off != null && off.getType() == Material.TOTEM_OF_UNDYING
                && off.hasItemMeta() && off.getItemMeta() != null && off.getItemMeta().isUnbreakable();
    }

    public enum EnumPlayerTotemSlot {
        MAIN_HAND, OFF_HAND
    }
}

