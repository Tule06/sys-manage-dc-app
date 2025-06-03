package com.example.demofirestore3.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserListScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val authUser = FirebaseAuth.getInstance().currentUser
    val petData = remember { mutableStateOf<Map<String, Any>?>(null) }
    val healthRecords = remember { mutableStateListOf<Map<String, String>>() }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var notFound by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    val email = authUser?.email ?: ""

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; notFound = false },
                label = { Text("Nhập tên vật nuôi") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Button(
                onClick = {
                    if (searchQuery.isNotEmpty()) {
                        isSearching = true
                        notFound = false
                        scope.launch {
                            try {
                                val ownerSnapshot = db.collection("owners")
                                    .whereEqualTo("email", email)
                                    .get().await()

                                val ownerId = ownerSnapshot.documents.firstOrNull()?.id

                                if (ownerId != null) {
                                    val petSnapshot = db.collection("pets")
                                        .whereEqualTo("name", searchQuery)
                                        .whereEqualTo("ownerId", ownerId)
                                        .get().await()

                                    if (petSnapshot.documents.isNotEmpty()) {
                                        val petDoc = petSnapshot.documents[0]
                                        petData.value = petDoc.data

                                        healthRecords.clear()
                                        val record = mapOf(
                                            "healthStatus" to (petDoc.getString("healthStatus") ?: "Không rõ"),
                                            "lastCheckHealthDate" to (petDoc.getString("lastCheckHealthDate") ?: "Không rõ")
                                        )
                                        healthRecords.add(record)
                                    } else {
                                        petData.value = null
                                        healthRecords.clear()
                                        notFound = true
                                    }
                                } else {
                                    notFound = true
                                }
                            } catch (e: Exception) {
                                Log.e("UserListScreen", "Lỗi truy vấn Firestore: ${e.message}")
                                Toast.makeText(navController.context, "Lỗi kết nối hoặc dữ liệu!", Toast.LENGTH_SHORT).show()
                                notFound = true
                            }
                            isSearching = false
                        }
                    } else {
                        Toast.makeText(navController.context, "Vui lòng nhập tên vật nuôi!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tra cứu")
            }
        }

        item {
            Button(
                onClick = { navController.navigate("home") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("← Quay về trang chính")
            }
        }

        if (isSearching) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (petData.value != null && !notFound) {
            val pet = petData.value!!
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🐾 Thông tin thú nuôi", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tên: ${pet["name"] ?: "Không rõ"}")
                        Text("Tuổi: ${pet["age"] ?: "Không rõ"}")
                        Text("Loài: ${pet["species"] ?: "Không rõ"}")
                        Text("Giới tính: ${pet["gender"] ?: "Không rõ"}")
                    }
                }
            }

            if (healthRecords.isNotEmpty()) {
                val record = healthRecords.first()
                val rawDate = record["lastCheckHealthDate"] ?: "Không rõ"
                val formattedDate = try {
                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val formatter = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
                    val date = parser.parse(rawDate)
                    if (date != null) formatter.format(date) else "Không rõ"
                } catch (e: Exception) {
                    "Không rõ"
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📋 Tình trạng sức khỏe", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tình trạng: ${record["healthStatus"]}")
                            Text("Ngày khám gần nhất: $formattedDate")
                        }
                    }
                }
            }
        }

        if (notFound) {
            item {
                Text(
                    "❌ Không tìm thấy vật nuôi với tên này",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
