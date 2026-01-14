package com.example.peerlink_v002.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.peerlink_v002.R
import com.example.peerlink_v002.data.AppDatabase
import com.example.peerlink_v002.data.MessageEntity
import com.example.peerlink_v002.network.EspConnector
import com.example.peerlink_v002.network.EspDiscovery
import com.example.peerlink_v002.network.UdpChat
import com.example.peerlink_v002.utils.EspNode
import com.example.peerlink_v002.utils.MessageType
import com.example.peerlink_v002.utils.Peer
import com.example.peerlink_v002.utils.UdpPacket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class MeshService : Service() {

    private val binder = MeshBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Dependencies
    private lateinit var espConnector: EspConnector
    private lateinit var udpChat: UdpChat
    private lateinit var database: AppDatabase
    private val espDiscovery = EspDiscovery()

    // State
    private val _serviceState = MutableStateFlow(MeshState())
    val serviceState = _serviceState.asStateFlow()

    private var myDeviceId: String = UUID.randomUUID().toString() // Persistent ID needed in production

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        
        espConnector = EspConnector(this)
        udpChat = UdpChat(myDeviceId)
        database = AppDatabase.getDatabase(this)
        
        observeNetwork()
    }
    
    private fun startForegroundService() {
        val channelId = "PeerLinkServiceChannel"
        val channelName = "PeerLink Mesh Service"
        
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("PeerLink Active")
            .setContentText("Listening for mesh messages...")
            .setSmallIcon(R.mipmap.ic_launcher) // Assuming default icon exists
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun observeNetwork() {
        serviceScope.launch {
            // Listen for UDP packets
            udpChat.incomingMessages.collect { packet ->
                handlePacket(packet)
            }
        }

        serviceScope.launch {
            // Listen for ESP Discovery
            espDiscovery.listenForRelays(udpChat.incomingMessages).collect { esp ->
                _serviceState.update { it.copy(connectedEsp = esp) }
                // Once we see an ESP, ask for clients
                requestPeerList()
            }
        }
    }

    private fun handlePacket(packet: UdpPacket) {
        serviceScope.launch {
            when (packet.type) {
                "chat", "CHAT" -> {
                    // Save to DB
                    val msg = MessageEntity(
                        id = packet.msg_id,
                        senderId = packet.from,
                        receiverId = myDeviceId,
                        content = packet.payload,
                        timestamp = packet.timestamp,
                        isIncoming = true
                    )
                    database.messageDao().insertMessage(msg)
                }
                "clients_list" -> {
                    // Parse payload to update peers list
                    // Payload format: "id,name;id,name"
                    val peers = packet.payload.split(";").mapNotNull { 
                        val parts = it.split(",")
                        if (parts.size >= 2) Peer(parts[0], parts[1], "") else null
                    }
                    _serviceState.update { it.copy(peers = peers) }
                }
            }
        }
    }

    // --- Public Actions ---

    fun startDiscovery() {
        _serviceState.update { it.copy(isDiscoverying = true) }
        serviceScope.launch {
            espConnector.connectToEspNetwork().collect { connected ->
                _serviceState.update { 
                    it.copy(
                        isConnectedToEsp = connected, 
                        isDiscoverying = false // Stop discovery animation on any result (success or failure/cancel)
                    ) 
                }
                if (connected) {
                    _serviceState.update { it.copy(isDiscoverying = false) }
                    udpChat.startListening()
                    // Send Presence
                    announcePresence()
                } else {
                    udpChat.close()
                }
            }
        }
    }

    fun sendMessage(toPeerId: String, content: String) {
        serviceScope.launch {
            val packet = UdpPacket(
                type = "chat",
                from = myDeviceId,
                to = toPeerId,
                payload = content
            )
            // Save local
            val localMsg = MessageEntity(
                id = packet.msg_id,
                senderId = myDeviceId,
                receiverId = toPeerId,
                content = content,
                timestamp = packet.timestamp,
                isIncoming = false
            )
            database.messageDao().insertMessage(localMsg)
            
            // Send network
            udpChat.sendMessage(packet)
        }
    }
    
    fun announcePresence() {
        serviceScope.launch {
            val packet = UdpPacket(
                type = "presence",
                from = myDeviceId,
                to = "BROADCAST",
                payload = android.os.Build.MODEL // Send Device Name
            )
            udpChat.sendMessage(packet)
            requestPeerList()
        }
    }
    
    fun requestPeerList() {
        serviceScope.launch {
            val packet = UdpPacket(
                type = "get_clients",
                from = myDeviceId,
                to = "BROADCAST",
                payload = ""
            )
            udpChat.sendMessage(packet)
        }
    }
    
    fun getMyDeviceId(): String = myDeviceId
    
    fun disconnect() {
        espConnector.disconnect()
        udpChat.close()
        _serviceState.update { it.copy(isConnectedToEsp = false, isDiscoverying = false) }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class MeshBinder : Binder() {
        fun getService(): MeshService = this@MeshService
    }
}

data class MeshState(
    val isConnectedToEsp: Boolean = false,
    val isDiscoverying: Boolean = false,
    val connectedEsp: EspNode? = null,
    val peers: List<Peer> = emptyList()
)
