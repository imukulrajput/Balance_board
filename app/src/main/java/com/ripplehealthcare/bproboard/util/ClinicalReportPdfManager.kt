package com.ripplehealthcare.bproboard.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.ripplehealthcare.bproboard.domain.model.*
import com.ripplehealthcare.bproboard.ui.screens.GroupedSession
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ClinicalReportPdfManager {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 50f
    private const val BOTTOM_MARGIN = 50f

    // Helper to match the UI's clinical weighting
    private fun getClinicalWeight(posture: String?, level: Int?): Float {
        val pWeight = if (posture.equals("STANDING", ignoreCase = true)) 1.2f else 1.0f
        val lWeight = when (level) {
            3 -> 1.5f
            2 -> 1.25f
            else -> 1.0f
        }
        return pWeight * lWeight
    }

    fun generateAndSharePdf(
        context: Context,
        patient: Patient,
        session: GroupedSession,
        isShare: Boolean
    ) {
        val pdfDocument = PdfDocument()

        // --- PAINTS SETUP ---
        val titlePaint = Paint().apply { textSize = 24f; isFakeBoldText = true; color = Color.parseColor("#4A44D4") }
        val sectionHeaderPaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.parseColor("#4A44D4") }
        val headerPaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = Color.DKGRAY }
        val textPaint = Paint().apply { textSize = 12f; color = Color.BLACK }
        val smallTextPaint = Paint().apply { textSize = 10f; color = Color.GRAY }

        val successPaint = Paint().apply { textSize = 12f; color = Color.parseColor("#4CAF50") }
        val failPaint = Paint().apply { textSize = 12f; color = Color.parseColor("#E53935") }

        // Graph Paints
        val graphBorderPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.LTGRAY; isAntiAlias = true }
        val graphFillTeal = Paint().apply { style = Paint.Style.FILL; color = Color.parseColor("#4D188B97"); isAntiAlias = true }
        val graphStrokeTeal = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.parseColor("#188B97"); isAntiAlias = true }
        val graphTextPaint = Paint().apply { textSize = 10f; color = Color.DKGRAY; textAlign = Paint.Align.CENTER; isAntiAlias = true }

        val dateFormater = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

        // --- PAGE TRACKING ---
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        var currentY = MARGIN

        fun checkPageBreak(requiredSpace: Float) {
            if (currentY + requiredSpace > PAGE_HEIGHT - BOTTOM_MARGIN) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN
            }
        }

        // ==========================================
        // 1. HEADER & PATIENT INFO
        // ==========================================
        canvas.drawText("BPro Board - Comprehensive Clinical Report", MARGIN, currentY, titlePaint)
        currentY += 40f

        canvas.drawText("Patient Name: ${patient.name}", MARGIN, currentY, headerPaint)
        currentY += 20f
        canvas.drawText("Patient ID: ${patient.patientId} | Age: ${patient.age} | Gender: ${patient.gender}", MARGIN, currentY, textPaint)
        currentY += 20f
        canvas.drawText("Session Date: ${dateFormater.format(session.timestamp)}", MARGIN, currentY, textPaint)
        currentY += 40f

        // ==========================================
        // 2. OVERALL SUMMARY METRICS
        // ==========================================
        canvas.drawText("Session Overview", MARGIN, currentY, sectionHeaderPaint)
        currentY += 25f
        canvas.drawText("Total Modules Played: ${session.totalModulesPlayed}", MARGIN, currentY, textPaint)
        currentY += 20f
        canvas.drawText("Total Falls Recorded: ${session.totalFalls}", MARGIN, currentY, textPaint)
        currentY += 20f
        canvas.drawText("Total Active Time: ${session.totalTimeMs / 1000} seconds", MARGIN, currentY, textPaint)
        currentY += 40f

        // ==========================================
        // 3. CLINICAL GRAPHS (Radar & Heatmap)
        // ==========================================
        val latestStatic = session.staticResults.maxByOrNull { it.timestamp }
        val latestPattern = session.patternResults.maxByOrNull { it.timestamp }
        val latestShape = session.shapeResults.maxByOrNull { it.timestamp }

        val hasClinicalData = latestStatic != null || latestPattern != null || latestShape != null

        if (hasClinicalData) {
            checkPageBreak(250f)
            canvas.drawText("Clinical Biomechanical Analysis", MARGIN, currentY, sectionHeaderPaint)
            currentY += 30f

            val graphCenterY = currentY + 100f
            val radarCenterX = MARGIN + 100f
            val heatmapCenterX = PAGE_WIDTH - MARGIN - 100f
            val graphRadius = 80f

            // --- DRAW RADAR CHART ---
            canvas.drawText("Functional Domains", radarCenterX, currentY, graphTextPaint)

            // Draw background web
            for (i in 1..4) {
                val r = graphRadius * (i / 4f)
                canvas.drawCircle(radarCenterX, graphCenterY, r, graphBorderPaint)
            }

            val labels = listOf("Endurance", "Control", "Agility")
            val rawStatic = (latestStatic?.efficiencyPercentage?.toFloat() ?: 0f) / 100f
            val rawPattern = if (latestPattern != null && latestPattern.totalTargets > 0) (latestPattern.targetsHit.toFloat() / latestPattern.totalTargets) else 0f
            val rawShape = (latestShape?.score?.toFloat() ?: 0f) / 10f

            val staticScore = (rawStatic * getClinicalWeight(latestStatic?.gameMode, latestStatic?.level)).coerceIn(0f, 1f)
            val patternScore = (rawPattern * getClinicalWeight(latestPattern?.gameMode, latestPattern?.level)).coerceIn(0f, 1f)
            val shapeScore = (rawShape * getClinicalWeight(latestShape?.gameMode, latestShape?.level)).coerceIn(0f, 1f)
            val scores = listOf(staticScore, patternScore, shapeScore)

            val radarPath = Path()
            for (i in 0..2) {
                val angle = -Math.PI / 2 + (2 * Math.PI * i / 3).toFloat()
                val r = graphRadius * scores[i]
                val x = radarCenterX + (r * cos(angle)).toFloat()
                val y = graphCenterY + (r * sin(angle)).toFloat()

                // Draw Axis Line & Label
                val axisX = radarCenterX + (graphRadius * 1.2f * cos(angle)).toFloat()
                val axisY = graphCenterY + (graphRadius * 1.2f * sin(angle)).toFloat()
                canvas.drawLine(radarCenterX, graphCenterY, axisX, axisY, graphBorderPaint)
                canvas.drawText(labels[i], axisX, axisY + 15f, smallTextPaint)

                if (i == 0) radarPath.moveTo(x, y) else radarPath.lineTo(x, y)
                canvas.drawCircle(x, y, 4f, graphStrokeTeal.apply { style = Paint.Style.FILL })
            }
            radarPath.close()
            canvas.drawPath(radarPath, graphFillTeal)
            canvas.drawPath(radarPath, graphStrokeTeal)

            // --- DRAW SWAY HEATMAP ---
            canvas.drawText("Center of Mass Sway", heatmapCenterX, currentY, graphTextPaint)
            canvas.drawCircle(heatmapCenterX, graphCenterY, graphRadius, graphBorderPaint)
            canvas.drawLine(heatmapCenterX - graphRadius, graphCenterY, heatmapCenterX + graphRadius, graphCenterY, graphBorderPaint)
            canvas.drawLine(heatmapCenterX, graphCenterY - graphRadius, heatmapCenterX, graphCenterY + graphRadius, graphBorderPaint)

            val maxTilt = 16f
            fun plotPoints(frontal: List<Float>, sagittal: List<Float>, colorStr: String) {
                val pointPaint = Paint().apply { style = Paint.Style.FILL; color = Color.parseColor(colorStr); alpha = 80; isAntiAlias = true }
                val sizeToUse = minOf(frontal.size, sagittal.size)
                for (i in 0 until sizeToUse) {
                    val rawX = (sagittal[i] / maxTilt) * graphRadius
                    val rawY = -(frontal[i] / maxTilt) * graphRadius
                    val dist = sqrt(rawX * rawX + rawY * rawY)
                    val scale = if (dist > graphRadius) graphRadius / dist else 1f

                    val px = heatmapCenterX + (rawX * scale)
                    val py = graphCenterY + (rawY * scale)
                    canvas.drawCircle(px, py, 2f, pointPaint)
                }
            }

            if (latestStatic != null) plotPoints(latestStatic.frontalData, latestStatic.sagittalData, "#188B97") // Teal
            if (latestPattern != null) plotPoints(latestPattern.frontalData, latestPattern.sagittalData, "#4CAF50") // Green
            if (latestShape != null) plotPoints(latestShape.frontalData, latestShape.sagittalData, "#E53935") // Red

            currentY += 220f
        }

        // ==========================================
        // 4. DETAILED MODULE BREAKDOWN
        // ==========================================
        checkPageBreak(60f)
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, graphBorderPaint)
        currentY += 30f
        canvas.drawText("Detailed Module Metrics", MARGIN, currentY, sectionHeaderPaint)
        currentY += 30f

        // --- STATIC BALANCE ---
        if (session.staticResults.isNotEmpty()) {
            checkPageBreak(40f + (session.staticResults.size * 30f))
            canvas.drawText("Static Balance Training", MARGIN, currentY, headerPaint); currentY += 20f
            session.staticResults.forEachIndexed { index, res ->
                val timeStr = "${res.totalTimeMs / 1000}s"
                val balStr = "${res.balanceTimeMs / 1000}s"
                canvas.drawText("  Run ${index + 1} (${res.gameMode} LVL ${res.level}):", MARGIN, currentY, headerPaint); currentY += 15f
                canvas.drawText("    • Efficiency: ${res.efficiencyPercentage}% | Balanced: $balStr / $timeStr", MARGIN, currentY, textPaint); currentY += 15f
                canvas.drawText("    • Falls: ${res.fallCount} | Avg Error: ${res.fallErrors.takeIf{it.isNotEmpty()}?.average()?.let{ String.format("%.1f", it) } ?: "0"}°", MARGIN, currentY, textPaint); currentY += 20f
            }
        }

        // --- PATTERN DRAWING ---
        if (session.patternResults.isNotEmpty()) {
            checkPageBreak(40f + (session.patternResults.size * 30f))
            canvas.drawText("Pattern Kinematic Control", MARGIN, currentY, headerPaint); currentY += 20f
            session.patternResults.forEachIndexed { index, res ->
                canvas.drawText("  Run ${index + 1} (${res.gameMode} - ${res.levelName}):", MARGIN, currentY, headerPaint); currentY += 15f
                canvas.drawText("    • Accuracy: ${res.targetsHit} / ${res.totalTargets} Targets Hit | Time: ${res.timeTakenMs/1000}s", MARGIN, currentY, textPaint); currentY += 15f
                canvas.drawText("    • Falls: ${res.fallCount} | Path Error: ${res.angularErrors.takeIf{it.isNotEmpty()}?.average()?.let{ String.format("%.1f", it) } ?: "0"}°", MARGIN, currentY, textPaint); currentY += 20f
            }
        }

        // --- SHAPE TRAINING ---
        if (session.shapeResults.isNotEmpty()) {
            checkPageBreak(40f + (session.shapeResults.size * 30f))
            canvas.drawText("Shape Reactive Agility", MARGIN, currentY, headerPaint); currentY += 20f
            session.shapeResults.forEachIndexed { index, res ->
                canvas.drawText("  Run ${index + 1} (${res.gameMode} LVL ${res.level}):", MARGIN, currentY, headerPaint); currentY += 15f
                canvas.drawText("    • Score: ${res.score} shapes collected | Time: ${res.timeTakenMs/1000}s", MARGIN, currentY, textPaint); currentY += 15f
                canvas.drawText("    • Falls: ${res.fallCount} | Reaction Error: ${res.angularErrors.takeIf{it.isNotEmpty()}?.average()?.let{ String.format("%.1f", it) } ?: "0"}°", MARGIN, currentY, textPaint); currentY += 20f
            }
        }

        // --- GAMIFICATION MODULES ---
        val hasGames = session.colorSorterResults.isNotEmpty() || session.ratPuzzleResults.isNotEmpty() || session.starshipResults.isNotEmpty() || session.holePuzzleResults.isNotEmpty() || session.stepGameResults.isNotEmpty()

        if (hasGames) {
            checkPageBreak(60f)
            canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, graphBorderPaint)
            currentY += 30f
            canvas.drawText("Neuro-Motor Gamification Metrics", MARGIN, currentY, sectionHeaderPaint)
            currentY += 30f

            if (session.colorSorterResults.isNotEmpty()) {
                checkPageBreak(40f + (session.colorSorterResults.size * 25f))
                canvas.drawText("Color Sorter (Reactive Speed):", MARGIN, currentY, headerPaint); currentY += 20f
                session.colorSorterResults.forEachIndexed { index, res ->
                    canvas.drawText("  Run ${index + 1}: Score: ${res.score} | Red: ${res.redCollected} | Green: ${res.greenCollected} | Missed: ${res.missedCount}", MARGIN, currentY, textPaint); currentY += 20f
                }
            }

            if (session.ratPuzzleResults.isNotEmpty()) {
                checkPageBreak(40f + (session.ratPuzzleResults.size * 25f))
                canvas.drawText("Maze Balance (Motor Precision):", MARGIN, currentY, headerPaint); currentY += 20f
                session.ratPuzzleResults.forEachIndexed { index, res ->
                    val paintToUse = if (res.isWin) successPaint else failPaint
                    canvas.drawText("  Run ${index + 1}: ${if(res.isWin) "Solved" else "Failed"} in ${res.timeTakenMs / 1000}s | Lives left: ${res.livesRemaining}", MARGIN, currentY, paintToUse); currentY += 20f
                }
            }

            if (session.starshipResults.isNotEmpty()) {
                checkPageBreak(40f + (session.starshipResults.size * 25f))
                canvas.drawText("Starship Defender (Dynamic Targeting):", MARGIN, currentY, headerPaint); currentY += 20f
                session.starshipResults.forEachIndexed { index, res ->
                    val paintToUse = if (res.isWin) successPaint else failPaint
                    canvas.drawText("  Run ${index + 1}: Score: ${res.score} | Survived: ${res.timeSurvivedMs / 1000}s | Aliens: ${res.aliensDestroyed}", MARGIN, currentY, paintToUse); currentY += 20f
                }
            }

            if (session.holePuzzleResults.isNotEmpty()) {
                checkPageBreak(40f + (session.holePuzzleResults.size * 25f))
                canvas.drawText("Hole Navigator (Dynamic Endurance):", MARGIN, currentY, headerPaint); currentY += 20f
                session.holePuzzleResults.forEachIndexed { index, res ->
                    val paintToUse = if (res.isWin) successPaint else failPaint
                    canvas.drawText("  Run ${index + 1}: Score: ${res.score} | Survived: ${res.timeSurvivedMs / 1000}s | Dodged: ${res.holesDodged}", MARGIN, currentY, paintToUse); currentY += 20f
                }
            }

            if (session.stepGameResults.isNotEmpty()) {
                checkPageBreak(40f + (session.stepGameResults.size * 25f))
                canvas.drawText("Cognitive Stepping (Processing):", MARGIN, currentY, headerPaint); currentY += 20f
                session.stepGameResults.forEachIndexed { index, res ->
                    canvas.drawText("  Run ${index + 1}: Score: ${res.score} | Correct: ${res.correctHits} | Errors: ${res.incorrectHits}", MARGIN, currentY, textPaint); currentY += 20f
                }
            }
        }

        // Finish the final page
        pdfDocument.finishPage(page)

        // ==========================================
        // 5. SAVE & LAUNCH INTENT
        // ==========================================
        val pdfsFolder = File(context.cacheDir, "pdfs")
        if (!pdfsFolder.exists()) pdfsFolder.mkdirs()

        val safePatientName = patient.name.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(pdfsFolder, "Clinical_Report_${safePatientName}.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            launchIntent(context, file, isShare)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
        }
    }

    private fun launchIntent(context: Context, file: File, isShare: Boolean) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(if (isShare) Intent.ACTION_SEND else Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (isShare) {
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BPro Clinical Report")
            }
        }

        val chooserTitle = if (isShare) "Share Report via..." else "Open Report with..."
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}