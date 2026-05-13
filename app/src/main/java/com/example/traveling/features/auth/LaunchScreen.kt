package com.example.traveling.features.auth

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traveling.data.repository.AnonymousAuthRepository
import com.example.traveling.ui.theme.*
import kotlinx.coroutines.launch

import com.example.traveling.R

@Composable
fun LaunchScreen(
    onNavigateLogin: () -> Unit,
    onNavigateAnonymous: () -> Unit
) {
    val anonymousAuthRepository = remember { AnonymousAuthRepository() }
    val scope = rememberCoroutineScope()
    var isAnonymousLoading by remember { mutableStateOf(false) }
    var anonymousError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_launch),
            contentDescription = "Fond d'écran Voyage",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x66991B1B), shape = RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x66FBBF24), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("游", color = Color(0xFFFDE047), fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Voyageur du Monde",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, Color(0x66FBBF24), Color.Transparent))
                    ))
                    Text(
                        text = " TRAVELING ",
                        color = Color(0x99FDE047),
                        fontSize = 10.sp,
                        letterSpacing = 3.sp
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, Color(0x66FBBF24), Color.Transparent))
                    ))
                }

                Text(
                    text = buildAnnotatedString {
                        append("Parcourez le monde,\n")
                        withStyle(SpanStyle(color = Color(0xFFFDE047))) {
                            append("admirez les merveilles")
                        }
                    },
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 40.sp
                )

                Text(
                    text = "Capturez vos voyages, planifiez vos aventures et découvrez les beautés de la Chine et du monde.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onNavigateLogin,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(Color(0xFFB91C1C), Color(0xFF991B1B))))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Commencer le voyage", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            if (isAnonymousLoading) return@OutlinedButton
                            isAnonymousLoading = true
                            anonymousError = null
                            scope.launch {
                                anonymousAuthRepository.signInAnonymouslyIfNeeded()
                                    .onSuccess {
                                        isAnonymousLoading = false
                                        onNavigateAnonymous()
                                    }
                                    .onFailure { error ->
                                        isAnonymousLoading = false
                                        Log.e("AnonAuth", "anon failed", error)
                                        anonymousError = error.localizedMessage ?: "Connexion anonyme impossible"
                                    }

                            }
                        },
                        enabled = !isAnonymousLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0x1AFFFFFF)),
                        border = BorderStroke(1.dp, Color(0x33FBBF24))
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xCCFDE047), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isAnonymousLoading) "Connexion anonyme..." else "Navigation anonyme",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (anonymousError != null) {
                        Text(
                            text = anonymousError ?: "",
                            color = Color(0xFFFECACA),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(Color(0xFFB91C1C), Color(0xFFD97706), Color(0xFF7C3AED), Color(0xFF0D9488))
                    val names = listOf("赵", "钱", "孙", "李")

                    Box(modifier = Modifier.height(28.dp).width(88.dp)) {
                        names.forEachIndexed { index, name ->
                            Box(
                                modifier = Modifier
                                    .padding(start = (index * 20).dp)
                                    .size(28.dp)
                                    .background(colors[index], shape = CircleShape)
                                    .border(2.dp, Color(0x4D000000), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xE6FDE047), fontWeight = FontWeight.Bold)) {
                                append("2,4k+ ")
                            }
                            append("voyageurs actifs")
                        },
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
