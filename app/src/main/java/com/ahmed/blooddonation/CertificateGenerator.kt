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

object CertificateGenerator {

    fun generateAndShare(context: Context, donorName: String, donationCount: Int) {
        val bitmap = createCertificateBitmap(context, donorName, donationCount)
        val uri = saveBitmapAndGetUri(context, bitmap) ?: return

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.certificate_share_text, donationCount))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, context.getString(R.string.share_certificate_button))
        )
    }

    private fun createCertificateBitmap(context: Context, donorName: String, donationCount: Int): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val backgroundPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#1FAE8E"),
                Color.parseColor("#0E7A63"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val borderPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
            alpha = 180
        }
        canvas.drawRect(40f, 40f, (width - 40).toFloat(), (height - 40).toFloat(), borderPaint)

        val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val cx = width / 2f
        val dropTop = 160f
        val dropPath = android.graphics.Path().apply {
            moveTo(cx, dropTop)
            cubicTo(cx, dropTop, cx + 90, dropTop + 170, cx + 90, dropTop + 230)
            cubicTo(cx + 90, dropTop + 290, cx + 45, dropTop + 320, cx, dropTop + 320)
            cubicTo(cx - 45, dropTop + 320, cx - 90, dropTop + 290, cx - 90, dropTop + 230)
            cubicTo(cx - 90, dropTop + 170, cx, dropTop, cx, dropTop)
            close()
        }
        canvas.drawPath(dropPath, dropPaint)

        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1FAE8E")
            style = Paint.Style.STROKE
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
        }
        val arcRect = android.graphics.RectF(cx - 55f, dropTop + 195f, cx + 55f, dropTop + 275f)
        canvas.drawArc(arcRect, 20f, 140f, false, arcPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.certificate_title), cx, 620f, titlePaint)

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 220
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.certificate_subtitle), cx, 680f, subtitlePaint)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 56f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(donorName, cx, 820f, namePaint)

        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 100f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(donationCount.toString(), cx, 970f, countPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 230
            textSize = 38f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.certificate_donations_label), cx, 1030f, labelPaint)

        val livesSaved = donationCount * 3
        val livesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 220
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            context.getString(R.string.certificate_lives_saved, livesSaved),
            cx, 1100f, livesPaint
        )

        val appNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 255
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(context.getString(R.string.app_name), cx, 1240f, appNamePaint)

        return bitmap
    }

    private fun saveBitmapAndGetUri(context: Context, bitmap: Bitmap): android.net.Uri? {
        return try {
            val certDir = File(context.cacheDir, "certificates")
            if (!certDir.exists()) certDir.mkdirs()
            val file = File(certDir, "raja_certificate.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }
}
