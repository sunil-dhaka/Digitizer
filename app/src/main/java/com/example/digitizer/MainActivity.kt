package com.example.digitizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import com.example.digitizer.ui.theme.DigitizerTheme
import com.example.digitizer.ui.theme.GradientEnd
import com.example.digitizer.ui.theme.GradientStart

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DigitizerTheme {
                // Create an infinite transition to animate the gradient
                val infiniteTransition = rememberInfiniteTransition(label = "backgroundTransition")
                
                // Animate the gradient position
                val offsetX by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(20000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "gradientOffsetX"
                )
                
                val offsetY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(25000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "gradientOffsetY"
                )
                
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RectangleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.background,
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                                        MaterialTheme.colorScheme.background
                                    ),
                                    start = Offset(offsetX * 1000f, 0f),
                                    end = Offset(0f, offsetY * 1000f)
                                )
                            )
                    ) {
                        DocumentScannerScreen()
                    }
                }
            }
        }
    }
}