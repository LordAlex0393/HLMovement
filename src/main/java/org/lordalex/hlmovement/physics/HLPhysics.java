package org.lordalex.hlmovement.physics;

import org.lordalex.hlmovement.MovementConfig;
import org.lordalex.hlmovement.PlayerMoveState;

public final class HLPhysics {

    private HLPhysics() {}

    /**
     * Трение на земле (Source PM_Friction).
     * speed = |v|; если speed < stopspeed → control = stopspeed, иначе control = speed.
     * drop = control * friction * dt; новая скорость = max(0, speed - drop).
     */
    public static void applyFriction(PlayerMoveState s, MovementConfig cfg) {
        double speed = Math.sqrt(s.vx * s.vx + s.vz * s.vz);
        if (speed < 1.0e-6) {
            s.vx = 0;
            s.vz = 0;
            return;
        }
        double control = speed < cfg.stopspeed ? cfg.stopspeed : speed;
        double drop = control * cfg.friction * 0.05; // dt = 1/20 = 0.05
        double newSpeed = Math.max(0.0, speed - drop);
        double scale = newSpeed / speed;
        s.vx *= scale;
        s.vz *= scale;
    }

    /**
     * Наземное ускорение (Source PM_Accelerate).
     * addspeed = wishspeed - dot(v, wishdir); если >0 — добавляем accelspeed*wishdir.
     */
    public static void accelerate(PlayerMoveState s, double wishspeed, double accel) {
        double currentSpeed = s.vx * s.wishX + s.vz * s.wishZ; // dot
        double addSpeed = wishspeed - currentSpeed;
        if (addSpeed <= 0) return;

        double accelSpeed = accel * wishspeed * 0.05; // dt
        if (accelSpeed > addSpeed) accelSpeed = addSpeed;

        s.vx += s.wishX * accelSpeed;
        s.vz += s.wishZ * accelSpeed;
    }

    /**
     * Воздушное ускорение (Source PM_AirAccelerate) — СЕРДЦЕ БАННИХОПА.
     * wishspeed здесь мал (0.28 BPT = ~30 unit/s), поэтому можно добавлять скорость
     * вбок, не теряя её вперёд. Это и даёт "strafe"-разгон.
     */
    public static void airAccelerate(PlayerMoveState s, double wishspeedCap, double accel) {
        // Ограничиваем желаемую скорость малой величиной
        double wishspeed = wishspeedCap; // 0.28 по дефолту
        double currentSpeed = s.vx * s.wishX + s.vz * s.wishZ;
        double addSpeed = wishspeed - currentSpeed;
        if (addSpeed <= 0) return;

        double accelSpeed = accel * wishspeed * 0.05; // dt
        if (accelSpeed > addSpeed) accelSpeed = addSpeed;

        s.vx += s.wishX * accelSpeed;
        s.vz += s.wishZ * accelSpeed;
    }
}