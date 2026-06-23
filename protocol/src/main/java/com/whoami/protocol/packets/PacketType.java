package com.whoami.protocol.packets;

public enum PacketType {
    AUTH_REQUEST(1),
    AUTH_RESPONSE(2),
    ROOM_CREATE(3),
    ROOM_JOIN(4),
    GAME_START(5),
    GAME_STATE(6),
    QUESTION(7),
    ANSWER(8),
    GUESS(9),
    GAME_OVER(10),
    ADMIN_BLOCK(11),
    ROOM_LEAVE(12),
    REMATCH(13),
    ADMIN_LIST(14),
    ADMIN_KICK(15),
    ADMIN_ADD_CHARACTER(16),
    ADMIN_STATS(17),
    ERROR(18);

    private final int id;

    PacketType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static PacketType fromId(int id) {
        for (PacketType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PacketType id: " + id);
    }
}
