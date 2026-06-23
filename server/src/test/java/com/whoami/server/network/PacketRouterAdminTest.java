package com.whoami.server.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;
import com.whoami.server.admin.AdminService;
import com.whoami.server.admin.AdminServices;
import com.whoami.server.admin.InMemoryCharacterRepository;
import com.whoami.server.admin.InMemoryUserAdminRepository;
import com.whoami.server.session.SessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PacketRouterAdminTest {

    private AdminService adminService;
    private ClientHandler handler;

    @BeforeEach
    public void setUp() {
        adminService = new AdminService(new SessionRegistry(),
                new InMemoryCharacterRepository(), new InMemoryUserAdminRepository());
        AdminServices.set(adminService);
        handler = mock(ClientHandler.class);
    }

    private Packet adminPacket(PacketType type, String payload) {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        return new Packet(Packet.MAGIC_BYTE, 0, type.getId(), data.length, (short) 0, data);
    }

    @Test
    public void adminCanAddCharacter() {
        when(handler.isAdmin()).thenReturn(true);

        PacketRouter.route(adminPacket(PacketType.ADMIN_ADD_CHARACTER, "Mario:Video Games"), handler);

        assertEquals(1, adminService.listCharacters().size());
        assertEquals("Mario", adminService.listCharacters().get(0).name());

        ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
        verify(handler).sendPacket(captor.capture());
        Packet response = captor.getValue();
        assertEquals(PacketType.ADMIN_ADD_CHARACTER.getId(), response.getPacketType());
        assertTrue(new String(response.getPayload(), StandardCharsets.UTF_8).startsWith("SUCCESS:"));
    }

    @Test
    public void nonAdminIsForbidden() {
        when(handler.isAdmin()).thenReturn(false);

        PacketRouter.route(adminPacket(PacketType.ADMIN_ADD_CHARACTER, "Mario:Video Games"), handler);

        assertTrue(adminService.listCharacters().isEmpty(), "non-admin must not modify the bank");

        ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
        verify(handler).sendPacket(captor.capture());
        Packet response = captor.getValue();
        assertEquals(PacketType.ERROR.getId(), response.getPacketType());
        assertEquals("FORBIDDEN", new String(response.getPayload(), StandardCharsets.UTF_8));
    }
}
