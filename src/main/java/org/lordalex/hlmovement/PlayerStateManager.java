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

    // --- НАСТРОЙКИ ---
    // Прибавка скорости за каждый тик, когда ты крутишь мышкой.
    // УВЕЛИЧЕНО В 4 РАЗА (было 0.05, стало 0.2).
    // Если будет слишком быстро — уменьши до 0.1. Если медленно — увеличь до 0.4.
    private final double ACCEL_PER_TICK = 0.2;

    // Абсолютный потолок (чтобы сервер не взорвался). 10.0 = 200 блоков/сек.
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

            // Скорость для сайдбара (сглаживание)
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

            // --- ФИЗИКА ---
            if (!s.onGround) {
                // В ВОЗДУХЕ: смотрим, крутит ли игрок мышкой
                float yawDelta = s.yaw - s.lastYaw;
                if (yawDelta > 180) yawDelta -= 360;
                if (yawDelta < -180) yawDelta += 360;

                // Если игрок крутит мышку (даже чуть-чуть, порог 0.05 градуса)
                if (Math.abs(yawDelta) > 0.05f) {
                    Vector currentVel = p.getVelocity();
                    double velX = currentVel.getX();
                    double velZ = currentVel.getZ();
                    double velY = currentVel.getY();

                    double currentSpeed = Math.sqrt(velX * velX + velZ * velZ);

                    // Если мы уже двигаемся (чтобы не разгоняться с нуля в воздухе)
                    if (currentSpeed > 0.05) {
                        // Направление, куда мы летим прямо сейчас
                        Vector currentDir = new Vector(velX / currentSpeed, 0, velZ / currentSpeed);

                        // Вектор взгляда (куда смотрит мышь)
                        Vector lookDir = p.getLocation().getDirection().setY(0).normalize();

                        // Вектор стрейфа (вправо от взгляда)
                        Vector strafeDir = new Vector(-lookDir.getZ(), 0, lookDir.getX());

                        // Определяем, в какую сторону крутится мышь
                        double turn = Math.signum(yawDelta);

                        // Если крутим вправо — стрейф вправо, если влево — влево
                        double strafeX = strafeDir.getX() * turn;
                        double strafeZ = strafeDir.getZ() * turn;

                        // --- ГЛАВНОЕ: ПРИБАВКА СКОРОСТИ ---
                        // Мы добавляем фиксированную прибавку (ACCEL_PER_TICK) каждый тик,
                        // когда крутим мышкой. Это плавно накапливает скорость.
                        double newX = velX + (strafeX * ACCEL_PER_TICK) + (currentDir.getX() * ACCEL_PER_TICK * 0.5);
                        double newZ = velZ + (strafeZ * ACCEL_PER_TICK) + (currentDir.getZ() * ACCEL_PER_TICK * 0.5);

                        // Потолок (чтобы не было переполнения)
                        double newSpeed = Math.sqrt(newX * newX + newZ * newZ);
                        if (newSpeed > MAX_CAP) {
                            newX = (newX / newSpeed) * MAX_CAP;
                            newZ = (newZ / newSpeed) * MAX_CAP;
                        }

                        p.setVelocity(new Vector(newX, velY, newZ));
                    }
                }
            }

            s.lastYaw = s.yaw;
        }
    }
}