package xyz.leafing.miniGameManager;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.leafing.miniGameManager.api.MiniGameAPI;
import xyz.leafing.miniGameManager.implementation.MiniGameServiceImpl;
import xyz.leafing.miniGameManager.listeners.PlayerConnectionListener;

public final class MiniGameManager extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("MiniGameManager 正在启动...");

        // 1. 创建 API 的实现实例
        MiniGameAPI api = new MiniGameServiceImpl(this);

        // 2. 将 API 注册到 Bukkit 的服务管理器中，使其可被其他插件发现和使用
        getServer().getServicesManager().register(
                MiniGameAPI.class,      // 注册的接口类型
                api,                    // 具体的实现实例
                this,                   // 提供此服务的插件
                ServicePriority.Normal  // 服务的优先级
        );

        // 3. 注册我们的安全保障监听器
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(api, this), this);

        getLogger().info("MiniGameManager 服务已成功加载并向服务器注册！");
    }

    @Override
    public void onDisable() {
        // 插件卸载时，取消注册服务（虽然服务器关闭时会自动处理）
        getServer().getServicesManager().unregisterAll(this);
        getLogger().info("MiniGameManager 插件已卸载。");
    }
}