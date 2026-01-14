package com.example.peerlink_v002.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.peerlink_v002.data.MessageEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerName: String,
    messages: List<MessageEntity>,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    var textState by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(peerName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                reverseLayout = true // Show newest at bottom (if list is reversed) or normal
            ) {
                // Assuming messages are newest last. If reverseLayout=true, list should be newest first.
                // Let's assume list is chronological (Oldest -> Newest).
                // So normal layout.
                items(messages) { msg ->
                    ChatBubble(msg)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, Color.White)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (textState.isEmpty()) {
                            Text("Type a message...", color = Color.Gray)
                        }
                        innerTextField()
                    }
                )
                
                IconButton(onClick = {
                    if (textState.isNotBlank()) {
                        onSendMessage(textState)
                        textState = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: MessageEntity) {
    val isMe = !message.isIncoming
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(if (isMe) Color.White else Color.DarkGray)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (isMe) Color.Black else Color.White
            )
        }
        Text(
            text = java.text.SimpleDateFormat("HH:mm").format(java.util.Date(message.timestamp)),
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
