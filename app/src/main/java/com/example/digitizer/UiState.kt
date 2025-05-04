package com.example.digitizer

import android.graphics.Bitmap

/**
 * A sealed hierarchy describing the state of the document digitization process.
 */
sealed interface UiState {

    /**
     * Empty state when the screen is first shown
     */
    object Initial : UiState

    /**
     * Still loading
     */
    object Loading : UiState

    /**
     * Document processed successfully
     */
    data class Success(
        val documents: List<ProcessedDocument>,
        val currentDocumentIndex: Int = 0
    ) : UiState {
        val currentDocument: ProcessedDocument? get() = 
            if (documents.isNotEmpty() && currentDocumentIndex < documents.size) 
                documents[currentDocumentIndex] 
            else null
    }

    /**
     * There was an error during processing
     */
    data class Error(val errorMessage: String) : UiState
}

/**
 * Represents a single processed document
 */
data class ProcessedDocument(
    val extractedText: String,
    val markdownText: String,
    val suggestedFilename: String,
    val bitmap: Bitmap
)