package dev.siea.discord2fa.common.event;

public enum EventType {
    BLOCK_BREAK,
    BLOCK_PLACE,
    MOVE,
    CHAT,
    DROP,
    INVENTORY,
    /** Using blocks and items: buttons, levers, doors, buckets, flint and steel, eating, entity interaction. */
    INTERACT,
    /** Damaging another entity, directly or with a projectile. */
    ATTACK,
    /** Picking up items and arrows. */
    PICKUP,
    COMMAND
}
