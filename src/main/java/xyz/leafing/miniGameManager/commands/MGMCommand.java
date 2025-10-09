package xyz.leafing.miniGameManager.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.leafing.miniGameManager.MiniGameManager;
import xyz.leafing.miniGameManager.api.MiniGameAPI;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MGMCommand implements CommandExecutor {

    private final MiniGameAPI api;
    private final MiniGameManager plugin;

    public MGMCommand(MiniGameAPI api, MiniGameManager plugin) {
        this.api = api;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("minigamemanager.admin")) {
            sender.sendMessage(Component.text("你没有权限执行此命令。", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String playerName = args[1];

        // 使用 getOfflinePlayer 获取 UUID，这对于在线和离线玩家都有效
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Component.text("找不到名为 " + playerName + " 的玩家。", NamedTextColor.RED));
            return true;
        }
        UUID targetUUID = target.getUniqueId();

        switch (subCommand) {
            case "save":
                Player onlinePlayer = target.getPlayer();
                if (onlinePlayer == null) {
                    sender.sendMessage(Component.text("玩家 " + playerName + " 必须在线才能保存数据。", NamedTextColor.RED));
                    return true;
                }
                if (api.savePlayerData(onlinePlayer)) {
                    sender.sendMessage(Component.text("成功保存 " + playerName + " 的数据。", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("保存 " + playerName + " 的数据失败。", NamedTextColor.RED));
                }
                break;

            case "restore":
                if (!api.hasPendingData(targetUUID)) {
                    sender.sendMessage(Component.text("玩家 " + playerName + " 没有待恢复的数据。", NamedTextColor.YELLOW));
                    return true;
                }
                sender.sendMessage(Component.text("正在为 " + playerName + " 恢复数据... (这是一个异步操作)", NamedTextColor.GRAY));
                api.restorePlayerData(targetUUID).thenAccept(success -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            sender.sendMessage(Component.text("成功为 " + playerName + " 恢复数据。", NamedTextColor.GREEN));
                        } else {
                            sender.sendMessage(Component.text("为 " + playerName + " 恢复数据失败。", NamedTextColor.RED));
                        }
                    });
                });
                break;

            case "setgamemode":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /mgm setgamemode <玩家> <模式>", NamedTextColor.RED));
                    return true;
                }
                try {
                    GameMode gm = GameMode.valueOf(args[2].toUpperCase());
                    sender.sendMessage(Component.text("正在为 " + playerName + " 设置游戏模式为 " + gm.name() + "... (异步)", NamedTextColor.GRAY));
                    handleFuture(api.setGameMode(targetUUID, gm), sender, "设置游戏模式成功", "设置游戏模式失败");
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text("无效的游戏模式: " + args[2], NamedTextColor.RED));
                }
                break;

            case "teleport":
                if (args.length < 5) {
                    sender.sendMessage(Component.text("用法: /mgm teleport <玩家> <x> <y> <z> [世界]", NamedTextColor.RED));
                    return true;
                }
                try {
                    double x = Double.parseDouble(args[2]);
                    double y = Double.parseDouble(args[3]);
                    double z = Double.parseDouble(args[4]);
                    World world = (args.length > 5) ? Bukkit.getWorld(args[5]) : ((sender instanceof Player p) ? p.getWorld() : Bukkit.getWorlds().get(0));
                    if (world == null) {
                        sender.sendMessage(Component.text("无效的世界名称。", NamedTextColor.RED));
                        return true;
                    }
                    Location loc = new Location(world, x, y, z);
                    sender.sendMessage(Component.text("正在传送 " + playerName + "... (异步)", NamedTextColor.GRAY));
                    handleFuture(api.teleport(targetUUID, loc), sender, "传送成功", "传送失败");
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("坐标必须是数字。", NamedTextColor.RED));
                }
                break;

            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("--- MiniGameManager Admin ---", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/mgm save <在线玩家> - 保存玩家数据", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mgm restore <玩家> - 恢复玩家数据 (在线/离线)", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mgm setgamemode <玩家> <模式> - 设置游戏模式 (在线/离线)", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mgm teleport <玩家> <x> <y> <z> [世界] - 传送 (在线/离线)", NamedTextColor.YELLOW));
    }

    private void handleFuture(CompletableFuture<Boolean> future, CommandSender sender, String successMsg, String failMsg) {
        future.thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    sender.sendMessage(Component.text(successMsg, NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(failMsg, NamedTextColor.RED));
                }
            });
        });
    }
}