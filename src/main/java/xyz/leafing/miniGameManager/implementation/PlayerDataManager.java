package xyz.leafing.miniGameManager.implementation;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import xyz.leafing.miniGameManager.MiniGameManager;
import xyz.leafing.miniGameManager.utils.PlayerDataSnapshot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerDataManager {

    private final MiniGameManager plugin;
    private final File dataFolder;

    public PlayerDataManager(MiniGameManager plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFolder.mkdirs();
        }
    }

    public boolean saveData(Player player) {
        File playerFile = getPlayerFile(player.getUniqueId());
        FileConfiguration data = YamlConfiguration.loadConfiguration(playerFile);

        data.set("inventory.main", serializeItemStackArray(player.getInventory().getContents()));
        data.set("inventory.armor", serializeItemStackArray(player.getInventory().getArmorContents()));
        data.set("inventory.extra", serializeItemStackArray(player.getInventory().getExtraContents()));
        data.set("enderchest", serializeItemStackArray(player.getEnderChest().getContents()));

        data.set("stats.health", player.getHealth());
        data.set("stats.max_health", Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue());
        data.set("stats.food", player.getFoodLevel());
        data.set("stats.saturation", player.getSaturation());

        data.set("exp.level", player.getLevel());
        data.set("exp.progress", player.getExp());
        data.set("exp.total", player.getTotalExperience());

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
        Optional<PlayerDataSnapshot> snapshotOpt = loadSnapshot(player.getUniqueId());
        if (snapshotOpt.isEmpty()) {
            plugin.getLogger().warning("找不到玩家 " + player.getName() + " 的数据快照，无法恢复。");
            return false;
        }
        PlayerDataSnapshot snapshot = snapshotOpt.get();

        try {
            clearData(player);

            player.getInventory().setContents(snapshot.inventory());
            player.getInventory().setArmorContents(snapshot.armor());
            player.getInventory().setExtraContents(snapshot.extra());
            player.getEnderChest().setContents(snapshot.enderChest());

            // **--- FIX START ---**
            // 1. 恢复基础最大生命值
            AttributeInstance maxHealthAttribute = Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH));
            maxHealthAttribute.setBaseValue(snapshot.maxHealth());

            // 2. 恢复药水效果 (这会应用 health_boost 等修正值)
            for (PotionEffect effect : snapshot.potionEffects()) {
                player.addPotionEffect(effect);
            }

            // 3. 在所有属性和效果都恢复后，最后设置当前生命值
            //    使用 Math.min 确保不会超过当前有效的最大生命值
            player.setHealth(Math.min(snapshot.health(), maxHealthAttribute.getValue()));
            // **--- FIX END ---**

            player.setFoodLevel(snapshot.foodLevel());
            player.setSaturation(snapshot.saturation());

            player.setTotalExperience(0);
            player.setLevel(0);
            player.setExp(0);
            player.giveExp(snapshot.totalExperience());

            player.setGameMode(snapshot.gameMode());
            player.setAllowFlight(snapshot.allowFlight());
            player.setFlying(snapshot.flying());

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "恢复玩家 " + player.getName() + " 的数据时发生严重错误！数据文件将暂时保留以供排查。", e);
            return false;
        }

        return deleteDataFile(player.getUniqueId());
    }

    public void clearData(Player player) {
        clearFullData(player);
        Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(20);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    public void clearFullData(Player player) {
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.getInventory().setArmorContents(null);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0f);
    }

    public boolean hasData(UUID uuid) {
        return getPlayerFile(uuid).exists();
    }

    public boolean deleteDataFile(UUID uuid) {
        File playerFile = getPlayerFile(uuid);
        if (playerFile.exists()) {
            if (!playerFile.delete()) {
                plugin.getLogger().warning("无法删除玩家 " + uuid + " 的数据文件: " + playerFile.getPath());
                return false;
            }
        }
        return true;
    }

    public Optional<PlayerDataSnapshot> loadSnapshot(UUID uuid) {
        File playerFile = getPlayerFile(uuid);
        if (!playerFile.exists()) {
            return Optional.empty();
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(playerFile);
        try {
            ItemStack[] inventory = deserializeItemStackArray(data.getStringList("inventory.main"));
            ItemStack[] armor = deserializeItemStackArray(data.getStringList("inventory.armor"));
            ItemStack[] extra = deserializeItemStackArray(data.getStringList("inventory.extra"));
            ItemStack[] enderChest = deserializeItemStackArray(data.getStringList("enderchest"));

            double health = data.getDouble("stats.health", 20.0);
            double maxHealth = data.getDouble("stats.max_health", 20.0);
            int foodLevel = data.getInt("stats.food", 20);
            float saturation = (float) data.getDouble("stats.saturation", 5.0);
            int level = data.getInt("exp.level", 0);
            float exp = (float) data.getDouble("exp.progress", 0.0);
            int totalExperience = data.getInt("exp.total", 0);

            GameMode gameMode = GameMode.valueOf(data.getString("gamemode", "SURVIVAL"));

            @SuppressWarnings("unchecked")
            Collection<PotionEffect> potionEffects = (Collection<PotionEffect>) data.getList("potioneffects", List.of());

            boolean flying = data.getBoolean("flying", false);
            boolean allowFlight = data.getBoolean("allow-flight", false);

            return Optional.of(new PlayerDataSnapshot(inventory, armor, extra, enderChest, health, maxHealth, foodLevel, saturation, level, exp, totalExperience, gameMode, potionEffects, flying, allowFlight));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "加载玩家 " + uuid + " 的数据快照失败！", e);
            return Optional.empty();
        }
    }

    private List<String> serializeItemStackArray(ItemStack[] items) {
        List<String> nbtStrings = new ArrayList<>();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                nbtStrings.add("");
            } else {
                NBTCompound fullItemNbt = NBTItem.convertItemtoNBT(item);
                nbtStrings.add(fullItemNbt.toString());
            }
        }
        return nbtStrings;
    }

    private ItemStack[] deserializeItemStackArray(List<String> nbtStrings) {
        ItemStack[] items = new ItemStack[nbtStrings.size()];
        for (int i = 0; i < nbtStrings.size(); i++) {
            String nbtString = nbtStrings.get(i);
            if (nbtString == null || nbtString.isEmpty()) {
                items[i] = null;
            } else {
                try {
                    items[i] = NBTItem.convertNBTtoItem(new NBTContainer(nbtString));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "无法从NBT字符串反序列化物品: " + nbtString, e);
                    items[i] = null;
                }
            }
        }
        return items;
    }

    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }
}