// UserList.kt
package com.example.demofirestore3.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UserList(users: List<Map<String, Any>>) {
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        items(users) { user ->
            Text("- ${user["uid"]} | ${user["name"]} | ${user["birth"]}")
        }
    }
}
