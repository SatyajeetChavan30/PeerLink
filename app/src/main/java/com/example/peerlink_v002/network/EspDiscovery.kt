package com.example.peerlink_v002.network

import com.example.peerlink_v002.utils.EspNode
import com.example.peerlink_v002.utils.MessageType
import com.example.peerlink_v002.utils.UdpPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class EspDiscovery {
    
    fun listenForRelays(incomingPackets: Flow<UdpPacket>): Flow<EspNode> {
        return incomingPackets
            .filter { it.type == "presence" || it.type == "PRESENCE" } // Case insensitive check
            .map { packet ->
                // Assuming payload contains SSID or identifying info, or just use sender
                // Packet from ESP: from = "ESP_ID", payload = "SSID"
                EspNode(
                    ssid = packet.payload, // ESP sends SSID in payload
                    ipAddress = "192.168.4.1", // Standard ESP AP IP, or extract from packet meta if possible
                    lastSeen = System.currentTimeMillis()
                )
            }
    }
}
