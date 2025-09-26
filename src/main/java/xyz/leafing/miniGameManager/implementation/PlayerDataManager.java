package xyz.leafing.miniGameManager.implementation;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import xyz.leafing.miniGameManager.MiniGameManager;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataManager {

    private final MiniGameManager plugin;
    private final File dataFolder;

    public PlayerDataManager(MiniGameManager plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public boolean saveData(Player player) {
        File playerFile = getPlayerFile(player.getUniqueId());
        FileConfiguration data = YamlConfiguration.loadConfiguration(playerFile);

        data.set("inventory.main", player.getInventory().getContents());
        data.set("inventory.armor", player.getInventory().getArmorContents());
        data.set("inventory.extra", player.getInventory().getExtraContents()); // For 1.9+ off-hand
        data.set("enderchest", player.getEnderChest().getContents());
        data.set("stats.health", player.getHealth());
        data.set("stats.food", player.getFoodLevel());
        data.set("stats.saturation", player.getSaturation());
        data.set("exp.level", player.getLevel());
        data.set("exp.progress", player.getExp());
        data.set("gamemode", player.getGameMode().name());
        data.set("potioneffects", player.getActivePotionEffects());
        data.set("flying", player.isFlying());
        data.set("allow-flight", player.getAllowFlight());

        try {
            data.save(playerFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "无法保存玩家 " + player.getName() + " 的数据！", e);
            return false;
        }
    }

    public boolean restoreData(Player player) {
        File playerFile = getPlayerFile(player.getUniqueId());
        if (!playerFile.exists()) {
            plugin.getLogger().warning("找不到玩家 " + player.getName() + " 的数据文件，无法恢复。");
            return false;
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(playerFile);

        try {
            clearData(player); // Start from a clean slate before restoring

            player.getInventory().setContents(loadItemStackArray(data, "inventory.main"));
            player.getInventory().setArmorContents(loadItemStackArray(data, "inventory.armor"));
            player.getInventory().setExtraContents(loadItemStackArray(data, "inventory.extra"));
            player.getEnderChest().setContents(loadItemStackArray(data, "enderchest"));

            player.setHealth(data.getDouble("stats.health", 20.0));
            player.setFoodLevel(data.getInt("stats.food", 20));
            player.setSaturation((float) data.getDouble("stats.saturation", 5.0));
            player.setLevel(data.getInt("exp.level", 0));
            player.setExp((float) data.getDouble("exp.progress", 0.0));
            player.setGameMode(GameMode.valueOf(data.getString("gamemode", "SURVIVAL")));

            Collection<PotionEffect> effects = (Collection<PotionEffect>) data.get("potioneffects");
            if (effects != null) {
                player.addPotionEffects(effects);
            }

            player.setAllowFlight(data.getBoolean("allow-flight", false));
            player.setFlying(data.getBoolean("flying", false));

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "恢复玩家 " + player.getName() + " 的数据时发生严重错误！数据文件将暂时保留以供排查。", e);
            return false; // Do not delete the file on failure
        }

        if (!playerFile.delete()) {
            plugin.getLogger().warning("无法删除玩家 " + player.getName() + " 的数据文件: " + playerFile.getPath());
        }
        return true;
    }

    public boolean hasData(UUID uuid) {
        return getPlayerFile(uuid).exists();
    }

    public void clearData(Player player) {
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.getInventory().setArmorContents(null);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setLevel(0);
        player.setExp(0f);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    private ItemStack[] loadItemStackArray(FileConfiguration data, String path) {
        return ((List<ItemStack>) data.get(path, new java.util.ArrayList<ItemStack>())).toArray(new ItemStack[0]);
    }
}