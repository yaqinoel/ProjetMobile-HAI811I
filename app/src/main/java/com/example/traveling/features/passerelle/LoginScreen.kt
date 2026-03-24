package com.example.traveling.features.passerelle

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth // 引入 Firebase Auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    // 1. 获取 Firebase Auth 的实例（这是与 Firebase 通信的桥梁）
    val auth = FirebaseAuth.getInstance()
    // 获取当前的上下文环境（用来显示弹窗 Toast）
    val context = LocalContext.current

    // 状态管理
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 2. 新增两个状态：用来显示“正在加载”和“错误信息”
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        IconButton(onClick = { /* TODO: 处理返回逻辑 */ }) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Bon retour", fontSize = 28.sp, fontWeight = FontWeight.Bold) // [cite: 77]
        Text(
            text = "Connectez-vous pour continuer votre voyage", // [cite: 78]
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        // Email 输入框
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 密码输入框
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 如果有错误信息，显示一段红色的文字提示用户
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        TextButton(
            onClick = { /* TODO: 忘记密码逻辑 */ },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Mot de passe oublié ?", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 红色的登录按钮
        Button(
            // 3. 核心逻辑：点击按钮时调用 Firebase 接口
            onClick = {
                // 先检查邮箱和密码是不是空的
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true // 按钮开始加载
                    errorMessage = null // 清空之前的错误

                    // 调用 Firebase 登录接口
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false // 结束加载状态
                            if (task.isSuccessful) {
                                // 登录成功！
                                Toast.makeText(context, "登录成功！欢迎回来", Toast.LENGTH_SHORT).show()
                                // TODO: 这里写跳转到 App 首页的逻辑
                            } else {
                                // 登录失败，显示具体的错误原因（比如密码错误、账号不存在）
                                errorMessage = task.exception?.localizedMessage ?: "登录失败，请重试"
                            }
                        }
                } else {
                    errorMessage = "邮箱和密码不能为空"
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA30000)),
            enabled = !isLoading // 如果正在加载，按钮变灰不可点击，防止用户狂点
        ) {
            // 4. 根据状态改变按钮文字：如果正在加载，显示 "Connexion en cours..."
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Se connecter", fontSize = 16.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pas encore de compte ? ")
            TextButton(onClick = { /* TODO: 跳转到注册页面 */ }) {
                Text("S'inscrire", color = Color(0xFFA30000), fontWeight = FontWeight.Bold) // [cite: 91]
            }
        }
    }
}