package com.example.peerlink_v002

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.peerlink_v002.data.AppDatabase
import com.example.peerlink_v002.data.MessageEntity
import com.example.peerlink_v002.mesh.MeshService
import com.example.peerlink_v002.mesh.MeshState
import com.example.peerlink_v002.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var meshServiceIntent: Intent
    private var meshService: MeshService? = null
    private var isBound = false
    
    private val mainViewModel by lazy { MainViewModel() }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MeshService.MeshBinder
            meshService = binder.getService()
            mainViewModel.setService(binder.getService())
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start Service
        meshServiceIntent = Intent(this, MeshService::class.java)
        startService(meshServiceIntent) // Start FG Service
        bindService(meshServiceIntent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            PeerLinkTheme {
                AppNavigation(mainViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}

class MainViewModel : ViewModel() {
    private val _meshState = MutableStateFlow(MeshState())
    val meshState = _meshState.asStateFlow()

    private var meshService: MeshService? = null
    
    // Chat messages cache or Flow from Room
    private val _currentChatMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentChatMessages = _currentChatMessages.asStateFlow()

    fun setService(service: MeshService) {
        meshService = service
        viewModelScope.launch {
            service.serviceState.collect { state ->
                _meshState.value = state
            }
        }
    }

    fun startDiscovery() {
        meshService?.startDiscovery()
    }
    
    fun disconnect() {
        meshService?.disconnect()
    }
    
    fun sendMessage(peerId: String, content: String) {
        meshService?.sendMessage(peerId, content)
    }

    fun loadMessagesForPeer(context: Context, peerId: String) {
        // Basic implementation: Should ideally be in Repository or directly use DAO flow
        val db = AppDatabase.getDatabase(context)
        viewModelScope.launch {
            db.messageDao().getMessagesForPeer(peerId).collect {
                _currentChatMessages.value = it
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val meshState by viewModel.meshState.collectAsState()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onTimeout = {
                navController.navigate("permissions") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        
        composable("permissions") {
            PermissionScreen(onPermissionsGranted = {
                navController.navigate("discovery") {
                    popUpTo("permissions") { inclusive = true }
                }
            })
        }
        
        composable("discovery") {
            DiscoveryScreen(
                state = meshState,
                onStartDiscovery = { viewModel.startDiscovery() },
                onNavigateToDeviceList = { // Triggered by state change or manual
                     navController.navigate("device_list")
                }
            )
        }
        
        composable("device_list") {
            DeviceListScreen(
                state = meshState,
                onRefresh = { viewModel.startDiscovery() }, // Re-triggering discovery acts as refresh
                onDisconnect = {
                    viewModel.disconnect()
                    navController.popBackStack("discovery", false)
                },
                onPeerSelected = { peer ->
                    navController.navigate("chat/${peer.id}/${peer.name}")
                }
            )
        }
        
        composable(
            "chat/{peerId}/{peerName}",
            arguments = listOf(
                navArgument("peerId") { type = NavType.StringType },
                navArgument("peerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
            val peerName = backStackEntry.arguments?.getString("peerName") ?: "Unknown"

            // Load messages
            LaunchedEffect(peerId) {
                viewModel.loadMessagesForPeer(context, peerId)
            }
            
            val messages by viewModel.currentChatMessages.collectAsState()

            ChatScreen(
                peerName = peerName,
                messages = messages,
                onSendMessage = { content ->
                    viewModel.sendMessage(peerId, content)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}