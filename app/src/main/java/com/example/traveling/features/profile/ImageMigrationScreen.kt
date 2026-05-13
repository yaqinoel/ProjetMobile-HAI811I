package com.example.traveling.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.traveling.data.repository.FirestoreSeeder
import com.example.traveling.core.utils.ImageMigrationHelper
import com.example.traveling.ui.theme.CardBg
import com.example.traveling.ui.theme.ProfileCardBg
import com.example.traveling.ui.theme.ProfilePageBg
import com.example.traveling.ui.theme.RedPrimary
import com.example.traveling.ui.theme.Stone400
import com.example.traveling.ui.theme.Stone800
import com.example.traveling.ui.theme.StoneBorder
import com.example.traveling.ui.theme.StoneMuted
import com.example.traveling.ui.theme.StoneText
import kotlinx.coroutines.launch

/**
 * One-time admin screen to upload destination and attraction images to Storage.
 */
@Composable
fun ImageMigrationScreen(onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var seedDone by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var currentItem by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ImageMigrationHelper.MigrationResult?>(null) }
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = ProfilePageBg,
        topBar = {
            Surface(color = ProfileCardBg) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, enabled = !isRunning) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Stone800)
                    }
                    Text(
                        text = "Migration des images",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StoneText
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Outil d'administration", fontWeight = FontWeight.Bold, color = StoneText)
                    Text(
                        text = "Ce script recree les donnees Firestore, cherche de vraies photos sur Wikimedia Commons, upload les fichiers sous travelpath dans Firebase Storage, puis remplace les imageUrl et imageUrls par les URLs Storage.",
                        fontSize = 13.sp,
                        color = StoneMuted,
                        lineHeight = 20.sp
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Etape 1", fontWeight = FontWeight.Bold, color = StoneText)
                    Text("Reinitialise les destinations et attractions dans Firestore.", fontSize = 13.sp, color = StoneMuted)
                    Button(
                        onClick = {
                            FirestoreSeeder.seedAll(clearFirst = true)
                            seedDone = true
                            result = null
                            progress = 0
                            total = 0
                            currentItem = ""
                        },
                        enabled = !isRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (seedDone) "Seed relance" else "Lancer le seed")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Etape 2", fontWeight = FontWeight.Bold, color = StoneText)
                    Text("Upload une image pour chaque destination et trois images pour chaque attraction.", fontSize = 13.sp, color = StoneMuted)

                    if (isRunning) {
                        LinearProgressIndicator(
                            progress = { if (total > 0) progress.toFloat() / total else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = RedPrimary,
                            trackColor = StoneBorder
                        )
                        Text("$progress / $total - $currentItem", fontSize = 12.sp, color = Stone400)
                    }

                    Button(
                        onClick = {
                            isRunning = true
                            result = null
                            scope.launch {
                                try {
                                    val migrationResult = ImageMigrationHelper.migrateAll { current, targetTotal, name ->
                                        progress = current
                                        total = targetTotal
                                        currentItem = name
                                    }
                                    result = migrationResult
                                } catch (e: Exception) {
                                    result = ImageMigrationHelper.MigrationResult(
                                        total = total,
                                        success = 0,
                                        failed = 1,
                                        errors = listOf(e.message ?: "Migration failed")
                                    )
                                } finally {
                                    isRunning = false
                                }
                            }
                        },
                        enabled = !isRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isRunning) "Migration..." else "Migrer les images")
                    }
                }
            }

            result?.let { migration ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (migration.failed == 0) {
                            Color(0xFFE8F7EF)
                        } else {
                            Color(0xFFFFECEC)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Resultat", fontWeight = FontWeight.Bold, color = StoneText)
                        Text("Total: ${migration.total}", fontSize = 13.sp, color = StoneMuted)
                        Text("Reussis: ${migration.success}", fontSize = 13.sp, color = Color(0xFF047857))
                        Text("Echecs: ${migration.failed}", fontSize = 13.sp, color = Color(0xFFB91C1C))

                        migration.errors.take(8).forEach { error ->
                            Text(error, fontSize = 11.sp, color = Color(0xFFB91C1C), lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
