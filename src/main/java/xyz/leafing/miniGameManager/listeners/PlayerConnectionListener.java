package xyz.leafing.miniGameManager.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.leafing.miniGameManager.MiniGameManager;
import xyz.leafing.miniGameManager.api.MiniGameAPI;

public class PlayerConnectionListener implements Listener {

    private final MiniGameAPI api;
    private final MiniGameManager plugin;

    public PlayerConnectionListener(MiniGameAPI api, MiniGameManager plugin) {
        this.api = api;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 检查玩家是否有因服务器崩溃等意外情况而未被恢复的数据
        if (api.hasPendingData(player.getUniqueId())) {
            plugin.getLogger().info("检测到玩家 " + player.getName() + " 有待恢复的数据，正在为其恢复...");
            if (api.restorePlayerData(player)) {
                player.sendMessage(ChatColor.GREEN + "[游戏管理器] 检测到您有未恢复的游戏数据，已自动为您恢复。");
                plugin.getLogger().info("成功恢复玩家 " + player.getName() + " 的数据。");
            } else {
                player.sendMessage(ChatColor.RED + "[游戏管理器] 尝试恢复您的数据失败！请联系管理员！");
                plugin.getLogger().severe("为玩家 " + player.getName() + " 恢复数据失败！");
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // 如果玩家在游戏中掉线（"拔线"、客户端崩溃等）
        if (api.isInGame(player)) {
            JavaPlugin owner = api.getOwningPlugin(player).orElse(null);
            if (owner != null) {
                // 自动恢复数据并释放他，防止物品丢失和状态卡死
                api.restorePlayerData(player);
                api.leaveGame(player, owner);
                plugin.getLogger().info("玩家 " + player.getName() + " 在 " + owner.getName() + " 游戏中掉线，已自动恢复其数据并释放状态。");
            }
        }
    }
}