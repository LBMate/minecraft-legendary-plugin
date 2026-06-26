package dev.legendary.listeners;

import dev.legendary.LegendaryWeapons;
import dev.legendary.items.WeaponRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FrostmourneListener implements Listener {

    private final LegendaryWeapons plugin;
    // Tick task per player hogy folyamatosan ellenőrizze a tűzállóságot
    private final Map<UUID, BukkitTask> fireTasks = new HashMap<>();

    private static final int FREEZE_TICKS = 60; // ~3 másodperc fagyasztás találatonként

    public FrostmourneListener(LegendaryWeapons plugin) {
        this.plugin = plugin;
        // Minden 20 tickben ellenőrzi az összes online játékost
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkFireResistance, 20L, 20L);
    }

    // =========================================================================
    // PASSIVE I — Freeze találatonként (mint powdered snow)
    // =========================================================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.FROSTMOURNE)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Freeze ticks hozzáadása — max 140 tick (7 mp), találatonként +60
        int current = target.getFreezeTicks();
        target.setFreezeTicks(Math.min(current + FREEZE_TICKS, 140));

        // Jég particle
        target.getWorld().spawnParticle(Particle.SNOWFLAKE,
            target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.8f);
    }

    // =========================================================================
    // PASSIVE II — Soul harvest + tűzállóság 5+ lélekre
    // =========================================================================
    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event) {
        // Csak player kill számít léleknek
        if (!(event.getEntity() instanceof Player killed)) return;
        if (!(killed.getKiller() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.FROSTMOURNE)) return;

        // Soul count növelése
        var meta = held.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        int souls = pdc.getOrDefault(WeaponRegistry.KEY_FROST_SOULS, PersistentDataType.INTEGER, 0);
        souls = Math.min(souls + 1, 5); // max 5

        pdc.set(WeaponRegistry.KEY_FROST_SOULS, PersistentDataType.INTEGER, souls);
        updateSoulLore(meta, souls);
        held.setItemMeta(meta);

        // Soul particle + hang
        player.getWorld().spawnParticle(Particle.SOUL,
            player.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.8f);
        player.sendActionBar(Component.text(
            "❄ Frostmourne hungered... (" + souls + "/5 souls)" +
            (souls >= 5 ? " — Fire Resistance active!" : ""),
            NamedTextColor.AQUA
        ));
    }

    // =========================================================================
    // Folyamatos tűzállóság ellenőrzés — ha 5 lélek és kezében van a fegyver
    // =========================================================================
    private void checkFireResistance() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            ItemStack held = player.getInventory().getItemInMainHand();
            boolean hasFrost = WeaponRegistry.isWeapon(held, WeaponRegistry.FROSTMOURNE);

            if (hasFrost) {
                var pdc = held.getItemMeta().getPersistentDataContainer();
                int souls = pdc.getOrDefault(WeaponRegistry.KEY_FROST_SOULS, PersistentDataType.INTEGER, 0);

                if (souls >= 5) {
                    // Adjunk tűzállóságot folyamatosan (40 tickenként megújul)
                    player.addPotionEffect(new PotionEffect(
                        PotionEffectType.FIRE_RESISTANCE, 40, 0, true, false, true));
                }
            } else {
                // Ha levette a fegyvert és a tűzállóság Frostmourne-tól jött, távolítsuk el
                // (ambient=true jelzi hogy tőlünk jött)
                var effect = player.getPotionEffect(PotionEffectType.FIRE_RESISTANCE);
                if (effect != null && effect.isAmbient() && !effect.hasParticles()) {
                    player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
                }
            }
        }
    }

    private void updateSoulLore(org.bukkit.inventory.meta.ItemMeta meta, int souls) {
        var lore = meta.lore();
        if (lore == null) return;
        for (int i = 0; i < lore.size(); i++) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(lore.get(i));
            if (plain.startsWith("Souls:")) {
                lore.set(i, Component.text("Souls: " + souls + " / 5", souls >= 5 ? NamedTextColor.GOLD : NamedTextColor.WHITE)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                break;
            }
        }
        meta.lore(lore);
    }
}
