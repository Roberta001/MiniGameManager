package xyz.leafing.miniGameManager.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import xyz.leafing.miniGameManager.MiniGameManager;
import xyz.leafing.miniGameManager.utils.OfflinePlayerManager;

public class PlayerConnectionListener implements Listener {

    private final MiniGameManager plugin;
    private final OfflinePlayerManager offlineManager;

    public PlayerConnectionListener(MiniGameManager plugin, OfflinePlayerManager offlineManager) {
        this.plugin = plugin;
        this.offlineManager = offlineManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        // 在玩家登录的最早阶段检查锁
        if (offlineManager.isLocked(event.getUniqueId())) {
            plugin.getLogger().warning("已阻止玩家 " + event.getName() + " 登录，因为其数据正在被后台操作。");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("您的玩家数据正在被后台系统处理。\n", NamedTextColor.YELLOW)
                            .append(Component.text("这是一个罕见情况，请在几秒钟后重试。", NamedTextColor.WHITE))
            );
        }
    }

    // 你原有的 onPlayerJoin 和 onPlayerQuit 逻辑可以保留，用于处理在线玩家的意外情况
    // 这里为了简洁，暂时省略，你可以将它们合并进来。
}
