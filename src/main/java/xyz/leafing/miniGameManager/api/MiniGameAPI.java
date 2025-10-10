
package xyz.leafing.miniGameManager.api;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MiniGameAPI {

    // --- 游戏状态管理 ---
    boolean enterGame(Player player, JavaPlugin sourcePlugin);

    /**
     * 将一个玩家从“游戏中”状态释放，无论玩家是否在线。
     * 只有将玩家标记为“游戏中”的同一个插件才能释放该玩家，以防止冲突。
     *
     * @param playerUUID   要释放的玩家的UUID
     * @param sourcePlugin 调用此方法的小游戏插件实例
     * @return 如果成功释放，返回 true；如果插件不匹配或玩家不在游戏中，返回 false。
     */
    boolean leaveGame(UUID playerUUID, JavaPlugin sourcePlugin);

    boolean isInGame(Player player);
    Optional<JavaPlugin> getOwningPlugin(Player player);

    // --- 玩家数据备份与恢复 ---
    boolean savePlayerData(Player player);
    CompletableFuture<Boolean> restorePlayerData(UUID playerUUID);
    boolean hasPendingData(UUID uuid);

    // --- 玩家数据修改 ---
    CompletableFuture<Boolean> setGameMode(UUID playerUUID, GameMode gameMode);
    CompletableFuture<Boolean> teleport(UUID playerUUID, Location location);
    void clearPlayerData(Player player);
    CompletableFuture<Boolean> clearFullPlayerData(UUID playerUUID);
}