package com.example.demofirestore3.utils


import android.util.Log
import androidx.compose.runtime.MutableState
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
suspend fun loadUsersByUid(db: FirebaseFirestore, userList: MutableList<Map<String, Any>>, uid: String) {
    try {
        val result = db.collection("users")
            .whereEqualTo("uid", uid)
            .get()
            .await()

        userList.clear()
        for (doc in result) {
            userList.add(doc.data)
        }
    } catch (e: Exception) {
        Log.w("Firestore", "Error querying users by uid", e)
    }
}
suspend fun loadPetsFromFirestore(db: FirebaseFirestore, petList: MutableList<Map<String, Any>>) {
    try {
        val result = db.collection("pets").get().await()
        petList.clear()
        for (doc in result) {
            petList.add(doc.data)
        }
    } catch (e: Exception) {
        Log.w("Firestore", "Error reading pets", e)
    }
}
suspend fun loadPetById(
    db: FirebaseFirestore,
    uid: String,
    result: MutableState<Map<String, Any>?>
) {
    try {
        val doc = db.collection("pets").document(uid).get().await()
        if (doc.exists()) {
            result.value = doc.data
        } else {
            result.value = null // Không tìm thấy
        }
    } catch (e: Exception) {
        result.value = null
    }
}

