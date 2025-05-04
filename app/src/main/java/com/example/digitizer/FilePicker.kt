package com.example.digitizer

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

    fun createImageUri(): Uri {
        val imageDir = File(context.cacheDir, "images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
        val file = File(imageDir, "IMG_${System.currentTimeMillis()}.jpg")
        val authority = "${context.packageName}.provider" // Ensure this matches AndroidManifest
        return FileProvider.getUriForFile(context, authority, file)
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
    var tempUri: Uri? = null
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageCaptured(tempUri)
        } else {
            onImageCaptured(null)
        }
    }
    
    // Define the lambda separately
    val uriProviderLambda: () -> Uri? = { 
        tempUri = FilePickerUtils(context).createImageUri()
        tempUri
    }

    return remember(launcher) {
        CameraLauncher(launcher).apply {
            // Pass the defined lambda
            setUriProvider(uriProviderLambda)
        }
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

class CameraLauncher(private val launcher: androidx.activity.result.ActivityResultLauncher<Uri>) {
    private var uriProvider: (() -> Uri?)? = null

    fun setUriProvider(provider: () -> Uri?) {
        this.uriProvider = provider
    }

    fun launch() {
        val uri = uriProvider?.invoke()
        uri?.let { launcher.launch(it) }
    }
}

class DirectoryPicker(private val launcher: androidx.activity.result.ActivityResultLauncher<Uri?>) {
    fun launch() {
        launcher.launch(null)
    }
} 