package com.mesada.app.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.mesada.app.data.DaySummary
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Exporta el historial de evolución a un PDF y lo envía por mail al destinatario fijo. */
object HistoryPdf {
    /** La exportación siempre se envía a este mail. */
    const val RECIPIENT = "feliforte14@gmail.com"

    private val AR = Locale("es", "AR")
    private const val W = 595 // A4 portrait @72dpi
    private const val H = 842
    private const val M = 40f

    /** Genera el PDF y abre el mail ya dirigido a [RECIPIENT] con el archivo adjunto. */
    fun email(context: Context, history: List<DaySummary>) {
        val file = build(context, history)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(RECIPIENT))
            putExtra(Intent.EXTRA_SUBJECT, "Nomi — Mi evolución")
            putExtra(Intent.EXTRA_TEXT, "Adjunto mi evolución exportada desde la app Nomi.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Filtra el selector a apps de mail manteniendo el adjunto.
            selector = Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:") }
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, "No hay una app de mail configurada en el dispositivo.", Toast.LENGTH_LONG).show() }
    }

    private fun fmt(n: Int) = NumberFormat.getIntegerInstance(AR).format(n)

    private fun build(context: Context, history: List<DaySummary>): File {
        val doc = PdfDocument()
        val title = Paint().apply { color = Color.BLACK; textSize = 24f; isFakeBoldText = true; isAntiAlias = true }
        val header = Paint().apply { color = Color.rgb(31, 110, 82); textSize = 11f; isFakeBoldText = true; isAntiAlias = true }
        val body = Paint().apply { color = Color.rgb(40, 40, 40); textSize = 12f; isAntiAlias = true }
        val muted = Paint().apply { color = Color.rgb(110, 120, 115); textSize = 11f; isAntiAlias = true }
        val line = Paint().apply { color = Color.rgb(210, 216, 210) }

        val cols = floatArrayOf(M, M + 165, M + 260, M + 335, M + 410, M + 480) // Fecha, kcal, Prot, Hidr, Gras, Pasos
        val headers = listOf("FECHA", "KCAL", "PROT", "HIDR", "GRAS", "PASOS")
        val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy", AR)
        fun rowDate(iso: String) = runCatching {
            LocalDate.parse(iso).format(dateFmt).replaceFirstChar { it.uppercase() }
        }.getOrDefault(iso)

        val withData = history.filter { it.steps > 0 || it.totals.kcal > 0 }
        val avgSteps = if (withData.isEmpty()) 0 else withData.sumOf { it.steps } / withData.size
        val avgKcal = if (withData.isEmpty()) 0.0 else withData.sumOf { it.totals.kcal } / withData.size

        var page: PdfDocument.Page? = null
        var c: Canvas? = null
        var y = 0f
        var pageNo = 1

        fun startPage() {
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo++).create())
            c = page!!.canvas
            y = M + 12
            c!!.drawText("Nomi — Evolución", M, y, title); y += 26
            c!!.drawText("Promedio ${fmt(avgSteps)} pasos/día · ${avgKcal.roundToInt()} kcal/día", M, y, muted); y += 28
            headers.forEachIndexed { i, htxt -> c!!.drawText(htxt, cols[i], y, header) }
            y += 8; c!!.drawLine(M, y, W - M, y, line); y += 18
        }
        fun finishPage() { page?.let { doc.finishPage(it) } }

        startPage()
        if (history.isEmpty()) {
            c!!.drawText("Todavía no hay días registrados.", M, y, body)
        }
        for (d in history) {
            if (y > H - M) { finishPage(); startPage() }
            val t = d.totals
            c!!.drawText(rowDate(d.date), cols[0], y, body)
            c!!.drawText(t.kcal.roundToInt().toString(), cols[1], y, body)
            c!!.drawText("${t.protein.roundToInt()} g", cols[2], y, body)
            c!!.drawText("${t.carbs.roundToInt()} g", cols[3], y, body)
            c!!.drawText("${t.fat.roundToInt()} g", cols[4], y, body)
            c!!.drawText(fmt(d.steps), cols[5], y, body)
            y += 20
        }
        finishPage()

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "mesada_evolucion.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
