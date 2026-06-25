package dev.legendary.listeners;

import dev.legendary.LegendaryWeapons;
import dev.legendary.items.WeaponRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class DarkinBladeListener implements Listener {

    private final LegendaryWeapons plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    // Track players in leap phase so we can apply the crash on land
    private final Map<UUID, BukkitTask> leapTasks = new HashMap<>();
    private final Set<UUID> inLeap = new HashSet<>();

    private static final long COOLDOWN_MS = 8_000L;
    private static final double AOE_RADIUS = 3.5;
    private static final double AOE_DAMAGE = 6.0;

    public DarkinBladeListener(LegendaryWeapons plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // PASSIVE — Wither II on hit
    // =========================================================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.DARKIN_BLADE)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 3 * 20, 1, false, true, true));
    }

    // =========================================================================
    // ACTIVE — Leap upward then crash down with AOE
    // =========================================================================
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.DARKIN_BLADE)) return;

        event.setCancelled(true);

        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long ready = cooldowns.getOrDefault(uid, 0L);

        if (now < ready) {
            long remaining = (ready - now) / 1000;
            player.sendActionBar(Component.text("⏳ Leap: " + remaining + "s", NamedTextColor.RED));
            return;
        }
        if (inLeap.contains(uid)) return;

        cooldowns.put(uid, now + COOLDOWN_MS);
        inLeap.add(uid);

        // Phase 1: Launch upward
        player.setVelocity(new Vector(0, 1.4, 0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.8f);
        player.sendActionBar(Component.text("☠ Darkin Leap!", NamedTextColor.DARK_RED));

        // Phase 2: After ~0.6s, slam downward
        BukkitTask slamTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) { inLeap.remove(uid); return; }
            player.setVelocity(new Vector(player.getVelocity().getX(), -2.5, player.getVelocity().getZ()));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.6f);
            startLandingDetection(player);
        }, 12L);

        leapTasks.put(uid, slamTask);
    }

    /** Poll every tick until the player lands, then trigger AOE */
    private void startLandingDetection(Player player) {
        UUID uid = player.getUniqueId();
        final int[] ticksWaiting = {0};

        BukkitTask detector = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            ticksWaiting[0]++;
            if (!player.isOnline() || ticksWaiting[0] > 60) {
                inLeap.remove(uid);
                return;
            }
            if (player.isOnGround() || ticksWaiting[0] > 40) {
                triggerCrashAOE(player);
                inLeap.remove(uid);
            }
        }, 1L, 1L);

        // Store detection task so we can cancel it if needed
        leapTasks.put(uid + "_detect", detector);
    }

    private void triggerCrashAOE(Player player) {
        Location loc = player.getLocation();
        World world = player.getWorld();

        // Shockwave particles
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            for (double r = 0.5; r <= AOE_RADIUS; r += 0.8) {
                Location pLoc = loc.clone().add(Math.cos(angle) * r, 0.1, Math.sin(angle) * r);
                world.spawnParticle(Particle.LARGE_SMOKE, pLoc, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.FLAME, pLoc, 1, 0, 0, 0, 0.02);
            }
        }

        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
        world.playSound(loc, Sound.ENTITY_WITHER_DEATH, 0.6f, 1.5f);

        // Damage all nearby living entities (not the player)
        for (Entity entity : world.getNearbyEntities(loc, AOE_RADIUS, AOE_RADIUS, AOE_RADIUS)) {
            if (entity == player) continue;
            if (!(entity instanceof LivingEntity living)) continue;

            living.damage(AOE_DAMAGE, player);
            // Knock back away from player
            Vector kb = entity.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.8).setY(0.4);
            entity.setVelocity(kb);
        }

        player.sendActionBar(Component.text("☠ Darkin Crash!", NamedTextColor.DARK_RED));
    }
}
