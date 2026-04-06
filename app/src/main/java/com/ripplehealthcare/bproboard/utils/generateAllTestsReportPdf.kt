package com.ripplehealthcare.bproboard.utils

import FourStageResult
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.ripplehealthcare.bproboard.R
import com.ripplehealthcare.bproboard.domain.model.*
import com.ripplehealthcare.bproboard.utils.PhenotypeCalculator.calculateDomainScores
import com.ripplehealthcare.bproboard.utils.ReportInterpretor.getClinicalInterpretation
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.ceil

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val MARGIN = 40f
private const val PAGE_CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
private const val ROW_HEIGHT = 20f
private const val SECTION_SPACING = 40f
private const val FOOTER_HEIGHT = 130f
private const val CONTENT_BOTTOM_LIMIT = PAGE_HEIGHT - FOOTER_HEIGHT - 20f

data class AllTestReport(
    val sessionId: String = "",
    val centerId: String = "",
    val doctorId: String = "",
    val patient: Patient,
    val date: String,
    val time: String,
    val doctorNotes: String = "",

    val centerName: String = "",
    val doctorName: String = "",

    val fiveRepResult: FiveRepResult? = null,
    val thirtySecResult: ThirtySecResult? = null,
    val tugResult: TugResult? = null,
    val fourStageResult: FourStageResult? = null,

    val individualScores: Map<TestType, Double> = emptyMap(),
    val totalBalanceScore: Int = 0
)

class ReportRenderer(
    private val context: Context,
    private val document: PdfDocument,
    val report: AllTestReport
) {
    private var pageNumber = 0
    private lateinit var currentPage: PdfDocument.Page
    lateinit var canvas: Canvas
    var yPos = MARGIN

    fun startNewPage() {
        if (pageNumber > 0) {
            drawFooter()
            document.finishPage(currentPage)
        }

        pageNumber++
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        currentPage = document.startPage(pageInfo)
        canvas = currentPage.canvas

        yPos = MARGIN

        if (pageNumber == 1) {
            drawHeader("Comprehensive Balance Report")
        }
    }

    fun checkSpace(heightNeeded: Float) {
        if (yPos + heightNeeded > CONTENT_BOTTOM_LIMIT) {
            startNewPage()
        }
    }

    fun finish() {
        if (pageNumber > 0) {
            drawFooter()
            document.finishPage(currentPage)
        }
    }

    // --- Internal Drawing Methods ---

    private fun drawHeader(title: String, isSmall: Boolean = false) {
        if (!isSmall) {
            // Full Header
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ripple_logo)
            if (logoBitmap != null) {
                val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 120, 50, true)
                canvas.drawBitmap(scaledLogo, MARGIN, yPos, null)
            }

            ReportPaints.headerTitle.textAlign = Paint.Align.RIGHT
            canvas.drawText(title, PAGE_WIDTH - MARGIN, yPos + 35f, ReportPaints.headerTitle)
            ReportPaints.headerTitle.textAlign = Paint.Align.LEFT

            yPos += 60f
            canvas.drawLine(MARGIN, yPos, PAGE_WIDTH - MARGIN, yPos, ReportPaints.border)
            yPos += SECTION_SPACING
        } else {
            // Small Header
            canvas.drawText(title, PAGE_WIDTH - MARGIN, yPos + 15f, ReportPaints.headerTitle.apply { textAlign = Paint.Align.RIGHT })
            ReportPaints.headerTitle.textAlign = Paint.Align.LEFT
            yPos += 30f
        }
    }

    private fun drawFooter() {
        val startY = PAGE_HEIGHT - FOOTER_HEIGHT + 20f

        // QR Code
        val qrCodeBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.qr_code)
        if (qrCodeBitmap != null) {
            val scaledQrCode = Bitmap.createScaledBitmap(qrCodeBitmap, 80, 80, true)
            canvas.drawBitmap(scaledQrCode, MARGIN, startY, null)
//            canvas.drawText("Click to Contact Us", MARGIN, startY + scaledQrCode.height + 15f, ReportPaints.footerClickContact)
        }

        val contactInfoX = MARGIN + 100f // Adjusted spacing
        var textY = startY + 15f

        drawIconText(R.drawable.ic_phone, "+91 9205140234", contactInfoX, textY)
        drawIconText(R.drawable.ic_web, "www.ripplehealthcare.in", contactInfoX + 130f, textY)
        drawIconText(R.drawable.ic_mail, "info@ripplehealthcare.in", contactInfoX + 290f, textY)

        textY += 40f

        // Address
        val locationIcon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_location)
        if (locationIcon != null) {
            val scaledLoc = Bitmap.createScaledBitmap(locationIcon, 20, 20, true)
            canvas.drawBitmap(scaledLoc, contactInfoX, textY - 10, null)
        }

        val addrTextX = contactInfoX + 30f
        val addrPaint = ReportPaints.footerAddressText
        canvas.drawText("Ripple Healthcare Pvt. Ltd.", addrTextX, textY, addrPaint)
        canvas.drawText("Nasscom Innovation Hub, Plot No. 1, Udyog Vihar Phase 1, Sector 20", addrTextX, textY + 12f, addrPaint)
        canvas.drawText("Gurugram, Haryana - 122022", addrTextX, textY + 24f, addrPaint)

        // Bottom Line
        val lineY = PAGE_HEIGHT - MARGIN
        canvas.drawLine(MARGIN, lineY+15, PAGE_WIDTH - MARGIN, lineY+15, ReportPaints.footerLine)

        // Page Number
        canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN, lineY - 10f, ReportPaints.footerText.apply { textAlign = Paint.Align.RIGHT })
        ReportPaints.footerText.textAlign = Paint.Align.LEFT
    }

    private fun drawIconText(iconId: Int, text: String, x: Float, y: Float) {
        val bitmap = BitmapFactory.decodeResource(context.resources, iconId)
        if (bitmap != null) {
            val scaled = Bitmap.createScaledBitmap(bitmap, 20, 20, true)
            canvas.drawBitmap(scaled, x, y - 10, null)
        }
        canvas.drawText(text, x + 28f, y + 5f, ReportPaints.footerContactText)
    }
}

object ReportPaints {
    val headerTitle = Paint().apply { textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
    val sectionTitle = Paint().apply { textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true; color = Color.rgb(50, 50, 50) }
    val tableHeader = Paint().apply { textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.WHITE; isAntiAlias = true }
    val tableCell = Paint().apply { textSize = 10f; isAntiAlias = true; color = Color.BLACK }
    val tableCellBold = Paint(tableCell).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val highlightGreen = Paint(tableCellBold).apply { color = Color.rgb(0, 128, 0) }
    val highlightRed = Paint(tableCellBold).apply { color = Color.rgb(192, 0, 0) }
    val tableHeaderBackground = Paint().apply { color = Color.rgb(79, 129, 189); style = Paint.Style.FILL }
    val border = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f; style = Paint.Style.STROKE }
    val bar = Paint().apply { color = Color.rgb(155, 187, 217); style = Paint.Style.FILL }
    val chartLabel = Paint().apply { textSize = 8f; color = Color.DKGRAY; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    val normLine = Paint().apply { color = Color.RED; strokeWidth = 1.5f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f) }
    val normLineLabel = Paint().apply { textSize = 8f; color = Color.RED; isAntiAlias = true }

    val barTrackGray = Paint().apply { color = Color.rgb(240, 240, 240); style = Paint.Style.FILL; isAntiAlias = true }
    val barFillGreen = Paint().apply { color = Color.rgb(76, 175, 80); style = Paint.Style.FILL; isAntiAlias = true }
    val barFillRed = Paint().apply { color = Color.rgb(211, 47, 47); style = Paint.Style.FILL; isAntiAlias = true }

    val bubbleGreen = Paint().apply { color = Color.rgb(200, 255, 200); style = Paint.Style.FILL; isAntiAlias = true }
    val bubbleRed = Paint().apply { color = Color.rgb(255, 200, 200); style = Paint.Style.FILL; isAntiAlias = true }

    val interpretationNoteText = Paint().apply {
        textSize = 12f // Larger than standard cell text
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.rgb(30, 30, 30) // Darker for better contrast
        isAntiAlias = true
    }

    val interpretationBoxBg = Paint().apply {
        color = Color.rgb(235, 240, 245) // Slight blue-gray tint to draw the eye
        style = Paint.Style.FILL
    }

    val bubbleText = Paint().apply {
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        color = Color.rgb(40, 40, 40)
    }

    // Graph Paints
    val accLineX = Paint().apply { color = Color.RED; strokeWidth = 1.5f; style = Paint.Style.STROKE; isAntiAlias = true }
    val accLineY = Paint().apply { color = Color.GREEN; strokeWidth = 1.5f; style = Paint.Style.STROKE; isAntiAlias = true }
    val accLineZ = Paint().apply { color = Color.BLUE; strokeWidth = 1.5f; style = Paint.Style.STROKE; isAntiAlias = true }
    val swayZoneGreen = Paint().apply { color = Color.argb(50, 0, 255, 0); style = Paint.Style.FILL }
    val swayZoneRed = Paint().apply { color = Color.argb(50, 255, 0, 0); style = Paint.Style.FILL }
    val swayPath = Paint().apply { color = Color.DKGRAY; strokeWidth = 1.5f; style = Paint.Style.STROKE; isAntiAlias = true }

    // Footer Paints
    val footerLine = Paint().apply { color = Color.rgb(0, 150, 150); strokeWidth = 2f; style = Paint.Style.FILL }
    val footerText = Paint().apply { textSize = 9f; color = Color.GRAY; isAntiAlias = true }
    val footerContactText = Paint().apply { textSize = 10f; color = Color.DKGRAY; isAntiAlias = true }
    val footerAddressText = Paint(footerContactText).apply { textSize = 9f }
    val footerClickContact = Paint().apply { textSize = 9f; color = Color.DKGRAY; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) }
}

@SuppressLint("NewApi")
fun generateAllTestsReportPdf(context: Context, rawReport: AllTestReport, toCache: Boolean = false): Uri? {

    val age = rawReport.patient.age.toIntOrNull() ?: 65
    val gender = Gender.entries.find { it.displayName == rawReport.patient.gender } ?: Gender.OTHER

    val sTug = BalanceScoreCalculator.calculateTimedScore(rawReport.tugResult?.totalTimeSeconds?.toDouble() ?: 0.0,
        getTugNorm(age, gender)?.let { NormativeRange(it.meanValue, it.failureValue) } ?: NormativeRange(10.0, 20.0))

    val s4S = BalanceScoreCalculator.calculateFourStageScore(rawReport.fourStageResult?.stages?.associateBy { it.stageNumber } ?: emptyMap())

    val s30 = BalanceScoreCalculator.calculateRepetitionScore(rawReport.thirtySecResult?.totalRepetitions ?: 0,
        getThirtySTSNorm(age, gender)?.let { NormativeRange(it.meanValue, it.failureValue) } ?: NormativeRange(12.0, 5.0))

    val s5 = BalanceScoreCalculator.calculateTimedScore(rawReport.fiveRepResult?.totalTimeSeconds?.toDouble() ?: 0.0,
        getFiveSTSNorm(age, gender)?.let { NormativeRange(it.meanValue, it.failureValue) } ?: NormativeRange(12.0, 25.0))

    val finalScore = (sTug * 0.35 + s4S * 0.30 + s30 * 0.20 + s5 * 0.15).toInt()
    val reportWithScores = rawReport.copy(
        individualScores = mapOf(TestType.TUG to sTug, TestType.FOUR_STAGE_BALANCE to s4S, TestType.THIRTY_SECONDS to s30, TestType.FIVE_REPS to s5),
        totalBalanceScore = finalScore
    )

    val pdfDocument = PdfDocument()
    val renderer = ReportRenderer(context, pdfDocument, reportWithScores)

    try {
        renderer.startNewPage()

        // 1. Patient Info
        drawPatientInfoTable(renderer, reportWithScores.patient, reportWithScores.date, reportWithScores.time)

        drawBalanceScoreDashboard(renderer, reportWithScores)

        val testCount = listOfNotNull(reportWithScores.fiveRepResult, reportWithScores.thirtySecResult, reportWithScores.tugResult, reportWithScores.fourStageResult).size
        val isSingleTestReport = testCount == 1

        // 2. 5-Rep Test
        if (reportWithScores.fiveRepResult != null) {
            drawFiveRepSection(renderer, reportWithScores.fiveRepResult, reportWithScores.patient)
        }

        // 3. 30-Sec Test
        if (reportWithScores.thirtySecResult != null) {
            drawThirtySecSection(renderer, reportWithScores.thirtySecResult, reportWithScores.patient)
        }

        // 4. TUG Test
        if (reportWithScores.tugResult != null) {
            drawTugSection(renderer, reportWithScores.tugResult, reportWithScores.patient, forceNewPage = !isSingleTestReport)
        }

        // 5. Four Stage Balance
        if (reportWithScores.fourStageResult != null) {
            drawFourStageSection(renderer, reportWithScores.fourStageResult, forceNewPage = !isSingleTestReport)
        }

        drawDoctorAnalysisSection(renderer, reportWithScores.doctorNotes)

        renderer.finish()
        return if (toCache) {
            savePdfToCache(context, pdfDocument)
        } else {
            savePdf(context, pdfDocument)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        pdfDocument.close()
    }
}

private fun drawPatientInfoTable(renderer: ReportRenderer, patient: Patient, date: String, time: String) {
    renderer.checkSpace(100f)
    var y = renderer.yPos
    val canvas = renderer.canvas

    y = drawInfoRow(canvas, y, "Patient Name", patient.name, "Report Date", date)
    y = drawInfoRow(canvas, y, "Phone No.", patient.phone, "Report Time", time)
    y = drawInfoRow(canvas, y, "Age", patient.age, "Gender", patient.gender)
    y = drawInfoRow(canvas, y, "Height(cm)", patient.height, "Weight(kg)", patient.weight)

    renderer.yPos = y + SECTION_SPACING
}

private fun drawBalanceScoreDashboard(renderer: ReportRenderer, report: AllTestReport) {
    renderer.checkSpace(250f)
    val canvas = renderer.canvas
    var y = renderer.yPos

    canvas.drawText("Balance Assessment Summary", MARGIN, y, ReportPaints.sectionTitle)
    y += 40f

    val barWidth = PAGE_CONTENT_WIDTH - 60f
    val spacingBetweenBars = 45f

    // Define the list of tests and their scores
    val tests = listOf(
        "Total Balance Score" to report.totalBalanceScore.toDouble(),
        "TUG (Timed Up and Go)" to (report.individualScores[TestType.TUG] ?: 0.0),
        "4-Stage Balance" to (report.individualScores[TestType.FOUR_STAGE_BALANCE] ?: 0.0),
        "30s Sit to Stand" to (report.individualScores[TestType.THIRTY_SECONDS] ?: 0.0),
        "5-Rep Sit to Stand" to (report.individualScores[TestType.FIVE_REPS] ?: 0.0)
    )

    // Draw each bar
    tests.forEach { (label, score) ->
        drawScoreBar(canvas, MARGIN + 20f, y, barWidth, label, score)
        y += spacingBetweenBars
    }

    val interpretation = getClinicalInterpretation(report)

    y += 10f
    // Create a taller box for the bold, larger text
    val noteBoxHeight = 45f
    val noteRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + noteBoxHeight)
    canvas.drawRoundRect(noteRect, 6f, 6f, ReportPaints.interpretationBoxBg)
    canvas.drawRoundRect(noteRect, 6f, 6f, ReportPaints.border) // Optional subtle border

    // Draw the bold text
    canvas.drawText(interpretation, MARGIN + 15f, y + 28f, ReportPaints.interpretationNoteText)

    y += noteBoxHeight + 40f

    // 3. Domain Score Section
    canvas.drawText("Domain-Specific Performance Analysis", MARGIN, y, ReportPaints.sectionTitle)
    y += 40f

    val domains = calculateDomainScores(report)
    domains.forEach { (label, score) ->
        drawScoreBar(canvas, MARGIN + 20f, y, barWidth, label, score)
        y += spacingBetweenBars
    }
}

private fun drawFiveRepSection(renderer: ReportRenderer, result: FiveRepResult, patient: Patient) {
    // 1. Initial Space Check
    renderer.startNewPage()

    var y = renderer.yPos // Initialize y
    val canvas = renderer.canvas
    val score = renderer.report.individualScores[TestType.FIVE_REPS]?.toInt() ?: 0

    canvas.drawText("5-Rep Sit to Stand Test - Score: $score/100", MARGIN, y, ReportPaints.sectionTitle)
    y += 25f

    // Calculations
    val durations = result.repTimes.map { it / 1000.0 }
    val fatigue = PhenotypeCalculator.calculateFatigueFromDurations(durations)
    val variability = PhenotypeCalculator.calculateRepetitionVariability(result.repTimes)
    val symmetry = PhenotypeCalculator.calculateLegAsymmetry(result.accLeft, result.accRight)

    val leftRepPowers = PhenotypeCalculator.calculatePowerPerRep(
        patient.weight.toDouble(), result.accLeft, result.repTimes, result.calAccLeft
    )
    val rightRepPowers = PhenotypeCalculator.calculatePowerPerRep(
        patient.weight.toDouble(), result.accRight, result.repTimes, result.calAccRight
    )

    // 2. Summary Table
    val colWidth = PAGE_CONTENT_WIDTH / 3
    drawTableCell(canvas, MARGIN, y, colWidth, "Total Time", ReportPaints.tableHeader, true)
    drawTableCell(canvas, MARGIN + colWidth, y, colWidth, "Normative Value", ReportPaints.tableHeader, true)
    drawTableCell(canvas, MARGIN + colWidth*2, y, colWidth, "Comparison", ReportPaints.tableHeader, true)

    y += ROW_HEIGHT
    val genderEnum = Gender.entries.find { it.displayName == patient.gender } ?: Gender.OTHER
    val age = patient.age.toIntOrNull() ?: 60
    val normObj = getFiveSTSNorm(age, genderEnum)
    val normTime = normObj?.meanValue ?: 12.0
    val comparison = evaluateTimePerformance(result.totalTimeSeconds, normTime)

    drawTableCell(canvas, MARGIN, y, colWidth, "${String.format("%.2f", result.totalTimeSeconds)} s", ReportPaints.tableCellBold)
    drawTableCell(canvas, MARGIN + colWidth, y, colWidth, "$normTime s", ReportPaints.tableCell)
    drawTableCell(canvas, MARGIN + colWidth*2, y, colWidth, comparison, getRatingPaint(comparison))

    // Update global yPos
    renderer.yPos = y + ROW_HEIGHT + 20f

    renderer.checkSpace(160f)
    y = renderer.yPos
    val colWidth6 = PAGE_CONTENT_WIDTH / 6

    canvas.drawText("Power Analysis per Repetition (Watts)", MARGIN, y, ReportPaints.tableCellBold)
    y += 15f

    // Header Row
    drawTableCell(canvas, MARGIN, y, colWidth6, "Leg", ReportPaints.tableHeader, true)
    for (i in 1..5) {
        drawTableCell(canvas, MARGIN + colWidth6 * i, y, colWidth6, "Rep $i", ReportPaints.tableHeader, true)
    }

    // Left Leg Row
    y += ROW_HEIGHT
    drawTableCell(canvas, MARGIN, y, colWidth6, "Left (Mean)", ReportPaints.tableCellBold)
    leftRepPowers.forEachIndexed { i, power ->
        drawTableCell(canvas, MARGIN + colWidth6 * (i + 1), y, colWidth6, "${power.first.toInt()}W", ReportPaints.tableCell)
    }

    y += ROW_HEIGHT
    drawTableCell(canvas, MARGIN, y, colWidth6, "Right (Mean)", ReportPaints.tableCellBold)
    rightRepPowers.forEachIndexed { i, power ->
        drawTableCell(canvas, MARGIN + colWidth6 * (i + 1), y, colWidth6, "${power.first.toInt()}W", ReportPaints.tableCell)
    }

    // Symmetry Row
    y += ROW_HEIGHT
    drawTableCell(canvas, MARGIN, y, colWidth6, "Asymmetry", ReportPaints.tableCellBold)
    for (i in 0 until 5) {
        val l = leftRepPowers.getOrNull(i)?.first ?: 0.0
        val r = rightRepPowers.getOrNull(i)?.first ?: 0.0
        val sym = if (l + r > 0) (Math.abs(l - r) / (l + r) * 100).toInt() else 0
        drawTableCell(canvas, MARGIN + colWidth6 * (i + 1), y, colWidth6, "$sym%", ReportPaints.tableCell)
    }

    renderer.yPos = y + ROW_HEIGHT + SECTION_SPACING

    // 3. Draw Advanced Metrics
    renderer.checkSpace(80f)
    y = renderer.yPos

    renderer.canvas.drawText("Advanced Metrics", MARGIN, y, ReportPaints.tableCellBold)
    y += 15f

    drawTableCell(renderer.canvas, MARGIN, y, colWidth, "Fatigue Index", ReportPaints.tableHeader, true)
    drawTableCell(renderer.canvas, MARGIN + colWidth, y, colWidth, "Variability (CV)", ReportPaints.tableHeader, true)
    drawTableCell(renderer.canvas, MARGIN + colWidth*2, y, colWidth, "Asymmetry", ReportPaints.tableHeader, true)

    y += ROW_HEIGHT

    drawTableCell(renderer.canvas, MARGIN, y, colWidth, "${String.format("%.1f", fatigue)}%", ReportPaints.tableCell)
    drawTableCell(renderer.canvas, MARGIN + colWidth, y, colWidth, "${String.format("%.1f", variability)}%", ReportPaints.tableCell)
    drawTableCell(renderer.canvas, MARGIN + colWidth*2, y, colWidth, "${String.format("%.1f", symmetry)}%", ReportPaints.tableCell)


    renderer.yPos = y + ROW_HEIGHT + SECTION_SPACING

    // 4. Repetition Consistency (FIX: Re-sync y after checkSpace)
    renderer.checkSpace(160f)
    y = renderer.yPos // RE-SYNC

    renderer.canvas.drawText("Repetition Consistency", MARGIN, y, ReportPaints.tableCellBold)
    y += 15f
    val normMeanPerRep = (normObj?.meanValue ?: 0.0) / 5.0

    // drawRepetitionBarChart should return the new Y position
    y = drawRepetitionBarChart(renderer.canvas, y, durations, normMeanPerRep)
    renderer.yPos = y + 20f

    // 5. Acceleration Chart (FIX: Re-sync y after checkSpace)
    renderer.checkSpace(160f)
    y = renderer.yPos // RE-SYNC

    renderer.canvas.drawText("Waist Acceleration (Motion Intensity)", MARGIN, y, ReportPaints.tableCellBold)
    y += 15f
    y = drawAccelerationChart(renderer.canvas, y, result.accCenter)

    renderer.yPos = y + SECTION_SPACING
}

private fun drawThirtySecSection(renderer: ReportRenderer, result: ThirtySecResult, patient: Patient) {
    // 1. Initial space check for header and first table
//    renderer.checkSpace(120f)
    renderer.startNewPage()

    var y = renderer.yPos
    val canvas = renderer.canvas
    val score = renderer.report.individualScores[TestType.THIRTY_SECONDS]?.toInt() ?: 0

    canvas.drawText("30-Sec Sit to Stand Test - Score: $score/100", MARGIN, y, ReportPaints.sectionTitle)
    y += 25f

    // 2. Calculations
    val durations = result.repTimestamps.map { it / 1000.0 }
    val fatigue = PhenotypeCalculator.calculateFatigueFromDurations(durations)
    val variability = PhenotypeCalculator.calculateRepetitionVariability(result.repTimestamps)
    val symmetry = PhenotypeCalculator.calculateLegAsymmetry(result.accLeft, result.accRight)

    // 3. Draw Summary Table
    val colWidth = PAGE_CONTENT_WIDTH / 3
    drawTableCell(canvas, MARGIN, y, colWidth, "Total Reps", ReportPaints.tableHeader, true)
    drawTableCell(canvas, MARGIN + colWidth, y, colWidth, "Normative Value", ReportPaints.tableHeader, true)
    drawTableCell(canvas, MARGIN + colWidth*2, y, colWidth, "Comparison", ReportPaints.tableHeader, true)

    y += ROW_HEIGHT
    val genderEnum = Gender.entries.find { it.displayName == patient.gender } ?: Gender.OTHER
    val age = patient.age.toIntOrNull() ?: 60
    val normReps = getThirtySTSNorm(age, genderEnum)?.meanValue ?: 15
    val comparison = evaluateRepPerformance(result.totalRepetitions, normReps.toInt())

    drawTableCell(canvas, MARGIN, y, colWidth, "${result.totalRepetitions}", ReportPaints.tableCellBold)
    drawTableCell(canvas, MARGIN + colWidth, y, colWidth, "$normReps", ReportPaints.tableCell)
    drawTableCell(canvas, MARGIN + colWidth*2, y, colWidth, comparison, getRatingPaint(comparison))

    // Update the renderer's position after the first table
    renderer.yPos = y + ROW_HEIGHT + 20f

    // 4. Draw Phenotype Insights (Fatigue, Variability, Symmetry)
    renderer.checkSpace(80f)
    y = renderer.yPos

    renderer.canvas.drawText("Advanced Metrics", MARGIN, y, ReportPaints.tableCellBold)
    y += 15f

    drawTableCell(renderer.canvas, MARGIN, y, colWidth, "Fatigue Index", ReportPaints.tableHeader, true)
    drawTableCell(renderer.canvas, MARGIN + colWidth, y, colWidth, "Variability (CV)", ReportPaints.tableHeader, true)
    drawTableCell(renderer.canvas, MARGIN + colWidth*2, y, colWidth, "Leg Symmetry", ReportPaints.tableHeader, true)

    y += ROW_HEIGHT
    drawTableCell(renderer.canvas, MARGIN, y, colWidth, "${String.format("%.1f", fatigue)}%", ReportPaints.tableCell)
    drawTableCell(renderer.canvas, MARGIN + colWidth, y, colWidth, "${String.format("%.1f", variability)}%", ReportPaints.tableCell)
    drawTableCell(renderer.canvas, MARGIN + colWidth*2, y, colWidth, "${String.format("%.1f", symmetry)}%", ReportPaints.tableCell)

    renderer.yPos = y + ROW_HEIGHT + SECTION_SPACING

    // 5. Repetition Pace Chart
    renderer.checkSpace(160f)
    renderer.canvas.drawText("Repetition Pace", MARGIN, renderer.yPos, ReportPaints.tableCellBold)
    renderer.yPos = drawRepetitionBarChart(renderer.canvas, renderer.yPos + 5f, durations, 0.0)

    // 6. Acceleration Chart
    renderer.checkSpace(180f)
    renderer.canvas.drawText("Waist Acceleration (Motion Intensity)", MARGIN, renderer.yPos, ReportPaints.tableCellBold)
    renderer.yPos = drawAccelerationChart(renderer.canvas, renderer.yPos + 5f, result.accCenter)
}

private fun drawTugSection(renderer: ReportRenderer, result: TugResult, patient: Patient, forceNewPage: Boolean) {
//    if (forceNewPage) {
//        renderer.startNewPage()
//    } else {
//        renderer.checkSpace(200f) // Ensure enough space if continuing
//    }
    renderer.startNewPage()

    var y = renderer.yPos
    val canvas = renderer.canvas

    val score = renderer.report.individualScores[TestType.TUG]?.toInt() ?: 0
    canvas.drawText("Timed Up and Go (TUG) - Score: $score/100", MARGIN, y, ReportPaints.sectionTitle)
    y += 20f

    val genderEnum = Gender.entries.find { it.displayName == patient.gender } ?: Gender.OTHER
    val age = patient.age.toIntOrNull() ?: 60

    val normTime = getTugNorm(age,genderEnum)?.meanValue?:10.0

    // Custom logic for TUG since we want specific bands
    val comparison = when {
        result.totalTimeSeconds <= normTime -> "Good"
        result.totalTimeSeconds <= (normTime*1.2) -> "Normal"
        else -> "Bad"
    }

    // Draw Summary Table
    val colWidth = PAGE_CONTENT_WIDTH / 3
    drawTableCell(canvas, MARGIN, y, colWidth, "Total Time", ReportPaints.tableHeader, true)
    drawTableCell(canvas, MARGIN + colWidth, y, colWidth, "Normative Value", ReportPaints.tableHeader, true)
    drawTableCell(canvas, MARGIN + colWidth*2, y, colWidth, "Comparison", ReportPaints.tableHeader, true)

    val rowY = y + ROW_HEIGHT
    drawTableCell(canvas, MARGIN, rowY, colWidth, "${result.totalTimeSeconds} s", ReportPaints.tableCellBold)
    drawTableCell(canvas, MARGIN + colWidth, rowY, colWidth, "< 10.0 s", ReportPaints.tableCell)
    drawTableCell(canvas, MARGIN + colWidth*2, rowY, colWidth, comparison, getRatingPaint(comparison))

    y = rowY + ROW_HEIGHT + 30f
    renderer.yPos = y

    renderer.checkSpace(160f)
    y = renderer.yPos
    renderer.canvas.drawText("Waist Acceleration (Motion Analysis)", MARGIN, y, ReportPaints.tableCellBold)
    y += 15f
    y = drawAccelerationChart(renderer.canvas, y, result.accCenter)

    renderer.yPos = y + SECTION_SPACING
}

private fun drawFourStageSection(renderer: ReportRenderer, result: FourStageResult, forceNewPage: Boolean) {
//    if (forceNewPage) {
//        renderer.startNewPage()
//    } else {
//        renderer.checkSpace(200f)
//    }

    renderer.startNewPage()

    var y = renderer.yPos
    val canvas = renderer.canvas

    val score = renderer.report.individualScores[TestType.FOUR_STAGE_BALANCE]?.toInt() ?: 0

    canvas.drawText("Four Stage Balance Test - Score: $score/100", MARGIN, y, ReportPaints.sectionTitle)
    y += 40f

    renderer.yPos = y

    // Grid Layout: 2 rows of 2 circles
    val graphSize = 160f
    // Increased row height slightly to accommodate the new stats text below the graph
    val rowHeight = graphSize + 50f

    // Row 1
    renderer.checkSpace(rowHeight)
    y = renderer.yPos
    val col1X = MARGIN + 40f
    val col2X = MARGIN + PAGE_CONTENT_WIDTH / 2 + 30f

    fun getStagePoints(stageNum: Int) = result.stages.find { it.stageNumber == stageNum }?.pointsCenter ?: emptyList()

    drawSwayChart(renderer.canvas, col1X, y, graphSize, "1. Feet Together", getStagePoints(1))
    drawSwayChart(renderer.canvas, col2X, y, graphSize, "2. Semi-Tandem", getStagePoints(2))

    y += rowHeight
    renderer.yPos = y

    // Row 2
    renderer.checkSpace(rowHeight)
    y = renderer.yPos

    drawSwayChart(renderer.canvas, col1X, y, graphSize, "3. Tandem", getStagePoints(3))
    drawSwayChart(renderer.canvas, col2X, y, graphSize, "4. One Leg", getStagePoints(4))

    renderer.yPos = y + rowHeight + SECTION_SPACING
}

private fun drawDoctorAnalysisSection(renderer: ReportRenderer, notes: String) {
    renderer.startNewPage()

    var y = renderer.yPos
    val canvas = renderer.canvas
    val width = PAGE_CONTENT_WIDTH

    // 1. Title
    canvas.drawText("Doctor's Analysis & Notes", MARGIN, y, ReportPaints.sectionTitle)
    y += 40f

    // 2. Calculate Available Height
    val boxHeight = PAGE_HEIGHT - FOOTER_HEIGHT - y - 20f
    val boxBottom = y + boxHeight

    // 3. Draw Outer Border
    canvas.drawRect(MARGIN, y, MARGIN + width, boxBottom, ReportPaints.border)

    // 4. Draw Ruled Lines
    val lineSpacing = 30f
    var lineY = y + lineSpacing

    val linePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }

    while (lineY < boxBottom) {
        canvas.drawLine(MARGIN, lineY, MARGIN + width, lineY, linePaint)
        lineY += lineSpacing
    }

    // 5. DRAW ACTUAL TEXT (With Dynamic Wrapping)
    if (notes.isNotEmpty()) {
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        // Calculate exact width available for text (Width - Padding)
        val availableTextWidth = width - 20f
        val lines = notes.split("\n")

        // Start drawing text slightly above the first ruled line
        var textY = y + 25f

        for (paragraph in lines) {
            var remaining = paragraph

            // Handle empty lines (user pressed enter multiple times)
            if (remaining.isEmpty()) {
                textY += lineSpacing
                continue
            }

            while (remaining.isNotEmpty()) {
                // Stop if we run out of box space
                if (textY > boxBottom - 5) break

                // breakText returns the number of characters that fit in 'availableTextWidth'
                val charsThatFit = textPaint.breakText(remaining, true, availableTextWidth, null)

                var breakIndex = charsThatFit

                // If the line is longer than fits, try to break at a word boundary (space)
                if (breakIndex < remaining.length) {
                    val lastSpace = remaining.lastIndexOf(' ', breakIndex)
                    // If we found a space, break there. Otherwise (huge word), break mid-word.
                    if (lastSpace != -1) {
                        breakIndex = lastSpace
                    }
                }

                val lineToDraw = remaining.substring(0, breakIndex)
                canvas.drawText(lineToDraw, MARGIN + 10f, textY, textPaint)

                // Move to next line
                textY += lineSpacing

                // Prepare next chunk: Remove the printed part + any leading whitespace (like the split space)
                remaining = remaining.substring(breakIndex).trimStart()
            }
        }
    }

    renderer.yPos = boxBottom
}

private fun drawScoreBar(canvas: Canvas, x: Float, y: Float, width: Float, label: String, score: Double) {
    val barHeight = 12f
    val cornerRadius = 6f
    val isGood = score >= 50

    // 1. Draw Label
    canvas.drawText(label, x, y - 10f, ReportPaints.tableCellBold)

    // 2. Draw Track (Light gray background)
    val trackRect = RectF(x, y, x + width, y + barHeight)
    canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, ReportPaints.barTrackGray)

    // 3. Draw Fill
    val fillWidth = (score / 100.0 * width).toFloat().coerceIn(cornerRadius * 2, width)
    val fillRect = RectF(x, y, x + fillWidth, y + barHeight)
    val fillPaint = if (isGood) ReportPaints.barFillGreen else ReportPaints.barFillRed
    canvas.drawRoundRect(fillRect, cornerRadius, cornerRadius, fillPaint)

    // 4. Draw the Bubble (Indicator)
    val centerX = x + fillWidth
    val centerY = y + (barHeight / 2)

    // Outer Circle of Bubble
    val bubblePaint = if (isGood) ReportPaints.bubbleGreen else ReportPaints.bubbleRed
    canvas.drawCircle(centerX, centerY, 10f, bubblePaint)
    canvas.drawCircle(centerX, centerY, 10f, Paint(fillPaint).apply { style = Paint.Style.STROKE; strokeWidth = 2f })

    // Bubble Text Box (Small rectangle above the circle)
    val textRect = RectF(centerX - 12f, y - 28f, centerX + 12f, y - 12f)
    canvas.drawRoundRect(textRect, 4f, 4f, bubblePaint)

    // Bubble Pointer (Small triangle)
    val path = Path().apply {
        moveTo(centerX - 4f, y - 12f)
        lineTo(centerX + 4f, y - 12f)
        lineTo(centerX, y - 8f)
        close()
    }
    canvas.drawPath(path, bubblePaint)

    // Draw Score Text
    canvas.drawText(score.toInt().toString(), centerX, y - 17f, ReportPaints.bubbleText)
}

private fun evaluateTimePerformance(value: Float, norm: Double): String {
    // Good: Faster than norm
    // Normal: Within 20% slower than norm
    // Average: More than 20% slower
    val limit = norm * 1.2 // 20% buffer
    return when {
        value <= norm -> "Good"
        value <= limit -> "Normal"
        else -> "Bad"
    }
}

private fun getRatingPaint(rating: String): Paint {
    return when (rating) {
        "Good" -> ReportPaints.highlightGreen // Green
        "Normal" -> Paint(ReportPaints.tableCellBold).apply { color = Color.rgb(255, 165, 0) } // Orange
        "Bad" -> ReportPaints.highlightRed   // Red
        else -> ReportPaints.tableCellBold
    }
}

private fun evaluateRepPerformance(value: Int, norm: Int): String {
    // Good: More than or equal to norm
    // Normal: Within 20% fewer than norm
    // Average: More than 20% fewer
    val limit = norm * 0.8 // 20% buffer
    return when {
        value >= norm -> "Good"
        value >= limit -> "Normal"
        else -> "Bad"
    }
}
// ================= LOW LEVEL DRAWING HELPERS =================

private fun drawInfoRow(canvas: Canvas, y: Float, l1: String, v1: String, l2: String, v2: String): Float {
    val mid = PAGE_WIDTH / 2f
    canvas.drawText("$l1: ", MARGIN, y + 15, ReportPaints.tableCellBold)
    canvas.drawText(v1, MARGIN + 80, y + 15, ReportPaints.tableCell)

    canvas.drawText("$l2: ", mid, y + 15, ReportPaints.tableCellBold)
    canvas.drawText(v2, mid + 80, y + 15, ReportPaints.tableCell)
    return y + ROW_HEIGHT
}

private fun drawSummaryTable(canvas: Canvas, y: Float, label: String, value: String, norm: String, comp: String): Float {
    val colWidth = PAGE_CONTENT_WIDTH / 3

    // Headers
    drawTableCell(canvas, MARGIN, y, colWidth, label, ReportPaints.tableHeader, true)
    drawTableCell(canvas, MARGIN + colWidth, y, colWidth, "Normative Value", ReportPaints.tableHeader, true)
    drawTableCell(canvas, MARGIN + colWidth*2, y, colWidth, "Score", ReportPaints.tableHeader, true)

    // Values
    val rowY = y + ROW_HEIGHT
    drawTableCell(canvas, MARGIN, rowY, colWidth, value, ReportPaints.tableCellBold)
    drawTableCell(canvas, MARGIN + colWidth, rowY, colWidth, norm, ReportPaints.tableCell)

    val compPaint = if (comp.contains("Below") || comp.contains("Risk")) ReportPaints.highlightRed else ReportPaints.highlightGreen
    drawTableCell(canvas, MARGIN + colWidth*2, rowY, colWidth, comp, compPaint)

    return rowY + ROW_HEIGHT + 10f
}

private fun drawTableCell(canvas: Canvas, x: Float, y: Float, width: Float, text: String, paint: Paint, isHeader: Boolean = false) {
    if (isHeader) {
        canvas.drawRect(x, y, x + width, y + ROW_HEIGHT, ReportPaints.tableHeaderBackground)
    }
    canvas.drawRect(x, y, x + width, y + ROW_HEIGHT, ReportPaints.border)
    canvas.drawText(text, x + 5f, y + ROW_HEIGHT / 2 + 5, paint)
}

private fun drawAccelerationChart(canvas: Canvas, y: Float, points: List<AccPoint>): Float {
    val height = 120f
    val width = PAGE_CONTENT_WIDTH
    val bottom = y + height

    // --- 1. Define Layout with Label Space ---
    val yLabelWidth = 25f // Space reserved for "-2.0", "4.5", etc.
    val chartLeft = MARGIN + yLabelWidth
    val chartRight = MARGIN + width
    val chartWidth = chartRight - chartLeft

    // Draw Box around the chart area (excluding labels)
    canvas.drawRect(chartLeft, y, chartRight, bottom, ReportPaints.border)

    if (points.isEmpty()) {
        canvas.drawText("No Data", chartLeft + chartWidth/2, y + height/2, ReportPaints.chartLabel)
        return bottom + 25f
    }

    // --- 2. Calculate Range ---
    // Ensure range is at least +/- 2g for readability
    val maxVal = points.maxOf { maxOf(it.x, it.y, it.z) }.let { if (it < 2f) 2f else it }
    val minVal = points.minOf { minOf(it.x, it.y, it.z) }.let { if (it > -2f) -2f else it }
    val range = maxVal - minVal

    // --- 3. Draw Y-Axis Labels & Grid Lines ---
    // We will draw 5 labels: Min, 25%, 50% (Mid), 75%, Max
    val steps = 4
    for (i in 0..steps) {
        val fraction = i.toFloat() / steps
        val value = minVal + (range * fraction)
        val yPos = bottom - (height * fraction)

        // Draw Label (Right-aligned to the chart border)
        val label = String.format("%.1f", value)
        val textPaint = Paint(ReportPaints.chartLabel).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(label, chartLeft - 5f, yPos + 3f, textPaint) // -5f padding

        // Draw Grid Line (Optional: lighter line for better readability)
        if (i > 0 && i < steps) { // Don't draw over top/bottom borders
            canvas.drawLine(chartLeft, yPos, chartRight, yPos, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })
        }
    }

    // Draw distinct Zero Line if it's within the graph
    if (minVal < 0 && maxVal > 0) {
        val zeroY = bottom - (height * (0 - minVal) / range)
        canvas.drawLine(chartLeft, zeroY, chartRight, zeroY, Paint().apply { color = Color.GRAY; strokeWidth = 1f })
    }

    // --- 4. Draw Data Lines ---
    val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

    fun drawLine(extractor: (AccPoint) -> Float, paint: Paint) {
        val pts = FloatArray((points.size - 1) * 4)
        var idx = 0
        for (i in 0 until points.size - 1) {
            val y1 = bottom - (height * (extractor(points[i]) - minVal) / range)
            val y2 = bottom - (height * (extractor(points[i+1]) - minVal) / range)

            // Clamp values to stay strictly inside box
            val clampedY1 = y1.coerceIn(y, bottom)
            val clampedY2 = y2.coerceIn(y, bottom)

            pts[idx++] = chartLeft + (i * stepX)
            pts[idx++] = clampedY1
            pts[idx++] = chartLeft + ((i + 1) * stepX)
            pts[idx++] = clampedY2
        }
        canvas.drawLines(pts, paint)
    }

    drawLine({ it.x }, ReportPaints.accLineX)
    drawLine({ it.y }, ReportPaints.accLineY)
    drawLine({ it.z }, ReportPaints.accLineZ)

    // --- 5. Draw Legend ---
    val legendY = bottom + 15f
    val legX = MARGIN + yLabelWidth + 10f // Align with chart
    canvas.drawText("X-Axis (Left-Right)", legX, legendY, Paint(ReportPaints.chartLabel).apply { color = Color.RED })
    canvas.drawText("Y-Axis (Up-Down)", legX + 100f, legendY, Paint(ReportPaints.chartLabel).apply { color = Color.GREEN })
    canvas.drawText("Z-Axis (Forward-Backward)", legX + 200f, legendY, Paint(ReportPaints.chartLabel).apply { color = Color.BLUE })

    return legendY + 15f
}

fun drawRepetitionBarChart(canvas: Canvas, y: Float, repTimes: List<Double>, normMeanPerRep: Double): Float {
    if (repTimes.isEmpty()) return y

    var currentY = y + 20f
    val chartHeight = 120f
    val chartBottom = currentY + chartHeight
    val chartLeft = MARGIN + 30f
    val chartRight = PAGE_WIDTH - MARGIN - 30f

    val maxTime = (repTimes.maxOrNull() ?: 0.0)
    // Ensure ceiling is at least slightly higher than max to avoid text clipping
    val ceilingVal = ceil(maxOf(maxTime, normMeanPerRep)).toFloat().coerceAtLeast(3f)

    // Axis Lines
    canvas.drawLine(chartLeft, currentY, chartLeft, chartBottom, ReportPaints.border)
    canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, ReportPaints.border)

    // Y-Axis Labels & Grid
//    for (i in 0..ceilingVal.toInt()) {
//        val ly = chartBottom - (i / ceilingVal) * chartHeight
//        canvas.drawText("${i}s", MARGIN + 5f, ly + 4f, ReportPaints.chartLabel)
//        canvas.drawLine(chartLeft, ly, chartRight, ly, ReportPaints.border)
//    }

    // Norm Line
    if (normMeanPerRep > 0) {
        val ny = chartBottom - (normMeanPerRep.toFloat() / ceilingVal) * chartHeight
        canvas.drawLine(chartLeft, ny, chartRight, ny, ReportPaints.normLine)
        canvas.drawText("Norm", chartRight + 5f, ny + 3f, ReportPaints.normLineLabel)
    }

    // Bars
    val slotWidth = (chartRight - chartLeft) / repTimes.size
    val barWidth = slotWidth * 0.6f

    repTimes.forEachIndexed { index, time ->
        val safeTime = time.toFloat().coerceAtLeast(0f)
        val barH = (safeTime / ceilingVal) * chartHeight
        val barLeft = chartLeft + (index * slotWidth) + (slotWidth * 0.2f)
        val barTop = chartBottom - barH

        // Draw Bar
        canvas.drawRect(barLeft, barTop, barLeft + barWidth, chartBottom, ReportPaints.bar)

        // Draw X-Axis Label (Rep Number)
        canvas.drawText("${index + 1}", barLeft + barWidth/2, chartBottom + 12f, ReportPaints.chartLabel)

        // --- NEW: Draw Value on Top of Bar ---
        val valueText = String.format("%.1fs", safeTime)
        // Position text 5 pixels above the bar
        canvas.drawText(valueText, barLeft + barWidth/2, barTop - 5f, ReportPaints.chartLabel)
    }

    return chartBottom + 30f
}

private fun drawSwayChart(canvas: Canvas, x: Float, y: Float, size: Float, title: String, points: List<SwayPoint>) {
    val cx = x + size / 2
    val cy = y + size / 2 + 10f // Shift down slightly for title
    val radius = size / 2 - 10f

    // 1. Draw Title
    val titlePaint = Paint(ReportPaints.tableCellBold).apply { textAlign = Paint.Align.CENTER }
    canvas.drawText(title, cx, y, titlePaint)

    // 2. Draw Background Zones (Max 20 degrees)
    val maxAngle = 20f
    val pxPerDeg = radius / maxAngle

    // Outer Circle (Border - 20 deg)
    canvas.drawCircle(cx, cy, radius, ReportPaints.border)
    // Safe Zone (Green - 5 deg)
    canvas.drawCircle(cx, cy, 5 * pxPerDeg, ReportPaints.swayZoneGreen)
    // Warning Zone Border (10 deg)
    canvas.drawCircle(cx, cy, 10 * pxPerDeg, Paint().apply { style = Paint.Style.STROKE; color = Color.LTGRAY })

    // Crosshairs
    canvas.drawLine(cx - radius, cy, cx + radius, cy, ReportPaints.border)
    canvas.drawLine(cx, cy - radius, cx, cy + radius, ReportPaints.border)

    // 3. Calculate Stats (Max X, Max Y, Max Overall)
    var maxTiltX = 0f
    var maxTiltY = 0f
    var maxOverall = 0f

    if (points.isNotEmpty()) {
        maxTiltX = points.maxOf { kotlin.math.abs(it.x) }
        maxTiltY = points.maxOf { kotlin.math.abs(it.y) }
        maxOverall = points.maxOf { kotlin.math.sqrt(it.x * it.x + it.y * it.y) }

        // 4. Draw Path (With Clamping)
        val pts = FloatArray((points.size - 1) * 4)
        var idx = 0

        for (i in 0 until points.size - 1) {
            // Helper function to clamp point to circle edge
            fun getClampedPixelCoords(p: SwayPoint): Pair<Float, Float> {
                val magnitude = kotlin.math.sqrt(p.x * p.x + p.y * p.y)

                // If point is outside 20 degrees, scale it down to exactly 20
                val scaleFactor = if (magnitude > maxAngle && magnitude > 0) {
                    maxAngle / magnitude
                } else {
                    1.0f
                }

                val clampedX = p.x * scaleFactor
                val clampedY = p.y * scaleFactor

                // Convert to pixels
                val px = cx + (clampedX * pxPerDeg)
                val py = cy - (clampedY * pxPerDeg) // Subtract because Canvas Y is inverted
                return Pair(px, py)
            }

            val (x1, y1) = getClampedPixelCoords(points[i])
            val (x2, y2) = getClampedPixelCoords(points[i+1])

            pts[idx++] = x1; pts[idx++] = y1
            pts[idx++] = x2; pts[idx++] = y2
        }
        canvas.drawLines(pts, ReportPaints.swayPath)
    }

    // 5. Draw Stats Text Below Graph
    val statsY = cy + radius + 15f // Position below the circle
    val statsPaint = Paint(ReportPaints.chartLabel).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 8f
    }

    // Line 1: Max X and Y
    canvas.drawText("Max X: %.1f° | Max Y: %.1f°".format(maxTiltX, maxTiltY), cx, statsY, statsPaint)
    // Line 2: Max Overall
    canvas.drawText("Max Overall: %.1f°".format(maxOverall), cx, statsY + 10f, statsPaint)
}

fun drawFooter(canvas: Canvas, pageNumber: Int) {
    // Note: Footer drawing is implemented in the Renderer class to handle resources (bitmaps) easily.
    // However, if called statically, we need Context, which isn't available here easily.
    // Rely on ReportRenderer.drawFooter for the actual logic.
}

// Actual Footer Implementation used by Renderer

@SuppressLint("NewApi")
private fun savePdf(context: Context, pdfDocument: PdfDocument): Uri? {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "Comprehensive_Balance_Report_${System.currentTimeMillis()}.pdf")
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
    }

    var uri: Uri? = null
    try {
        uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Failed to create new MediaStore record.")
        resolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
            ?: throw IOException("Failed to get output stream.")
    } catch (e: IOException) {
        uri?.let { resolver.delete(it, null, null) }
        e.printStackTrace()
        uri = null
    } finally {
        pdfDocument.close()
    }
    return uri
}

private fun savePdfToCache(context: Context, pdfDocument: PdfDocument): Uri? {
    try {
        // Create a 'reports' subdirectory in cache
        val imagesDir = File(context.cacheDir, "reports")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        // Overwrite the same file to save space, or use timestamp if you prefer
        val file = File(imagesDir, "shared_report.pdf")

        FileOutputStream(file).use {
            pdfDocument.writeTo(it)
        }

        // Return a content:// URI using FileProvider
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    } finally {
        pdfDocument.close()
    }
}
