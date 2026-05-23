package com.george.voicediary.presentation.ui.components

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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.george.voicediary.domain.model.DiaryEntry
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
                .padding(bottom = 32.dp)
        ) {
            ListItem(
                headlineContent = { Text("Share as Text") },
                leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                modifier = Modifier.clickable {
                    shareEntryAsText(context, entry)
                    onDismiss()
                }
            )
            ListItem(
                headlineContent = { Text("Export as PDF") },
                leadingContent = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                modifier = Modifier.clickable {
                    scope.launch {
                        exportEntryAsPdf(context, entry)
                        onExportComplete("Saved to Downloads ✓")
                        onDismiss()
                    }
                }
            )
        }
    }
}

private fun shareEntryAsText(context: Context, entry: DiaryEntry) {
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(entry.createdAt))
    val tagsStr = entry.tags.joinToString(", ")
    val text = "${entry.title ?: "Untitled"}\n${dateStr} · ${entry.mood.emoji}\n\n${entry.body}\n\nTags: $tagsStr"
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share entry"))
}

private suspend fun exportEntryAsPdf(context: Context, entry: DiaryEntry) = withContext(Dispatchers.IO) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()

    val margin = 40f
    var yPos = 80f

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
    canvas.drawLine(margin, yPos, 555f, yPos, paint)
    yPos += 25f

    // Body (Manual word wrap)
    paint.textSize = 13f
    val maxWidth = 515f
    val lines = mutableListOf<String>()
    val words = entry.body.split(" ")
    var currentLine = StringBuilder()

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
        if (paint.measureText(testLine) <= maxWidth) {
            currentLine.append(if (currentLine.isEmpty()) word else " $word")
        } else {
            lines.add(currentLine.toString())
            currentLine = StringBuilder(word)
        }
    }
    if (currentLine.isNotEmpty()) {
        lines.add(currentLine.toString())
    }

    for (line in lines) {
        if (yPos > 800f) break // Simple page limit
        canvas.drawText(line, margin, yPos, paint)
        yPos += paint.textSize * 1.5f
    }

    pdfDocument.finishPage(page)

    val fileName = "${entry.title ?: "DiaryEntry"}_${System.currentTimeMillis()}.pdf"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
        resolver.openOutputStream(uri)!!.use { pdfDocument.writeTo(it) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    } else {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
    }
    
    pdfDocument.close()
}
