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

        // Сброс уровня опыта при выходе (чтобы не залипло)
        p.setLevel(0);
        p.setExp(0);
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

            // 1. Считаем сглаженную скорость для физики
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

            // 2. ФИЗИКА РАЗГОНА (HL1 + Стрейф)
            if (plugin.isMovementEnabled(p) && !s.onGround) {
                double currentSpeed = Math.sqrt(s.vx * s.vx + s.vz * s.vz);

                if (currentSpeed > 0.01) {
                    // Направление текущего полета
                    double dirX = s.vx / currentSpeed;
                    double dirZ = s.vz / currentSpeed;

                    // Направление взгляда
                    Vector lookDir = p.getLocation().getDirection().setY(0).normalize();

                    // Вектор стрейфа (вправо от взгляда)
                    Vector strafeDir = new Vector(-lookDir.getZ(), 0, lookDir.getX());

                    // Вычисляем, нажимает ли игрок A/D (смотрит вбок)
                    double strafeDot = (dirX * strafeDir.getX()) + (dirZ * strafeDir.getZ());
                    boolean isStrafing = Math.abs(strafeDot) > 0.3;

                    float yawDelta = s.yaw - s.lastYaw;
                    if (yawDelta > 180) yawDelta -= 360;
                    if (yawDelta < -180) yawDelta += 360;

                    if (Math.abs(yawDelta) > 0.05f) {
                        double turn = Math.signum(yawDelta);
                        double turnDot = turn * strafeDot;

                        // Множитель буста
                        double mult = 1.0;
                        if (isStrafing && turnDot > 0.3) {
                            mult = 8.0; // Идеальный стрейф (A/D + мышь в ту же сторону)
                        } else if (!isStrafing) {
                            mult = 0.3; // Просто крутим мышкой
                        }

                        // Применяем ускорение
                        double accel = settings.getAccelPerTick() * mult;

                        Vector pushDir = new Vector(
                                (lookDir.getX() * settings.getForwardMultiplier()) + (strafeDir.getX() * turn * settings.getStrafeMultiplier()),
                                0,
                                (lookDir.getZ() * settings.getForwardMultiplier()) + (strafeDir.getZ() * turn * settings.getStrafeMultiplier())
                        ).normalize();

                        Vector currentVel = p.getVelocity();
                        double newX = currentVel.getX() + pushDir.getX() * accel;
                        double newZ = currentVel.getZ() + pushDir.getZ() * accel;
                        double newY = currentVel.getY();

                        // Ограничение скорости
                        double newSpeed = Math.sqrt(newX * newX + newZ * newZ);
                        if (newSpeed > settings.getMaxCap()) {
                            newX = (newX / newSpeed) * settings.getMaxCap();
                            newZ = (newZ / newSpeed) * settings.getMaxCap();
                        }

                        p.setVelocity(new Vector(newX, newY, newZ));
                    }
                }
            }

            s.lastYaw = s.yaw;
        }
    }
}