package com.example.thwlogin

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.widget.RemoteViews
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

class BarcodeWidgetProvider : AppWidgetProvider() {

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BarcodeWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences("MeineBarcodeAppPrefs", Context.MODE_PRIVATE)
            val barcodeValue = prefs.getString("letzterBarcode", "") ?: ""
            val isVertical = prefs.getBoolean("widget_is_vertical", false)

            val views = RemoteViews(context.packageName, R.layout.barcode_widget_layout)

            if (barcodeValue.isNotEmpty()) {
                val bitmap = createRotatedBarcodeBitmap(barcodeValue, isVertical)
                views.setImageViewBitmap(R.id.widget_barcode_image, bitmap)
                views.setTextViewText(R.id.widget_barcode_text, barcodeValue)
            } else {
                views.setTextViewText(R.id.widget_barcode_text, "Kein Barcode vorhanden")
            }

            // Click anywhere on Widget -> Open MainActivity
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createRotatedBarcodeBitmap(wert: String, isVertical: Boolean): Bitmap {
            val width = 800
            val height = 220

            val bitMatrix = MultiFormatWriter().encode(wert, BarcodeFormat.CODE_128, width, height)
            val horizontalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val thwBlue = 0xFF003399.toInt()
            val white = Color.WHITE

            for (x in 0 until width) {
                for (y in 0 until height) {
                    horizontalBitmap.setPixel(x, y, if (bitMatrix[x, y]) thwBlue else white)
                }
            }

            return if (isVertical) {
                val matrix = Matrix().apply { postRotate(90f) }
                Bitmap.createBitmap(
                    horizontalBitmap,
                    0,
                    0,
                    horizontalBitmap.width,
                    horizontalBitmap.height,
                    matrix,
                    true
                )
            } else {
                horizontalBitmap
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
