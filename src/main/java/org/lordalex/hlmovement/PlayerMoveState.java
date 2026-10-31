package org.lordalex.hlmovement;

public class PlayerMoveState {
    public double vx, vz;
    public float yaw;
    public float lastYaw;
    public boolean onGround;
    public double wishX, wishZ;

    public double getSpeedBpt() {
        return Math.sqrt(vx * vx + vz * vz);
    }

    public double getSpeedBlocksPerSecond() {
        return getSpeedBpt() * 20.0;
    }
}