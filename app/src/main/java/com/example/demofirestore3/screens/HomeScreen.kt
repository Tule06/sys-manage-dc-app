package com.example.demofirestore3.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demofirestore3.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onNavigate: (String) -> Unit, onLogout: () -> Unit) {
    val menuItems = listOf(
        Triple("Thông tin cá nhân", "profile", Icons.Default.AccountCircle),
        Triple("Tra cứu thông tin vật nuôi", "find_uid", Icons.Default.Search),
        Triple("Tình trạng sức khỏe vật nuôi", "user_list", Icons.Default.Favorite),
        Triple("Tình trạng tiêm phòng", "Vaccine", Icons.Default.PersonAdd),
        Triple("Tra cứu vi phạm vật nuôi", "Violation", Icons.Default.Pets)
    )

    var profileMenuExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchDropdownExpanded by remember { mutableStateOf(false) }

    val unreadCount = remember { mutableStateOf(0) }

    LaunchedEffect(true) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val email = auth.currentUser?.email

        if (email != null) {
            try {
                val ownerSnapshot = db.collection("owners")
                    .whereEqualTo("email", email)
                    .get()
                    .await()

                val ownerId = ownerSnapshot.documents.firstOrNull()?.id ?: return@LaunchedEffect

                var hasPending = false

                // 🔴 Kiểm tra violations có Pending không
                val violationsSnapshot = db.collection("violations")
                    .whereEqualTo("ownerId", ownerId)
                    .whereEqualTo("status", "Pending")
                    .get()
                    .await()

                if (!violationsSnapshot.isEmpty) {
                    hasPending = true
                }

                // 🟠 Nếu chưa có, kiểm tra vaccinations senderTo[] có Pending không
                if (!hasPending) {
                    val vaccinationsSnapshot = db.collection("vaccinations")
                        .get()
                        .await()

                    hasPending = vaccinationsSnapshot.documents.any { doc ->
                        val senderToList = doc.get("senderTo") as? List<*> ?: return@any false
                        senderToList.any { entry ->
                            val sender = entry as? Map<*, *> ?: return@any false
                            val userId = sender["userId"] as? String ?: return@any false
                            val status = sender["status"] as? String ?: return@any false
                            userId.startsWith(ownerId) && status == "Pending"
                        }
                    }
                }

                unreadCount.value = if (hasPending) 1 else 0

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 🔼 Header: Chào + Tìm kiếm + Chuông + Avatar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Xin chào, bạn 👋",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            searchDropdownExpanded = it.isNotEmpty()
                        },
                        placeholder = { Text("Tìm kiếm...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp)
                    )

                    DropdownMenu(
                        expanded = searchDropdownExpanded,
                        onDismissRequest = { searchDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        menuItems.filter {
                            it.first.contains(searchQuery, ignoreCase = true)
                        }.forEach { (label, route, _) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    searchDropdownExpanded = false
                                    searchQuery = ""
                                    onNavigate(route)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 🔔 Chuông có badge custom
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = { onNavigate("notification_screen") }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Thông báo",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        if (unreadCount.value > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = (-2).dp, y = 2.dp)
                                    .background(Color.Red, shape = CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 👤 Tài khoản
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            IconButton(onClick = { profileMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Tài khoản",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = profileMenuExpanded,
                            onDismissRequest = { profileMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Thông tin cá nhân") },
                                onClick = {
                                    profileMenuExpanded = false
                                    onNavigate("profile")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tra cứu") },
                                onClick = {
                                    profileMenuExpanded = false
                                    onNavigate("find_uid")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Đăng xuất") },
                                onClick = {
                                    profileMenuExpanded = false
                                    onLogout()
                                }
                            )
                        }
                    }
                }
            }
        }

        // 🖼️ Hình ảnh minh họa
        Image(
            painter = painterResource(id = R.drawable.bg_header),
            contentDescription = "Pet Illustration",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(horizontal = 16.dp),
            contentScale = ContentScale.Fit
        )

        // 🔽 Menu chức năng
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            val buttons = menuItems.drop(1)
            buttons.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { (label, route, icon) ->
                        Card(
                            modifier = Modifier
                                .width(170.dp)
                                .height(140.dp)
                                .padding(8.dp),
                            onClick = { onNavigate(route) },
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0053A0)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
