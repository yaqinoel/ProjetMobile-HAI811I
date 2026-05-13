package com.example.traveling.features.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.example.traveling.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit = {},
    onNavigateLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) } // 标记是否发送成功

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Red50, Amber50_30)))
            .verticalScroll(scrollState)
    ) {
        // 顶部返回按钮区域
        Box(modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Surface(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0x80FEE2E2)),
                shadowElevation = 1.dp
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color(0xFF44403C),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // 核心内容区域
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {

            // --- Logo 区域 ---
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 32.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .background(Red700, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x80DC2626), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("游", color = Color(0xFFFDE047), fontSize = 18.sp)
                }
                Spacer(Modifier.width(10.dp))
                Text("Voyageur du Monde", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Stone800, letterSpacing = 0.5.sp)
            }

            // --- 标题 ---
            Text("Mot de passe oublié", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Stone800, modifier = Modifier.padding(bottom = 8.dp))
            Text(
                text = "Ne vous inquiétez pas ! Entrez votre adresse e-mail ci-dessous et nous vous enverrons un lien pour réinitialiser votre mot de passe.",
                fontSize = 14.sp,
                color = Stone500,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // --- 表单区域 ---
            if (isSuccess) {
                // 如果发送成功，显示成功提示，隐藏输入框
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFECFDF5), RoundedCornerShape(12.dp)) // 浅绿色背景
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Email envoyé !", color = Color(0xFF059669), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        Text(
                            "Veuillez vérifier votre boîte de réception (et vos spams) pour réinitialiser votre mot de passe.",
                            color = Color(0xFF047857), fontSize = 13.sp, textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // 如果还没发送成功，显示输入框
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Email 输入框
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Email de votre compte", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Stone500, modifier = Modifier.padding(start = 4.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("votre@email.com", color = Color(0xFFD6D3D1)) },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = Stone400, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                                unfocusedBorderColor = Stone200, focusedBorderColor = Color(0xFFF87171)
                            ),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        )
                    }

                    // 错误提示
                    if (errorMessage != null) {
                        Text(text = errorMessage!!, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- 发送按钮 (Firebase 逻辑) ---
                    Button(
                        onClick = {
                            if (email.isNotEmpty()) {
                                isLoading = true
                                errorMessage = null

                                // 调用 Firebase 的重置密码接口
                                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            isSuccess = true // 标记为成功，UI会自动变成绿色的提示框
                                            Toast.makeText(context, "Lien de réinitialisation envoyé", Toast.LENGTH_SHORT).show()
                                        } else {
                                            errorMessage = task.exception?.localizedMessage ?: "Échec de l'envoi"
                                        }
                                    }
                            } else {
                                errorMessage = "Veuillez entrer votre email"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        contentPadding = PaddingValues(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        enabled = !isLoading
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(Red700, Red800)), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0x4DDC2626), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Envoyer le lien", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // --- 底部返回登录入口 ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Retourner à la connexion",
                    fontSize = 14.sp,
                    color = Stone500,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateLogin() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    ForgotPasswordScreen()
}