package com.example.demofirestore3.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun FindUidScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var uid by remember { mutableStateOf("") }
    var petData = remember { mutableStateOf<Map<String, Any>?>(null) }
    var ownerData = remember { mutableStateOf<Map<String, Any>?>(null) }
    var notFound by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        OutlinedTextField(
            value = uid,
            onValueChange = {
                uid = it
                notFound = false
            },
            label = { Text("Nhập tên vật nuôi") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    try {
                        val userEmail = auth.currentUser?.email
                        if (userEmail != null) {
                            val ownerSnapshot = db.collection("owners")
                                .whereEqualTo("email", userEmail)
                                .get()
                                .await()

                            if (ownerSnapshot.documents.isNotEmpty()) {
                                val ownerId = ownerSnapshot.documents[0].id

                                val petDoc = db.collection("pets")
                                    .whereEqualTo("ownerId", ownerId)
                                    .whereEqualTo("name", uid)
                                    .get()
                                    .await()

                                if (petDoc.documents.isNotEmpty()) {
                                    petData.value = petDoc.documents[0].data
                                    ownerData.value = ownerSnapshot.documents[0].data
                                    notFound = false
                                } else {
                                    petData.value = null
                                    ownerData.value = null
                                    notFound = true
                                }
                            } else {
                                petData.value = null
                                ownerData.value = null
                                notFound = true
                            }
                        }
                    } catch (e: Exception) {
                        petData.value = null
                        ownerData.value = null
                        notFound = true
                    }
                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.End)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đang tra cứu...", color = Color.White)
            } else {
                Text("🔍 Tra cứu")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        petData.value?.let { pet ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🐾 Thông tin thú nuôi", style = MaterialTheme.typography.titleMedium)

                    InfoRow(Icons.Default.Pets, "Tên", pet["name"])
                    InfoRow(Icons.Default.Cake, "Tuổi", pet["age"])
                    InfoRow(Icons.Default.Pets, "Loài", pet["breed"])
                    InfoRow(Icons.Default.Pets, "Giống loài", pet["species"])
                    InfoRow(Icons.Default.Transgender, "Giới tính", pet["gender"])
                    InfoRow(Icons.Default.CheckCircle, "Trạng thái", pet["status"])
                    InfoRow(Icons.Default.Vaccines, "Tiêm chủng", pet["vaccinationStatus"])
                    InfoRow(Icons.Default.Warning, "Vi phạm", pet["violationStatus"])
                    // Đã bỏ dòng: InfoRow(Icons.Default.Favorite, "Sức khỏe", pet["healthStatus"])
                }
            }
        }

        ownerData.value?.let { owner ->
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👤 Thông tin chủ nuôi", style = MaterialTheme.typography.titleMedium)

                    InfoRow(Icons.Default.Person, "Họ tên", owner["fullName"])
                    InfoRow(Icons.Default.Email, "Email", owner["email"])
                    InfoRow(Icons.Default.LocationOn, "Địa chỉ", owner["address"])
                    InfoRow(Icons.Default.Phone, "SĐT", owner["phone"])
                    InfoRow(Icons.Default.Pets, "Số thú nuôi", owner["numberOfPets"])
                }
            }
        }

        if (notFound && uid.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("⚠ Không tìm thấy vật nuôi này!", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { navController.navigate("home") }) {
            Text("← Quay về trang chính")
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: Any?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF1565C0))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "$label: ${value ?: "Không có"}",
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2
        )
    }
}