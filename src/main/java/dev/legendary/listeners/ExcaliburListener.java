package dev.legendary.listeners;

import dev.legendary.LegendaryWeapons;
import dev.legendary.items.WeaponRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ExcaliburListener implements Listener {

    private final LegendaryWeapons plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Long> recentlySaved = new HashMap<>();

    private static final long COOLDOWN_MS = 30_000L;
    private static final double SONIC_BOOM_DAMAGE = 6.0;
    private static final double DAMAGE_ABSORB_RATIO = 0.30;
    private static final int EFFECT_DURATION_TICKS = 5 * 60 * 20; // 5 perc

    public ExcaliburListener(LegendaryWeapons plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // PASSIVE I — 30% sebzés elnyelése durability csökkenésként
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

        int durabilityLoss = Math.max(1, (int) Math.round(absorbed / 1.5));
        int currentDurability = held.getType().getMaxDurability() - held.getDurability();

        if (currentDurability > durabilityLoss) {
            held.setDurability((short) (held.getDurability() + durabilityLoss));
        } else {
            held.setDurability((short) (held.getType().getMaxDurability() - 1));
            player.sendActionBar(Component.text("⚔ Excalibur's edge dulls...", NamedTextColor.YELLOW));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (WeaponRegistry.isWeapon(event.getItem(), WeaponRegistry.EXCALIBUR)) {
            event.setCancelled(true);
        }
    }

    // =========================================================================
    // PASSIVE II — 30% esély halálos találatnál megmenekülni, 5 szívra gyógyul
    // =========================================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.EXCALIBUR)) return;

        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (finalHealth >= 1.0) return;

        long now = System.currentTimeMillis();
        Long lastSaved = recentlySaved.get(player.getUniqueId());
        if (lastSaved != null && now - lastSaved < 10_000L) return;

        if (Math.random() < 0.30) {
            event.setCancelled(true);
            player.setHealth(10.0);
            recentlySaved.put(player.getUniqueId(), now);

            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.2f);
            player.showTitle(Title.title(
                Component.text("✦ Excalibur's Will ✦", NamedTextColor.GOLD),
                Component.text("Fate has been defied.", NamedTextColor.YELLOW)
            ));
        }
    }

    // =========================================================================
    // PASSIVE III — negatív effektek konvertálása pozitívvá
    // Csak: Gyengeség, Sötétség, Vakság, Wither, Mérgezés
    // =========================================================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEffectApplied(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.EXCALIBUR)) return;

        PotionEffect applied = event.getNewEffect();
        if (applied == null) return;

        PotionEffectType type = applied.getType();
        PotionEffect replacement = getPositiveReplacement(type);

        if (replacement != null) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.removePotionEffect(type);
                player.addPotionEffect(replacement);

                // Sötétség esetén Speed II is jár
                if (type.equals(PotionEffectType.DARKNESS)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION_TICKS, 1));
                }

                player.sendActionBar(Component.text("✦ Excalibur converted the curse!", NamedTextColor.GOLD));
            }, 1L);
        }
    }

    /**
     * Csak a meghatározott negatív effektek:
     * Gyengeség → Erő I
     * Sötétség  → Erő III (+ Speed II az onEffectApplied-ban)
     * Vakság    → Éjjellátás
     * Wither    → Regeneráció I
     * Mérgezés  → Regeneráció I
     */
    private PotionEffect getPositiveReplacement(PotionEffectType type) {
        if (type == null) return null;
        int dur = EFFECT_DURATION_TICKS;

        if (type.equals(PotionEffectType.WEAKNESS))  return new PotionEffect(PotionEffectType.STRENGTH,      dur, 0);
        if (type.equals(PotionEffectType.DARKNESS))  return new PotionEffect(PotionEffectType.STRENGTH,      dur, 2); // Strength III
        if (type.equals(PotionEffectType.BLINDNESS)) return new PotionEffect(PotionEffectType.NIGHT_VISION,  dur, 0);
        if (type.equals(PotionEffectType.WITHER))    return new PotionEffect(PotionEffectType.REGENERATION,  dur, 0);
        if (type.equals(PotionEffectType.POISON))    return new PotionEffect(PotionEffectType.REGENERATION,  dur, 0);

        return null; // minden más effekt normálisan hat
    }

    // =========================================================================
    // ACTIVE — Sonic Boom: sugár a nézési irányba, 16 blokk, true damage
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
            player.sendActionBar(Component.text("⏳ Sonic Boom: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        fireSonicBoom(player);
        cooldowns.put(uid, now + COOLDOWN_MS);
    }

    private void fireSonicBoom(Player player) {
        var eyeLoc = player.getEyeLocation();
        var dir = eyeLoc.getDirection().normalize();

        // Particle trail végig a sugár mentén
        Set<UUID> alreadyHit = new HashSet<>();
        boolean hitSomething = false;

        for (double dist = 1.0; dist <= 16.0; dist += 0.5) {
            var point = eyeLoc.clone().add(dir.clone().multiply(dist));

            // Sonic boom particle minden 1 blokkban
            if (dist % 1.0 == 0) {
                player.getWorld().spawnParticle(org.bukkit.Particle.SONIC_BOOM, point, 1);
            }

            // Ellenfelek találat ellenőrzése (1.2 blokk sugarú gömb a sugár mentén)
            for (Entity entity : player.getWorld().getNearbyEntities(point, 1.2, 1.2, 1.2)) {
                if (entity == player) continue;
                if (!(entity instanceof LivingEntity living)) continue;
                if (alreadyHit.contains(entity.getUniqueId())) continue;

                // True damage: páncélt figyelmen kívül hagyja
                double currentHealth = living.getHealth();
                living.setHealth(Math.max(0.0, currentHealth - SONIC_BOOM_DAMAGE));
                // Combat tag a kill creditért
                living.damage(0.001, player);

                alreadyHit.add(entity.getUniqueId());
                hitSomething = true;
            }
        }

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        player.sendActionBar(Component.text(
            hitSomething ? "⚡ Sonic Boom!" : "⚡ Sonic Boom! (no target)",
            NamedTextColor.YELLOW
        ));
    }
}
