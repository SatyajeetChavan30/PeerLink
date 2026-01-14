package com.example.peerlink_v002.network

import android.util.Log
import com.example.peerlink_v002.utils.UdpPacket
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class UdpChat(
    private val myDeviceId: String
) {
    private val gson = Gson()
    private val PORT = 8888
    private var socket: DatagramSocket? = null
    private var isRunning = false
    
    private val _incomingMessages = MutableSharedFlow<UdpPacket>(replay = 0)
    val incomingMessages = _incomingMessages.asSharedFlow()

    suspend fun startListening() = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext
        isRunning = true
        
        try {
            // Bind to all interfaces or specific port
            socket = DatagramSocket(null)
            socket?.reuseAddress = true
            socket?.bind(InetSocketAddress(PORT))
            
            Log.d("UdpChat", "UDP Listening on port $PORT")

            val buffer = ByteArray(4096)
            while (isRunning) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket?.receive(packet)
                
                val json = String(packet.data, 0, packet.length)
                try {
                    val udpPacket = gson.fromJson(json, UdpPacket::class.java)
                    if (udpPacket.from != myDeviceId) { // Ignore own messages if looped back
                        _incomingMessages.emit(udpPacket)
                    }
                } catch (e: Exception) {
                    Log.e("UdpChat", "Parse error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("UdpChat", "Socket error: ${e.message}")
        } finally {
            close()
        }
    }

    suspend fun sendMessage(packet: UdpPacket, destinationIp: String = "192.168.4.1") = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(packet)
            val data = json.toByteArray()
            val addr = InetAddress.getByName(destinationIp)
            val dPacket = DatagramPacket(data, data.size, addr, PORT)
            
            // If socket isn't open for listening, open a transient one if needed? 
            // Better to use the listening socket for sending to keep port consistent if NAT traversal needed (though not needed here)
            if (socket == null || socket?.isClosed == true) {
                 socket = DatagramSocket(null)
                 socket?.reuseAddress = true
                 socket?.bind(InetSocketAddress(PORT))
            }
            
            socket?.send(dPacket)
            Log.d("UdpChat", "Sent: $json to $destinationIp")
        } catch (e: Exception) {
            Log.e("UdpChat", "Send error: ${e.message}")
        }
    }

    fun close() {
        isRunning = false
        socket?.close()
        socket = null
    }
}
