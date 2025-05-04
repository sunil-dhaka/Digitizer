package com.example.digitizer

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.digitizer.ui.components.AnimatedContent
import com.example.digitizer.ui.components.GlossyCard
import com.example.digitizer.ui.components.GlossyFloatingActionButton
import com.example.digitizer.ui.components.GradientButton
import com.example.digitizer.ui.components.GradientDivider
import com.example.digitizer.ui.theme.GradientEnd
import com.example.digitizer.ui.theme.GradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    documentViewModel: DocumentViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by documentViewModel.uiState.collectAsState()
    
    var selectedTargetDirectory by rememberSaveable { mutableStateOf<Uri?>(null) }
    var selectedFilename by rememberSaveable { mutableStateOf("") }
    var hasRequestedPermissions by rememberSaveable { mutableStateOf(false) }
    
    val imagePicker = rememberImagePicker { uris ->
        if (uris.isNotEmpty()) {
            documentViewModel.processMultipleDocuments(context, uris)
        }
    }
    
    val directoryPicker = rememberDirectoryPicker { uri ->
        selectedTargetDirectory = uri
    }
    
    // Add the camera launcher
    val cameraLauncher = rememberCameraLauncher { uri ->
        uri?.let { 
            // Call ViewModel to process the captured image
            documentViewModel.processCameraImage(context, it)
        } ?: run {
            Toast.makeText(context, "Camera capture cancelled or failed.", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Observe feedback events
    LaunchedEffect(key1 = documentViewModel) {
        documentViewModel.feedbackEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    LaunchedEffect(key1 = Unit) {
        if (!hasRequestedPermissions) {
            hasRequestedPermissions = true
        }
    }
    
    // Reset filename when document changes
    LaunchedEffect(key1 = uiState) {
        if (uiState is UiState.Success) {
            val successState = uiState as UiState.Success
            val currentDoc = successState.currentDocument
            
            if (currentDoc != null) {
                selectedFilename = currentDoc.suggestedFilename
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Document Digitizer", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlossyFloatingActionButton(
                    onClick = { 
                        imagePicker.launch() 
                    },
                    icon = Icons.Default.Photo,
                    contentDescription = "Select from gallery",
                    gradientStart = MaterialTheme.colorScheme.secondary,
                    gradientEnd = MaterialTheme.colorScheme.secondaryContainer
                )
                
                GlossyFloatingActionButton(
                    onClick = { 
                        // Launch camera
                        cameraLauncher.launch()
                    },
                    icon = Icons.Default.PhotoCamera,
                    contentDescription = "Take photo"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    color = MaterialTheme.colorScheme.background
                )
        ) {
            AnimatedContent(
                visible = uiState is UiState.Initial,
                content = { InitialState() }
            )
            
            AnimatedContent(
                visible = uiState is UiState.Loading,
                content = { LoadingState() }
            )
            
            AnimatedContent(
                visible = uiState is UiState.Success,
                content = {
                    val successState = uiState as? UiState.Success ?: return@AnimatedContent
                    
                    SuccessState(
                        state = successState,
                        selectedFilename = selectedFilename,
                        onFilenameChange = { selectedFilename = it },
                        targetDirectory = selectedTargetDirectory,
                        onSelectDirectory = { directoryPicker.launch() },
                        onSave = {
                            if (selectedTargetDirectory == null) {
                                Toast.makeText(context, "Please select a target directory", Toast.LENGTH_SHORT).show()
                            } else {
                                documentViewModel.saveDocumentFiles(context, selectedTargetDirectory!!, selectedFilename)
                            }
                        }
                    )
                }
            )
            
            AnimatedContent(
                visible = uiState is UiState.Error,
                content = {
                    val errorState = uiState as? UiState.Error ?: return@AnimatedContent
                    ErrorState(errorMessage = errorState.errorMessage)
                }
            )
        }
    }
    
    RequestPermissions(
        onPermissionsGranted = {
            // Ready to use camera and storage
        },
        onPermissionsDenied = {
            Toast.makeText(
                context,
                "Camera and storage permissions are required",
                Toast.LENGTH_LONG
            ).show()
        }
    )
}

@Composable
fun InitialState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .shadow(10.dp, shape = RoundedCornerShape(60.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        ),
                        shape = RoundedCornerShape(60.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(500)) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(500)
                )
            ) {
                Text(
                    text = "Scan & Digitize Documents",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(700)) + slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(700)
                )
            ) {
                Text(
                    text = "Use the camera or select images to convert your documents to digital text",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(900))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlossyCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clickable { /* TODO */ }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Camera")
                        }
                    }
                    
                    GlossyCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clickable { /* TODO */ }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Gallery")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Create a pulsating animation for the loading indicator
        val pulsate by animateFloatAsState(
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(pulsate)
                .shadow(8.dp, shape = RoundedCornerShape(60.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        )
                    ),
                    shape = RoundedCornerShape(60.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                color = Color.White,
                strokeWidth = 6.dp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Processing Documents",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Converting your documents to digital format...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SuccessState(
    state: UiState.Success,
    selectedFilename: String,
    onFilenameChange: (String) -> Unit,
    targetDirectory: Uri?,
    onSelectDirectory: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val currentDocument = state.currentDocument
    val viewModel = viewModel<DocumentViewModel>()

    // If there's no current document, show an error or return
    if (currentDocument == null) {
        ErrorState(errorMessage = "No document available to display")
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Document navigation and counter
        if (state.documents.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { viewModel.navigateToPreviousDocument() },
                    enabled = state.currentDocumentIndex > 0
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Previous",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Text(
                    text = "Document ${state.currentDocumentIndex + 1} of ${state.documents.size}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                
                TextButton(
                    onClick = { viewModel.navigateToNextDocument() },
                    enabled = state.currentDocumentIndex < state.documents.size - 1
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Next",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            GradientDivider(modifier = Modifier.padding(bottom = 24.dp))
        }
        
        // Preview image
        Text(
            text = "Document Preview",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Display current document image
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Image(
                bitmap = currentDocument.bitmap.asImageBitmap(),
                contentDescription = "Document image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Output filename
        GlossyCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Filename",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = selectedFilename,
                    onValueChange = onFilenameChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit filename",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Target directory
        GlossyCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Save Location",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            color = if (targetDirectory != null)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelectDirectory() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Select folder",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (targetDirectory != null) {
                            FilePickerUtils(context).getTargetDirectoryName(targetDirectory)
                        } else {
                            "Select a directory"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (targetDirectory != null) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Extracted text
        GlossyCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Extracted Text",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            color = MaterialTheme.colorScheme.surface
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = currentDocument.extractedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Markdown text
        GlossyCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Markdown Format",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = currentDocument.markdownText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Save button
        GradientButton(
            text = "Save Document",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Save
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ErrorState(errorMessage: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(8.dp, shape = RoundedCornerShape(50.dp))
                .background(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(50.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Something Went Wrong",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        GradientButton(
            text = "Try Again",
            onClick = { /* Reset to initial state */ },
            icon = Icons.Default.PhotoCamera
        )
    }
} 