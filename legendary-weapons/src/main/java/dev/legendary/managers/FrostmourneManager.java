package dev.legendary.managers;

import dev.legendary.LegendaryWeapons;
import dev.legendary.items.WeaponRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class FrostmourneManager {

    private final LegendaryWeapons plugin;
    // playerUUID -> list of summoned entities
    private final Map<UUID, List<Entity>> summons = new HashMap<>();
    private BukkitTask cleanupTask;

    // Max souls before reset
    public static final int SOUL_THRESHOLD = 5;
    public static final int MAX_SOULS = 10;

    public FrostmourneManager(LegendaryWeapons plugin) {
        this.plugin = plugin;
        // Periodically remove dead summons from tracking
        cleanupTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanDeadSummons, 100L, 100L);
    }

    /** Called when a player with Frostmourne kills an entity */
    public void onKill(Player player, ItemStack frostmourne) {
        var pdc = frostmourne.getItemMeta().getPersistentDataContainer();
        int souls = pdc.getOrDefault(WeaponRegistry.KEY_FROST_SOULS, PersistentDataType.INTEGER, 0);
        souls++;

        // Particle + sound per soul
        player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.8f);

        if (souls >= SOUL_THRESHOLD) {
            souls = 0; // reset after summon
            summonCreature(player);
        }

        // Update item meta with new soul count
        var meta = frostmourne.getItemMeta();
        meta.getPersistentDataContainer().set(WeaponRegistry.KEY_FROST_SOULS, PersistentDataType.INTEGER, souls);
        updateSoulLore(meta, souls);
        frostmourne.setItemMeta(meta);
    }

    private void summonCreature(Player player) {
        boolean summonGiant = Math.random() < 0.5;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.6f);
        player.sendActionBar(Component.text(
            summonGiant ? "❄ A Frost Giant rises!" : "❄ A Rime Spectre emerges!",
            NamedTextColor.AQUA
        ));

        Entity summoned;
        if (summonGiant) {
            // Frost Giant — use a Giant with boosted stats
            Giant giant = (Giant) player.getWorld().spawnEntity(
                player.getLocation().add(2, 0, 0), EntityType.GIANT);
            giant.setCustomName("§bFrost Giant");
            giant.setCustomNameVisible(true);
            Objects.requireNonNull(giant.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(80.0);
            giant.setHealth(80.0);
            Objects.requireNonNull(giant.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(10.0);
            Objects.requireNonNull(giant.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.15);
            // Immune to Permafrost (slowness) — tracked via tag
            giant.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "frost_giant"), PersistentDataType.BYTE, (byte) 1);
            summoned = giant;
        } else {
            // Rime Spectre — use a Vex with boosted stats
            Vex vex = (Vex) player.getWorld().spawnEntity(
                player.getLocation().add(0, 1, 0), EntityType.VEX);
            vex.setCustomName("§3Rime Spectre");
            vex.setCustomNameVisible(true);
            Objects.requireNonNull(vex.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(30.0);
            vex.setHealth(30.0);
            Objects.requireNonNull(vex.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(6.0);
            vex.setCharging(true); // makes it slightly larger/different texture
            summoned = vex;
        }

        // Track this summon
        summons.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(summoned);

        // Auto-despawn after 60 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (summoned.isValid()) {
                summoned.getWorld().spawnParticle(Particle.SNOWFLAKE, summoned.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                summoned.remove();
            }
        }, 60 * 20L);
    }

    private void updateSoulLore(org.bukkit.inventory.meta.ItemMeta meta, int souls) {
        var lore = meta.lore();
        if (lore == null) return;
        // Soul count line is index 6 in our lore list
        for (int i = 0; i < lore.size(); i++) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(lore.get(i));
            if (plain.startsWith("Souls:")) {
                lore.set(i, Component.text("Souls: " + souls + " / " + SOUL_THRESHOLD, NamedTextColor.WHITE)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                break;
            }
        }
        meta.lore(lore);
    }

    private void cleanDeadSummons() {
        for (var entry : summons.entrySet()) {
            entry.getValue().removeIf(e -> !e.isValid());
        }
    }

    public void cleanup() {
        if (cleanupTask != null) cleanupTask.cancel();
        summons.values().forEach(list -> list.forEach(e -> { if (e.isValid()) e.remove(); }));
    }
}
