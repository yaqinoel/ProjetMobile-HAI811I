package com.example.traveling.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.data.repository.FirestoreSeeder
import com.example.traveling.data.repository.ImageMigrationHelper
import com.example.traveling.ui.theme.*
import kotlinx.coroutines.launch

/**
 * One-time admin screen to trigger image migration.
 * Navigate here from ProfileScreen settings, run migration, then remove from nav.
 */
@Composable
fun ImageMigrationScreen(onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

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
                    Text("Migration des images", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StoneText)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            Surface(
                color = CardBg,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⚠️ Outil d'administration", fontWeight = FontWeight.Bold, color = StoneText)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ce script va :\n" +
                        "1. Re-seed les données Firestore\n" +
                        "2. Télécharger les images depuis Pexels\n" +
                        "3. Les uploader dans Firebase Storage\n" +
                        "4. Mettre à jour les URLs dans Firestore\n\n" +
                        "⏱ Cela peut prendre 2-5 minutes.",
                        fontSize = 13.sp, color = StoneMuted, lineHeight = 20.sp
                    )
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Re-seed Firestore
                Button(
                    onClick = {
                        logs = logs + "🔄 Re-seeding Firestore data..."
                        FirestoreSeeder.seedAll(clearFirst = true)
                        logs = logs + "✅ Firestore re-seeded"
                    },
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("1. Re-seed DB", fontSize = 12.sp)
                }

                // Migrate images
                Button(
                    onClick = {
                        isRunning = true
                        scope.launch {
                            try {
                                ImageMigrationHelper.migrateAll { msg ->
                                    logs = logs + msg
                                }
                            } catch (e: Exception) {
                                logs = logs + "💥 Erreur fatale: ${e.message}"
                            } finally {
                                isRunning = false
                            }
                        }
                    },
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text("2. Migrer Images", fontSize = 12.sp)
                }
            }

            // Progress indicator
            if (isRunning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = RedPrimary,
                    trackColor = StoneBorder
                )
            }

            // Log console
            Surface(
                color = Color(0xFF1E1E1E),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(12.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text("Console de migration...", color = Color(0xFF6B6B6B), fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                    logs.forEach { line ->
                        Text(
                            text = line,
                            color = when {
                                line.contains("✅") -> Color(0xFF4ADE80)
                                line.contains("❌") || line.contains("💥") -> Color(0xFFF87171)
                                line.contains("🚀") || line.contains("🏁") -> Color(0xFF60A5FA)
                                else -> Color(0xFFD4D4D4)
                            },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
