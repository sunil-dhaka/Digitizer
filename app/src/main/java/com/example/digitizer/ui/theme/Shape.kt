package com.example.digitizer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Custom shapes for UI elements
val Shapes = Shapes(
    // Smaller components like buttons, chips, text fields
    small = RoundedCornerShape(8.dp),
    
    // Medium-sized components like cards, dialogs
    medium = RoundedCornerShape(12.dp),
    
    // Larger components like bottom sheets, expanded components
    large = RoundedCornerShape(16.dp),
    
    // Extra shape for other components if needed
    extraLarge = RoundedCornerShape(24.dp)
) 