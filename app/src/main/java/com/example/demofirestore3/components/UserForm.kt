package com.example.demofirestore3.components

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

fun uploadDataToFirestore(db: FirebaseFirestore, user: Map<String, Any>, onSuccess: () -> Unit) {
    db.collection("users")
        .add(user)
        .addOnSuccessListener {
            Log.d("Firestore", "Added user with ID: ${it.id}")
            onSuccess()
        }
        .addOnFailureListener {
            Log.w("Firestore", "Error adding user", it)
        }
}

suspend fun loadUsersFromFirestore(db: FirebaseFirestore, userList: MutableList<Map<String, Any>>) {
    try {
        val result = db.collection("users").get().await()
        userList.clear()
        for (doc in result) {
            userList.add(doc.data)
        }
    } catch (e: Exception) {
        Log.w("Firestore", "Error reading users", e)
    }
}

fun extractDateFromString(birthDate: String): String? {
    return try {
        val formats = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
        val date = formats.firstNotNullOfOrNull {
            try {
                SimpleDateFormat(it, Locale.US).parse(birthDate)
            } catch (_: Exception) {
                null
            }
        }
        date?.let { SimpleDateFormat("dd-MM-yyyy", Locale.US).format(it) }
    } catch (e: Exception) {
        null
    }
}
