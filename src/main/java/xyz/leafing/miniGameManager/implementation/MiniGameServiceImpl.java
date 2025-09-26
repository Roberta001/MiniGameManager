package xyz.leafing.miniGameManager.implementation;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.leafing.miniGameManager.MiniGameManager;
import xyz.leafing.miniGameManager.api.MiniGameAPI;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MiniGameServiceImpl implements MiniGameAPI {

    private final PlayerDataManager dataManager;
    // 使用 ConcurrentHashMap 保证在多线程事件处理中的线程安全
    private final Map<UUID, JavaPlugin> playersInGame = new ConcurrentHashMap<>();

    public MiniGameServiceImpl(MiniGameManager plugin) {
        this.dataManager = new PlayerDataManager(plugin);
    }

    @Override
    public boolean enterGame(Player player, JavaPlugin sourcePlugin) {
        // putIfAbsent 是原子操作，如果 key 不存在则放入，并返回 null
        // 如果 key 已存在，则不放入，并返回已存在的值
        return playersInGame.putIfAbsent(player.getUniqueId(), sourcePlugin) == null;
    }

    @Override
    public boolean leaveGame(Player player, JavaPlugin sourcePlugin) {
        UUID uuid = player.getUniqueId();
        // 确保是所有者插件在移除玩家
        if (sourcePlugin.equals(playersInGame.get(uuid))) {
            playersInGame.remove(uuid);
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
    public boolean restorePlayerData(Player player) {
        return dataManager.restoreData(player);
    }

    @Override
    public boolean hasPendingData(UUID uuid) {
        return dataManager.hasData(uuid);
    }

    @Override
    public void clearPlayerData(Player player) {
        dataManager.clearData(player);
    }
}
