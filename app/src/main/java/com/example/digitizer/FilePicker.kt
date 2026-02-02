package com.example.digitizer

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext

class FilePickerUtils(private val context: Context) {
    
    fun getTargetDirectoryName(uri: Uri?): String {
        if (uri == null) return "Unknown Directory"
        
        val documentFile = DocumentFile.fromTreeUri(context, uri)
        return documentFile?.name ?: "Unknown Directory"
    }
    
//    fun getFullPath(uri: Uri?): String {
//        if (uri == null) return ""
//        return uri.toString()
//    }
    
    fun createFilename(baseName: String, timestamp: Boolean = true): String {
        val timeStamp = if (timestamp) {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            "_${dateFormat.format(Date())}"
        } else {
            ""
        }
        
        return "${baseName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}$timeStamp"
    }

    fun createImageUri(): Uri? {
        try {
            val imageDir = File(context.cacheDir, "images")
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }
            val file = File(imageDir, "IMG_${System.currentTimeMillis()}.jpg")
            val authority = "${context.packageName}.provider" // Ensure this matches AndroidManifest
            return FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

@Composable
fun rememberImagePicker(
    onImagesPicked: (List<Uri>) -> Unit
): ImagePicker {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        onImagesPicked(uris)
    }
    
    return remember(launcher) {
        ImagePicker(launcher)
    }
}

@Composable
fun rememberCameraLauncher(
    onImageCaptured: (Uri?) -> Unit
): CameraLauncher {
    val context = LocalContext.current
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempUri != null) {
            onImageCaptured(tempUri)
        } else {
            onImageCaptured(null)
        }
    }
    
    return remember(launcher) {
        CameraLauncher(context, launcher, onUriCreated = { uri -> tempUri = uri })
    }
}

@Composable
fun rememberDirectoryPicker(
    onDirectoryPicked: (Uri) -> Unit
): DirectoryPicker {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { onDirectoryPicked(it) }
    }
    
    return remember(launcher) {
        DirectoryPicker(launcher)
    }
}

class ImagePicker(private val launcher: androidx.activity.result.ActivityResultLauncher<String>) {
    fun launch() {
        launcher.launch("image/*")
    }
}

class CameraLauncher(
    private val context: Context,
    private val launcher: androidx.activity.result.ActivityResultLauncher<Uri>,
    private val onUriCreated: (Uri?) -> Unit
) {
    fun launch() {
        try {
            val utils = FilePickerUtils(context)
            val uri = utils.createImageUri()
            
            if (uri != null) {
                onUriCreated(uri)
                launcher.launch(uri)
            } else {
                onUriCreated(null) // No valid URI created
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onUriCreated(null)
        }
    }
}

class DirectoryPicker(private val launcher: androidx.activity.result.ActivityResultLauncher<Uri?>) {
    fun launch() {
        launcher.launch(null)
    }
} 