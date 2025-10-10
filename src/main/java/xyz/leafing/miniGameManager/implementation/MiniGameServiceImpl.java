package xyz.leafing.miniGameManager.implementation;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.leafing.miniGameManager.MiniGameManager;
import xyz.leafing.miniGameManager.api.MiniGameAPI;
import xyz.leafing.miniGameManager.utils.OfflinePlayerManager;
import xyz.leafing.miniGameManager.utils.PlayerDataSnapshot;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MiniGameServiceImpl implements MiniGameAPI {

    private final MiniGameManager plugin;
    private final PlayerDataManager dataManager;
    private final OfflinePlayerManager offlineManager;
    private final Map<UUID, JavaPlugin> playersInGame = new ConcurrentHashMap<>();

    public MiniGameServiceImpl(MiniGameManager plugin, OfflinePlayerManager offlineManager) {
        this.plugin = plugin;
        this.dataManager = new PlayerDataManager(plugin);
        this.offlineManager = offlineManager;
    }

    @Override
    public boolean enterGame(Player player, JavaPlugin sourcePlugin) {
        return playersInGame.putIfAbsent(player.getUniqueId(), sourcePlugin) == null;
    }

    @Override
    public boolean leaveGame(UUID playerUUID, JavaPlugin sourcePlugin) {
        // 直接使用 UUID 进行判断和移除
        if (sourcePlugin.equals(playersInGame.get(playerUUID))) {
            playersInGame.remove(playerUUID);
            return true;
        }
        return false;
    }

    @Override
    public boolean isInGame(Player player) {
        return playersInGame.containsKey(player.getUniqueId());
    }

    @Override
    public Optional<JavaPlugin> getOwningPlugin(Player player) {
        return Optional.ofNullable(playersInGame.get(player.getUniqueId()));
    }

    @Override
    public boolean savePlayerData(Player player) {
        return dataManager.saveData(player);
    }

    @Override
    public boolean hasPendingData(UUID uuid) {
        return dataManager.hasData(uuid);
    }

    @Override
    public void clearPlayerData(Player player) {
        dataManager.clearData(player);
    }

    @Override
    public CompletableFuture<Boolean> restorePlayerData(UUID playerUUID) {
        if (!hasPendingData(playerUUID)) {
            return CompletableFuture.completedFuture(false);
        }

        Player onlinePlayer = Bukkit.getPlayer(playerUUID);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            return CompletableFuture.supplyAsync(() -> dataManager.restoreData(onlinePlayer), runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
        } else {
            return CompletableFuture.supplyAsync(() -> {
                Optional<PlayerDataSnapshot> snapshotOpt = dataManager.loadSnapshot(playerUUID);
                if (snapshotOpt.isPresent()) {
                    boolean success = offlineManager.restorePlayerDataNBT(playerUUID, snapshotOpt.get());
                    if (success) {
                        dataManager.deleteDataFile(playerUUID);
                    }
                    return success;
                }
                return false;
            });
        }
    }

    @Override
    public CompletableFuture<Boolean> setGameMode(UUID playerUUID, GameMode gameMode) {
        Player onlinePlayer = Bukkit.getPlayer(playerUUID);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> onlinePlayer.setGameMode(gameMode));
            return CompletableFuture.completedFuture(true);
        } else {
            return CompletableFuture.supplyAsync(() -> offlineManager.setGameModeNBT(playerUUID, gameMode));
        }
    }

    @Override
    public CompletableFuture<Boolean> teleport(UUID playerUUID, Location location) {
        Player onlinePlayer = Bukkit.getPlayer(playerUUID);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            return CompletableFuture.supplyAsync(() -> onlinePlayer.teleport(location), runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
        } else {
            return CompletableFuture.supplyAsync(() -> offlineManager.teleportNBT(playerUUID, location));
        }
    }

    @Override
    public CompletableFuture<Boolean> clearFullPlayerData(UUID playerUUID) {
        Player onlinePlayer = Bukkit.getPlayer(playerUUID);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> dataManager.clearFullData(onlinePlayer));
            return CompletableFuture.completedFuture(true);
        } else {
            return CompletableFuture.supplyAsync(() -> offlineManager.clearFullDataNBT(playerUUID));
        }
    }
}