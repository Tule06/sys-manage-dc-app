package com.example.demofirestore3.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demofirestore3.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(true) } // true = Nam, false = Nữ
    var birthday by remember { mutableStateOf(Date()) }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val birthdayText = remember(birthday) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(birthday)
    }
    val isoFormatter = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) CircularProgressIndicator(color = Color(0xFF0053A0))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.bg_header),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp).padding(bottom = 16.dp)
            )

            Text("Đăng ký tài khoản", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Họ và tên") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Địa chỉ") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = cccd, onValueChange = { cccd = it }, label = { Text("Số CCCD") }, modifier = Modifier.fillMaxWidth())

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Giới tính:")
                RadioButton(selected = sex, onClick = { sex = true })
                Text("Nam", modifier = Modifier.padding(end = 16.dp))
                RadioButton(selected = !sex, onClick = { sex = false })
                Text("Nữ")
            }

            OutlinedTextField(
                value = birthdayText,
                onValueChange = {},
                enabled = false,
                label = { Text("Ngày sinh") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val calendar = Calendar.getInstance().apply { time = birthday }
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> birthday = Calendar.getInstance().apply { set(y, m, d) }.time },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
            )

            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Xác nhận mật khẩu") },
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (error.isNotEmpty()) {
                Text(text = error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (fullName.isBlank() || phone.isBlank() || address.isBlank() || cccd.isBlank()
                        || email.isBlank() || password.isBlank() || confirmPassword.isBlank()
                    ) {
                        error = "Vui lòng nhập đầy đủ thông tin!"
                        return@Button
                    }
                    if (!cccd.matches(Regex("^\\d{12}$"))) {
                        error = "CCCD phải gồm đúng 12 chữ số!"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        error = "Mật khẩu không khớp!"
                        return@Button
                    }

                    isLoading = true
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            val nowStr = isoFormatter.format(Date())
                            val data = hashMapOf(
                                "fullName" to fullName,
                                "sex" to sex,
                                "birthday" to isoFormatter.format(birthday),
                                "phone" to phone,
                                "address" to address,
                                "cccd" to cccd,
                                "email" to email,
                                "lastModifiedBy" to "App",
                                "createAt" to nowStr,
                                "updatedAt" to nowStr,
                                "status" to "Active"
                            )

                            db.collection("owners").document("owner_$cccd")
                                .set(data)
                                .addOnSuccessListener {
                                    isLoading = false
                                    Toast.makeText(context, "🎉 Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                    onBackToLogin()
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    error = "Không thể lưu thông tin chủ nuôi!"
                                }
                        }
                        .addOnFailureListener {
                            isLoading = false
                            error = it.message ?: "Đăng ký thất bại!"
                        }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0053A0))
            ) {
                Text("Đăng ký", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBackToLogin) {
                Text("← Quay lại đăng nhập", color = Color(0xFF0053A0))
            }
        }
    }
}
