package org.lordalex.hlmovement.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.lordalex.hlmovement.HLMovement;
import org.lordalex.hlmovement.PlayerMoveState;
import org.lordalex.hlmovement.PlayerStateManager;
/**
 * Читает PacketPlayInSteerVehicle — единственный пакет, где есть W/A/S/D + jump.
 * ProtocolLib сам разбирается с версиями, NMS не нужен.
 */
public class SteerVehicleListener extends PacketAdapter {

    private final PlayerStateManager stateManager;

    public SteerVehicleListener(HLMovement plugin, PlayerStateManager sm) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.STEER_VEHICLE);
        this.stateManager = sm;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player p = event.getPlayer();
        if (p == null) return;

        PacketContainer packet = event.getPacket();
        // В 1.8 поля: float a (strafe), float b (forward), boolean jump, boolean sneak
        float strafe = packet.getFloat().read(0);
        float forward = packet.getFloat().read(1);
        boolean jump = packet.getBooleans().read(0);

        System.out.println("[HL] " + p.getName() + " strafe=" + strafe + " forward=" + forward + " jump=" + jump + " yaw=" + p.getLocation().getYaw());

        PlayerMoveState s = stateManager.get(p);
        s.strafe = strafe;
        s.forward = forward;
        s.jumpPressed = jump;
    }
}