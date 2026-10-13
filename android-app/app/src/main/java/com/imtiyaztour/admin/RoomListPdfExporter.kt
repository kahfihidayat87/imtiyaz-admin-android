package com.imtiyaztour.admin

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fase 3 D3 rev4: Export daftar kamar ke PDF (A4, tanpa library tambahan).
 */
object RoomListPdfExporter {

    fun export(context: Context, trip: Trip, hotel: Hotel, rooms: List<Room>): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val file = File(dir, "roomlist_${hotel.id}_$ts.pdf")

        val doc = PdfDocument()
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK; textSize = 16f; isFakeBoldText = true; isAntiAlias = true
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK; textSize = 10f; isFakeBoldText = true; isAntiAlias = true
        }
        val paint = Paint().apply {
            color = Color.BLACK; textSize = 10f; isAntiAlias = true
        }
        val grayPaint = Paint().apply {
            color = Color.GRAY; textSize = 9f; isAntiAlias = true
        }

        var y = 45f
        val left = 40f

        canvas.drawText("DAFTAR KAMAR HOTEL", left, y, titlePaint); y += 24
        canvas.drawText("Trip: ${trip.nama}", left, y, paint); y += 14
        canvas.drawText("Hotel: ${hotel.nama} (${hotel.tipe.uppercase()})", left, y, paint); y += 14
        val totalPax = rooms.sumOf { it.penghuni.size }
        val totalKap = rooms.sumOf { it.kapasitas }
        canvas.drawText("Total: ${rooms.size} kamar  -  $totalPax/$totalKap pax", left, y, paint); y += 14
        val now = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        canvas.drawText("Dicetak: $now", left, y, grayPaint); y += 18

        canvas.drawLine(left, y, 555f, y, grayPaint); y += 14
        canvas.drawText("No", 40f, y, boldPaint)
        canvas.drawText("Kamar", 75f, y, boldPaint)
        canvas.drawText("Tipe", 130f, y, boldPaint)
        canvas.drawText("Kap", 185f, y, boldPaint)
        canvas.drawText("Penghuni", 220f, y, boldPaint)
        y += 4
        canvas.drawLine(left, y, 555f, y, grayPaint); y += 14

        rooms.sortedBy { it.nomor }.forEachIndexed { idx, r ->
            if (y > 800) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, idx + 2).create())
                canvas = page.canvas
                y = 45f
            }
            canvas.drawText("${idx + 1}", 40f, y, paint)
            canvas.drawText(r.nomor, 75f, y, paint)
            canvas.drawText(r.tipe, 130f, y, paint)
            canvas.drawText("${r.kapasitas}", 185f, y, paint)
            val penStr = if (r.penghuni.isEmpty()) "(kosong)"
                         else r.penghuni.joinToString(", ") { it.nama }
            canvas.drawText(penStr.take(60), 220f, y, paint)
            y += 14
        }

        y += 10
        canvas.drawText("-- Selesai --", left, y, grayPaint)

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Daftar Kamar Hotel")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan PDF"))
    }
}
