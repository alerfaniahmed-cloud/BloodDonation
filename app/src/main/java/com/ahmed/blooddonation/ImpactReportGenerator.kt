package com.ahmed.blooddonation

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ImpactReportGenerator {

    fun generateAndShare(
        context: Context,
        donorName: String,
        donationCount: Int,
        livesSaved: Int,
        requestsPublished: Int,
        year: Int
    ) {
        val bitmap = createReportBitmap(context, donorName, donationCount, livesSaved, requestsPublished, year)
        val uri = saveBitmapAndGetUri(context, bitmap) ?: return

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.impact_report_share_text, donationCount, livesSaved))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, context.getString(R.string.impact_report_share_title))
        )
    }

    private fun createReportBitmap(
        context: Context,
        donorName: String,
        donationCount: Int,
        livesSaved: Int,
        requestsPublished: Int,
        year: Int
    ): Bitmap {
        val width = 1080
        val height = 1500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val backgroundPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#0E2A24"),
                Color.parseColor("#1FAE8E"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val yearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 200
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        val cx = width / 2f
        canvas.drawText(context.getString(R.string.impact_report_year_label, year), cx, 130f, yearPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.impact_report_title), cx, 210f, titlePaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 230
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(donorName, cx, 300f, namePaint)

        drawStatBlock(canvas, context, cx, 480f, donationCount.toString(), R.string.impact_report_donations_label)
        drawStatBlock(canvas, context, cx, 780f, livesSaved.toString(), R.string.impact_report_lives_label)
        drawStatBlock(canvas, context, cx, 1080f, requestsPublished.toString(), R.string.impact_report_requests_label)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 255
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.app_name), cx, 1400f, footerPaint)

        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 200
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.menu_subtitle), cx, 1445f, taglinePaint)

        return bitmap
    }

    private fun drawStatBlock(canvas: Canvas, context: Context, cx: Float, centerY: Float, value: String, labelRes: Int) {
        val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 110f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(value, cx, centerY, numberPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 220
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(labelRes), cx, centerY + 55f, labelPaint)
    }

    private fun saveBitmapAndGetUri(context: Context, bitmap: Bitmap): android.net.Uri? {
        return try {
            val reportDir = File(context.cacheDir, "certificates")
            if (!reportDir.exists()) reportDir.mkdirs()
            val file = File(reportDir, "raja_impact_report.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }
}
