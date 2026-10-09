package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.util.FileUtils
import com.example.util.TimeFormatter
import java.io.File
import java.io.FileOutputStream
import java.util.Date

data class NoteExportItem(
    val title: String,
    val tagName: String,
    val body: String,
    val writingTimeMs: Long,
    val images: List<String> // relative paths
)

object ExportHelper {

    fun exportPlainText(
        context: Context,
        sectionName: String,
        notes: List<NoteExportItem>
    ): Uri {
        val exportsDir = FileUtils.getExportsDirectory(context)
        val cleanName = sectionName.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30)
        val file = File(exportsDir, "${cleanName}_export_${System.currentTimeMillis()}.txt")

        val stringBuilder = StringBuilder()
        stringBuilder.append("=== SECTION: $sectionName ===\n")
        stringBuilder.append("Exported: ${TimeFormatter.formatDateTime(System.currentTimeMillis())}\n")
        stringBuilder.append("Total Notes: ${notes.size}\n\n")

        notes.forEachIndexed { index, note ->
            stringBuilder.append("--------------------------------------------------\n")
            stringBuilder.append("${index + 1}. ${note.title}\n")
            stringBuilder.append("Tag: ${note.tagName}\n")
            stringBuilder.append("Writing Time: ${TimeFormatter.formatHumanDuration(note.writingTimeMs)}\n\n")
            stringBuilder.append(note.body.trim())
            stringBuilder.append("\n")

            if (note.images.isNotEmpty()) {
                stringBuilder.append("\n[Attached Images]\n")
                note.images.forEach { relPath ->
                    val fileName = File(relPath).name
                    stringBuilder.append("- $fileName\n")
                }
            }
            stringBuilder.append("\n")
        }

        FileOutputStream(file).use { out ->
            out.write(stringBuilder.toString().toByteArray(Charsets.UTF_8))
        }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun exportPdf(
        context: Context,
        sectionName: String,
        notes: List<NoteExportItem>
    ): Uri {
        val exportsDir = FileUtils.getExportsDirectory(context)
        val cleanName = sectionName.replace("[^a-zA-Z0-9_-]".toRegex(), "_").take(30)
        val file = File(exportsDir, "${cleanName}_export_${System.currentTimeMillis()}.pdf")

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        val margin = 40f
        val contentWidth = (pageWidth - (margin * 2)).toInt()

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = margin

        // Paints
        val headerTitlePaint = TextPaint().apply {
            color = Color.rgb(30, 58, 47)
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val metaPaint = TextPaint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            isAntiAlias = true
        }

        val noteTitlePaint = TextPaint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val tagBadgePaint = TextPaint().apply {
            color = Color.rgb(13, 148, 136)
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11.5f
            isAntiAlias = true
        }

        val dividerPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        // Helper to advance page
        fun newPage() {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin
        }

        // Draw Document Header
        canvas.drawText("Fieldnotes — $sectionName", margin, currentY + 18f, headerTitlePaint)
        currentY += 28f
        canvas.drawText(
            "Exported ${TimeFormatter.formatDateTime(System.currentTimeMillis())} • ${notes.size} note(s)",
            margin,
            currentY + 10f,
            metaPaint
        )
        currentY += 20f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, dividerPaint)
        currentY += 16f

        // Draw Notes
        notes.forEachIndexed { index, note ->
            // Check if note header fits
            if (currentY + 80f > pageHeight - margin) {
                newPage()
            }

            // Note Title
            canvas.drawText("${index + 1}. ${note.title}", margin, currentY + 14f, noteTitlePaint)
            currentY += 22f

            // Tag & Writing Time meta
            val tagInfo = "Tag: ${note.tagName}   •   Writing Time: ${TimeFormatter.formatHumanDuration(note.writingTimeMs)}"
            canvas.drawText(tagInfo, margin, currentY + 10f, tagBadgePaint)
            currentY += 18f

            // Body text with StaticLayout
            if (note.body.isNotBlank()) {
                val layout = StaticLayout.Builder.obtain(
                    note.body,
                    0,
                    note.body.length,
                    bodyPaint,
                    contentWidth
                )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(2f, 1.15f)
                    .setIncludePad(false)
                    .build()

                // Check page fitting for body
                if (currentY + layout.height > pageHeight - margin) {
                    // Split lines or create new page
                    newPage()
                }

                canvas.save()
                canvas.translate(margin, currentY)
                layout.draw(canvas)
                canvas.restore()
                currentY += layout.height + 12f
            } else {
                currentY += 8f
            }

            // Attached Images
            if (note.images.isNotEmpty()) {
                note.images.forEach { relPath ->
                    val file = FileUtils.getFileFromRelativePath(context, relPath)
                    if (file.exists()) {
                        val bitmap = FileUtils.decodeSampledBitmap(file.absolutePath, 500, 400)
                        if (bitmap != null) {
                            // Scale down to fit contentWidth, preserving aspect ratio
                            val maxWidth = contentWidth.toFloat()
                            val maxHeight = 260f
                            val scale = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height, 1f)
                            val drawWidth = bitmap.width * scale
                            val drawHeight = bitmap.height * scale

                            if (currentY + drawHeight > pageHeight - margin) {
                                newPage()
                            }

                            val rect = RectF(margin, currentY, margin + drawWidth, currentY + drawHeight)
                            canvas.drawBitmap(bitmap, null, rect, null)
                            bitmap.recycle()
                            currentY += drawHeight + 10f
                        }
                    }
                }
            }

            // Separator between notes
            currentY += 10f
            if (currentY + 20f > pageHeight - margin) {
                newPage()
            } else {
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, dividerPaint)
                currentY += 16f
            }
        }

        pdfDocument.finishPage(page)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun shareExport(context: Context, fileUri: Uri, mimeType: String, chooserTitle: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
