package org.lordalex.hlmovement.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.lordalex.hlmovement.HLMovement;
import org.lordalex.hlmovement.PlayerMoveState;
import org.lordalex.hlmovement.PlayerStateManager;

public class SteerVehicleListener extends PacketAdapter {

    private final PlayerStateManager stateManager;

    public SteerVehicleListener(HLMovement plugin, PlayerStateManager sm) {
        // Ловим пакеты позиции (где есть прыжки и движение)
        super(plugin, ListenerPriority.LOWEST, PacketType.Play.Client.POSITION, PacketType.Play.Client.POSITION_LOOK);
        this.stateManager = sm;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player p = event.getPlayer();
        if (p == null) return;

        PacketContainer packet = event.getPacket();

        // Пытаемся прочитать Y-координату из пакета позиции
        try {
            double y = packet.getDoubles().read(1); // Y в пакете
            double currentY = p.getLocation().getY();

            // Если Y в пакете больше, чем текущий Y игрока (значит он прыгнул)
            if (y > currentY + 0.1) {
                // ТЕСТ: Даем ему скорость вперед!
                Vector lookDir = p.getLocation().getDirection().setY(0).normalize();
                p.setVelocity(lookDir.multiply(5.5)); // Пинок вперед (скорость 30 бл/с)

                // Убираем спам, пишем только раз в 10 прыжков
                System.out.println("[HL-TEST] Игрок прыгнул! Даем буст вперед.");
            }
        } catch (Exception e) {
            // Если пакет не имеет координат Y (например, LOOK), игнорируем
        }
    }
}