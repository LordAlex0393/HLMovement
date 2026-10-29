package org.lordalex.hlmovement;


import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.lordalex.hlmovement.listener.PlayerMoveListener;
import org.lordalex.hlmovement.listener.SteerVehicleListener;
import org.lordalex.hlmovement.ui.SpeedScoreboard;

public class HLMovement extends JavaPlugin {

    private static HLMovement instance;
    private MovementConfig config;
    private PlayerStateManager stateManager;
    private SpeedScoreboard scoreboard;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.config = new MovementConfig(getConfig());
        this.stateManager = new PlayerStateManager(this, config);
        this.scoreboard = new SpeedScoreboard(this, stateManager, config);

        // Регистрируем ProtocolLib-слушатель
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        pm.addPacketListener(new SteerVehicleListener(this, stateManager));

        // Отмена ванильного горизонтального движения + чтение yaw
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(stateManager), this);

        // Тик-цикл физики
        stateManager.start();

        // Автозапуск scoreboard для уже зашедших
        if (config.scoreboardDefault) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                scoreboard.enable(p);
            }
        }
        scoreboard.start();

        getLogger().info("HLMovement enabled. Physics constants: maxspeed=" + config.maxspeed
                + " airaccel=" + config.airaccelerate + " autoBhop=" + config.autoBhop);

        // Включаем сайдбар всем, кто зайдет на сервер
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                if (config.scoreboardDefault) {
                    scoreboard.enable(event.getPlayer());
                }
            }
        }, this);
    }

    @Override
    public void onDisable() {
        if (stateManager != null) stateManager.stop();
        if (scoreboard != null) scoreboard.stop();
        instance = null;
    }

    public static HLMovement get() { return instance; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("hlspeed")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cТолько для игроков.");
                return true;
            }
            scoreboard.toggle((Player) sender);
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("hlreload")) {
            if (!sender.hasPermission("hlmovement.reload")) {
                sender.sendMessage("§cНет прав.");
                return true;
            }
            reloadConfig();
            this.config = new MovementConfig(getConfig());
            scoreboard.refreshConfig(config);
            sender.sendMessage("§aHLMovement перезагружен.");
            return true;
        }
        return false;
    }
}