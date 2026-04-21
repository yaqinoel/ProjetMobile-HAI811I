package com.example.traveling.features.passerelle

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
import coil.compose.AsyncImage
import com.example.traveling.ui.theme.*

import com.example.traveling.R
@Composable
fun LaunchScreen(
    onNavigateLogin: () -> Unit,
    onNavigateAnonymous: () -> Unit
) {
    // 外层容器
    Box(modifier = Modifier.fillMaxSize()) {

        // 背景图片
        Image(
            painter = painterResource(id = R.drawable.bg_lunch),
            contentDescription = "Fond d'écran Voyage", // 图片描述
            contentScale = ContentScale.Crop, // 保持铺满屏幕的裁剪方式
            modifier = Modifier.fillMaxSize() // 铺满整个屏幕
        )

        // 渐变遮罩
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

        // 核心内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp), // 留出上下左右的边距
            verticalArrangement = Arrangement.SpaceBetween // 上下两端对齐
        ) {

            // --- 顶部 Logo 区域 ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Logo 图标框
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
                // Logo 文字
                Text(
                    text = "Voyageur du Monde",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // --- 底部 CTA 与文字区域 ---
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 装饰线 (TRAVELING)
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

                // 大标题 (使用 AnnotatedString 实现一部分白色、一部分金色)
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

                // 副标题
                Text(
                    text = "Capturez vos voyages, planifiez vos aventures et découvrez les beautés de la Chine et du monde.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                // 按钮组
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 登录/开始旅行按钮 (红色渐变)
                    Button(
                        onClick = onNavigateLogin,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues() // 去掉默认内边距以让渐变铺满
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

                    // 匿名导航按钮 (毛玻璃/带边框效果)
                    OutlinedButton(
                        onClick = onNavigateAnonymous,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0x1AFFFFFF)), // 10% 白色模拟磨砂
                        border = BorderStroke(1.dp, Color(0x33FBBF24)) // 20% 金色边框
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xCCFDE047), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Navigation anonyme", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // 底部信任指标 (头像重叠显示活跃用户)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(Color(0xFFB91C1C), Color(0xFFD97706), Color(0xFF7C3AED), Color(0xFF0D9488))
                    val names = listOf("赵", "钱", "孙", "李")

                    // 重叠的头像
                    Box(modifier = Modifier.height(28.dp).width(88.dp)) {
                        names.forEachIndexed { index, name ->
                            Box(
                                modifier = Modifier
                                    .padding(start = (index * 20).dp) // 通过调整起始 padding 实现重叠
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

                    // 活跃人数文本
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