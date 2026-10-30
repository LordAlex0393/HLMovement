package org.lordalex.hlmovement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateManager {

    private final HLMovement plugin;
    private Settings settings;
    private final Map<UUID, PlayerMoveState> states = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private BukkitTask task;

    public PlayerStateManager(HLMovement plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void updateSettings(Settings newSettings) {
        this.settings = newSettings;
    }

    public PlayerMoveState get(Player p) {
        return states.computeIfAbsent(p.getUniqueId(), k -> new PlayerMoveState());
    }

    public void remove(Player p) {
        states.remove(p.getUniqueId());
        lastLocations.remove(p.getUniqueId());
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) task.cancel();
        states.clear();
        lastLocations.clear();
    }

    private void tick() {
        if (!settings.isEnabled()) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerMoveState s = get(p);
            Location currentLoc = p.getLocation();
            Location lastLoc = lastLocations.get(p.getUniqueId());

            // 1. Обновляем скорость для сайдбара (как было)
            if (lastLoc != null) {
                double dx = currentLoc.getX() - lastLoc.getX();
                double dz = currentLoc.getZ() - lastLoc.getZ();
                if (Math.abs(dx) < 2.0 && Math.abs(dz) < 2.0) {
                    s.vx = s.vx * 0.8 + dx * 0.2;
                    s.vz = s.vz * 0.8 + dz * 0.2;
                }
            }
            lastLocations.put(p.getUniqueId(), currentLoc.clone());
            s.onGround = p.isOnGround();

            // 2. Физика (только если включена)
            if (plugin.isMovementEnabled(p) && !s.onGround) {
                float yawDelta = s.yaw - s.lastYaw;
                if (yawDelta > 180) yawDelta -= 360;
                if (yawDelta < -180) yawDelta += 360;

                if (Math.abs(yawDelta) > 0.05f) {
                    Vector currentVel = p.getVelocity();
                    double velX = currentVel.getX();
                    double velZ = currentVel.getZ();
                    double velY = currentVel.getY();

                    double currentSpeed = Math.sqrt(velX * velX + velZ * velZ);

                    if (currentSpeed > 0.05) {
                        Vector lookDir = p.getLocation().getDirection().setY(0).normalize();
                        Vector strafeDir = new Vector(-lookDir.getZ(), 0, lookDir.getX());

                        double turn = Math.signum(yawDelta);

                        // Берем из конфига настройки направлений (в твоем коде было 0.6 и 0.4)
                        double pushX = (lookDir.getX() * settings.getForwardMultiplier()) + (strafeDir.getX() * turn * settings.getStrafeMultiplier());
                        double pushZ = (lookDir.getZ() * settings.getForwardMultiplier()) + (strafeDir.getZ() * turn * settings.getStrafeMultiplier());

                        double pushLen = Math.sqrt(pushX * pushX + pushZ * pushZ);
                        pushX /= pushLen;
                        pushZ /= pushLen;

                        // Берем из конфига силу разгона и лимит
                        double newX = velX + (pushX * settings.getAccelPerTick());
                        double newZ = velZ + (pushZ * settings.getAccelPerTick());

                        double newSpeed = Math.sqrt(newX * newX + newZ * newZ);
                        if (newSpeed > settings.getMaxCap()) {
                            newX = (newX / newSpeed) * settings.getMaxCap();
                            newZ = (newZ / newSpeed) * settings.getMaxCap();
                        }

                        p.setVelocity(new Vector(newX, velY, newZ));
                    }
                }
            }

            s.lastYaw = s.yaw;
        }
    }
}