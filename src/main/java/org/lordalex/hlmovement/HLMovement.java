package org.lordalex.hlmovement;

import com.comphenix.protocol.ProtocolLibrary;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.lordalex.hlmovement.listener.PlayerMoveListener;
import org.lordalex.hlmovement.listener.SteerVehicleListener;
import org.lordalex.hlmovement.ui.SpeedScoreboard;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HLMovement extends JavaPlugin implements Listener {

    private static HLMovement instance;
    private MovementConfig config;
    private PlayerStateManager stateManager;
    private SpeedScoreboard scoreboard;
    private final Set<UUID> disabledMovement = new HashSet<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.config = new MovementConfig(getConfig());
        this.stateManager = new PlayerStateManager(this, config);
        this.scoreboard = new SpeedScoreboard(this, stateManager, config);

        ProtocolLibrary.getProtocolManager().addPacketListener(new SteerVehicleListener(this, stateManager));
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(stateManager), this);
        Bukkit.getPluginManager().registerEvents(this, this);

        stateManager.start();
        scoreboard.start();
    }

    @Override
    public void onDisable() {
        if (stateManager != null) stateManager.stop();
        if (scoreboard != null) scoreboard.stop();
        instance = null;
    }

    public static HLMovement get() { return instance; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (config.scoreboardDefault) {
            scoreboard.enable(event.getPlayer());
        }
    }

    public boolean isMovementEnabled(Player p) {
        return !disabledMovement.contains(p.getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cТолько для игроков.");
            return true;
        }
        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("hl")) {
            if (args.length == 0) {
                // Красивое меню
                p.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "----------------------------------");
                p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "       HL MOVEMENT");
                p.sendMessage(ChatColor.GRAY + " Ваш статус мувмента: " + (isMovementEnabled(p) ? "§aВКЛ" : "§cВЫКЛ"));
                p.sendMessage(ChatColor.GRAY + " Сайдбар скорости: " + (scoreboard.isEnabled(p) ? "§aВКЛ" : "§cВЫКЛ"));
                p.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "----------------------------------");
                p.sendMessage(ChatColor.YELLOW + "/hl move" + ChatColor.GRAY + " — вкл/выкл распрыжку для себя");
                p.sendMessage(ChatColor.YELLOW + "/hl scoreboard" + ChatColor.GRAY + " — вкл/выкл сайдбар скорости");
                p.sendMessage(ChatColor.YELLOW + "/hl reload" + ChatColor.GRAY + " — перезагрузить конфиг (админ)");
                p.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "----------------------------------");
                return true;
            }

            if (args[0].equalsIgnoreCase("move")) {
                UUID uuid = p.getUniqueId();
                if (disabledMovement.contains(uuid)) {
                    disabledMovement.remove(uuid);
                    p.sendMessage("§aHL Movement включен для вас.");
                } else {
                    disabledMovement.add(uuid);
                    p.sendMessage("§cHL Movement выключен для вас.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("scoreboard") || args[0].equalsIgnoreCase("sb")) {
                scoreboard.toggle(p);
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!p.hasPermission("hlmovement.reload")) {
                    p.sendMessage("§cНет прав.");
                    return true;
                }
                reloadConfig();
                this.config = new MovementConfig(getConfig());
                scoreboard.refreshConfig(config);
                p.sendMessage("§aКонфиг HL Movement перезагружен.");
                return true;
            }
        }
        return false;
    }
}