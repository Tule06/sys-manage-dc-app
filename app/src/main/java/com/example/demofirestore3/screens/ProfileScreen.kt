package com.example.demofirestore3.screens

import android.app.DatePickerDialog
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.demofirestore3.components.OwnerViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(navController: NavHostController) {
    val authUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val viewModel: OwnerViewModel = viewModel()
    val ownerState by viewModel.ownerInfo.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedPhone by remember { mutableStateOf("") }
    var editedAddress by remember { mutableStateOf("") }
    var editedSex by remember { mutableStateOf(true) }
    var editedBirthday by remember { mutableStateOf(Date()) }
    var isLoading by remember { mutableStateOf(true) }

    val isoFormatter = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }
    val displayFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.fetchOwnerData()
        kotlinx.coroutines.delay(1500)
        isLoading = false
    }

    fun openEditDialog() {
        ownerState?.let {
            editedName = it["fullName"] as? String ?: ""
            editedPhone = it["phone"] as? String ?: ""
            editedAddress = it["address"] as? String ?: ""
            editedSex = it["sex"] as? Boolean ?: true
            editedBirthday = try {
                isoFormatter.parse(it["birthday"] as? String ?: "") ?: Date()
            } catch (_: Exception) {
                Date()
            }
            showDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Thông tin cá nhân",
                style = MaterialTheme.typography.titleLarge.copy(color = Color.Black),
                modifier = Modifier
                    .padding(vertical = 16.dp, horizontal = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                authUser?.let {
                    ProfileInfoRow(Icons.Default.Email, "Email", it.email ?: "Không rõ")
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1565C0))
                    }
                } else {
                    ownerState?.let { owner ->
                        ProfileInfoRow(Icons.Default.Person, "Họ tên", owner["fullName"])
                        ProfileInfoRow(Icons.Default.Phone, "SĐT", owner["phone"])
                        ProfileInfoRow(Icons.Default.LocationOn, "Địa chỉ", owner["address"])
                        ProfileInfoRow(Icons.Default.Badge, "CCCD", owner["cccd"])
                        ProfileInfoRow(
                            Icons.Default.Cake, "Ngày sinh",
                            try {
                                (owner["birthday"] as? String)?.let {
                                    displayFormatter.format(isoFormatter.parse(it)!!)
                                }
                            } catch (_: Exception) {
                                "Không rõ"
                            } ?: "Không rõ"
                        )
                        ProfileInfoRow(
                            Icons.Default.Transgender, "Giới tính",
                            if (owner["sex"] as? Boolean == true) "Nam" else "Nữ"
                        )
                        ProfileInfoRow(Icons.Default.Pets, "Số thú nuôi", owner["numberOfPets"])
                    } ?: Text("❌ Không tìm thấy dữ liệu chủ nuôi.", color = MaterialTheme.colorScheme.error)

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { openEditDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0053A0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("✏️ Chỉnh sửa", color = Color.White)
                    }
                }
            }
        }

        Button(
            onClick = {
                navController.navigate("home") {
                    popUpTo("profile") { inclusive = true }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0053A0)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("← Quay về trang chính", color = Color.White)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val email = authUser?.email ?: return@TextButton
                    scope.launch {
                        try {
                            val snapshot = FirebaseFirestore.getInstance()
                                .collection("owners")
                                .whereEqualTo("email", email)
                                .get()
                                .await()

                            val docId = snapshot.documents.firstOrNull()?.id
                            docId?.let {
                                val update = mapOf(
                                    "fullName" to editedName,
                                    "phone" to editedPhone,
                                    "address" to editedAddress,
                                    "sex" to editedSex,
                                    "birthday" to isoFormatter.format(editedBirthday),
                                    "lastModifiedBy" to "App",
                                    "updatedAt" to isoFormatter.format(Date())
                                )

                                FirebaseFirestore.getInstance()
                                    .collection("owners")
                                    .document(it)
                                    .update(update)
                                    .await()

                                viewModel.fetchOwnerData()
                                showDialog = false
                                Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Huỷ")
                }
            },
            title = { Text("Cập nhật thông tin") },
            text = {
                Column {
                    OutlinedTextField(value = editedName, onValueChange = { editedName = it }, label = { Text("Họ tên") })
                    OutlinedTextField(value = editedPhone, onValueChange = { editedPhone = it }, label = { Text("SĐT") })
                    OutlinedTextField(value = editedAddress, onValueChange = { editedAddress = it }, label = { Text("Địa chỉ") })

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Giới tính:")
                    Row {
                        RadioButton(selected = editedSex, onClick = { editedSex = true })
                        Text("Nam")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = !editedSex, onClick = { editedSex = false })
                        Text("Nữ")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ngày sinh:")
                    val calendar = Calendar.getInstance()
                    calendar.time = editedBirthday
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)

                    OutlinedButton(onClick = {
                        DatePickerDialog(context, { _, y, m, d ->
                            calendar.set(y, m, d)
                            editedBirthday = calendar.time
                        }, year, month, day).show()
                    }) {
                        Text(displayFormatter.format(editedBirthday))
                    }
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: Any?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF1565C0))
        Spacer(modifier = Modifier.width(12.dp))
        Text("$label: ${value ?: "Không rõ"}", style = MaterialTheme.typography.bodyMedium)
    }
}
