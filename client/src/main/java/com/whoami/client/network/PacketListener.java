package com.whoami.client.network;

import com.whoami.protocol.packets.Packet;

public interface PacketListener {
    void onPacketReceived(Packet packet);
    void onDisconnected();

    /** Invoked after the client transparently re-established a dropped connection. */
    default void onReconnected() {
    }
}
