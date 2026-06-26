package dev.legendary.managers;

import dev.legendary.LegendaryWeapons;
import dev.legendary.items.WeaponRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class MjolnirManager {

    private final LegendaryWeapons plugin;
    private final Map<UUID, ThrowData> activeThrows = new HashMap<>();
    private BukkitTask tickTask;

    public MjolnirManager(LegendaryWeapons plugin) {
        this.plugin = plugin;
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public boolean isThrown(UUID playerUUID) {
        return activeThrows.containsKey(playerUUID);
    }

    public void throwHammer(Player player, ItemStack hammer) {
        if (activeThrows.containsKey(player.getUniqueId())) return;

        player.getInventory().setItemInMainHand(null);

        // Snowball projectile CustomModelData 1005-tel (Mjölnir textúra)
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(player.getEyeLocation().getDirection().multiply(2.0));
        projectile.setGravity(false);

        // item_model beállítása a projectile item-jére
        ItemStack snowballItem = new ItemStack(org.bukkit.Material.SNOWBALL);
        ItemMeta sm = snowballItem.getItemMeta();
        sm.setItemModel(new org.bukkit.NamespacedKey("legendary", "mjolnir_projectile"));
        snowballItem.setItemMeta(sm);
        projectile.setItem(snowballItem);

        ThrowData data = new ThrowData(player, projectile, hammer, System.currentTimeMillis());
        activeThrows.put(player.getUniqueId(), data);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.2f);
    }

    private void tick() {
        Iterator<Map.Entry<UUID, ThrowData>> it = activeThrows.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ThrowData data = entry.getValue();
            Player player = data.player;

            if (!player.isOnline() || !data.projectile.isValid()) {
                returnHammer(data);
                it.remove();
                continue;
            }

            long elapsed = System.currentTimeMillis() - data.thrownAt;
            Location projLoc = data.projectile.getLocation();

            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, projLoc, 3, 0.1, 0.1, 0.1, 0.02);

            if (elapsed < 600) {
                hitNearbyEntities(data, player, projLoc);
            } else {
                Vector toPlayer = player.getEyeLocation().toVector()
                    .subtract(projLoc.toVector()).normalize().multiply(2.2);
                data.projectile.setVelocity(toPlayer);

                if (projLoc.distanceSquared(player.getLocation()) < 2.5) {
                    returnHammer(data);
                    it.remove();
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
                    player.sendActionBar(Component.text("⚡ Mjölnir returned!", NamedTextColor.GOLD));
                }
            }
        }
    }

    private void hitNearbyEntities(ThrowData data, Player player, Location loc) {
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
            if (entity == player) continue;
            if (entity == data.projectile) continue;
            if (!(entity instanceof LivingEntity living)) continue;
            if (data.hitEntities.contains(entity.getUniqueId())) continue;

            living.damage(8.0, player);
            data.hitEntities.add(entity.getUniqueId());

            loc.getWorld().strikeLightningEffect(loc);
            loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.0f);

            if (loc.getWorld().hasStorm()) {
                living.damage(4.0, player);
            }
        }
    }

    private void returnHammer(ThrowData data) {
        if (data.projectile.isValid()) data.projectile.remove();
        Player player = data.player;
        if (player.isOnline()) {
            player.getInventory().setItemInMainHand(data.hammer);
        }
    }

    public void cleanup() {
        if (tickTask != null) tickTask.cancel();
        activeThrows.values().forEach(d -> {
            if (d.projectile.isValid()) d.projectile.remove();
            if (d.player.isOnline()) d.player.getInventory().addItem(d.hammer);
        });
    }

    private static class ThrowData {
        final Player player;
        final Snowball projectile;
        final ItemStack hammer;
        final long thrownAt;
        final Set<UUID> hitEntities = new HashSet<>();

        ThrowData(Player player, Snowball projectile, ItemStack hammer, long thrownAt) {
            this.player = player;
            this.projectile = projectile;
            this.hammer = hammer;
            this.thrownAt = thrownAt;
        }
    }
}
