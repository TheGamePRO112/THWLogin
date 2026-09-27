package com.example.thwlogin

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {

    // --- Views ---
    private lateinit var barcodeAnzeige: ImageView
    private lateinit var barcodeText: TextView
    private lateinit var barcodeHinzufuegenButton: Button
    private lateinit var movableBarcodeGroup: LinearLayout
    private lateinit var infoButton: ImageButton
    private lateinit var lockButton: ImageButton

    // --- Drag & Drop State ---
    private var initialTouchY: Float = 0f
    private var initialBias: Float = 0f

    // --- App Logic State ---
    private var isBarcodeMovementLocked = false

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            updateBarcode(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        loadSavedStates()
        setupListeners()
    }

    private fun setupViews() {
        barcodeAnzeige = findViewById(R.id.barcodeAnzeige)
        barcodeText = findViewById(R.id.barcodeText)
        barcodeHinzufuegenButton = findViewById(R.id.barcodeHinzufuegenButton)
        movableBarcodeGroup = findViewById(R.id.movable_barcode_group)
        infoButton = findViewById(R.id.info_button)
        lockButton = findViewById(R.id.lock_button)
    }

    private fun loadSavedStates() {
        ladeLetztenBarcode()
        ladeBarcodePosition()
        ladeLockState()
    }

    private fun setupListeners() {
        barcodeHinzufuegenButton.setOnClickListener { starteScanner() }
        setupInfoButtonListener()
        setupLockButtonListener()
        aktiviereDragAndDropListener()
    }

    private fun setupInfoButtonListener() {
        infoButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("App-Info")
                .setMessage("Allgemeine Funktion:\nScanne einen Barcode, um ihn auf dem Bildschirm anzuzeigen. Die App merkt sich immer den zuletzt gescannten Code.\n\nPosition ändern:\nLege deinen Finger auf den Barcode-Block und bewege ihn nach oben oder unten. Die Position wird automatisch gespeichert.\n\nPosition fixieren:\nDrücke das Schloss-Symbol oben rechts, um die Position zu sperren und ein versehentliches Verschieben zu verhindern.\n\nDesigned by Robert Haase")
                .setPositiveButton("Verstanden", null)
                .show()
        }
    }

    private fun setupLockButtonListener() {
        lockButton.setOnClickListener {
            isBarcodeMovementLocked = !isBarcodeMovementLocked
            speichereLockState(isBarcodeMovementLocked)
            updateLockIcon()
            val feedbackText = if (isBarcodeMovementLocked) "Position fixiert" else "Position entsperrt"
            Toast.makeText(this, feedbackText, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLockIcon() {
        val iconRes = if (isBarcodeMovementLocked) R.drawable.ic_lock_closed else R.drawable.ic_lock_open
        lockButton.setImageResource(iconRes)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun aktiviereDragAndDropListener() {
        movableBarcodeGroup.setOnTouchListener { view, event ->
            if (isBarcodeMovementLocked) return@setOnTouchListener false

            val params = view.layoutParams as ConstraintLayout.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchY = event.rawY
                    initialBias = params.verticalBias
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = event.rawY - initialTouchY
                    val parentHeight = (view.parent as View).height
                    val biasDelta = dy / parentHeight
                    // NEU: Bewegung auf die obere Hälfte beschränken (0.0f = oben, 0.5f = mitte)
                    params.verticalBias = (initialBias + biasDelta).coerceIn(0f, 0.5f)
                    view.layoutParams = params
                    true
                }
                MotionEvent.ACTION_UP -> {
                    speichereBarcodePosition(params.verticalBias)
                    view.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun starteScanner() {
        val options = ScanOptions()
        options.setPrompt("Barcode scannen")
        options.setOrientationLocked(true)
        barcodeLauncher.launch(options)
    }

    private fun updateBarcode(wert: String) {
        try {
            barcodeAnzeige.setImageBitmap(erstelleEchtenBarcode(wert))
            barcodeText.text = wert
            speichereLetztenBarcode(wert)
        } catch (e: Exception) {
            Toast.makeText(this, "Fehler beim Erstellen des Barcodes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun erstelleEchtenBarcode(wert: String): Bitmap {
        val bitMatrix = MultiFormatWriter().encode(wert, BarcodeFormat.CODE_128, 800, 200)
        val bitmap = Bitmap.createBitmap(800, 200, Bitmap.Config.ARGB_8888)
        val thwBlue = ContextCompat.getColor(this, R.color.thw_blue)
        val white = ContextCompat.getColor(this, R.color.white)
        for (x in 0 until 800) {
            for (y in 0 until 200) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) thwBlue else white)
            }
        }
        return bitmap
    }

    private fun getPrefs() = getSharedPreferences("MeineBarcodeAppPrefs", MODE_PRIVATE)

    private fun speichereLetztenBarcode(wert: String) {
        getPrefs().edit().putString("letzterBarcode", wert).apply()
    }

    private fun ladeLetztenBarcode() {
        val letzterBarcode = getPrefs().getString("letzterBarcode", null)
        if (!letzterBarcode.isNullOrEmpty()) {
            updateBarcode(letzterBarcode)
        } else {
            barcodeText.text = ""
        }
    }

    private fun speichereBarcodePosition(bias: Float) {
        getPrefs().edit().putFloat("barcode_position_bias", bias).apply()
    }

    private fun ladeBarcodePosition() {
        var bias = getPrefs().getFloat("barcode_position_bias", 0.05f)
        // NEU: Stelle sicher, dass auch eine alte, gespeicherte Position das Limit respektiert
        if (bias > 0.5f) {
            bias = 0.5f
        }
        val params = movableBarcodeGroup.layoutParams as ConstraintLayout.LayoutParams
        params.verticalBias = bias
        movableBarcodeGroup.layoutParams = params
    }

    private fun speichereLockState(isLocked: Boolean) {
        getPrefs().edit().putBoolean("barcode_lock_state", isLocked).apply()
    }

    private fun ladeLockState() {
        isBarcodeMovementLocked = getPrefs().getBoolean("barcode_lock_state", false)
        updateLockIcon()
    }
}