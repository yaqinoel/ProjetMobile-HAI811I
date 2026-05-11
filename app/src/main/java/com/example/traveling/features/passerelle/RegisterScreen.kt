package com.example.traveling.features.passerelle

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import com.example.traveling.data.repository.UserRepository
import com.example.traveling.ui.components.UserAvatar
import com.example.traveling.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {},
    onNavigateLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val userRepository = remember { UserRepository() }
    val coroutineScope = rememberCoroutineScope()

    // 状态管理
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedAvatarUri = uri
    }

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
            Text("Créer un compte", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Stone800, modifier = Modifier.padding(bottom = 4.dp))
            Text("Rejoignez la communauté des voyageurs", fontSize = 14.sp, color = Stone500, modifier = Modifier.padding(bottom = 32.dp))

            // --- 表单区域 ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color(0xFFFECACA), CircleShape)
                            .clickable(enabled = !isLoading) { avatarPicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUri = selectedAvatarUri
                        if (avatarUri != null) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            UserAvatar(
                                avatarUrl = null,
                                fallbackText = name.firstOrNull()?.uppercase() ?: "V",
                                backgroundColor = Red700,
                                modifier = Modifier.fillMaxSize(),
                                textSize = 28.sp
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { avatarPicker.launch("image/*") },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (selectedAvatarUri == null) "Choisir un avatar" else "Changer l'avatar")
                    }
                }

                // Nom 输入框
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nom", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Stone500, modifier = Modifier.padding(start = 4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Votre nom", color = Color(0xFFD6D3D1)) },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Stone400, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                            unfocusedBorderColor = Stone200, focusedBorderColor = Color(0xFFF87171)
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Signature", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Stone500, modifier = Modifier.padding(start = 4.dp))
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it.take(120) },
                        placeholder = { Text("Quelques mots sur vous", color = Color(0xFFD6D3D1)) },
                        singleLine = false,
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                            unfocusedBorderColor = Stone200, focusedBorderColor = Color(0xFFF87171)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                            unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                            unfocusedBorderColor = Stone200, focusedBorderColor = Color(0xFFF87171)
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
                        placeholder = { Text("Au moins 8 caractères", color = Color(0xFFD6D3D1)) },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Stone400, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Afficher/Masquer",
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

                // --- 注册按钮 ---
                Button(
                    onClick = {
                        if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                            if (password.length < 6) {
                                errorMessage = "Le mot de passe doit contenir au moins 6 caractères"
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null
                            val auth = FirebaseAuth.getInstance()

                            // 1. Créer utilisateur Firebase Auth
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // 2. Mettre à jour displayName côté Firebase Auth
                                        val user = auth.currentUser
                                        val uid = user?.uid
                                        val profileUpdates = UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .build()

                                        user?.updateProfile(profileUpdates)?.addOnCompleteListener { updateTask ->
                                            if (!updateTask.isSuccessful || uid == null) {
                                                isLoading = false
                                                errorMessage = updateTask.exception?.localizedMessage
                                                    ?: "Échec de la mise à jour du profil"
                                                return@addOnCompleteListener
                                            }

                                            // 3. Initialiser users/{uid} dans Firestore
                                            coroutineScope.launch {
                                                try {
                                                    val avatarUrl = selectedAvatarUri?.let { uri ->
                                                        userRepository.uploadUserAvatar(uid, uri)
                                                    }
                                                    userRepository.createUserDocumentIfMissing(
                                                        userId = uid,
                                                        displayName = name,
                                                        email = email,
                                                        avatarUrl = avatarUrl,
                                                        bio = bio.trim()
                                                    )
                                                    isLoading = false
                                                    Toast.makeText(context, "Compte créé avec succès !", Toast.LENGTH_SHORT).show()
                                                    onRegisterSuccess()
                                                } catch (e: Exception) {
                                                    isLoading = false
                                                    errorMessage = e.localizedMessage
                                                        ?: "Échec de l'initialisation du profil"
                                                }
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        errorMessage = task.exception?.localizedMessage ?: "Échec de l'inscription"
                                    }
                                }
                        } else {
                            errorMessage = "Veuillez remplir tous les champs"
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
                            Text("S'inscrire", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 隐私条款说明 ---
            Text(
                text = "En vous inscrivant, vous acceptez nos conditions d'utilisation et notre politique de confidentialité.",
                fontSize = 12.sp,
                color = Stone400,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- 底部登录入口 ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vous avez déjà un compte ? ", fontSize = 14.sp, color = Stone500)
                Text(
                    text = "Se connecter",
                    fontSize = 14.sp,
                    color = Red700,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateLogin() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen()
}
