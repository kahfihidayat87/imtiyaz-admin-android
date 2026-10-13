package com.imtiyaztour.admin

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fase 3 D3 rev5: Export daftar kamar ke PDF profesional (A4, native Android).
 */
object RoomListPdfExporter {

    private const val A4_W = 595
    private const val A4_H = 842
    private const val M = 40f
    private const val CONTENT_W = A4_W - 2 * M
    private const val ROW_H = 22f
    private const val TABLE_HEAD_H = 26f
    private const val FOOTER_Y = A4_H - 35f

    private val BRAND = Color.rgb(15, 122, 90)
    private val TEXT_DARK = Color.rgb(17, 24, 39)
    private val TEXT_GRAY = Color.rgb(107, 114, 128)
    private val BORDER = Color.rgb(209, 213, 219)
    private val ROW_ALT = Color.rgb(249, 250, 251)

    private const val W_NO = 32f
    private const val W_KAMAR = 75f
    private const val W_TIPE = 70f
    private const val W_KAP = 40f
    private fun wPenghuni() = CONTENT_W - W_NO - W_KAMAR - W_TIPE - W_KAP

    fun export(context: Context, trip: Trip, hotel: Hotel, rooms: List<Room>): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        val file = File(dir, "roomlist_${hotel.id}_$ts.pdf")

        val sorted = rooms.sortedBy { it.nomor }
        val totalPax = sorted.sumOf { it.penghuni.size }
        val totalKap = sorted.sumOf { it.kapasitas }

        val rowsFirstPage = 20
        val rowsNextPage = 26
        val totalPages = when {
            sorted.isEmpty() -> 1
            sorted.size <= rowsFirstPage -> 1
            else -> 1 + ((sorted.size - rowsFirstPage + rowsNextPage - 1) / rowsNextPage)
        }

        val doc = PdfDocument()
        var pageNum = 1
        var idx = 0

        // Paints
        val pTitle = Paint().apply { color = Color.WHITE; textSize = 18f; isFakeBoldText = true; isAntiAlias = true }
        val pSub = Paint().apply { color = Color.WHITE; textSize = 10f; isAntiAlias = true }
        val pLabel = Paint().apply { color = TEXT_GRAY; textSize = 9f; isAntiAlias = true }
        val pValue = Paint().apply { color = TEXT_DARK; textSize = 10f; isAntiAlias = true }
        val pValueBold = Paint().apply { color = TEXT_DARK; textSize = 10f; isFakeBoldText = true; isAntiAlias = true }
        val pTh = Paint().apply { color = Color.WHITE; textSize = 10f; isFakeBoldText = true; isAntiAlias = true }
        val pTd = Paint().apply { color = TEXT_DARK; textSize = 10f; isAntiAlias = true }
        val pFooter = Paint().apply { color = TEXT_GRAY; textSize = 8f; isAntiAlias = true }
        val pBorder = Paint().apply { color = BORDER; strokeWidth = 0.6f; style = Paint.Style.STROKE; isAntiAlias = true }
        val pBrandBg = Paint().apply { color = BRAND; style = Paint.Style.FILL }
        val pRowAlt = Paint().apply { color = ROW_ALT; style = Paint.Style.FILL }
        val pLogo = Paint().apply { color = Color.argb(50, 255, 255, 255); style = Paint.Style.FILL; isAntiAlias = true }
        val pLogoText = Paint().apply { color = Color.WHITE; textSize = 15f; isFakeBoldText = true; isAntiAlias = true }

        while (idx < sorted.size || pageNum == 1) {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(A4_W, A4_H, pageNum).create())
            val c = page.canvas

            // HEADER
            c.drawRect(0f, 0f, A4_W.toFloat(), 85f, pBrandBg)
            c.drawText("DAFTAR KAMAR HOTEL", M, 42f, pTitle)
            c.drawText("Imtiyaz Tour & Travel  ·  pastiumrah.com", M, 62f, pSub)
            c.drawCircle(A4_W - 55f, 42f, 28f, pLogo)
            c.drawText("IT", A4_W - 66f, 48f, pLogoText)

            var y = 110f

            // INFO BLOCK (page 1)
            if (pageNum == 1) {
                val boxTop = y
                val boxH = 76f
                c.drawRoundRect(RectF(M, boxTop, A4_W - M, boxTop + boxH), 6f, 6f, pBorder)
                val labelX = M + 12f
                val valueX = M + 90f
                c.drawText("Trip", labelX, boxTop + 18f, pLabel)
                c.drawText(trip.nama, valueX, boxTop + 18f, pValueBold)
                c.drawText("Hotel", labelX, boxTop + 36f, pLabel)
                c.drawText("${hotel.nama}  (${hotel.tipe.uppercase()})", valueX, boxTop + 36f, pValue)
                c.drawText("Total", labelX, boxTop + 54f, pLabel)
                c.drawText("${sorted.size} kamar  ·  $totalPax / $totalKap pax terisi", valueX, boxTop + 54f, pValue)
                val now = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date())
                c.drawText("Dicetak", labelX, boxTop + 72f, pLabel)
                c.drawText(now, valueX, boxTop + 72f, pValue)
                y = boxTop + boxH + 14f
            }

            // TABLE HEADER
            c.drawRect(M, y, A4_W - M, y + TABLE_HEAD_H, pBrandBg)
            var cx = M + 8f
            val thY = y + 17f
            c.drawText("NO", cx, thY, pTh); cx += W_NO
            c.drawText("KAMAR", cx, thY, pTh); cx += W_KAMAR
            c.drawText("TIPE", cx, thY, pTh); cx += W_TIPE
            c.drawText("KAP", cx, thY, pTh); cx += W_KAP
            c.drawText("PENGHUNI", cx, thY, pTh)
            y += TABLE_HEAD_H

            val limit = if (pageNum == 1) rowsFirstPage else rowsNextPage
            var rowsDrawn = 0

            while (idx < sorted.size && rowsDrawn < limit) {
                val r = sorted[idx]
                val rowTop = y
                if (idx % 2 == 1) {
                    c.drawRect(M, rowTop, A4_W - M, rowTop + ROW_H, pRowAlt)
                }
                c.drawLine(M, rowTop + ROW_H, A4_W - M, rowTop + ROW_H, pBorder)
                var vx = M + W_NO
                c.drawLine(vx, rowTop, vx, rowTop + ROW_H, pBorder); vx += W_KAMAR
                c.drawLine(vx, rowTop, vx, rowTop + ROW_H, pBorder); vx += W_TIPE
                c.drawLine(vx, rowTop, vx, rowTop + ROW_H, pBorder); vx += W_KAP
                c.drawLine(vx, rowTop, vx, rowTop + ROW_H, pBorder)

                val ty = rowTop + 15f
                var tx = M + 8f
                c.drawText("${idx + 1}", tx, ty, pTd); tx += W_NO
                c.drawText(r.nomor, tx, ty, pValueBold); tx += W_KAMAR
                c.drawText(r.tipe, tx, ty, pTd); tx += W_TIPE
                c.drawText("${r.kapasitas}", tx, ty, pTd); tx += W_KAP

                val penghuniStr = if (r.penghuni.isEmpty()) "— kosong —"
                    else r.penghuni.joinToString(", ") { it.nama }
                c.drawText(truncate(penghuniStr, wPenghuni(), pTd), tx, ty, pTd)

                y += ROW_H
                idx++
                rowsDrawn++
            }

            if (idx >= sorted.size) {
                y += 14f
                c.drawText(
                    "TOTAL: ${sorted.size} kamar  ·  $totalPax pax  ·  ${totalKap - totalPax} kursi tersisa",
                    M, y, pValueBold
                )
            }

            // FOOTER
            c.drawLine(M, FOOTER_Y - 6f, A4_W - M, FOOTER_Y - 6f, pBorder)
            c.drawText("Imtiyaz Tour & Travel", M, FOOTER_Y + 6f, pFooter)
            val footerRight = "Halaman $pageNum dari $totalPages"
            val footerW = pFooter.measureText(footerRight)
            c.drawText(footerRight, A4_W - M - footerW, FOOTER_Y + 6f, pFooter)

            doc.finishPage(page)
            pageNum++

            if (idx >= sorted.size) break
        }

        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun truncate(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth - 8f) return text
        var s = text
        while (s.length > 3 && paint.measureText("$s…") > maxWidth - 8f) {
            s = s.substring(0, s.length - 1)
        }
        return "$s…"
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
