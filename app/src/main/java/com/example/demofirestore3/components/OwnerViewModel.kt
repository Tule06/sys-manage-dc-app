package com.example.demofirestore3.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OwnerViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val authUser = FirebaseAuth.getInstance().currentUser

    private val _ownerInfo = MutableStateFlow<Map<String, Any>?>(null)
    val ownerInfo: StateFlow<Map<String, Any>?> = _ownerInfo

    fun fetchOwnerData() {
        viewModelScope.launch {
            try {
                val email = authUser?.email ?: return@launch
                val snapshot = db.collection("owners")
                    .whereEqualTo("email", email)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val ownerId = doc.id

                    val petQuery = db.collection("pets")
                        .whereEqualTo("ownerId", ownerId)
                        .get()
                        .await()

                    val numberOfPets = petQuery.size()

                    db.collection("owners").document(ownerId)
                        .update("numberOfPets", numberOfPets)
                        .await()

                    _ownerInfo.value = doc.data?.toMutableMap()?.apply {
                        put("id", ownerId)
                        put("numberOfPets", numberOfPets)
                    }
                }
            } catch (e: Exception) {
                _ownerInfo.value = null
            }
        }
    }
}
