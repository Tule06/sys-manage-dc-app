package com.example.demofirestore3.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun VaccineLookupScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val authUser = FirebaseAuth.getInstance().currentUser

    var notFound by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("Tất cả") }

    val vaccinationRecords = remember { mutableStateListOf<Map<String, Any>>() }
    val email = authUser?.email ?: ""

    LaunchedEffect(Unit) {
        isSearching = true
        try {
            val ownerSnapshot = db.collection("owners")
                .whereEqualTo("email", email)
                .get().await()

            val ownerId = ownerSnapshot.documents.firstOrNull()?.id

            if (ownerId != null) {
                val petSnapshot = db.collection("pets")
                    .whereEqualTo("ownerId", ownerId)
                    .get().await()

                val senderKeys = petSnapshot.documents.map { "$ownerId|${it.id}" }
                val petNameMap = petSnapshot.documents.associate {
                    it.id to (it.getString("name") ?: "Không rõ")
                }

                val allVaccinationsSnapshot = db.collection("vaccinations").get().await()
                val matchedVaccinations = mutableListOf<Map<String, Any>>()

                for (doc in allVaccinationsSnapshot.documents) {
                    val data = doc.data ?: continue
                    val senderToList = data["senderTo"] as? List<Map<String, Any>> ?: continue

                    for (entry in senderToList) {
                        val userId = entry["userId"] as? String ?: continue
                        val status = entry["status"] as? String ?: "Không rõ"

                        if (userId in senderKeys) {
                            val petId = userId.substringAfter("|")
                            val petName = petNameMap[petId] ?: "Không rõ"
                            val merged = data.toMutableMap()
                            merged["status"] = status
                            merged["petName"] = petName
                            matchedVaccinations.add(merged)
                            break
                        }
                    }
                }

                vaccinationRecords.clear()
                vaccinationRecords.addAll(matchedVaccinations)
                notFound = matchedVaccinations.isEmpty()
            } else {
                notFound = true
            }
        } catch (e: Exception) {
            Log.e("VaccineLookupScreen", "Lỗi truy vấn Firestore: ${e.message}")
            Toast.makeText(navController.context, "Lỗi kết nối hoặc dữ liệu!", Toast.LENGTH_SHORT).show()
            notFound = true
        }
        isSearching = false
    }

    val filteredRecords = vaccinationRecords
        .filter { record ->
            val typeMatch = record["vaccineType"].toString().contains(searchQuery, ignoreCase = true)
            val statusMatch = when (filterStatus) {
                "Tất cả" -> true
                else -> record["status"].toString().equals(filterStatus, ignoreCase = true)
            }
            typeMatch && statusMatch
        }
        .sortedByDescending { record ->
            val status = record["status"]?.toString()?.lowercase()
            if (status == "pending") 1 else 0
        }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("📋 Danh sách lịch tiêm chủng của vật nuôi", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("🔍 Tìm theo loại vaccine") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                var expanded by remember { mutableStateOf(false) }
                val options = listOf("Tất cả", "Active", "Pending")

                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = filterStatus,
                        onValueChange = {},
                        label = { Text("Lọc theo trạng thái") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    filterStatus = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (isSearching) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (notFound) {
                item {
                    Text("Không tìm thấy dữ liệu tiêm chủng", color = Color.Red)
                }
            } else {
                if (filteredRecords.isEmpty()) {
                    item {
                        Text("Không có dữ liệu khớp với bộ lọc!", color = Color.Gray)
                    }
                } else {
                    items(filteredRecords) { record ->
                        val formatISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val formatDisplay = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())

                        val timeFrom = try {
                            formatDisplay.format(formatISO.parse(record["timeFrom"] as? String ?: "")!!)
                        } catch (e: Exception) {
                            "Không rõ"
                        }

                        val timeTo = try {
                            formatDisplay.format(formatISO.parse(record["timeTo"] as? String ?: "")!!)
                        } catch (e: Exception) {
                            "Không rõ"
                        }

                        val status = record["status"] as? String ?: "Không rõ"
                        val cardColor = when (status.lowercase()) {
                            "pending" -> Color(0xFFFFEBEE)
                            "active" -> Color(0xFFE8F5E9)
                            else -> Color.LightGray
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("💉 Loại: ${record["vaccineType"] ?: "Không rõ"}", style = MaterialTheme.typography.titleMedium)
                                Text("🐾 Vật nuôi: ${record["petName"] ?: "Không rõ"}")
                                Text("📍 Địa điểm tiêm: ${record["vaccineLocation"] ?: "Không rõ"}")
                                Text("🏢 Cơ quan cấp: ${record["issuingAuthority"] ?: "Không rõ"}")
                                Text("🕒 Thời gian: $timeFrom → $timeTo")
                                Text("💵 Chi phí: ${record["cost"] ?: "Không rõ"} VND")
                                Text("📌 Trạng thái: $status")
                                Text("📝 Ghi chú: ${record["notes"] ?: "Không có"}")
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { navController.navigate("home") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quay về trang chính")
        }
    }
}
