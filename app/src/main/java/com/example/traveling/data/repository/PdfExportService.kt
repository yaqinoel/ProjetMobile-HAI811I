package com.example.traveling.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.traveling.data.model.RouteStop
import com.example.traveling.data.model.TravelRoute
import java.io.File
import java.io.FileOutputStream

class PdfExportService {

    fun exportItinerary(
        context: Context,
        route: TravelRoute,
        stops: List<RouteStop>,
        destName: String
    ): File? {
        return try {
            val document = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var yPos = 60f

            val titlePaint = Paint().apply {
                textSize = 22f; typeface = Typeface.DEFAULT_BOLD
                color = android.graphics.Color.parseColor("#B91C1C")
            }
            val subtitlePaint = Paint().apply {
                textSize = 14f; color = android.graphics.Color.parseColor("#78716C")
            }
            val bodyPaint = Paint().apply {
                textSize = 12f; color = android.graphics.Color.parseColor("#44403C")
            }
            val boldPaint = Paint().apply {
                textSize = 13f; typeface = Typeface.DEFAULT_BOLD
                color = android.graphics.Color.parseColor("#1C1917")
            }
            val headerLinePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#B91C1C")
                strokeWidth = 2f
            }

            canvas.drawText("🗺️ Voyageur du Monde", 40f, yPos, titlePaint)
            yPos += 28f
            canvas.drawText("Itinéraire : ${route.name} — $destName", 40f, yPos, subtitlePaint)
            yPos += 20f
            canvas.drawLine(40f, yPos, pageWidth - 40f, yPos, headerLinePaint)
            yPos += 20f

            canvas.drawText("Budget : ${route.budget} € | Durée : ${route.duration} | ${stops.size} arrêts | Note : ${route.rating}/5", 40f, yPos, bodyPaint)
            yPos += 30f

            stops.forEachIndexed { index, stop ->

                if (yPos > pageHeight - 100) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 60f
                }

                val circleX = 55f
                val circleY = yPos + 6f
                val circlePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#FEE2E2")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(circleX, circleY, 12f, circlePaint)
                val numPaint = Paint().apply {
                    textSize = 11f; typeface = Typeface.DEFAULT_BOLD
                    color = android.graphics.Color.parseColor("#B91C1C")
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("${index + 1}", circleX, circleY + 4f, numPaint)

                canvas.drawText("${stop.arrivalTime} — ${stop.name}", 80f, yPos + 10f, boldPaint)
                yPos += 22f

                val costStr = if (stop.cost > 0) "${stop.cost} €" else "Gratuit"
                canvas.drawText("${stop.type} · ${stop.duration} · $costStr · ★ ${stop.rating}", 80f, yPos, bodyPaint)
                yPos += 18f

                val desc = if (stop.description.length > 90) stop.description.take(90) + "…" else stop.description
                canvas.drawText(desc, 80f, yPos, subtitlePaint)
                yPos += 18f

                if (index < stops.size - 1 && stops[index + 1].distance != "Départ") {
                    canvas.drawText("→ ${stops[index + 1].distance} (${stops[index + 1].walkTime})", 80f, yPos, subtitlePaint)
                    yPos += 14f
                }

                if (index < stops.size - 1) {
                    val linePaint = Paint().apply {
                        color = android.graphics.Color.parseColor("#FEE2E2")
                        strokeWidth = 1.5f
                    }
                    canvas.drawLine(circleX, circleY + 12f, circleX, yPos + 8f, linePaint)
                }

                yPos += 16f
            }

            yPos += 10f
            canvas.drawLine(40f, yPos, pageWidth - 40f, yPos, headerLinePaint)
            yPos += 18f
            canvas.drawText("Généré par Voyageur du Monde — Bon voyage ! 🌍", 40f, yPos, subtitlePaint)

            document.finishPage(page)

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "itineraire_${destName.replace(" ", "_")}_${route.id}.pdf"
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
