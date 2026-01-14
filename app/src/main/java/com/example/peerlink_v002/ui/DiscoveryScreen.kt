package com.example.peerlink_v002.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.peerlink_v002.mesh.MeshState

@Composable
fun DiscoveryScreen(
    state: MeshState,
    onStartDiscovery: () -> Unit,
    onNavigateToDeviceList: () -> Unit
) {
    LaunchedEffect(state.isConnectedToEsp) {
        if (state.isConnectedToEsp) {
            onNavigateToDeviceList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DISCOVERY MODE",
                color = Color.White,
                fontSize = 24.sp,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            // Pulse animation or static for now
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
               // Show loading only when scanning
               if (state.isDiscoverying) {
                   CircularProgressIndicator(
                       color = Color.White,
                       trackColor = Color.DarkGray,
                       modifier = Modifier.fillMaxSize()
                   )
               } else {
                   // Static or icon when idle
                   Text(
                       text = "READY",
                       color = Color.DarkGray,
                       fontSize = 20.sp,
                       letterSpacing = 2.sp
                   )
               }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onStartDiscovery,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp)
            ) {
                Text("DISCOVER DEVICES")
            }
        }
    }
}
