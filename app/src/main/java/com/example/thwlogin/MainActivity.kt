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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlin.math.roundToInt

@Composable
private fun rememberBackdrop(): Any = remember { Any() }

@Composable
private fun LiquidButton(
    onClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") backdrop: Any,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDarkMode) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.65f),
            contentColor = if (isDarkMode) Color.White else Color(0xFF003399)
        ),
        border = BorderStroke(
            1.dp,
            if (isDarkMode) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f)
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        content = content
    )
}

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

            // Initialer Default für Drehsperre = standardmäßig aktiviert
            if (!prefs.contains("rotation_lock_default_set")) {
                prefs.edit().putBoolean("rotation_lock_state", true).putBoolean("rotation_lock_default_set", true).apply()
            }

            // States
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode_enabled", true)) }
            var barcodeValue by remember { mutableStateOf(prefs.getString("letzterBarcode", "") ?: "") }
            var barcodeBias by remember { mutableFloatStateOf(prefs.getFloat("barcode_position_bias", 0.08f).coerceIn(0f, 0.5f)) }
            var isLocked by remember { mutableStateOf(prefs.getBoolean("barcode_lock_state", false)) }
            var isRotationLocked by remember { mutableStateOf(prefs.getBoolean("rotation_lock_state", true)) }
            var showMenu by remember { mutableStateOf(false) }
            var showHelpDialog by remember { mutableStateOf(false) }
            var availableUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }

            // Statusleiste Icon-Farbe (hell/dunkel) anpassen
            val window = (context as? ComponentActivity)?.window
            SideEffect {
                window?.let {
                    WindowInsetsControllerCompat(it, it.decorView).isAppearanceLightStatusBars = !isDarkMode
                }
            }

            // Dynamische Farben für Liquid Glass Theme
            val bgGradient = if (isDarkMode) {
                Brush.verticalGradient(
                    listOf(Color(0xFF003399), Color(0xFF001A4D), Color(0xFF080808))
                )
            } else {
                Brush.verticalGradient(
                    listOf(Color(0xFFE4EFFB), Color(0xFFC8DEF8), Color(0xFFAECDF2))
                )
            }

            val primaryTextColor = if (isDarkMode) Color.White else Color(0xFF002A73)
            val iconTint = if (isDarkMode) Color.White else Color(0xFF003399)
            val glassButtonBg = if (isDarkMode) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.65f)
            val glassButtonBorder = if (isDarkMode) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f)

            val menuBgBrush = if (isDarkMode) {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color(0xFF001B48).copy(alpha = 0.82f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.92f),
                        Color(0xFFD3E4F8).copy(alpha = 0.88f)
                    )
                )
            }

            val menuBorderBrush = if (isDarkMode) {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.15f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.50f)
                    )
                )
            }

            val menuDividerColor = if (isDarkMode) Color.White.copy(alpha = 0.2f) else Color(0xFF003399).copy(alpha = 0.15f)

            val switchColors = if (isDarkMode) {
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF0055FF),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.35f)
                )
            } else {
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF003399),
                    uncheckedThumbColor = Color(0xFF003399).copy(alpha = 0.7f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.4f),
                    uncheckedBorderColor = Color(0xFF003399).copy(alpha = 0.25f)
                )
            }

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
                    .background(bgGradient)
            ) {
                val screenHeightPx = constraints.maxHeight.toFloat()

                // --- VERSCHIEBBARER BARCODE (Bis ganz nach oben = 0px möglich!) ---
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
                                    // 0.0f erlaubt das Schieben bis ganz an den oberen Rand (0px)!
                                    barcodeBias = (barcodeBias + deltaBias).coerceIn(0.0f, 0.65f)
                                }
                            }
                        }
                ) {
                    if (barcodeBitmap != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                bitmap = barcodeBitmap.asImageBitmap(),
                                contentDescription = "Barcode",
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                contentScale = ContentScale.FillBounds
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = barcodeValue,
                                color = primaryTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                    } else {
                        Surface(
                            color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, if (isDarkMode) Color.Transparent else Color.White.copy(alpha = 0.8f)),
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .padding(top = 80.dp)
                        ) {
                            Text(
                                text = "Kein Barcode vorhanden.\nScanne einen Code über den Button unten.",
                                color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color(0xFF002970),
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // --- HEADER LEISTE (Nur Icons, kein Text - überdeckt den Barcode nicht) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(glassButtonBg, CircleShape)
                                .border(1.dp, glassButtonBorder, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menü",
                                tint = iconTint,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Dropdown-Menü im Liquid Glass Stil
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = Color.Transparent,
                            shadowElevation = 14.dp,
                            border = BorderStroke(1.dp, menuBorderBrush),
                            modifier = Modifier
                                .background(
                                    brush = menuBgBrush,
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            val glassItemColors = MenuDefaults.itemColors(
                                textColor = primaryTextColor,
                                leadingIconColor = primaryTextColor,
                                trailingIconColor = primaryTextColor
                            )

                            DropdownMenuItem(
                                text = { Text("Position sperren", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    Switch(
                                        checked = isLocked,
                                        onCheckedChange = null,
                                        modifier = Modifier.padding(start = 12.dp),
                                        colors = switchColors
                                    )
                                },
                                colors = glassItemColors,
                                onClick = {
                                    isLocked = !isLocked
                                    prefs.edit().putBoolean("barcode_lock_state", isLocked).apply()
                                    Toast.makeText(context, if (isLocked) "Position fixiert" else "Position frei", Toast.LENGTH_SHORT).show()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Drehung sperren", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        if (isRotationLocked) Icons.Default.ScreenLockRotation else Icons.Default.ScreenRotation,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    Switch(
                                        checked = isRotationLocked,
                                        onCheckedChange = null,
                                        modifier = Modifier.padding(start = 12.dp),
                                        colors = switchColors
                                    )
                                },
                                colors = glassItemColors,
                                onClick = {
                                    isRotationLocked = !isRotationLocked
                                    prefs.edit().putBoolean("rotation_lock_state", isRotationLocked).apply()
                                    Toast.makeText(context, if (isRotationLocked) "Drehung gesperrt" else "Drehung frei", Toast.LENGTH_SHORT).show()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Dunkelmodus", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    Switch(
                                        checked = isDarkMode,
                                        onCheckedChange = null,
                                        modifier = Modifier.padding(start = 12.dp),
                                        colors = switchColors
                                    )
                                },
                                colors = glassItemColors,
                                onClick = {
                                    isDarkMode = !isDarkMode
                                    prefs.edit().putBoolean("dark_mode_enabled", isDarkMode).apply()
                                    Toast.makeText(context, if (isDarkMode) "Dunkelmodus aktiv" else "Hellmodus aktiv", Toast.LENGTH_SHORT).show()
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = menuDividerColor,
                                thickness = 1.dp
                            )

                            DropdownMenuItem(
                                text = { Text("Hilfe & Info", fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                                colors = glassItemColors,
                                onClick = {
                                    showMenu = false
                                    showHelpDialog = true
                                }
                            )
                        }
                    }

                    // Status-Indikatoren (nur Logos, noch kleiner & dezenter)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLocked) {
                            Surface(
                                color = glassButtonBg,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, glassButtonBorder)
                            ) {
                                Box(
                                    modifier = Modifier.size(26.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Position fixiert",
                                        tint = iconTint,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                        if (isRotationLocked) {
                            Surface(
                                color = glassButtonBg,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, glassButtonBorder)
                            ) {
                                Box(
                                    modifier = Modifier.size(26.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ScreenLockRotation,
                                        contentDescription = "Drehung gesperrt",
                                        tint = iconTint,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // --- UNTEN: SCANNER BUTTON (Liquid Glass) ---
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
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
                        isDarkMode = isDarkMode,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(54.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = if (isDarkMode) Color.White else Color(0xFF003399)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Barcode scannen",
                            color = if (isDarkMode) Color.White else Color(0xFF003399),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // --- HILFE & FEHLERMELDUNG DIALOG ---
                if (showHelpDialog) {
                    AlertDialog(
                        onDismissRequest = { showHelpDialog = false },
                        title = { Text("Hilfe & Info") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Version: $currentVersion\nScanne deinen THW-Barcode und verschiebe ihn vertikal an die gewünschte Position.")
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
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showHelpDialog = false }) {
                                Text("Schließen")
                            }
                        }
                    )
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