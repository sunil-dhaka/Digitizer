package com.example.digitizer

import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

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
                title = { Text("Document Digitizer") }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { 
                        imagePicker.launch() 
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Photo, "Select from gallery")
                }
                
                FloatingActionButton(
                    onClick = { 
                        // Launch camera
                        cameraLauncher.launch()
                    }
                ) {
                    Icon(Icons.Default.PhotoCamera, "Take photo")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is UiState.Initial -> {
                    InitialState()
                }
                is UiState.Loading -> {
                    LoadingState()
                }
                is UiState.Success -> {
                    val successState = uiState as UiState.Success
                    
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
                is UiState.Error -> {
                    val errorState = uiState as UiState.Error
                    ErrorState(errorMessage = errorState.errorMessage)
                }
            }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Create,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Select or capture document images",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Use the buttons below to add document images for processing",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
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
        CircularProgressIndicator(
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Processing documents...",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This may take a moment if multiple files are selected",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text("Previous")
                }
                
                Text(
                    text = "Document ${state.currentDocumentIndex + 1} of ${state.documents.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                TextButton(
                    onClick = { viewModel.navigateToNextDocument() },
                    enabled = state.currentDocumentIndex < state.documents.size - 1
                ) {
                    Text("Next")
                }
            }
        }
        
        // Preview image
        Text(
            text = "Document Image",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Display current document image
        Image(
            bitmap = currentDocument.bitmap.asImageBitmap(),
            contentDescription = "Document image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Output filename
        Text(
            text = "Filename",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = selectedFilename,
            onValueChange = onFilenameChange,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit filename"
                )
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Target directory
        Text(
            text = "Target Directory",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable { onSelectDirectory() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "Select folder"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (targetDirectory != null) {
                    FilePickerUtils(context).getTargetDirectoryName(targetDirectory)
                } else {
                    "Select a directory"
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Extracted text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Extracted Text",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentDocument.extractedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Markdown text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Markdown Text",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentDocument.markdownText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                        .fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Save button
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Save"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Document")
        }
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
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = { /* Reset to initial state */ }
        ) {
            Text("Try Again")
        }
    }
} 