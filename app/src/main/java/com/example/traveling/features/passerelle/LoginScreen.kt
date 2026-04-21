package com.example.traveling.features.passerelle

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.example.traveling.ui.theme.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
    onNavigateRegister: () -> Unit = {},
    onNavigateForgotPwd: () -> Unit = {}
) {
    val context = LocalContext.current

    // 状态管理
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) } // 密码是否可见
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 外层容器：带有渐变背景
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Red50, Amber50_30)))
    ) {
        // 1. 顶部返回按钮区域
        Box(modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Surface(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0x80FEE2E2)), // border-red-100/50
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

            // --- 欢迎文本 ---
            Text("Bon retour", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Stone800, modifier = Modifier.padding(bottom = 4.dp))
            Text("Connectez-vous pour continuer votre voyage", fontSize = 14.sp, color = Stone500, modifier = Modifier.padding(bottom = 32.dp))

            // --- 表单区域 ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Email 输入框
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Email", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Stone500, modifier = Modifier.padding(start = 4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("votre@email.com", color = Color(0xFFD6D3D1)) },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = Stone400, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Stone200,
                            focusedBorderColor = Color(0xFFF87171) // 焦点时的红色边框
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                }

                // 密码输入框
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mot de passe", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Stone500, modifier = Modifier.padding(start = 4.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("••••••••", color = Color(0xFFD6D3D1)) },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Stone400, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Afficher/Masquer le mot de passe",
                                    tint = Stone400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Stone200,
                            focusedBorderColor = Color(0xFFF87171)
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                }

                // 忘记密码
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "Mot de passe oublié ?",
                        color = Red700,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onNavigateForgotPwd() }
                    )
                }

                // 错误提示
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- 登录按钮 (保留了你的 Firebase 逻辑) ---
                Button(
                    onClick = {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            isLoading = true
                            errorMessage = null
                            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "Connexion réussie !", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = task.exception?.localizedMessage ?: "Échec de la connexion"
                                    }
                                }
                        } else {
                            errorMessage = "L'email et le mot de passe sont requis"
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
                            .background(
                                Brush.horizontalGradient(listOf(Red700, Red800)),
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, Color(0x4DDC2626), RoundedCornerShape(8.dp)), // border-red-600/30
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Se connecter", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- 底部注册入口 ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pas encore de compte ? ", fontSize = 14.sp, color = Stone500)
                Text(
                    text = "S'inscrire",
                    fontSize = 14.sp,
                    color = Red700,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateRegister() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}