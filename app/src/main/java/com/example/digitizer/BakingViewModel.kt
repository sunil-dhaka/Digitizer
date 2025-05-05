package com.example.digitizer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Enum to represent AI models available in the app
enum class AIModel(val displayName: String, val modelName: String) {
    GEMINI_2_FLASH("Gemini 2.0 Flash", "gemini-2.0-flash"),
    GEMINI_2_5_FLASH_PREVIEW_0417("Gemini 2.5 Flash Preview 04-17", "gemini-2.5-flash-preview-04-17"),
    GEMINI_2_5_PRO_PREVIEW_0325("Gemini 2.5 Pro Preview 03-25", "gemini-2.5-pro-preview-03-25")
}

class DocumentViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    // Add a SharedFlow for feedback events
    private val _feedbackEvent = MutableSharedFlow<String>()
    val feedbackEvent = _feedbackEvent.asSharedFlow()

    // Add state for selected AI model
    private val _selectedModel = MutableStateFlow(AIModel.GEMINI_2_5_FLASH_PREVIEW_0417)
    val selectedModel: StateFlow<AIModel> = _selectedModel.asStateFlow()

    // GenerativeModel is now created via a function based on the selected model
    private fun getGenerativeModel(): GenerativeModel {
        return GenerativeModel(
            modelName = _selectedModel.value.modelName,
            apiKey = BuildConfig.apiKey
        )
    }

    // Function to change the AI model
    fun setAIModel(model: AIModel) {
        val previousModel = _selectedModel.value
        _selectedModel.value = model
        
        if (previousModel != model) {
            _feedbackEvent.tryEmit("Model changed to ${model.displayName}")
        }
    }

    fun processDocument(bitmap: Bitmap) {
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Extract text using OCR
                val extractedText = extractTextFromImage(bitmap)
                
                // Convert to markdown
                val markdownText = convertToMarkdown(extractedText)
                
                // Generate filename suggestion
                val suggestedFilename = generateFilename(extractedText)
                
                val document = ProcessedDocument(
                    extractedText = extractedText,
                    markdownText = markdownText,
                    suggestedFilename = suggestedFilename,
                    bitmap = bitmap
                )
                
                _uiState.value = UiState.Success(
                    documents = listOf(document),
                    currentDocumentIndex = 0
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Processing failed")
            }
        }
    }
    
    fun processMultipleDocuments(context: Context, uris: List<Uri>) {
        _uiState.value = UiState.Loading
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val processedDocuments = mutableListOf<ProcessedDocument>()
                
                for (uri in uris) {
                    val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    
                    // Process each document individually
                    val extractedText = extractTextFromImage(bitmap)
                    val markdownText = convertToMarkdown(extractedText)
                    val suggestedFilename = generateFilename(extractedText)
                    
                    val document = ProcessedDocument(
                        extractedText = extractedText,
                        markdownText = markdownText,
                        suggestedFilename = suggestedFilename,
                        bitmap = bitmap
                    )
                    
                    processedDocuments.add(document)
                }
                
                if (processedDocuments.isEmpty()) {
                    throw Exception("No valid images found")
                }
                
                _uiState.value = UiState.Success(
                    documents = processedDocuments,
                    currentDocumentIndex = 0
                )
                
                _feedbackEvent.emit("Processed ${processedDocuments.size} document(s) successfully")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Processing failed")
            }
        }
    }

    // Add navigation methods
    fun navigateToNextDocument() {
        val currentState = _uiState.value as? UiState.Success ?: return
        if (currentState.currentDocumentIndex < currentState.documents.size - 1) {
            _uiState.value = currentState.copy(
                currentDocumentIndex = currentState.currentDocumentIndex + 1
            )
        }
    }
    
    fun navigateToPreviousDocument() {
        val currentState = _uiState.value as? UiState.Success ?: return
        if (currentState.currentDocumentIndex > 0) {
            _uiState.value = currentState.copy(
                currentDocumentIndex = currentState.currentDocumentIndex - 1
            )
        }
    }

    fun processCameraImage(context: Context, uri: Uri) {
        _uiState.value = UiState.Loading
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                
                // Extract text using OCR
                val extractedText = extractTextFromImage(bitmap)
                
                // Convert to markdown
                val markdownText = convertToMarkdown(extractedText)
                
                // Generate filename suggestion
                val suggestedFilename = generateFilename(extractedText)
                
                val document = ProcessedDocument(
                    extractedText = extractedText,
                    markdownText = markdownText,
                    suggestedFilename = suggestedFilename,
                    bitmap = bitmap
                )
                
                _uiState.value = UiState.Success(
                    documents = listOf(document),
                    currentDocumentIndex = 0
                )
            } catch (e: Exception) {
                val errorMessage = "Failed to process camera image: ${e.message ?: "Unknown error"}"
                _uiState.value = UiState.Error(errorMessage)
                _feedbackEvent.emit(errorMessage) // Also provide feedback for camera errors
            }
        }
    }

    private suspend fun extractTextFromImage(bitmap: Bitmap): String {
        val response = getGenerativeModel().generateContent(
            content {
                image(bitmap)
                text("Extract all text from this document image. Include all paragraphs, headings, and lists in the exact order they appear.")
            }
        )
        return response.text ?: "No text could be extracted"
    }
    
    private suspend fun convertToMarkdown(extractedText: String): String {
        val response = getGenerativeModel().generateContent(
            content {
                text("Convert the following text to markdown format, preserving its structure. Identify and format headings, paragraphs, lists, etc. appropriately:\n\n$extractedText")
            }
        )
        return response.text ?: extractedText
    }
    
    private suspend fun generateFilename(extractedText: String): String {
        val response = getGenerativeModel().generateContent(
            content {
                text("Generate a short, descriptive filename (without extension) based on this document content. Use only alphanumeric characters, underscores, and hyphens. Keep it under 50 characters:\n\n$extractedText")
            }
        )
        val suggestedName = response.text?.trim() ?: ""
        
        return if (suggestedName.isNotEmpty()) {
            suggestedName
        } else {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            "document_${dateFormat.format(Date())}"
        }
    }
    
    fun saveDocumentFiles(context: Context, targetDirectoryUri: Uri, filename: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentState = _uiState.value as? UiState.Success ?: return@launch
                val currentDocument = currentState.currentDocument ?: return@launch
                
                val directoryDocFile = DocumentFile.fromTreeUri(context, targetDirectoryUri)
                if (directoryDocFile == null || !directoryDocFile.isDirectory) {
                    _uiState.value = UiState.Error("Invalid target directory selected.")
                    return@launch
                }

                // Save the current document's image
                val imageFile = directoryDocFile.createFile("image/jpeg", "$filename.jpg")
                if (imageFile != null) {
                    context.contentResolver.openOutputStream(imageFile.uri)?.use { out ->
                        currentDocument.bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    } ?: throw Exception("Could not open output stream for image file.")
                } else {
                    throw Exception("Could not create image file.")
                }
                
                // Save the markdown file
                val markdownFile = directoryDocFile.createFile("text/markdown", "$filename.md")
                if (markdownFile != null) {
                    context.contentResolver.openOutputStream(markdownFile.uri)?.use { out ->
                        out.write(currentDocument.markdownText.toByteArray())
                    } ?: throw Exception("Could not open output stream for markdown file.")
                } else {
                    throw Exception("Could not create markdown file.")
                }

                // Create a new list of documents with the current one marked as saved (we could add a 'saved' flag to ProcessedDocument if needed)
                
                _feedbackEvent.emit("Document saved successfully to selected directory.")
                
                // If we have more documents to process, navigate to the next one
                if (currentState.currentDocumentIndex < currentState.documents.size - 1) {
                    navigateToNextDocument()
                } else {
                    // Otherwise reset to initial state when all documents are saved
                    _uiState.value = UiState.Initial
                    _feedbackEvent.emit("All documents processed and saved.")
                }

            } catch (e: Exception) {
                val errorMessage = "Failed to save files: ${e.message ?: "Unknown error"}"
                _uiState.value = UiState.Error(errorMessage)
                _feedbackEvent.emit(errorMessage) // Emit error message
            }
        }
    }
}