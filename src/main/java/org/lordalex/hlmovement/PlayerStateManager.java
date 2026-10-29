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

            // Обновляем скорость для сайдбара (со сглаживанием)
            if (lastLoc != null) {
                double dx = currentLoc.getX() - lastLoc.getX();
                double dz = currentLoc.getZ() - lastLoc.getZ();
                if (Math.abs(dx) < 2.0 && Math.abs(dz) < 2.0) {
                    // Сглаживание: берем 80% старой и 20% новой скорости, чтобы избежать скачков
                    s.vx = s.vx * 0.8 + dx * 0.2;
                    s.vz = s.vz * 0.8 + dz * 0.2;
                }
            }
            lastLocations.put(p.getUniqueId(), currentLoc.clone());

            s.onGround = p.isOnGround();

            // --- ФИЗИКА БАННИХОПА (На основе взгляда) ---
            if (s.onGround && s.jumpPressed) {
                // Ванильный прыжок. Сохраняем текущую скорость.
                Vector currentVel = p.getVelocity();
                p.setVelocity(new Vector(currentVel.getX(), 0.42, currentVel.getZ()));
                s.justJumped = true;
            } else if (!s.onGround) {
                // В ВОЗДУХЕ:
                float yawDelta = s.yaw - s.lastYaw;
                if (yawDelta > 180) yawDelta -= 360;
                if (yawDelta < -180) yawDelta += 360;

                // Если игрок повернул голову больше чем на 0.5 градуса за тик
                if (Math.abs(yawDelta) > 0.5f) {
                    Vector currentVel = p.getVelocity();
                    Vector lookDir = p.getLocation().getDirection().setY(0).normalize();

                    // 1. Считаем текущую скорость (только горизонтальную)
                    double currentSpeedX = currentVel.getX();
                    double currentSpeedZ = currentVel.getZ();
                    double currentSpeed = Math.sqrt(currentSpeedX * currentSpeedX + currentSpeedZ * currentSpeedZ);

                    // Если мы почти стоим, не ускоряем (чтобы нельзя было разогнаться с места в воздухе)
                    if (currentSpeed > 0.1) {
                        // Направление, куда мы летим прямо сейчас
                        Vector currentDir = new Vector(currentSpeedX / currentSpeed, 0, currentSpeedZ / currentSpeed);

                        // 2. Вычисляем "прямое" ускорение (вдоль текущего полета)
                        // Чем сильнее крутим мышку, тем больше ускорение вперед
                        double forwardAccel = Math.abs(yawDelta) * 0.015;
                        if (forwardAccel > 0.3) forwardAccel = 0.3;

                        // 3. Вычисляем "боковое" ускорение (чтобы плавно доворачивало)
                        // Оно очень маленькое, чтобы не сносило в бок
                        Vector strafeDir = new Vector(-lookDir.getZ(), 0, lookDir.getX()).multiply(Math.signum(yawDelta));
                        double strafeAccel = Math.abs(yawDelta) * 0.002;
                        if (strafeAccel > 0.05) strafeAccel = 0.05;

                        // 4. Собираем новую скорость
                        double newX = currentSpeedX + (currentDir.getX() * forwardAccel) + (strafeDir.getX() * strafeAccel);
                        double newZ = currentSpeedZ + (currentDir.getZ() * forwardAccel) + (strafeDir.getZ() * strafeAccel);

                        // Ограничиваем максимальную скорость
                        double speed = Math.sqrt(newX * newX + newZ * newZ);
                        if (speed > 4.0) {
                            newX = (newX / speed) * 4.0;
                            newZ = (newZ / speed) * 4.0;
                        }

                        p.setVelocity(new Vector(newX, currentVel.getY(), newZ));
                    }
                }
            }

            s.lastYaw = s.yaw;
            s.wasJumpPressed = s.jumpPressed;
            s.justJumped = false;
        }
    }
}