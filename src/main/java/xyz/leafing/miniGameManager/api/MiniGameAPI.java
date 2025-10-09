package xyz.leafing.miniGameManager.api;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MiniGameManager 插件提供的核心服务接口。
 * 其他小游戏插件通过此接口与 MiniGameManager 交互，以统一管理玩家的游戏状态和数据。
 */
public interface MiniGameAPI {

    // --- 游戏状态管理 ---
    boolean enterGame(Player player, JavaPlugin sourcePlugin);
    boolean leaveGame(Player player, JavaPlugin sourcePlugin);
    boolean isInGame(Player player);
    Optional<JavaPlugin> getOwningPlugin(Player player);

    // --- 玩家数据备份与恢复 ---
    boolean savePlayerData(Player player);
    CompletableFuture<Boolean> restorePlayerData(UUID playerUUID);
    boolean hasPendingData(UUID uuid);

    // --- 玩家数据修改 ---
    CompletableFuture<Boolean> setGameMode(UUID playerUUID, GameMode gameMode);
    CompletableFuture<Boolean> teleport(UUID playerUUID, Location location);

    /**
     * 清空在线玩家的数据和状态，为进入游戏做准备。
     * (设置满血、满饥饿度、生存模式、清空背包和药水效果等)
     * @param player 要被清空的玩家
     */
    void clearPlayerData(Player player);

    /**
     * 全面清空一个玩家的核心数据（背包、末影箱、经验、药水效果）。
     * 此方法是异步的，因为它可能涉及离线文件I/O。
     * 它会自动检测玩家是在线还是离线，并使用相应的方法。
     *
     * @param playerUUID 要清空数据的玩家UUID
     * @return 一个 CompletableFuture，在操作完成时以布尔值表示成功或失败。
     */
    CompletableFuture<Boolean> clearFullPlayerData(UUID playerUUID);
}