package dev.legendary.listeners;

import dev.legendary.LegendaryWeapons;
import dev.legendary.items.WeaponRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExcaliburListener implements Listener {

    private final LegendaryWeapons plugin;
    // Cooldown map: playerUUID -> system ms when ability becomes available again
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    // Track players currently saved by Passive II to prevent double-trigger
    private final Map<UUID, Long> recentlySaved = new HashMap<>();

    private static final int COOLDOWN_TICKS = 30 * 20; // 30 seconds in ticks
    private static final long COOLDOWN_MS = 30_000L;
    private static final double SONIC_BOOM_DAMAGE = 6.0;
    private static final double DAMAGE_ABSORB_RATIO = 0.30;
    private static final int EFFECT_DURATION_TICKS = 5 * 60 * 20; // 5 minutes

    public ExcaliburListener(LegendaryWeapons plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // PASSIVE I — absorb 30% of incoming damage as durability loss
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.EXCALIBUR)) return;

        double originalDamage = event.getDamage();
        double absorbed = originalDamage * DAMAGE_ABSORB_RATIO;
        double remaining = originalDamage - absorbed;

        event.setDamage(remaining);

        // Convert absorbed damage to durability loss (1 durability per ~1.5 damage absorbed)
        int durabilityLoss = Math.max(1, (int) Math.round(absorbed / 1.5));
        int currentDurability = held.getType().getMaxDurability() - held.getDurability();

        if (currentDurability > durabilityLoss) {
            held.setDurability((short) (held.getDurability() + durabilityLoss));
        } else {
            // Sword is about to break — repair it slightly so it never fully disappears
            held.setDurability((short) (held.getType().getMaxDurability() - 1));
            player.sendActionBar(Component.text("⚔ Excalibur's edge dulls...", NamedTextColor.YELLOW));
        }
    }

    // Prevent natural item damage from stacking on top of our manual durability system
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (WeaponRegistry.isWeapon(event.getItem(), WeaponRegistry.EXCALIBUR)) {
            event.setCancelled(true); // We handle durability manually in onPlayerDamaged
        }
    }

    // =========================================================================
    // PASSIVE II — 30% chance to survive a fatal hit, heal to 5 hearts
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.EXCALIBUR)) return;

        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (finalHealth >= 1.0) return; // not fatal

        // Check recently saved cooldown (prevent multiple saves in quick succession)
        long now = System.currentTimeMillis();
        Long lastSaved = recentlySaved.get(player.getUniqueId());
        if (lastSaved != null && now - lastSaved < 10_000L) return;

        if (Math.random() < 0.30) {
            event.setCancelled(true);
            player.setHealth(10.0); // 5 hearts = 10 HP
            recentlySaved.put(player.getUniqueId(), now);

            // Visual + sound feedback
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.2f);
            player.showTitle(net.kyori.adventure.title.Title.title(
                Component.text("✦ Excalibur's Will ✦", NamedTextColor.GOLD),
                Component.text("Fate has been defied.", NamedTextColor.YELLOW)
            ));
        }
    }

    // =========================================================================
    // PASSIVE III — convert negative effects to positive
    // =========================================================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEffectApplied(org.bukkit.event.entity.EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAction() != org.bukkit.event.entity.EntityPotionEffectEvent.Action.ADDED) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.EXCALIBUR)) return;

        PotionEffect applied = event.getNewEffect();
        if (applied == null) return;

        PotionEffectType negative = applied.getType();
        PotionEffect replacement = getPositiveReplacement(negative);

        if (replacement != null) {
            // Cancel the negative effect and schedule positive replacement
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.removePotionEffect(negative);
                player.addPotionEffect(replacement);
                player.sendActionBar(Component.text("✦ Excalibur converted the curse!", NamedTextColor.GOLD));
            }, 1L);
        }
    }

    /** Maps negative effects to their positive counterparts as per spec */
    private PotionEffect getPositiveReplacement(PotionEffectType type) {
        if (type == null) return null;
        int dur = EFFECT_DURATION_TICKS;

        if (type.equals(PotionEffectType.WEAKNESS))
            return new PotionEffect(PotionEffectType.STRENGTH, dur, 0);           // Weakness → Strength I
        if (type.equals(PotionEffectType.SLOWNESS))
            return new PotionEffect(PotionEffectType.SPEED, dur, 0);              // Slowness → Speed I
        if (type.equals(PotionEffectType.POISON))
            return new PotionEffect(PotionEffectType.REGENERATION, dur, 0);       // Poison → Regeneration I
        if (type.equals(PotionEffectType.WITHER))
            return new PotionEffect(PotionEffectType.REGENERATION, dur, 0);       // Wither → Regeneration I
        if (type.equals(PotionEffectType.DARKNESS))
            // Darkness → Strength III + Speed II (schedule second effect separately)
            return new PotionEffect(PotionEffectType.STRENGTH, dur, 2);           // handled below
        if (type.equals(PotionEffectType.MINING_FATIGUE))
            return new PotionEffect(PotionEffectType.HASTE, dur, 0);              // bonus: Mining Fatigue → Haste
        if (type.equals(PotionEffectType.HUNGER))
            return new PotionEffect(PotionEffectType.SATURATION, dur, 0);         // bonus: Hunger → Saturation
        if (type.equals(PotionEffectType.BLINDNESS))
            return new PotionEffect(PotionEffectType.NIGHT_VISION, dur, 0);       // bonus: Blindness → Night Vision

        return null; // not a handled negative effect
    }

    // Special case for Darkness which needs to apply TWO positive effects
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDarknessApplied(org.bukkit.event.entity.EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAction() != org.bukkit.event.entity.EntityPotionEffectEvent.Action.ADDED) return;
        if (event.getNewEffect() == null) return;
        if (!event.getNewEffect().getType().equals(PotionEffectType.DARKNESS)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.EXCALIBUR)) return;

        // The primary handler above already cancels it and adds Strength III.
        // Here we add the Speed II as well on the next tick.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (WeaponRegistry.isWeapon(player.getInventory().getItemInMainHand(), WeaponRegistry.EXCALIBUR)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION_TICKS, 1));
            }
        }, 2L);
    }

    // =========================================================================
    // ACTIVE — Sonic Boom (right-click, 30-second cooldown, 6 true damage)
    // =========================================================================
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.EXCALIBUR)) return;

        event.setCancelled(true);

        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long ready = cooldowns.getOrDefault(uid, 0L);

        if (now < ready) {
            long remaining = (ready - now) / 1000;
            player.sendActionBar(Component.text("⏳ Sonic Boom: " + remaining + "s remaining", NamedTextColor.RED));
            return;
        }

        // Find nearest entity in line of sight (up to 15 blocks)
        LivingEntity target = getNearestLookTarget(player, 15.0);
        if (target == null) {
            player.sendActionBar(Component.text("No target in range.", NamedTextColor.GRAY));
            return;
        }

        // Apply 6 true damage (bypasses armor)
        target.damage(0.01, player); // trigger combat tag
        target.setHealth(Math.max(0, target.getHealth() - SONIC_BOOM_DAMAGE));

        // Visual + sound
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        player.getWorld().spawnParticle(
            org.bukkit.Particle.SONIC_BOOM,
            target.getLocation().add(0, 1, 0),
            1
        );

        cooldowns.put(uid, now + COOLDOWN_MS);
        player.sendActionBar(Component.text("⚡ Sonic Boom!", NamedTextColor.YELLOW));
    }

    private LivingEntity getNearestLookTarget(Player player, double range) {
        var loc = player.getEyeLocation();
        var dir = loc.getDirection();
        LivingEntity closest = null;
        double closestDist = range * range;

        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), range, range, range)) {
            if (e == player) continue;
            if (!(e instanceof LivingEntity living)) continue;

            var toEntity = e.getLocation().add(0, 1, 0).subtract(loc).toVector();
            double dot = toEntity.normalize().dot(dir);
            if (dot < 0.95) continue; // roughly in line of sight (within ~18 degrees)

            double dist = e.getLocation().distanceSquared(player.getLocation());
            if (dist < closestDist) {
                closestDist = dist;
                closest = living;
            }
        }
        return closest;
    }
}
