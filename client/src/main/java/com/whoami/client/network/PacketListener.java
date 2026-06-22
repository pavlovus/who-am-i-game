package com.whoami.client.network;

import com.whoami.protocol.packets.Packet;

public interface PacketListener {
    void onPacketReceived(Packet packet);
    void onDisconnected();
}
