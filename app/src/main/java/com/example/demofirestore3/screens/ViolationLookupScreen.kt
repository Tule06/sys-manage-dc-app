package com.example.demofirestore3.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ViolationLookupScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val authUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var allRecords = remember { mutableStateListOf<Map<String, Any>>() }
    var loadError by remember { mutableStateOf<String?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var showPending by remember { mutableStateOf(true) }
    var showActive by remember { mutableStateOf(true) }

    val filteredRecords by remember {
        derivedStateOf {
            allRecords
                .filter {
                    val status = it["status"].toString().lowercase()
                    (showPending && status == "pending") || (showActive && status == "active")
                }
                .sortedWith(compareBy(
                    { it["status"].toString().lowercase() != "pending" },
                    {
                        val raw = it["violationTime"]
                        when (raw) {
                            is com.google.firebase.Timestamp -> -raw.seconds
                            is String -> {
                                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                val parsed = try { format.parse(raw) } catch (_: Exception) { null }
                                -(parsed?.time ?: 0L)
                            }
                            else -> 0L
                        }
                    }
                ))
        }
    }
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val email = authUser?.email ?: return@launch
                val ownerSnapshot = db.collection("owners").whereEqualTo("email", email).get().await()
                val ownerId = ownerSnapshot.documents.firstOrNull()?.id ?: return@launch

                val petSnapshot = db.collection("pets").whereEqualTo("ownerId", ownerId).get().await()
                val petMap = petSnapshot.documents.associateBy(
                    { it.id }, { it.getString("name") ?: "Không rõ" }
                )

                val violationsSnapshot = db.collection("violations")
                    .whereEqualTo("ownerId", ownerId)
                    .orderBy("violationTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()

                allRecords.clear()
                allRecords.addAll(violationsSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val petId = data["petId"] as? String
                    if (petId != null) {
                        data["petName"] = petMap[petId] ?: "Không rõ"
                    }
                    data
                })

            } catch (e: Exception) {
                Log.e("ViolationLookup", "Lỗi: ${e.message}")
                loadError = "Đã xảy ra lỗi khi tải dữ liệu."
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("📋 Danh sách vi phạm vật nuôi", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { showFilterDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Lọc")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                loadError != null -> Text(loadError!!, color = Color.Red)
                filteredRecords.isEmpty() -> Text("✅ Không có vi phạm nào khớp bộ lọc.")
                else -> ViolationList(filteredRecords) { url ->
                    selectedImageUrl = url
                    showDialog = true
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = { navController.navigate("home") }, modifier = Modifier.fillMaxWidth()) {
            Text("← Quay về trang chính")
        }
    }

    if (showDialog && selectedImageUrl != null) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                selectedImageUrl = null
            },
            confirmButton = {},
            text = {
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Ảnh phóng to",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                )
            },
            shape = RoundedCornerShape(8.dp)
        )
    }

    FilterDialog(
        show = showFilterDialog,
        showPending = showPending,
        showActive = showActive,
        onDismiss = { showFilterDialog = false },
        onUpdate = { pending, active ->
            showPending = pending
            showActive = active
        }
    )
}

@Composable
fun ViolationList(records: List<Map<String, Any>>, onImageClick: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(records) { record ->
            val formatISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatDisplay = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())

            val date = try {
                formatDisplay.format(formatISO.parse(record["violationTime"] as? String ?: "")!!)
            } catch (e: Exception) {
                "Không rõ"
            }

            val status = record["status"].toString().lowercase()
            val cardColor = when (status) {
                "active" -> Color(0xFFC8E6C9)
                "pending" -> Color(0xFFFFCDD2)
                else -> Color.White
            }

            val attachments = (record["attachments"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val petName = record["petName"] as? String ?: "Không rõ"

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (attachments.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(attachments) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Ảnh vi phạm",
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clickable { onImageClick(url) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text("🐾 Vật nuôi: $petName")
                    Text("⚠️ Mô tả: ${record["description"] ?: "-"}", style = MaterialTheme.typography.titleMedium)
                    Text("📅 Ngày vi phạm: $date")
                    Text("📝 Ghi chú: ${record["notes"] ?: "-"}")
                    Text("📍 Địa điểm: ${record["violationLocation"] ?: "-"}")
                    Text("🔖 Trạng thái: ${record["status"] ?: "-"}")
                }
            }
        }
    }
}

@Composable
fun FilterDialog(
    show: Boolean,
    showPending: Boolean,
    showActive: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (Boolean, Boolean) -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Đóng") }
            },
            title = { Text("⚙️ Lọc theo trạng thái") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showPending, onCheckedChange = { onUpdate(it, showActive) })
                        Text("Hiện vi phạm đang chờ xử lý (Pending)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = showActive, onCheckedChange = { onUpdate(showPending, it) })
                        Text("Hiện vi phạm đã xử lý (Active)")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }
}
