package com.example.traveling.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.data.repository.TravelPathImageMigrationProgress
import com.example.traveling.data.repository.TravelPathImageMigrationResult
import com.example.traveling.data.repository.TravelPathImageStorageRepository
import com.example.traveling.ui.theme.CardBg
import com.example.traveling.ui.theme.PageBg
import com.example.traveling.ui.theme.RedPrimary
import com.example.traveling.ui.theme.StoneMuted
import com.example.traveling.ui.theme.StoneText
import kotlinx.coroutines.launch

@Composable
fun TravelPathImageStorageScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = remember { TravelPathImageStorageRepository() }
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(TravelPathImageMigrationProgress("Pret", 0, 1)) }
    var result by remember { mutableStateOf<TravelPathImageMigrationResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = StoneText)
            }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text("Images TravelPath", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StoneText)
                Text("Upload vers Firebase Storage", fontSize = 12.sp, color = StoneMuted)
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(RedPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudUpload, null, tint = RedPrimary, modifier = Modifier.size(28.dp))
                }
                Text("Stockage permanent", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StoneText)
                Text(
                    "Telecharge les images Google, les enregistre dans travelpath/ sur Firebase Storage, puis remplace imageUrl et imageUrls dans Firestore.",
                    fontSize = 13.sp,
                    color = StoneMuted,
                    lineHeight = 19.sp
                )

                if (running) {
                    LinearProgressIndicator(
                        progress = { (progress.done.toFloat() / progress.total.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = RedPrimary,
                        trackColor = Color(0xFFE7E5E4)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = RedPrimary)
                        Text(
                            "${progress.label} ${progress.done}/${progress.total}",
                            modifier = Modifier.padding(start = 8.dp),
                            fontSize = 12.sp,
                            color = StoneMuted
                        )
                    }
                }

                Button(
                    onClick = {
                        running = true
                        result = null
                        scope.launch {
                            result = repository.migrateAllToStorage { progress = it }
                            running = false
                        }
                    },
                    enabled = !running,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (running) "Upload en cours..." else "Uploader les images", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        result?.let { migrationResult ->
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (migrationResult.failed == 0) Color(0xFFECFDF5) else Color(0xFFFFF1F2)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resultat", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StoneText)
                    Text("Total: ${migrationResult.total}", fontSize = 13.sp, color = StoneText)
                    Text("Reussis: ${migrationResult.success}", fontSize = 13.sp, color = Color(0xFF047857))
                    Text("Echecs: ${migrationResult.failed}", fontSize = 13.sp, color = Color(0xFFDC2626))
                    migrationResult.errors.take(20).forEach { error ->
                        Text(error, fontSize = 11.sp, color = Color(0xFFDC2626))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Retour")
        }
    }
}
