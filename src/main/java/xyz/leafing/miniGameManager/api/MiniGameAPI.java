
package xyz.leafing.miniGameManager.api;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;

/**
 * MiniGameManager 插件提供的核心服务接口。
 * 其他小游戏插件通过此接口与 MiniGameManager 交互，以统一管理玩家的游戏状态和数据。
 */
public interface MiniGameAPI {

    /**
     * 尝试将一个玩家标记为“游戏中”状态。
     * 如果玩家已在另一游戏中，此操作会失败。
     *
     * @param player       要标记的玩家
     * @param sourcePlugin 调用此方法的小游戏插件实例 (用于追踪归属)
     * @return 如果成功标记，返回 true；如果玩家已在游戏中，返回 false。
     */
    boolean enterGame(Player player, JavaPlugin sourcePlugin);

    /**
     * 将一个玩家从“游戏中”状态释放。
     * 只有将玩家标记为“游戏中”的同一个插件才能释放该玩家，以防止冲突。
     *
     * @param player       要释放的玩家
     * @param sourcePlugin 调用此方法的小游戏插件实例
     * @return 如果成功释放，返回 true；如果插件不匹配或玩家不在游戏中，返回 false。
     */
    boolean leaveGame(Player player, JavaPlugin sourcePlugin);

    /**
     * 检查一个玩家当前是否在任何受此管理器追踪的游戏中。
     *
     * @param player 玩家
     * @return 如果在游戏中，返回 true。
     */
    boolean isInGame(Player player);

    /**
     * 获取当前控制该玩家的小游戏插件。
     *
     * @param player 玩家
     * @return 如果玩家在游戏中，返回一个包含该插件实例的 Optional；否则返回 Optional.empty()。
     */
    Optional<JavaPlugin> getOwningPlugin(Player player);

    /**
     * 保存玩家的完整状态（背包、血量、经验、药水效果、游戏模式等）。
     * 这通常在玩家确认加入游戏后立即调用。
     *
     * @param player 要保存数据的玩家
     * @return 如果保存成功，返回 true。
     */
    boolean savePlayerData(Player player);

    /**
     * 从备份文件中恢复玩家之前保存的状态。
     * 成功恢复后，备份数据将被自动删除。
     * 这通常在玩家离开游戏、被淘汰或游戏结束后调用。
     *
     * @param player 要恢复数据的玩家
     * @return 如果恢复成功，返回 true。
     */
    boolean restorePlayerData(Player player);

    /**
     * 检查一个玩家是否有待恢复的数据。
     * 这对于处理服务器崩溃后玩家重新登录的情况非常有用。
     *
     * @param uuid 玩家的UUID
     * @return 如果存在备份数据，返回 true。
     */
    boolean hasPendingData(UUID uuid);

    /**
     * 清空玩家的数据和状态，为进入游戏做准备。
     * (设置满血、满饥饿度、生存模式、清空背包和药水效果等)
     *
     * @param player 要被清空的玩家
     */
    void clearPlayerData(Player player);
}