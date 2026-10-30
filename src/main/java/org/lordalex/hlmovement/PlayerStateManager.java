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
    private final MovementConfig config;
    private final Map<UUID, PlayerMoveState> states = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private BukkitTask task;

    // Прибавка скорости
    private final double ACCEL_PER_TICK = 0.15;
    private final double MAX_CAP = 10.0;

    public PlayerStateManager(HLMovement plugin, MovementConfig config) {
        this.plugin = plugin;
        this.config = config;
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
        if (!config.enabled) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerMoveState s = get(p);
            Location currentLoc = p.getLocation();
            Location lastLoc = lastLocations.get(p.getUniqueId());

            // 1. Обновляем скорость для сайдбара ВСЕГДА (даже если физика выключена)
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

            // 2. Физика - только если игрок не отключил её
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

                        double pushX = (lookDir.getX() * 0.6) + (strafeDir.getX() * turn * 0.4);
                        double pushZ = (lookDir.getZ() * 0.6) + (strafeDir.getZ() * turn * 0.4);

                        double pushLen = Math.sqrt(pushX * pushX + pushZ * pushZ);
                        pushX /= pushLen;
                        pushZ /= pushLen;

                        double newX = velX + (pushX * 0.15);
                        double newZ = velZ + (pushZ * 0.15);

                        double newSpeed = Math.sqrt(newX * newX + newZ * newZ);
                        if (newSpeed > 10.0) {
                            newX = (newX / newSpeed) * 10.0;
                            newZ = (newZ / newSpeed) * 10.0;
                        }

                        p.setVelocity(new Vector(newX, velY, newZ));
                    }
                }
            }

            s.lastYaw = s.yaw;
        }
    }
}