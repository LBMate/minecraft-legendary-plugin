package dev.legendary.listeners;

import dev.legendary.LegendaryWeapons;
import dev.legendary.items.WeaponRegistry;
import dev.legendary.managers.FrostmourneManager;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FrostmourneListener implements Listener {

    private final LegendaryWeapons plugin;
    private final FrostmourneManager manager;

    public FrostmourneListener(LegendaryWeapons plugin, FrostmourneManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // =========================================================================
    // PASSIVE — Permafrost (Slowness II) on hit
    // =========================================================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.FROSTMOURNE)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Don't apply Permafrost to Frost Giants (they're immune)
        NamespacedKey frostGiantKey = new NamespacedKey(plugin, "frost_giant");
        if (target.getPersistentDataContainer().has(frostGiantKey, PersistentDataType.BYTE)) return;

        // Permafrost: Slowness II for 3 seconds
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 1, false, true, true));

        // Ice particle burst on hit
        target.getWorld().spawnParticle(Particle.SNOWFLAKE,
            target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.05);
    }

    // =========================================================================
    // PASSIVE — Soul Harvest on kill
    // =========================================================================
    @EventHandler(priority = EventPriority.MONITOR)
    public void onKill(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!WeaponRegistry.isWeapon(held, WeaponRegistry.FROSTMOURNE)) return;

        // Don't harvest from our own summons
        NamespacedKey frostGiantKey = new NamespacedKey(plugin, "frost_giant");
        if (event.getEntity().getPersistentDataContainer().has(frostGiantKey, PersistentDataType.BYTE)) return;

        manager.onKill(player, held);

        // Display current soul count
        var meta = held.getItemMeta();
        int souls = meta.getPersistentDataContainer()
            .getOrDefault(WeaponRegistry.KEY_FROST_SOULS, PersistentDataType.INTEGER, 0);
        player.sendActionBar(Component.text(
            "❄ Frostmourne hungered... (" + souls + "/" + FrostmourneManager.SOUL_THRESHOLD + " souls)",
            NamedTextColor.AQUA
        ));
    }
}
