package com.example.demofirestore3.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.demofirestore3.utils.extractDateFromString
import com.example.demofirestore3.utils.uploadDataToFirestore

@Composable
fun InputForm(db: FirebaseFirestore, onUpload: () -> Unit) {
    var uid by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        TextField(value = uid, onValueChange = { uid = it }, label = { Text("UID") }, modifier = Modifier.fillMaxWidth())
        TextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        TextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Birth Date") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Button(
            onClick = {
                if (uid.isNotEmpty() && name.isNotEmpty() && birthDate.isNotEmpty()) {
                    val formattedDate = extractDateFromString(birthDate)
                    if (formattedDate != null) {
                        errorMessage = ""
                        val user = mapOf("uid" to uid, "name" to name, "birth" to formattedDate)
                        uploadDataToFirestore(db, user) {
                            uid = ""; name = ""; birthDate = ""
                            onUpload()
                        }
                    } else {
                        errorMessage = "Invalid date format!"
                    }
                } else {
                    errorMessage = "Please fill all fields."
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Upload Data to Firestore")
        }
    }
}
