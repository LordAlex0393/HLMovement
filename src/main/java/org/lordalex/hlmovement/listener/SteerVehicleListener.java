package org.lordalex.hlmovement.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.lordalex.hlmovement.HLMovement;

public class SteerVehicleListener extends PacketAdapter {

    public SteerVehicleListener(HLMovement plugin) {
        // Слушаем ВСЕ пакеты клиента
        super(plugin, ListenerPriority.LOWEST, PacketType.Play.Client.getInstance());
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        // Не спамим в консоль, показываем только движение и взгляд (10-20 раз в секунду)
        String name = event.getPacketType().name();
        if (name.contains("FLYING") || name.contains("LOOK") || name.contains("POSITION") || name.contains("STEER")) {
            System.out.println("[HL-DEBUG] Пришел пакет: " + name);
        }
    }
}