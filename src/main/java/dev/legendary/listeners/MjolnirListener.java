package dev.legendary.listeners;

import dev.legendary.LegendaryWeapons;
import dev.legendary.items.WeaponRegistry;
import dev.legendary.managers.MjolnirManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MjolnirListener implements Listener {

    private final LegendaryWeapons plugin;
    private final MjolnirManager manager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final long COOLDOWN_MS = 5_000L;

    public MjolnirListener(LegendaryWeapons plugin, MjolnirManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // =========================================================================
    // PASSIVE — Bonus lightning damage when raining
    // =========================================================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.MJOLNIR)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if (player.getWorld().hasStorm()) {
            // Extra 4 damage + lightning strike effect when raining
            event.setDamage(event.getDamage() + 4.0);
            player.getWorld().strikeLightningEffect(target.getLocation());
            player.sendActionBar(Component.text("⚡ Thor's Wrath!", NamedTextColor.GOLD));
        }
    }

    // =========================================================================
    // ACTIVE — Throw Mjölnir (returns like a boomerang)
    // =========================================================================
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.MJOLNIR)) return;

        event.setCancelled(true);

        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long ready = cooldowns.getOrDefault(uid, 0L);

        if (now < ready) {
            long remaining = (ready - now) / 1000;
            player.sendActionBar(Component.text("⏳ Mjölnir: " + remaining + "s", NamedTextColor.RED));
            return;
        }

        if (manager.isThrown(uid)) {
            player.sendActionBar(Component.text("Mjölnir is already in flight!", NamedTextColor.GRAY));
            return;
        }

        cooldowns.put(uid, now + COOLDOWN_MS);
        manager.throwHammer(player, held.clone());
        player.sendActionBar(Component.text("⚡ Mjölnir thrown!", NamedTextColor.YELLOW));
    }
}
