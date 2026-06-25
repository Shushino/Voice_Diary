package com.shushino.voicediary.presentation.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shushino.voicediary.domain.model.DiaryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    entry: DiaryEntry,
    onDismiss: () -> Unit,
    onExportComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Export Entry",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ListItem(
                headlineContent = { Text("Share as Text") },
                supportingContent = { Text("Send entry content to other apps") },
                leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                modifier = Modifier.clickable {
                    shareEntryAsText(context, entry)
                    onDismiss()
                }
            )

            ListItem(
                headlineContent = { Text("Export as PDF") },
                supportingContent = { Text("Save as a PDF document in Downloads") },
                leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier.clickable {
                    onDismiss()
                    scope.launch {
                        try {
                            exportEntryAsPdf(context, entry)
                            onExportComplete("PDF exported to Downloads ✓")
                        } catch (e: Exception) {
                            onExportComplete("Failed to export PDF: ${e.localizedMessage}")
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun buildEntryText(entry: DiaryEntry): String {
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(entry.createdAt))
    
    return """
        ${entry.title ?: "Untitled"}
        $dateStr | Mood: ${entry.mood.emoji}
        
        ${entry.body}
        
        Tags: ${entry.tags.joinToString(", ")}
    """.trimIndent()
}

private fun shareEntryAsText(context: Context, entry: DiaryEntry) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, buildEntryText(entry))
        putExtra(Intent.EXTRA_SUBJECT, entry.title ?: "Diary Entry")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

private suspend fun exportEntryAsPdf(context: Context, entry: DiaryEntry) = withContext(Dispatchers.IO) {
    val pdfDocument = PdfDocument()
    val paint = Paint()
    val margin = 40f
    val pageWidth = 595
    val pageHeight = 842
    val contentWidth = pageWidth - 2 * margin
    val bottomMargin = 40f
    val maxContentHeight = pageHeight - bottomMargin

    var yPos = 80f
    var pageNumber = 1
    
    fun startNewPage(): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
        return pdfDocument.startPage(pageInfo)
    }

    var currentPage = startNewPage()
    var canvas = currentPage.canvas

    // Title
    paint.textSize = 24f
    paint.isFakeBoldText = true
    canvas.drawText(entry.title ?: "Untitled", margin, yPos, paint)
    yPos += 40f

    // Date + Mood
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(entry.createdAt))
    paint.textSize = 14f
    paint.isFakeBoldText = false
    canvas.drawText("${dateStr} · ${entry.mood.emoji}", margin, yPos, paint)
    yPos += 20f

    // Tags
    if (entry.tags.isNotEmpty()) {
        paint.textSize = 12f
        paint.color = android.graphics.Color.GRAY
        canvas.drawText("Tags: ${entry.tags.joinToString(", ")}", margin, yPos, paint)
        yPos += 20f
    }

    // Horizontal Rule
    paint.color = android.graphics.Color.BLACK
    paint.strokeWidth = 1f
    canvas.drawLine(margin, yPos, pageWidth - margin, yPos, paint)
    yPos += 25f

    // Body (Manual word wrap + Pagination)
    paint.textSize = 13f
    val words = entry.body.split(Regex("\\s+"))
    var currentLine = StringBuilder()

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        if (paint.measureText(testLine) <= contentWidth) {
            currentLine.append(if (currentLine.isEmpty()) word else " $word")
        } else {
            // Draw current line
            canvas.drawText(currentLine.toString(), margin, yPos, paint)
            yPos += paint.textSize * 1.5f
            currentLine = StringBuilder(word)

            // Check for page break
            if (yPos > maxContentHeight) {
                pdfDocument.finishPage(currentPage)
                currentPage = startNewPage()
                canvas = currentPage.canvas
                yPos = margin + paint.textSize
            }
        }
    }
    
    // Draw last line
    if (currentLine.isNotEmpty()) {
        if (yPos > maxContentHeight) {
            pdfDocument.finishPage(currentPage)
            currentPage = startNewPage()
            canvas = currentPage.canvas
            yPos = margin + paint.textSize
        }
        canvas.drawText(currentLine.toString(), margin, yPos, paint)
    }

    pdfDocument.finishPage(currentPage)

    val fileName = "${entry.title?.replace(Regex("[^A-Za-z0-9]"), "_") ?: "DiaryEntry"}_${System.currentTimeMillis()}.pdf"

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw Exception("Failed to create MediaStore entry")
            
            resolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) } ?: throw Exception("Failed to open output stream")
            
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
        }
    } finally {
        pdfDocument.close()
    }
}
