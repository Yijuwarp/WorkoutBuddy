package com.example.workoutbuddy.ui.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val PLAY_STORE_URL =
    "https://play.google.com/store/apps/details?id=com.venkatchait.workoutbuddy"

const val SHARE_JOIN_TEXT = "Join me on WorkoutBuddy: $PLAY_STORE_URL"

/**
 * Captures the given [GraphicsLayer] (recorded from an achievement card) as a PNG and opens
 * the Android sharesheet with the image plus a brag message. The capture is composited onto
 * a solid backdrop so the card's rounded corners don't end up on a transparent canvas.
 */
suspend fun shareAchievementScreenshot(context: Context, layer: GraphicsLayer, message: String) {
    try {
        val captured = layer.toImageBitmap().asAndroidBitmap()
        val uri = withContext(Dispatchers.IO) {
            val swBitmap = if (captured.config == Bitmap.Config.HARDWARE) {
                captured.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                captured
            }
            val padding = 48
            val framed = Bitmap.createBitmap(
                swBitmap.width + padding * 2,
                swBitmap.height + padding * 2,
                Bitmap.Config.ARGB_8888
            )
            Canvas(framed).apply {
                drawColor(Color.parseColor("#EFF6FF"))
                drawBitmap(swBitmap, padding.toFloat(), padding.toFloat(), null)
            }
            val dir = File(context.cacheDir, "shares").apply { mkdirs() }
            val file = File(dir, "workoutbuddy_achievement.png")
            file.outputStream().use { framed.compress(Bitmap.CompressFormat.PNG, 100, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "$message\n\n$SHARE_JOIN_TEXT")
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share your achievement").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Log.e("ShareAchievement", "Failed to share achievement screenshot", e)
    }
}
