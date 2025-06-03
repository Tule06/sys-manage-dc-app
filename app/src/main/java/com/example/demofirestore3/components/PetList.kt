package com.example.demofirestore3.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PetList(pets: List<Map<String, Any>>) {
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        items(pets) { pet ->
            val name = pet["name"] ?: "Không tên"
            val species = pet["species"] ?: "Không rõ"
            val age = pet["age"] ?: "?"

            Text("- $name | Loài: $species | Tuổi: $age")
        }
    }
}
