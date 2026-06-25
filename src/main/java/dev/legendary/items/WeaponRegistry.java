package dev.legendary.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WeaponRegistry {

    // Namespace keys used to identify weapons and track state
    public static final String NAMESPACE = "legendary";

    // Weapon IDs
    public static final String DARKIN_BLADE   = "darkin_blade";
    public static final String FROSTMOURNE    = "frostmourne";
    public static final String EXCALIBUR      = "excalibur";
    public static final String MJOLNIR        = "mjolnir";

    // PDC keys
    public static final NamespacedKey KEY_WEAPON_ID      = key("weapon_id");
    public static final NamespacedKey KEY_FROST_SOULS     = key("frost_souls");
    public static final NamespacedKey KEY_DARKIN_COOLDOWN = key("darkin_cooldown");
    public static final NamespacedKey KEY_EXCAL_COOLDOWN  = key("excal_cooldown");
    public static final NamespacedKey KEY_MJOLNIR_COOLDOWN= key("mjolnir_cooldown");

    private static NamespacedKey key(String value) {
        return new NamespacedKey(NAMESPACE, value);
    }

    public ItemStack getWeapon(String id) {
        return switch (id) {
            case DARKIN_BLADE -> makeDarkinBlade();
            case FROSTMOURNE  -> makeFrostmourne();
            case EXCALIBUR    -> makeExcalibur();
            case MJOLNIR      -> makeMjolnir();
            default -> null;
        };
    }

    // -------------------------------------------------------------------------
    // DARKIN BLADE
    // -------------------------------------------------------------------------
    public ItemStack makeDarkinBlade() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Darkin Blade", NamedTextColor.DARK_RED)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(List.of(
            Component.text("A blade possessed by an ancient Darkin.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Passive: ", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Applies Wither II on hit.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.text("Active [Right-Click]: ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Leap upward then crash down with AOE damage.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.text("  Cooldown: 8 seconds", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("legendary:darkin_blade", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, true)
        ));

        applyWeaponId(meta, DARKIN_BLADE);
        applyNetheritieDamage(meta, "darkin_blade_dmg");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // FROSTMOURNE
    // -------------------------------------------------------------------------
    public ItemStack makeFrostmourne() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Frostmourne", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(List.of(
            Component.text("A runeblade that hungers for souls.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Passive: ", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Harvests souls on kill. At 5+ souls,", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.text("  summons a Frost Giant or Rime Spectre.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Passive: ", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Applies Slowness II (Permafrost) on hit.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.empty(),
            Component.text("Souls: 0", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("legendary:frostmourne", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, true)
        ));

        applyWeaponId(meta, FROSTMOURNE);
        meta.getPersistentDataContainer().set(KEY_FROST_SOULS, PersistentDataType.INTEGER, 0);
        applyNetheritieDamage(meta, "frostmourne_dmg");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // EXCALIBUR
    // -------------------------------------------------------------------------
    public ItemStack makeExcalibur() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Excalibur", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(List.of(
            Component.text("The legendary sword of kings.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Passive I: ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Absorbs 30% of incoming damage as durability loss.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.text("Passive II: ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("30% chance on fatal hit: survive with 5 hearts.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.text("Passive III: ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Converts negative effects to positive (5 min).", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.text("  Weakness→Strength  Slowness→Speed", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("  Poison/Wither→Regen  Darkness→Str III+Spd II", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Active [Right-Click]: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Sonic Boom — 6 true damage to target.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.text("  Cooldown: 30 seconds", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("legendary:excalibur", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, true)
        ));

        applyWeaponId(meta, EXCALIBUR);
        // Excalibur has high durability so passive I damage absorption works for a long time
        item.setType(Material.NETHERITE_SWORD);
        applyNetheritieDamage(meta, "excalibur_dmg");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        // NOT unbreakable — durability is used by passive I

        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // MJOLNIR
    // -------------------------------------------------------------------------
    public ItemStack makeMjolnir() {
        ItemStack item = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Mjölnir", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));

        meta.lore(List.of(
            Component.text("The hammer of Thor, god of thunder.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Passive: ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Deals bonus lightning damage when raining.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.text("Active [Right-Click]: ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("Throw Mjölnir — returns to your hand.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
            Component.text("  Hits all mobs in path. Strikes lightning on impact.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("  Cooldown: 5 seconds", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("legendary:mjolnir", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, true)
        ));

        applyWeaponId(meta, MJOLNIR);
        // Axe stats
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE,
            new AttributeModifier(new NamespacedKey(NAMESPACE, "mjolnir_dmg"),
                8.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED,
            new AttributeModifier(new NamespacedKey(NAMESPACE, "mjolnir_spd"),
                -3.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private void applyWeaponId(ItemMeta meta, String weaponId) {
        meta.getPersistentDataContainer().set(KEY_WEAPON_ID, PersistentDataType.STRING, weaponId);
    }

    /** Netherite sword base: +7 attack damage modifier (= 8 total with base), -2.4 speed */
    private void applyNetheritieDamage(ItemMeta meta, String modifierName) {
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE,
            new AttributeModifier(new NamespacedKey(NAMESPACE, modifierName),
                7.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED,
            new AttributeModifier(new NamespacedKey(NAMESPACE, modifierName + "_spd"),
                -2.4, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
    }

    /** Utility: check if an ItemStack is a specific legendary weapon */
    public static boolean isWeapon(ItemStack item, String weaponId) {
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return weaponId.equals(pdc.get(KEY_WEAPON_ID, PersistentDataType.STRING));
    }

    public static String getWeaponId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(KEY_WEAPON_ID, PersistentDataType.STRING);
    }
}
