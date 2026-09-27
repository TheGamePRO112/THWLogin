package com.example.thwlogin

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private var onBarcodeScanned: ((String) -> Unit)? = null
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            onBarcodeScanned?.invoke(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("MeineBarcodeAppPrefs", Context.MODE_PRIVATE) }
            val currentVersion = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
                } catch (_: Exception) { "1.0.0" }
            }

            // States
            var barcodeValue by remember { mutableStateOf(prefs.getString("letzterBarcode", "") ?: "") }
            var barcodeBias by remember { mutableFloatStateOf(prefs.getFloat("barcode_position_bias", 0.08f).coerceIn(0f, 0.5f)) }
            var isLocked by remember { mutableStateOf(prefs.getBoolean("barcode_lock_state", false)) }
            var isRotationLocked by remember { mutableStateOf(prefs.getBoolean("rotation_lock_state", false)) }
            var showMenu by remember { mutableStateOf(false) }
            var showHelpDialog by remember { mutableStateOf(false) }
            var availableUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }

            // Drehung festlegen
            LaunchedEffect(isRotationLocked) {
                requestedOrientation = if (isRotationLocked) {
                    ActivityInfo.SCREEN_ORIENTATION_LOCKED
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            // Scanner-Callback
            onBarcodeScanned = { code ->
                barcodeValue = code
                prefs.edit().putString("letzterBarcode", code).apply()
            }

            // GitHub Update Prüfung beim Start
            LaunchedEffect(Unit) {
                availableUpdate = AppUpdater.checkUpdate(currentVersion)
            }

            // Barcode vorbereiten
            val barcodeBitmap = remember(barcodeValue) {
                if (barcodeValue.isNotEmpty()) erstelleEchtenBarcode(barcodeValue, context) else null
            }

            val backdrop = rememberBackdrop()

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF003399), Color(0xFF001A4D), Color(0xFF080808))
                        )
                    )
                    .windowInsetsPadding(WindowInsets.systemBars) // Sicherer Abstand zu Notch & Navleiste
            ) {
                val screenHeightPx = constraints.maxHeight.toFloat()

                // --- HEADER LEISTE (Menü-Knopf) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        LiquidButton(
                            onClick = { showMenu = true },
                            backdrop = backdrop
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menü", tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Menü", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        // Dropdown-Menü
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isLocked) "Position entsperren" else "Position sperren") },
                                leadingIcon = {
                                    Icon(
                                        if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    isLocked = !isLocked
                                    prefs.edit().putBoolean("barcode_lock_state", isLocked).apply()
                                    showMenu = false
                                    Toast.makeText(context, if (isLocked) "Position fixiert" else "Position frei", Toast.LENGTH_SHORT).show()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(if (isRotationLocked) "Drehung entsperren" else "Drehung sperren") },
                                leadingIcon = {
                                    Icon(
                                        if (isRotationLocked) Icons.Default.ScreenLockRotation else Icons.Default.ScreenRotation,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    isRotationLocked = !isRotationLocked
                                    prefs.edit().putBoolean("rotation_lock_state", isRotationLocked).apply()
                                    showMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Neuen Barcode scannen") },
                                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val options = ScanOptions().apply {
                                        setPrompt("Barcode scannen")
                                        setOrientationLocked(true)
                                    }
                                    barcodeLauncher.launch(options)
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Hilfe & Info") },
                                leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showHelpDialog = true
                                }
                            )
                        }
                    }

                    // Kleiner Status-Indikator
                    if (isLocked) {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Fixiert", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // --- VERSCHIEBBARER BARCODE (Bis ganz nach oben = 0.0f möglich!) ---
                val currentOffsetY = (barcodeBias * screenHeightPx).roundToInt()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, currentOffsetY) }
                        .pointerInput(isLocked) {
                            if (!isLocked) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        prefs.edit().putFloat("barcode_position_bias", barcodeBias).apply()
                                    }
                                ) { _, dragAmount ->
                                    val deltaBias = dragAmount / screenHeightPx
                                    // 0.0f erlaubt das Schieben bis ganz an den oberen Rand!
                                    barcodeBias = (barcodeBias + deltaBias).coerceIn(0.0f, 0.5f)
                                }
                            }
                        }
                        .padding(horizontal = 16.dp)
                ) {
                    if (barcodeBitmap != null) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = Color.White,
                            shadowElevation = 10.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Image(
                                    bitmap = barcodeBitmap.asImageBitmap(),
                                    contentDescription = "Barcode",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    contentScale = ContentScale.FillBounds
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = barcodeValue,
                                    color = Color.Black,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Kein Barcode vorhanden.\nScanne einen Code über das Menü oder unten.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 15.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // --- UNTEN: SCANNER BUTTON (Liquid Glass) ---
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    LiquidButton(
                        onClick = {
                            val options = ScanOptions().apply {
                                setPrompt("Barcode scannen")
                                setOrientationLocked(true)
                            }
                            barcodeLauncher.launch(options)
                        },
                        backdrop = backdrop,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Barcode scannen",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // --- HILFE & FEHLERMELDUNG DIALOG ---
                Button(
                    onClick = {
                        // Öffnet direkt die GitHub Issues Seite deines Repositories im Browser
                        val issuesUrl = "https://github.com/TheGamePRO112/THWLogin/issues/new"
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(issuesUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(browserIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Problem / Fehler auf GitHub melden")
                }

                // --- UPDATE DIALOG ---
                availableUpdate?.let { update ->
                    AlertDialog(
                        onDismissRequest = { availableUpdate = null },
                        title = { Text("Update verfügbar!") },
                        text = { Text("Eine neue Version (${update.version}) steht auf GitHub bereit.") },
                        confirmButton = {
                            Button(onClick = {
                                AppUpdater.startDownload(context, update.downloadUrl)
                                availableUpdate = null
                            }) {
                                Text("Aktualisieren")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { availableUpdate = null }) {
                                Text("Später")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun erstelleEchtenBarcode(wert: String, context: Context): Bitmap {
        val bitMatrix = MultiFormatWriter().encode(wert, BarcodeFormat.CODE_128, 800, 220)
        val bitmap = Bitmap.createBitmap(800, 220, Bitmap.Config.ARGB_8888)
        val thwBlue = ContextCompat.getColor(context, R.color.thw_blue)
        val white = ContextCompat.getColor(context, R.color.white)
        for (x in 0 until 800) {
            for (y in 0 until 220) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) thwBlue else white)
            }
        }
        return bitmap
    }
}