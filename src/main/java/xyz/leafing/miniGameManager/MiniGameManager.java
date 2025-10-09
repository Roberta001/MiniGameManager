package xyz.leafing.miniGameManager;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.leafing.miniGameManager.api.MiniGameAPI;
import xyz.leafing.miniGameManager.commands.MGMCommand;
import xyz.leafing.miniGameManager.implementation.MiniGameServiceImpl;
import xyz.leafing.miniGameManager.listeners.PlayerConnectionListener;
import xyz.leafing.miniGameManager.utils.OfflinePlayerManager;

import java.util.Objects;

public final class MiniGameManager extends JavaPlugin {

    private MiniGameAPI api;
    private OfflinePlayerManager offlinePlayerManager;

    @Override
    public void onEnable() {
        getLogger().info("MiniGameManager 正在启动...");

        // 检查依赖
        if (getServer().getPluginManager().getPlugin("NBTAPI") == null) {
            getLogger().severe("未找到依赖 NBTAPI！插件将禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 1. 初始化核心组件
        this.offlinePlayerManager = new OfflinePlayerManager(this);
        this.api = new MiniGameServiceImpl(this, offlinePlayerManager);

        // 2. 注册 API 服务
        getServer().getServicesManager().register(
                MiniGameAPI.class,
                api,
                this,
                ServicePriority.Normal
        );

        // 3. 注册监听器
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this, offlinePlayerManager), this);

        // 4. 注册命令
        Objects.requireNonNull(getCommand("mgm")).setExecutor(new MGMCommand(api, this));

        getLogger().info("MiniGameManager 服务已成功加载并向服务器注册！");
    }

    @Override
    public void onDisable() {
        // 插件卸载时，检查是否有正在进行的操作
        if (offlinePlayerManager != null && offlinePlayerManager.hasActiveLocks()) {
            getLogger().warning("-----------------------------------------------------");
            getLogger().warning("警告：插件正在卸载，但仍有后台NBT操作在进行中！");
            getLogger().warning("这可能发生在重载或服务器关闭期间。");
            getLogger().warning("插件将尝试等待最多5秒以完成操作...");

            long startTime = System.currentTimeMillis();
            while (offlinePlayerManager.hasActiveLocks() && (System.currentTimeMillis() - startTime < 5000)) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (offlinePlayerManager.hasActiveLocks()) {
                getLogger().severe("等待超时！仍有未完成的操作。");
                getLogger().severe("以下玩家的数据可能处于不稳定状态: " + offlinePlayerManager.getLockedPlayerUUIDs());
                getLogger().severe("建议检查对应的 playerdata 文件和 .bak 备份文件。");
            } else {
                getLogger().info("所有后台操作已成功完成。");
            }
            getLogger().warning("-----------------------------------------------------");
        }

        getServer().getServicesManager().unregisterAll(this);
        getLogger().info("MiniGameManager 插件已卸载。");
    }
}