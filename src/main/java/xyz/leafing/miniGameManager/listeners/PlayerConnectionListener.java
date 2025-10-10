package xyz.leafing.miniGameManager.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.leafing.miniGameManager.MiniGameManager;
import xyz.leafing.miniGameManager.api.MiniGameAPI;
import xyz.leafing.miniGameManager.utils.OfflinePlayerManager;

public class PlayerConnectionListener implements Listener {

    private final MiniGameManager plugin;
    private final MiniGameAPI api;
    private final OfflinePlayerManager offlineManager;

    public PlayerConnectionListener(MiniGameManager plugin, MiniGameAPI api, OfflinePlayerManager offlineManager) {
        this.plugin = plugin;
        this.api = api;
        this.offlineManager = offlineManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (offlineManager.isLocked(event.getUniqueId())) {
            plugin.getLogger().warning("已阻止玩家 " + event.getName() + " 登录，因为其数据正在被后台操作。");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("您的玩家数据正在被后台系统处理。\n", NamedTextColor.YELLOW)
                            .append(Component.text("这是一个罕见情况，请在几秒钟后重试。", NamedTextColor.WHITE))
            );
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (api.hasPendingData(player.getUniqueId())) {
            plugin.getLogger().info("检测到玩家 " + player.getName() + " 有待恢复的数据，正在为其恢复...");
            api.restorePlayerData(player.getUniqueId()).thenAccept(success -> {
                if (success) {
                    player.sendMessage(Component.text("[游戏管理器] 检测到您有未恢复的游戏数据，已自动为您恢复。", NamedTextColor.GREEN));
                    plugin.getLogger().info("成功恢复玩家 " + player.getName() + " 的数据。");
                } else {
                    player.sendMessage(Component.text("[游戏管理器] 尝试恢复您的数据失败！请联系管理员！", NamedTextColor.RED));
                    plugin.getLogger().severe("为玩家 " + player.getName() + " 恢复数据失败！");
                }
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // 检查玩家是否在游戏中掉线
        if (api.isInGame(player)) {
            JavaPlugin owner = api.getOwningPlugin(player).orElse(null);
            if (owner != null) {
                // 新逻辑：不再自动恢复和移除。只记录日志，将控制权交给下游插件。
                plugin.getLogger().info("玩家 " + player.getName() + " 在 " + owner.getName() + " 游戏中掉线。现在由 " + owner.getName() + " 插件负责处理该玩家的状态。");
            }
        }
    }
}