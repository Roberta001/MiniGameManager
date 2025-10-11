package xyz.leafing.miniGameManager.utils;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTFile;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import xyz.leafing.miniGameManager.MiniGameManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class OfflinePlayerManager {

    private final MiniGameManager plugin;
    private final Set<UUID> lockedPlayers = ConcurrentHashMap.newKeySet();

    public OfflinePlayerManager(MiniGameManager plugin) {
        this.plugin = plugin;
    }

    public boolean isLocked(UUID uuid) {
        return lockedPlayers.contains(uuid);
    }

    public boolean hasActiveLocks() {
        return !lockedPlayers.isEmpty();
    }

    public Set<String> getLockedPlayerUUIDs() {
        return lockedPlayers.stream().map(UUID::toString).collect(Collectors.toSet());
    }

    public boolean restorePlayerDataNBT(UUID uuid, PlayerDataSnapshot snapshot) {
        return performSafeOperation(uuid, nbt -> {
            nbt.getCompoundList("Inventory").clear();
            nbt.getCompoundList("EnderItems").clear();
            nbt.getCompoundList("ActiveEffects").clear();

            NBTCompoundList inventory = nbt.getCompoundList("Inventory");
            for (int i = 0; i < snapshot.inventory().length; i++) {
                ItemStack item = snapshot.inventory()[i];
                if (item != null && !item.getType().isAir()) {
                    NBTCompound fullItemNbt = NBTItem.convertItemtoNBT(item);
                    fullItemNbt.setByte("Slot", (byte) i);
                    inventory.add(fullItemNbt);
                }
            }
            for (int i = 0; i < snapshot.armor().length; i++) {
                ItemStack item = snapshot.armor()[i];
                if (item != null && !item.getType().isAir()) {
                    NBTCompound fullItemNbt = NBTItem.convertItemtoNBT(item);
                    fullItemNbt.setByte("Slot", (byte) (100 + i));
                    inventory.add(fullItemNbt);
                }
            }
            if (snapshot.extra().length > 0 && snapshot.extra()[0] != null) {
                ItemStack item = snapshot.extra()[0];
                if (!item.getType().isAir()) {
                    NBTCompound fullItemNbt = NBTItem.convertItemtoNBT(item);
                    fullItemNbt.setByte("Slot", (byte) -106);
                    inventory.add(fullItemNbt);
                }
            }

            NBTCompoundList enderChest = nbt.getCompoundList("EnderItems");
            for (int i = 0; i < snapshot.enderChest().length; i++) {
                ItemStack item = snapshot.enderChest()[i];
                if (item != null && !item.getType().isAir()) {
                    NBTCompound fullItemNbt = NBTItem.convertItemtoNBT(item);
                    fullItemNbt.setByte("Slot", (byte) i);
                    enderChest.add(fullItemNbt);
                }
            }

            nbt.setFloat("Health", (float) snapshot.health());
            nbt.setInteger("foodLevel", snapshot.foodLevel());
            nbt.setFloat("foodSaturationLevel", snapshot.saturation());

            NBTCompoundList attributes = nbt.getCompoundList("Attributes");
            boolean maxHealthAttributeFound = false;
            // **--- FIX START ---**
            // 遍历时使用 ReadWriteNBT，然后检查并转换为 NBTCompound
            for (ReadWriteNBT attributeNBT : attributes) {
                if (attributeNBT instanceof NBTCompound attribute) {
                    if ("minecraft:generic.max_health".equals(attribute.getString("Name"))) {
                        attribute.setDouble("Base", snapshot.maxHealth());
                        maxHealthAttributeFound = true;
                        break;
                    }
                }
            }
            // **--- FIX END ---**
            if (!maxHealthAttributeFound) {
                NBTCompound maxHealthAttribute = attributes.addCompound();
                maxHealthAttribute.setString("Name", "minecraft:generic.max_health");
                maxHealthAttribute.setDouble("Base", snapshot.maxHealth());
            }

            nbt.setInteger("XpLevel", snapshot.level());
            nbt.setFloat("XpP", snapshot.exp());
            nbt.setInteger("XpTotal", snapshot.totalExperience());

            nbt.setInteger("playerGameType", snapshot.gameMode().getValue());

            NBTCompoundList activeEffects = nbt.getCompoundList("ActiveEffects");
            for (PotionEffect effect : snapshot.potionEffects()) {
                NBTCompound effectNBT = activeEffects.addCompound();
                // noinspection deprecation
                effectNBT.setByte("Id", (byte) effect.getType().getId());
                effectNBT.setByte("Amplifier", (byte) effect.getAmplifier());
                effectNBT.setInteger("Duration", effect.getDuration());
                effectNBT.setBoolean("Ambient", effect.isAmbient());
                effectNBT.setBoolean("ShowParticles", effect.hasParticles());
                effectNBT.setBoolean("ShowIcon", effect.hasIcon());
            }

            NBTCompound abilities = nbt.getCompound("abilities");
            if (abilities == null) abilities = nbt.addCompound("abilities");
            abilities.setBoolean("flying", snapshot.flying());
            abilities.setBoolean("mayfly", snapshot.allowFlight());
        });
    }

    public boolean clearFullDataNBT(UUID uuid) {
        return performSafeOperation(uuid, nbt -> {
            nbt.getCompoundList("Inventory").clear();
            nbt.getCompoundList("EnderItems").clear();
            nbt.getCompoundList("ActiveEffects").clear();

            nbt.setInteger("XpLevel", 0);
            nbt.setFloat("XpP", 0f);
            nbt.setInteger("XpTotal", 0);
        });
    }

    public boolean setGameModeNBT(UUID uuid, GameMode gameMode) {
        return performSafeOperation(uuid, nbt -> {
            int gameModeValue = gameMode.getValue();

            // **--- FIX START ---**
            // 1. 设置原版 Minecraft 的标签
            nbt.setInteger("playerGameType", gameModeValue);

            // 2. 设置 Bukkit/Spigot/Paper 的标签
            NBTCompound bukkitCompound = nbt.getOrCreateCompound("bukkit");
            bukkitCompound.setInteger("playerGameMode", gameModeValue);
            // **--- FIX END ---**
        });
    }

    public boolean teleportNBT(UUID uuid, Location location) {
        return performSafeOperation(uuid, nbt -> {
            // 1. 更新坐标和朝向
            var pos = nbt.getDoubleList("Pos");
            pos.clear();
            pos.add(location.getX());
            pos.add(location.getY());
            pos.add(location.getZ());

            var rotation = nbt.getFloatList("Rotation");
            rotation.clear();
            rotation.add(location.getYaw());
            rotation.add(location.getPitch());

            // 2. 更新维度名称
            nbt.setString("Dimension", location.getWorld().getKey().toString());

            // 3. **关键修复**: 更新维度的UUID
            UUID worldUUID = location.getWorld().getUID();
            nbt.setLong("WorldUUIDMost", worldUUID.getMostSignificantBits());
            nbt.setLong("WorldUUIDLeast", worldUUID.getLeastSignificantBits());
        });
    }

    private boolean performSafeOperation(UUID uuid, Consumer<NBTFile> nbtOperation) {
        if (isLocked(uuid)) {
            plugin.getLogger().warning("尝试操作一个已被锁定的玩家数据: " + uuid);
            return false;
        }

        File playerFile = getPlayerDatFile(uuid);
        if (playerFile == null || !playerFile.exists()) {
            plugin.getLogger().warning("找不到离线玩家 " + uuid + " 的数据文件。");
            return false;
        }

        lockedPlayers.add(uuid);
        File backupFile = new File(playerFile.getPath() + ".mgm_bak");

        try {
            Files.copy(playerFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            NBTFile nbt = new NBTFile(playerFile);
            nbtOperation.accept(nbt);
            nbt.save();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "操作离线玩家 " + uuid + " 数据时发生严重错误！将从备份恢复。", e);
            try {
                if (backupFile.exists()) {
                    Files.move(backupFile.toPath(), playerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("成功从备份恢复玩家 " + uuid + " 的数据。");
                }
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "从备份恢复玩家 " + uuid + " 数据失败！数据可能已损坏！", ex);
            }
            return false;
        } finally {
            if (backupFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                backupFile.delete();
            }
            lockedPlayers.remove(uuid);
        }
    }

    private File getPlayerDatFile(UUID uuid) {
        File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            return null;
        }
        return new File(playerDataFolder, uuid + ".dat");
    }
}