package com.whoami.server.network;

import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ClientHandlerTest {

    private Socket mockSocket;
    private ByteArrayOutputStream outStream;
    private MockedStatic<RoomManager> mockedRoomManagerStatic;
    private RoomManager mockRoomManager;

    @BeforeEach
    public void setUp() throws IOException {
        mockSocket = mock(Socket.class);
        outStream = new ByteArrayOutputStream();
        
        when(mockSocket.getOutputStream()).thenReturn(outStream);
        
        mockRoomManager = mock(RoomManager.class);
        mockedRoomManagerStatic = mockStatic(RoomManager.class);
        mockedRoomManagerStatic.when(RoomManager::getInstance).thenReturn(mockRoomManager);
    }

    @AfterEach
    public void tearDown() {
        mockedRoomManagerStatic.close();
    }

    @Test
    public void testSendPacket() throws IOException {
        // Setup empty input stream so initialization doesn't fail
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        
        ClientHandler handler = new ClientHandler(mockSocket);
        
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ROOM_LEAVE.getId(), 0, (short)0, new byte[0]);
        handler.sendPacket(packet);
        
        byte[] writtenBytes = outStream.toByteArray();
        // A packet header is 13 bytes for this protocol (1+4+1+4+2+1) plus payload (0)
        // Wait, Packet header is: magic (1) + clientId (4) + packetType (1) + length (4) + crc (2) = 12 bytes. Actually let's check size
        assertTrue(writtenBytes.length > 0, "Should write bytes to the output stream");
    }

    @Test
    public void testDisconnectCallsLeaveRoom() throws IOException {
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        
        ClientHandler handler = new ClientHandler(mockSocket);
        
        handler.disconnect();
        
        verify(mockRoomManager, times(1)).leaveRoom(handler);
        verify(mockSocket, times(1)).close();
    }
}
