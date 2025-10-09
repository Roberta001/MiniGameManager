package xyz.leafing.miniGameManager.utils;

import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import java.util.Collection;

public record PlayerDataSnapshot(
        ItemStack[] inventory,
        ItemStack[] armor,
        ItemStack[] extra,
        ItemStack[] enderChest,
        double health,
        double maxHealth, // 新增
        int foodLevel,
        float saturation,
        int level,
        float exp,
        int totalExperience,
        GameMode gameMode,
        Collection<PotionEffect> potionEffects,
        boolean flying,
        boolean allowFlight
) {}