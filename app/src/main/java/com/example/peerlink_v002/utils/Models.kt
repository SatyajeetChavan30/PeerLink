package com.example.peerlink_v002.utils

import java.util.UUID

// Enums
enum class MessageType {
    CHAT,
    PRESENCE, // To announce phone to ESP
    PEER_LIST // Response from ESP with list of peers
}

// Data Models
data class Peer(
    val id: String, // Device ID (UUID or WiFi MAC derived)
    val name: String, // Display Name
    val ipAddress: String, // Local IP
    val timestamp: Long = System.currentTimeMillis() // Last seen
)

data class UdpPacket(
    val msg_id: String = UUID.randomUUID().toString(),
    val type: String, // "chat", "presence", "get_clients", "clients_list"
    val from: String,
    val to: String = "BROADCAST", // "BROADCAST" or Target ID
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 5,
    val payload: String // JSON payload or Plain text
)

data class EspNode(
    val ssid: String,
    val ipAddress: String,
    val lastSeen: Long = System.currentTimeMillis()
)
