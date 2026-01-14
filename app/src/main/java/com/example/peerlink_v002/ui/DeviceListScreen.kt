package com.example.peerlink_v002.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.peerlink_v002.mesh.MeshState
import com.example.peerlink_v002.utils.Peer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    state: MeshState,
    onRefresh: () -> Unit,
    onPeerSelected: (Peer) -> Unit,
    onDisconnect: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CONNECTED DEVICES", fontSize = 16.sp, letterSpacing = 1.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // Status Header
            Container(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier
                        .size(10.dp)
                        .background(if (state.isConnectedToEsp) Color.Green else Color.Red))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isConnectedToEsp) "RELAY ONLINE: ${state.connectedEsp?.ssid ?: "Unknown"}" else "DISCONNECTED",
                        color = Color.White
                    )
                }
            }
            
            HorizontalDivider(color = Color.White)

            // Peers List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (state.peers.isEmpty()) {
                    item {
                        Text(
                            text = "No peers found. Pull to refresh.",
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                }
                
                items(state.peers) { peer ->
                    PeerItem(peer = peer, onClick = { onPeerSelected(peer) })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onDisconnect,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("DISCONNECT / SEARCH AGAIN")
            }
        }
    }
}

@Composable
fun Container(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier) { content() }
}

@Composable
fun PeerItem(peer: Peer, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = peer.name, color = Color.White, fontSize = 18.sp)
            Text(text = "ID: ${peer.id.take(8)}...", color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "ONLINE", color = Color.Green, fontSize = 12.sp)
    }
}

// fun Modifier.border removed - using standard import
