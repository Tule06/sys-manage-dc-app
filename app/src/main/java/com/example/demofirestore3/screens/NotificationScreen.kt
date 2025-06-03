// NotificationScreen.kt (đã sửa logic cập nhật isRead đúng cho từng thú nuôi trong tiêm phòng)
package com.example.demofirestore3.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

enum class NotificationFilterOption(val label: String) {
    ALL("Tất cả các thông báo"),
    VIOLATION("Chỉ vi phạm"),
    VACCINE("Chỉ vắc xin"),
    UNREAD("Chưa đọc"),
    READ("Đã đọc")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val notifications = remember { mutableStateListOf<Map<String, Any>>() }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingCount by remember { mutableStateOf(0) }
    var selectedNoti by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf(NotificationFilterOption.ALL) }

    val isoFormat = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    fun formatDate(raw: String?): String = runCatching {
        displayFormat.format(isoFormat.parse(raw ?: "") ?: Date())
    }.getOrDefault("Không rõ")

    suspend fun loadNotifications() {
        isLoading = true
        try {
            val currentEmail = auth.currentUser?.email ?: return
            val ownerSnapshot = db.collection("owners")
                .whereEqualTo("email", currentEmail)
                .get()
                .await()
            val ownerId = ownerSnapshot.documents.firstOrNull()?.id ?: return

            notifications.clear()
            val tempList = mutableListOf<Map<String, Any>>()

            val petSnapshot = db.collection("pets")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .await()

            val petMap = petSnapshot.documents.associateBy({ it.id }, { it.getString("name") ?: "Không rõ" })

            val violationSnapshot = db.collection("violations").get().await()
            for (doc in violationSnapshot) {
                if (doc.getString("ownerId") == ownerId && doc.getString("status") == "Pending") {
                    val petId = doc.getString("petId") ?: ""
                    val petName = petMap[petId] ?: "Không rõ"

                    tempList.add(
                        mapOf(
                            "type" to "violation",
                            "description" to (doc.getString("description") ?: ""),
                            "location" to (doc.getString("violationLocation") ?: "Không rõ"),
                            "time" to (doc.getString("violationTime") ?: ""),
                            "docId" to doc.id,
                            "isRead" to (doc.getBoolean("isRead") ?: false),
                            "notes" to (doc.getString("notes") ?: ""),
                            "attachments" to (doc.get("attachments") as? List<String> ?: emptyList<String>()),
                            "petName" to petName,
                            "petId" to petId
                        )
                    )
                }
            }

            val vaccinationSnapshot = db.collection("vaccinations").get().await()
            for (doc in vaccinationSnapshot) {
                val senderToList = doc.get("senderTo") as? List<*> ?: continue
                for (entry in senderToList) {
                    val senderMap = entry as? Map<*, *> ?: continue
                    val userId = senderMap["userId"] as? String ?: continue
                    val status = senderMap["status"] as? String ?: ""
                    val isRead = senderMap["isRead"] as? Boolean ?: false

                    if (status == "Pending" && userId.startsWith(ownerId)) {
                        val petId = userId.substringAfter("|")
                        val petName = petMap[petId] ?: "Không rõ"

                        tempList.add(
                            mapOf(
                                "type" to "vaccination",
                                "vaccineType" to (doc.getString("vaccineType") ?: ""),
                                "location" to (doc.getString("vaccineLocation") ?: "Không rõ"),
                                "timeFrom" to (doc.getString("timeFrom") ?: ""),
                                "timeTo" to (doc.getString("timeTo") ?: ""),
                                "petName" to petName,
                                "cost" to (doc.get("cost") ?: "Không rõ"),
                                "notes" to (doc.getString("notes") ?: ""),
                                "isRead" to isRead,
                                "docId" to doc.id,
                                "petId" to petId,
                                "userId" to "$ownerId|$petId"
                            )
                        )
                    }
                }
            }

            notifications.addAll(tempList)
            pendingCount = tempList.count { it["isRead"] == false }
        } catch (e: Exception) {
            error = "Không thể tải thông báo. Kiểm tra kết nối mạng."
        } finally {
            isLoading = false
        }
    }
    LaunchedEffect(true) {
        scope.launch { loadNotifications() }
    }

    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = isLoading)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BadgedBox(badge = {
                            if (pendingCount > 0) Badge { Text("$pendingCount") }
                        }) {
                            Text("🔔", fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Thông báo", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Trở về")
                    }
                },
                actions = {
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Lọc thông báo")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { scope.launch { loadNotifications() } }
        ) {
            val filteredList = notifications.filter {
                when (currentFilter) {
                    NotificationFilterOption.ALL -> true
                    NotificationFilterOption.VIOLATION -> it["type"] == "violation"
                    NotificationFilterOption.VACCINE -> it["type"] == "vaccination"
                    NotificationFilterOption.UNREAD -> it["isRead"] == false
                    NotificationFilterOption.READ -> it["isRead"] == true
                }
            }.sortedBy { it["isRead"] == true }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredList) { noti ->
                    NotificationCard(
                        noti = noti,
                        formatDate = ::formatDate,
                        // 🔁 Bên trong onClick (ở LazyColumn.items):
                        onClick = {
                            selectedNoti = noti
                            scope.launch {
                                try {
                                    val currentEmail = auth.currentUser?.email ?: return@launch
                                    val ownerSnapshot = db.collection("owners")
                                        .whereEqualTo("email", currentEmail)
                                        .get()
                                        .await()
                                    val ownerId = ownerSnapshot.documents.firstOrNull()?.id ?: return@launch

                                    if (noti["type"] == "violation") {
                                        db.collection("violations")
                                            .document(noti["docId"] as String)
                                            .update("isRead", true)
                                    } else {
                                        val docId = noti["docId"] as? String ?: return@launch
                                        val petId = noti["petId"] as? String ?: return@launch
                                        val expectedUserId = "$ownerId|$petId"

                                        val doc = db.collection("vaccinations")
                                            .document(docId)
                                            .get()
                                            .await()

                                        val senderToList = doc.get("senderTo") as? List<*> ?: return@launch
                                        val updatedSenderTo = senderToList.mapNotNull { entry ->
                                            val sender = (entry as? Map<*, *>)?.toMutableMap() ?: return@mapNotNull null
                                            val uid = sender["userId"] as? String ?: return@mapNotNull null

                                            if (uid == expectedUserId) {
                                                sender["isRead"] = true
                                            }

                                            sender.mapKeys { it.key.toString() } as Map<String, Any>
                                        }

                                        db.collection("vaccinations")
                                            .document(docId)
                                            .update("senderTo", updatedSenderTo)
                                    }

                                    // ✅ Cập nhật lại trạng thái trong danh sách hiển thị
                                    val index = notifications.indexOf(noti)
                                    if (index != -1) {
                                        val updated = noti.toMutableMap()
                                        updated["isRead"] = true
                                        notifications[index] = updated
                                    }
                                } catch (e: Exception) {
                                    error = "Lỗi cập nhật trạng thái đã xem"
                                }
                            }
                        }
                    )
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Bộ lọc thông báo", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    NotificationFilterOption.values().forEach { option ->
                        ListItem(
                            headlineContent = { Text(option.label) },
                            modifier = Modifier.clickable {
                                currentFilter = option
                                showBottomSheet = false
                            }
                        )
                    }
                }
            }
        }
        selectedNoti?.let { noti ->
            AlertDialog(
                onDismissRequest = { selectedNoti = null },
                confirmButton = {
                    Row(Modifier.fillMaxWidth(), Arrangement.End) {
                        TextButton(onClick = { selectedNoti = null }) { Text("Đóng") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = {
                            val route = if (noti["type"] == "violation") "Violation" else "Vaccine"
                            navController.navigate(route)
                            selectedNoti = null
                        }) { Text("Tra cứu thêm") }
                    }
                },
                title = {
                    Text(if (noti["type"] == "violation") "Chi tiết vi phạm" else "Thông tin vắc xin")
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (noti["type"] == "violation") {
                            val attachments = noti["attachments"] as? List<*> ?: emptyList<String>()
                            if (attachments.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(attachments.filterIsInstance<String>()) { url ->
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Ảnh vi phạm",
                                            modifier = Modifier
                                                .size(180.dp)
                                                .clickable { }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            val petName = noti["petName"] as? String ?: "Không rõ"
                            val description = noti["description"] as? String ?: ""
                            val time = noti["time"] as? String ?: ""
                            val notes = noti["notes"] as? String ?: ""
                            val location = noti["location"] as? String ?: ""

                            Text("🐾 Vật nuôi: $petName")
                            Text("⚠️ Mô tả: $description", style = MaterialTheme.typography.titleMedium)
                            Text("📅 Ngày vi phạm: ${if (time.isNotBlank()) formatDate(time) else ""}")
                            Text("📝 Ghi chú: $notes")
                            Text("📍 Địa điểm: $location")
                        } else {
                            val timeFrom = noti["timeFrom"] as? String ?: ""
                            val timeTo = noti["timeTo"] as? String ?: ""
                            val formattedFrom = try { formatDate(timeFrom) } catch (e: Exception) { "Không rõ" }
                            val formattedTo = try { formatDate(timeTo) } catch (e: Exception) { "Không rõ" }

                            Text("💉 Loại: ${noti["vaccineType"] ?: "Không rõ"}", style = MaterialTheme.typography.titleMedium)
                            Text("🐾 Vật nuôi: ${noti["petName"] ?: "Không rõ"}")
                            Text("📍 Địa điểm tiêm: ${noti["location"] ?: "Không rõ"}")
                            Text("🕒 Thời gian: $formattedFrom → $formattedTo")
                            Text("💵 Chi phí: ${noti["cost"] ?: "Không rõ"} VND")
                            Text("📝 Ghi chú: ${noti["notes"] ?: ""}")
                        }
                    }
                }
            )
        }
        error?.let {
            LaunchedEffect(it) {
                snackbarHostState.showSnackbar(it)
            }
        }
    }
}

@Composable
fun NotificationCard(
    noti: Map<String, Any>,
    formatDate: (String?) -> String,
    onClick: () -> Unit
) {
    val isViolation = noti["type"] == "violation"
    val isRead = noti["isRead"] == true
    val bgColor = when {
        isViolation && isRead -> Color(0xFFE0E0E0)
        isViolation -> Color(0xFFFFF3E0)
        !isViolation && isRead -> Color(0xFFE0E0E0)
        else -> Color(0xFFE3F2FD)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isViolation) {
                Text("🚫 Vi phạm: ${noti["description"]}", fontWeight = FontWeight.Bold)
                Text("📍 Địa điểm: ${noti["location"]}")
                Text("📅 Ngày: ${formatDate(noti["time"] as? String)}")
            } else {
                Text("💉 Vắc xin: ${noti["vaccineType"]}", fontWeight = FontWeight.Bold)
                Text("📍 Địa điểm: ${noti["location"]}")
                Text("📅 Từ: ${formatDate(noti["timeFrom"] as? String)}")
                Text("📅 Đến: ${formatDate(noti["timeTo"] as? String)}")
            }
        }
    }
}