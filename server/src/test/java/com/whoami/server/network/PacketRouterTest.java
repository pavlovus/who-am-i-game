package com.whoami.server.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PacketRouterTest {

    private ClientHandler mockClientHandler;
    private MockedStatic<RoomManager> mockedRoomManagerStatic;
    private RoomManager mockRoomManager;

    @BeforeEach
    public void setUp() {
        mockClientHandler = mock(ClientHandler.class);
        
        mockRoomManager = mock(RoomManager.class);
        mockedRoomManagerStatic = mockStatic(RoomManager.class);
        mockedRoomManagerStatic.when(RoomManager::getInstance).thenReturn(mockRoomManager);
    }

    @AfterEach
    public void tearDown() {
        mockedRoomManagerStatic.close();
    }

    @Test
    public void testRouteRoomCreate() {
        when(mockRoomManager.createRoom(mockClientHandler)).thenReturn("CODE12");

        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ROOM_CREATE.getId(), 0, (short)0, new byte[0]);
        
        PacketRouter.route(packet, mockClientHandler);
        
        ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
        verify(mockClientHandler, times(1)).sendPacket(packetCaptor.capture());
        
        Packet sentPacket = packetCaptor.getValue();
        assertEquals(PacketType.ROOM_CREATE.getId(), sentPacket.getPacketType());
        
        String payloadStr = new String(sentPacket.getPayload(), StandardCharsets.UTF_8);
        assertEquals("SUCCESS:CODE12", payloadStr);
    }

    @Test
    public void testRouteRoomJoinSuccess() {
        byte[] payload = "CODE12".getBytes(StandardCharsets.UTF_8);
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ROOM_JOIN.getId(), payload.length, (short)0, payload);
        
        when(mockRoomManager.joinRoom("CODE12", mockClientHandler)).thenReturn(true);
        
        PacketRouter.route(packet, mockClientHandler);
        
        ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
        verify(mockClientHandler, times(1)).sendPacket(packetCaptor.capture());
        
        Packet sentPacket = packetCaptor.getValue();
        assertEquals(PacketType.ROOM_JOIN.getId(), sentPacket.getPacketType());
        
        String payloadStr = new String(sentPacket.getPayload(), StandardCharsets.UTF_8);
        assertEquals("SUCCESS", payloadStr);
    }

    @Test
    public void testRouteRoomJoinFailure() {
        byte[] payload = "WRONG".getBytes(StandardCharsets.UTF_8);
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ROOM_JOIN.getId(), payload.length, (short)0, payload);
        
        when(mockRoomManager.joinRoom("WRONG", mockClientHandler)).thenReturn(false);
        
        PacketRouter.route(packet, mockClientHandler);
        
        ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
        verify(mockClientHandler, times(1)).sendPacket(packetCaptor.capture());
        
        Packet sentPacket = packetCaptor.getValue();
        assertEquals(PacketType.ROOM_JOIN.getId(), sentPacket.getPacketType());
        
        String payloadStr = new String(sentPacket.getPayload(), StandardCharsets.UTF_8);
        assertEquals("ERROR:Room full or not found", payloadStr);
    }

    @Test
    public void testRouteRoomLeave() {
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ROOM_LEAVE.getId(), 0, (short)0, new byte[0]);
        
        PacketRouter.route(packet, mockClientHandler);
        
        verify(mockRoomManager, times(1)).leaveRoom(mockClientHandler);
        
        ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
        verify(mockClientHandler, times(1)).sendPacket(packetCaptor.capture());
        
        Packet sentPacket = packetCaptor.getValue();
        assertEquals(PacketType.ROOM_LEAVE.getId(), sentPacket.getPacketType());
        
        String payloadStr = new String(sentPacket.getPayload(), StandardCharsets.UTF_8);
        assertEquals("SUCCESS", payloadStr);
    }

    @Test
    public void testRouteRejectsInvalidCredentials() {
        // Username too short -> server-side validation must reject before any DB/auth call.
        byte[] payload = "LOGIN:ab:1".getBytes(StandardCharsets.UTF_8);
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.AUTH_REQUEST.getId(), payload.length, (short)0, payload);

        PacketRouter.route(packet, mockClientHandler);

        ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
        verify(mockClientHandler, times(1)).sendPacket(packetCaptor.capture());

        Packet sentPacket = packetCaptor.getValue();
        assertEquals(PacketType.AUTH_RESPONSE.getId(), sentPacket.getPacketType());
        assertTrue(new String(sentPacket.getPayload(), StandardCharsets.UTF_8).startsWith("ERROR:"));
    }

    @Test
    public void testRouteMalformedAuthPayload() {
        // Normal payload is "LOGIN:user:pass" or "REGISTER:user:pass"
        // Let's send something weird: "JUSTABRAKADABRA" without colons
        byte[] payload = "JUSTABRAKADABRA".getBytes(StandardCharsets.UTF_8);
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.AUTH_REQUEST.getId(), payload.length, (short)0, payload);
        
        // This should log an error to System.err but NOT throw an exception, and NOT send any packet back
        assertDoesNotThrow(() -> PacketRouter.route(packet, mockClientHandler), "Router should handle malformed payloads gracefully without crashing");
        
        verify(mockClientHandler, never()).sendPacket(any(Packet.class));
    }
}
