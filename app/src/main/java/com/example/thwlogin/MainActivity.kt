package com.example.thwlogin

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
private fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    IosButton(
        onClick = onClick,
        modifier = modifier,
        isDarkMode = isDarkMode,
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
            val prefs = remember { context.getSharedPreferences("MeineBarcodeAppPrefs", MODE_PRIVATE) }
            val currentVersion = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.5"
                } catch (_: Exception) { "2.5" }
            }

            // Initialer Default für Drehsperre = standardmäßig aktiviert
            if (!prefs.contains("rotation_lock_default_set")) {
                prefs.edit().putBoolean("rotation_lock_state", true).putBoolean("rotation_lock_default_set", true).apply()
            }

            // States
            val coroutineScope = rememberCoroutineScope()
            var isCheckingUpdate by remember { mutableStateOf(false) }
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode_enabled", true)) }
            var barcodeValue by remember { mutableStateOf(prefs.getString("letzterBarcode", "") ?: "") }
            var barcodeBias by remember { mutableFloatStateOf(prefs.getFloat("barcode_position_bias", 0.08f).coerceIn(0f, 0.5f)) }
            var isLocked by remember { mutableStateOf(prefs.getBoolean("barcode_lock_state", false)) }
            var isRotationLocked by remember { mutableStateOf(prefs.getBoolean("rotation_lock_state", true)) }
            var isAutoUpdateEnabled by remember { mutableStateOf(prefs.getBoolean("auto_update_enabled", true)) }
            var isWidgetVertical by remember { mutableStateOf(prefs.getBoolean("widget_is_vertical", false)) }
            var showMenu by remember { mutableStateOf(false) }
            var showHelpDialog by remember { mutableStateOf(false) }
            var showManualEntryDialog by remember { mutableStateOf(false) }
            var showWorkTimeDisclaimerDialog by remember { mutableStateOf(false) }
            var manualBarcodeText by remember { mutableStateOf("") }
            var availableUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }

            // Zeiterfassung States
            var isTrackingWork by remember { mutableStateOf(WorkTimeManager.isTracking(context)) }
            var trackingStartTime by remember { mutableLongStateOf(WorkTimeManager.getStartTimeMillis(context)) }
            var elapsedSeconds by remember { mutableLongStateOf(0L) }
            var showWorkCalendarDialog by remember { mutableStateOf(false) }
            var editingWorkEntry by remember { mutableStateOf<WorkEntry?>(null) }
            var showEditEntryDialog by remember { mutableStateOf(false) }
            var workEntriesList by remember { mutableStateOf(WorkTimeManager.getEntries(context)) }

            // Notification Permission Launcher for Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            // Sync with Lifecycle (when returning from Notification stop action)
            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        isTrackingWork = WorkTimeManager.isTracking(context)
                        trackingStartTime = WorkTimeManager.getStartTimeMillis(context)
                        workEntriesList = WorkTimeManager.getEntries(context)
                    }
                }
                val lifecycle = (context as? ComponentActivity)?.lifecycle
                lifecycle?.addObserver(observer)
                onDispose {
                    lifecycle?.removeObserver(observer)
                }
            }

            // Live Timer Ticker & Notification Update
            LaunchedEffect(isTrackingWork, trackingStartTime) {
                if (isTrackingWork && WorkTimeManager.isTracking(context) && trackingStartTime > 0L) {
                    WorkTimeNotificationHelper.showOrUpdateNotification(context, trackingStartTime)
                    while (isTrackingWork && WorkTimeManager.isTracking(context)) {
                        val diff = (System.currentTimeMillis() - trackingStartTime) / 1000
                        elapsedSeconds = maxOf(0L, diff)
                        delay(1000)
                    }
                }
                if (!WorkTimeManager.isTracking(context)) {
                    isTrackingWork = false
                    trackingStartTime = 0L
                    elapsedSeconds = 0L
                    WorkTimeNotificationHelper.cancelNotification(context)
                }
            }

            val formattedTimerString = remember(elapsedSeconds) {
                val hours = elapsedSeconds / 3600
                val minutes = (elapsedSeconds % 3600) / 60
                val secs = elapsedSeconds % 60
                String.format(Locale.GERMANY, "%02d:%02d:%02d", hours, minutes, secs)
            }

            // Statusleiste Icon-Farbe (hell/dunkel) anpassen
            val window = (context as? ComponentActivity)?.window
            SideEffect {
                window?.let {
                    WindowInsetsControllerCompat(it, it.decorView).isAppearanceLightStatusBars = !isDarkMode
                }
            }

            // Edler Slate-Navy Hintergrundverlauf mit Tiefenwirkung
            val bgGradient = if (isDarkMode) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B132B), // Slate-Navy top
                        Color(0xFF1C2541), // Deep Indigo middle
                        Color(0xFF090E17)  // Onyx bottom
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF0F4F9), // Porcelain top
                        Color(0xFFE2ECF8), // Soft Ice Blue
                        Color(0xFFC4DCF8)  // Sky Blue bottom
                    )
                )
            }

            val primaryTextColor = if (isDarkMode) Color.White else Color(0xFF002A73)
            val iconTint = if (isDarkMode) Color.White else Color(0xFF003399)
            val glassButtonBg = if (isDarkMode) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.65f)
            val glassButtonBorder = if (isDarkMode) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f)

            // High-Contrast iOS Input Field Label & Text Colors
            val iosTextFieldColors = TextFieldDefaults.colors(
                focusedContainerColor = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                unfocusedContainerColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedLabelColor = if (isDarkMode) Color.White.copy(alpha = 0.90f) else Color(0xFF003399),
                unfocusedLabelColor = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF4A5568),
                focusedPlaceholderColor = if (isDarkMode) Color.White.copy(alpha = 0.50f) else Color.Gray,
                unfocusedPlaceholderColor = if (isDarkMode) Color.White.copy(alpha = 0.35f) else Color.Gray,
                focusedTextColor = primaryTextColor,
                unfocusedTextColor = primaryTextColor
            )

            // Blicksicheres, opakes Menü-Design ohne Durchscheinen
            val menuBgBrush = if (isDarkMode) {
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A).copy(alpha = 0.98f),
                        Color(0xFF1E293B).copy(alpha = 0.98f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.98f),
                        Color(0xFFE2E8F0).copy(alpha = 0.98f)
                    )
                )
            }

            val menuBorderBrush = if (isDarkMode) {
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.12f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        Color.White,
                        Color.White.copy(alpha = 0.60f)
                    )
                )
            }

            val menuDividerColor = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color(0xFF003399).copy(alpha = 0.12f)

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
                BarcodeWidgetProvider.updateAllWidgets(context)
            }

            // GitHub Update Prüfung beim Start (Release-Modus)
            LaunchedEffect(isAutoUpdateEnabled) {
                if (isAutoUpdateEnabled) {
                    availableUpdate = AppUpdater.checkUpdate(currentVersion)
                }
            }

            // Barcode vorbereiten
            val barcodeBitmap = remember(barcodeValue) {
                if (barcodeValue.isNotEmpty()) erstelleEchtenBarcode(barcodeValue, context) else null
            }

            val handleToggleWorkService = {
                if (isTrackingWork) {
                    val entry = WorkTimeManager.stopTracking(context)
                    WorkTimeNotificationHelper.cancelNotification(context)
                    isTrackingWork = false
                    trackingStartTime = 0L
                    elapsedSeconds = 0L
                    workEntriesList = WorkTimeManager.getEntries(context)
                    if (entry != null) {
                        val formatted = WorkTimeManager.formatMinutesToHours(entry.durationMinutes)
                        Toast.makeText(context, "Dienst beendet ($formatted erfasst)", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val hasSeenDisclaimer = prefs.getBoolean("has_seen_work_time_disclaimer", false)
                    if (!hasSeenDisclaimer) {
                        showWorkTimeDisclaimerDialog = true
                    } else {
                        WorkTimeManager.startTracking(context)
                        isTrackingWork = true
                        val now = System.currentTimeMillis()
                        trackingStartTime = now
                        WorkTimeNotificationHelper.showOrUpdateNotification(context, now)
                        Toast.makeText(context, "Dienst gestartet", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgGradient)
            ) {
                val screenHeightPx = constraints.maxHeight.toFloat()

                // Ambient Radial Glow Spot für Organische Tiefenwirkung
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 40.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isDarkMode) {
                                    listOf(Color(0xFF0055FF).copy(alpha = 0.22f), Color.Transparent)
                                } else {
                                    listOf(Color.White.copy(alpha = 0.60f), Color.Transparent)
                                }
                            ),
                            shape = CircleShape
                        )
                )

                // --- VERSCHIEBBARER BARCODE (Kein Layout-Sprung beim Sperren/Entsperren) ---
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
                                .tactilePress { showMenu = true }
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menü",
                                tint = iconTint,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Dropdown-Menü im Blicksicheren iOS-Settings Stil
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(24.dp),
                            containerColor = Color.Transparent,
                            shadowElevation = 20.dp,
                            border = BorderStroke(1.2.dp, menuBorderBrush),
                            modifier = Modifier
                                .background(
                                    brush = menuBgBrush,
                                    shape = RoundedCornerShape(24.dp)
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

                            DropdownMenuItem(
                                text = { Text("Barcode manuell eingeben", fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                colors = glassItemColors,
                                onClick = {
                                    showMenu = false
                                    manualBarcodeText = barcodeValue
                                    showManualEntryDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Auto-Update-Prüfung", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        if (isAutoUpdateEnabled) Icons.Default.SystemUpdate else Icons.Default.SyncDisabled,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    Switch(
                                        checked = isAutoUpdateEnabled,
                                        onCheckedChange = null,
                                        modifier = Modifier.padding(start = 12.dp),
                                        colors = switchColors
                                    )
                                },
                                colors = glassItemColors,
                                onClick = {
                                    isAutoUpdateEnabled = !isAutoUpdateEnabled
                                    prefs.edit().putBoolean("auto_update_enabled", isAutoUpdateEnabled).apply()
                                    Toast.makeText(
                                        context,
                                        if (isAutoUpdateEnabled) "Automatische Update-Prüfung aktiv" else "Automatische Update-Prüfung deaktiviert",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Widget vertikal drehen", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CropRotate,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {
                                    Switch(
                                        checked = isWidgetVertical,
                                        onCheckedChange = null,
                                        modifier = Modifier.padding(start = 12.dp),
                                        colors = switchColors
                                    )
                                },
                                colors = glassItemColors,
                                onClick = {
                                    isWidgetVertical = !isWidgetVertical
                                    prefs.edit().putBoolean("widget_is_vertical", isWidgetVertical).apply()
                                    BarcodeWidgetProvider.updateAllWidgets(context)
                                    Toast.makeText(
                                        context,
                                        if (isWidgetVertical) "Widget vertikal ausgerichtet" else "Widget horizontal ausgerichtet",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Stunden-Kalender", fontWeight = FontWeight.Medium) },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                                colors = glassItemColors,
                                onClick = {
                                    showMenu = false
                                    workEntriesList = WorkTimeManager.getEntries(context)
                                    showWorkCalendarDialog = true
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

                    // Status-Indikatoren
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

                // Dezent gedimmter Background Scrim wenn Menü geöffnet ist
                if (showMenu) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable { showMenu = false }
                    )
                }

                // --- UNTEN: ZEITERFASSUNG & SCANNER BUTTON ---
                // Sauberes Inset-Padding für Navigationsleisten / Gestenleisten
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 28.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // --- LIQUID GLASS TIMER CARD ---
                    IosGroupedCard(
                        isDarkMode = isDarkMode,
                        cornerRadius = 26.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Header row: Title + Kalender Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Zeiterfassung",
                                        color = primaryTextColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        workEntriesList = WorkTimeManager.getEntries(context)
                                        showWorkCalendarDialog = true
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .tactilePress {
                                            workEntriesList = WorkTimeManager.getEntries(context)
                                            showWorkCalendarDialog = true
                                        }
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = "Kalender",
                                        tint = iconTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Live Timer Status Row
                            if (isTrackingWork) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Pulsing Active Indicator Dot
                                    val transition = rememberInfiniteTransition(label = "dot_pulse")
                                    val alpha by transition.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 1.0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(800, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "dot_alpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color(0xFF00E676).copy(alpha = alpha), CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Dienst aktiv: $formattedTimerString",
                                        color = if (isDarkMode) Color(0xFF69F0AE) else Color(0xFF007E33),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "Kein aktiver Dienst. Tippe auf Einloggen.",
                                    color = primaryTextColor.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Toggle Login/Logout Button
                            Button(
                                onClick = handleToggleWorkService,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTrackingWork) Color(0xFFD32F2F) else Color(0xFF003399),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .tactilePress { handleToggleWorkService() }
                            ) {
                                Icon(
                                    if (isTrackingWork) Icons.Default.Logout else Icons.Default.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isTrackingWork) "Dienst beenden (Ausloggen)" else "Dienst starten (Einloggen)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // --- SCANNER BUTTON ---
                    LiquidButton(
                        onClick = {
                            val options = ScanOptions().apply {
                                setPrompt("Barcode scannen\n(Kamera fokussiert automatisch)")
                                setOrientationLocked(true)
                                setCaptureActivity(CustomCaptureActivity::class.java)
                            }
                            barcodeLauncher.launch(options)
                        },
                        isDarkMode = isDarkMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // --- ERSTMALIGER ZEITERFASSUNG HINWEIS DIALOG ---
                if (showWorkTimeDisclaimerDialog) {
                    Dialog(
                        onDismissRequest = { showWorkTimeDisclaimerDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        IosGroupedCard(
                            isDarkMode = isDarkMode,
                            cornerRadius = 28.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.90f)
                                .padding(vertical = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.WarningAmber,
                                        contentDescription = "Hinweis",
                                        tint = Color(0xFFFFB74D),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "Hinweis zur Zeiterfassung",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )
                                }

                                Text(
                                    text = "Achtung: Die Zeiterfassung ist eine rein lokale Funktion zur persönlichen Übersicht. Sie ist NICHT mit der offiziellen Online-Dienstzeiterfassung des THW synchronisiert.",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = primaryTextColor.copy(alpha = 0.85f)
                                )

                                IosButton(
                                    onClick = {
                                        prefs.edit().putBoolean("has_seen_work_time_disclaimer", true).apply()
                                        showWorkTimeDisclaimerDialog = false
                                        WorkTimeManager.startTracking(context)
                                        isTrackingWork = true
                                        val now = System.currentTimeMillis()
                                        trackingStartTime = now
                                        WorkTimeNotificationHelper.showOrUpdateNotification(context, now)
                                        Toast.makeText(context, "Dienst gestartet", Toast.LENGTH_SHORT).show()
                                    },
                                    isDarkMode = isDarkMode,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Verstanden", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // --- HILFE & FEHLERMELDUNG DIALOG (iOS MODAL SHEET) ---
                if (showHelpDialog) {
                    Dialog(
                        onDismissRequest = { showHelpDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        IosGroupedCard(
                            isDarkMode = isDarkMode,
                            cornerRadius = 28.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.90f)
                                .padding(vertical = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // iOS Header Row: Title + Round ✕ Close Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Hilfe & Info",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )

                                    Surface(
                                        onClick = { showHelpDialog = false },
                                        shape = CircleShape,
                                        color = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                                        modifier = Modifier
                                            .size(30.dp)
                                            .tactilePress { showHelpDialog = false }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Schließen",
                                                tint = primaryTextColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Version: $currentVersion\nScanne deinen THW-Barcode und verschiebe ihn vertikal an die gewünschte Position.",
                                    fontSize = 13.sp,
                                    color = primaryTextColor.copy(alpha = 0.8f)
                                )

                                HorizontalDivider(color = menuDividerColor)

                                // iOS Action Row: Nach Updates suchen
                                Surface(
                                    onClick = {
                                        if (!isCheckingUpdate) {
                                            isCheckingUpdate = true
                                            coroutineScope.launch {
                                                val update = AppUpdater.checkUpdate(currentVersion)
                                                isCheckingUpdate = false
                                                if (update != null) {
                                                    showHelpDialog = false
                                                    availableUpdate = update
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Kein Update gefunden (v$currentVersion ist aktuell)",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.70f),
                                    border = BorderStroke(1.dp, glassButtonBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .tactilePress {
                                            if (!isCheckingUpdate) {
                                                isCheckingUpdate = true
                                                coroutineScope.launch {
                                                    val update = AppUpdater.checkUpdate(currentVersion)
                                                    isCheckingUpdate = false
                                                    if (update != null) {
                                                        showHelpDialog = false
                                                        availableUpdate = update
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Kein Update gefunden (v$currentVersion ist aktuell)",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = if (isCheckingUpdate) "Prüfe..." else "Nach Updates suchen",
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryTextColor,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                // iOS Action Row: Problem auf GitHub melden
                                Surface(
                                    onClick = {
                                        val issuesUrl = "https://github.com/TheGamePRO112/THWLogin/issues/new"
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(issuesUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(browserIntent)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.70f),
                                    border = BorderStroke(1.dp, glassButtonBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .tactilePress {
                                            val issuesUrl = "https://github.com/TheGamePRO112/THWLogin/issues/new"
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(issuesUrl)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(browserIntent)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.BugReport, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Problem auf GitHub melden",
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryTextColor,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- MANUELLE BARCODE EINGABE DIALOG (iOS MODAL SHEET) ---
                if (showManualEntryDialog) {
                    Dialog(
                        onDismissRequest = { showManualEntryDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        IosGroupedCard(
                            isDarkMode = isDarkMode,
                            cornerRadius = 28.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.90f)
                                .padding(vertical = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // iOS Header Row: Title + Round ✕ Close Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Barcode manuell eingeben",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )

                                    Surface(
                                        onClick = { showManualEntryDialog = false },
                                        shape = CircleShape,
                                        color = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                                        modifier = Modifier
                                            .size(30.dp)
                                            .tactilePress { showManualEntryDialog = false }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Schließen",
                                                tint = primaryTextColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Gib die Barcode-Nummer ein (z. B. 12345678-99):",
                                    fontSize = 13.sp,
                                    color = primaryTextColor.copy(alpha = 0.8f)
                                )

                                TextField(
                                    value = manualBarcodeText,
                                    onValueChange = { input ->
                                        manualBarcodeText = input.filter { it.isDigit() || it == '-' }
                                    },
                                    placeholder = { Text("12345678-99") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = iosTextFieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                IosButton(
                                    onClick = {
                                        val trimmed = manualBarcodeText.trim()
                                        if (trimmed.isNotEmpty()) {
                                            barcodeValue = trimmed
                                            prefs.edit().putString("letzterBarcode", trimmed).apply()
                                            BarcodeWidgetProvider.updateAllWidgets(context)
                                            Toast.makeText(context, "Barcode gespeichert", Toast.LENGTH_SHORT).show()
                                        }
                                        showManualEntryDialog = false
                                    },
                                    isDarkMode = isDarkMode,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Speichern", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // --- STUNDEN-KALENDER DIALOG (iOS MODAL SHEET) ---
                if (showWorkCalendarDialog) {
                    val totalMinutesAll = remember(workEntriesList) { workEntriesList.sumOf { it.durationMinutes } }
                    val formattedTotalAll = remember(totalMinutesAll) { WorkTimeManager.formatMinutesToHours(totalMinutesAll) }
                    var calendarTab by remember { mutableIntStateOf(0) } // 0 = Liste, 1 = Kalender
                    var selectedMonthCal by remember { mutableStateOf(Calendar.getInstance()) }
                    var selectedDayFilter by remember { mutableStateOf<String?>(null) }

                    Dialog(
                        onDismissRequest = { showWorkCalendarDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        IosGroupedCard(
                            isDarkMode = isDarkMode,
                            cornerRadius = 28.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .padding(vertical = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(18.dp)
                                    .fillMaxWidth()
                                    .heightIn(max = 520.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // iOS Header Row: Title + Circular ✕ Close Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Dienstzeiten & Kalender",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )

                                    // Round ✕ Close Button
                                    Surface(
                                        onClick = { showWorkCalendarDialog = false },
                                        shape = CircleShape,
                                        color = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                                        modifier = Modifier
                                            .size(30.dp)
                                            .tactilePress { showWorkCalendarDialog = false }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Schließen",
                                                tint = primaryTextColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Summary Bar Card
                                Surface(
                                    color = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.60f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, glassButtonBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Gesamte Dienstzeit:",
                                            fontWeight = FontWeight.Medium,
                                            color = primaryTextColor,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = formattedTotalAll,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color(0xFF69F0AE) else Color(0xFF003399),
                                            fontSize = 15.sp
                                        )
                                    }
                                }

                                // Authentic iOS Segmented Control (Liste vs. Kalender)
                                IosSegmentedControl(
                                    items = listOf("Liste", "Kalender"),
                                    selectedIndex = calendarTab,
                                    onOptionSelected = { calendarTab = it },
                                    isDarkMode = isDarkMode,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (calendarTab == 0) {
                                    // --- LISTE TAB ---
                                    OutlinedButton(
                                        onClick = {
                                            editingWorkEntry = WorkEntry(
                                                date = WorkTimeManager.getTodayGermanDate(),
                                                startTime = "08:00",
                                                endTime = "16:00",
                                                durationMinutes = 480L,
                                                note = "Dienst"
                                            )
                                            showWorkCalendarDialog = false // Hide calendar dialog to prevent bleed-through
                                            showEditEntryDialog = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .tactilePress {
                                                editingWorkEntry = WorkEntry(
                                                    date = WorkTimeManager.getTodayGermanDate(),
                                                    startTime = "08:00",
                                                    endTime = "16:00",
                                                    durationMinutes = 480L,
                                                    note = "Dienst"
                                                )
                                                showWorkCalendarDialog = false
                                                showEditEntryDialog = true
                                            }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Stunden nachtragen")
                                    }

                                    HorizontalDivider(color = menuDividerColor)

                                    if (workEntriesList.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Noch keine Dienstzeiten erfasst.",
                                                color = primaryTextColor.copy(alpha = 0.6f),
                                                fontSize = 13.sp
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(workEntriesList.size) { index ->
                                                val entry = workEntriesList[index]
                                                Surface(
                                                    color = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.70f),
                                                    shape = RoundedCornerShape(16.dp),
                                                    border = BorderStroke(1.dp, glassButtonBorder),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "${entry.date} (${entry.note})",
                                                                fontWeight = FontWeight.Bold,
                                                                color = primaryTextColor,
                                                                fontSize = 13.sp
                                                            )
                                                            Text(
                                                                text = "${entry.startTime} - ${entry.endTime} Uhr  •  ${WorkTimeManager.formatMinutesToHours(entry.durationMinutes)}",
                                                                fontSize = 12.sp,
                                                                color = primaryTextColor.copy(alpha = 0.7f)
                                                            )
                                                        }
                                                        Row {
                                                            IconButton(
                                                                onClick = {
                                                                    editingWorkEntry = entry
                                                                    showWorkCalendarDialog = false // Hide calendar dialog to prevent bleed-through
                                                                    showEditEntryDialog = true
                                                                },
                                                                modifier = Modifier
                                                                    .size(32.dp)
                                                                    .tactilePress {
                                                                        editingWorkEntry = entry
                                                                        showWorkCalendarDialog = false
                                                                        showEditEntryDialog = true
                                                                    }
                                                            ) {
                                                                Icon(Icons.Default.Edit, contentDescription = "Bearbeiten", tint = iconTint, modifier = Modifier.size(16.dp))
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    WorkTimeManager.deleteEntry(context, entry.id)
                                                                    workEntriesList = WorkTimeManager.getEntries(context)
                                                                },
                                                                modifier = Modifier
                                                                    .size(32.dp)
                                                                    .tactilePress {
                                                                        WorkTimeManager.deleteEntry(context, entry.id)
                                                                        workEntriesList = WorkTimeManager.getEntries(context)
                                                                    }
                                                            ) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Löschen", modifier = Modifier.size(16.dp), tint = Color(0xFFFF5252))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // --- KALENDER TAB ---
                                    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.GERMANY) }
                                    val currentMonthTitle = monthYearFormat.format(selectedMonthCal.time)

                                    // Month Navigation
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val c = selectedMonthCal.clone() as Calendar
                                                c.add(Calendar.MONTH, -1)
                                                selectedMonthCal = c
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.ChevronLeft, contentDescription = "Vorheriger Monat", tint = primaryTextColor)
                                        }

                                        Text(
                                            text = currentMonthTitle.replaceFirstChar { it.uppercase() },
                                            fontWeight = FontWeight.Bold,
                                            color = primaryTextColor,
                                            fontSize = 14.sp
                                        )

                                        IconButton(
                                            onClick = {
                                                val c = selectedMonthCal.clone() as Calendar
                                                c.add(Calendar.MONTH, 1)
                                                selectedMonthCal = c
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.ChevronRight, contentDescription = "Nächster Monat", tint = primaryTextColor)
                                        }
                                    }

                                    // Weekdays header
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                        listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { day ->
                                            Text(text = day, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryTextColor.copy(alpha = 0.5f), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                        }
                                    }

                                    // Month Grid
                                    val daysInMonth = remember(selectedMonthCal) {
                                        val cal = selectedMonthCal.clone() as Calendar
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
                                        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                        Pair(firstDayOfWeek, maxDays)
                                    }

                                    val yearInt = selectedMonthCal.get(Calendar.YEAR)
                                    val monthInt = selectedMonthCal.get(Calendar.MONTH) + 1

                                    val entriesForMonth = remember(workEntriesList, yearInt, monthInt) {
                                        workEntriesList.filter { entry ->
                                            val key = WorkTimeManager.getSortKey(entry.date)
                                            val targetMonthKey = String.format(Locale.GERMANY, "%04d%02d", yearInt, monthInt)
                                            key.startsWith(targetMonthKey)
                                        }
                                    }

                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val totalCells = daysInMonth.first + daysInMonth.second
                                        val rows = (totalCells + 6) / 7

                                        for (r in 0 until rows) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                                for (c in 0 until 7) {
                                                    val cellIndex = r * 7 + c
                                                    val dayNum = cellIndex - daysInMonth.first + 1

                                                    if (cellIndex >= daysInMonth.first && dayNum <= daysInMonth.second) {
                                                        val dateFormatted = String.format(Locale.GERMANY, "%02d.%02d.%04d", dayNum, monthInt, yearInt)
                                                        val dayEntries = entriesForMonth.filter { WorkTimeManager.formatDateToGerman(it.date) == dateFormatted }
                                                        val dayMinutes = dayEntries.sumOf { it.durationMinutes }
                                                        val hasEntries = dayMinutes > 0
                                                        val isSelected = selectedDayFilter == dateFormatted

                                                        Surface(
                                                            color = when {
                                                                isSelected -> Color(0xFF0055FF)
                                                                hasEntries -> Color(0xFF003399).copy(alpha = 0.5f)
                                                                else -> Color.Transparent
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(38.dp)
                                                                .padding(1.dp),
                                                            onClick = {
                                                                selectedDayFilter = if (isSelected) null else dateFormatted
                                                            }
                                                        ) {
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.Center,
                                                                modifier = Modifier.fillMaxSize()
                                                            ) {
                                                                Text(
                                                                    text = "$dayNum",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = if (hasEntries) FontWeight.Bold else FontWeight.Normal,
                                                                    color = primaryTextColor
                                                                )
                                                                if (hasEntries) {
                                                                    Text(
                                                                        text = WorkTimeManager.formatMinutesToHours(dayMinutes),
                                                                        fontSize = 9.sp,
                                                                        color = Color(0xFF69F0AE),
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Filtered entries for selected day
                                    selectedDayFilter?.let { filterDate ->
                                        HorizontalDivider(color = menuDividerColor)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Einträge für $filterDate:", fontWeight = FontWeight.Bold, color = primaryTextColor, fontSize = 12.sp)
                                            TextButton(
                                                onClick = {
                                                    editingWorkEntry = WorkEntry(
                                                        date = filterDate,
                                                        startTime = "08:00",
                                                        endTime = "16:00",
                                                        durationMinutes = 480L,
                                                        note = "Dienst"
                                                    )
                                                    showWorkCalendarDialog = false // Hide calendar dialog to prevent bleed-through
                                                    showEditEntryDialog = true
                                                }
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Nachtragen", fontSize = 12.sp)
                                            }
                                        }

                                        val filteredDayEntries = workEntriesList.filter { WorkTimeManager.formatDateToGerman(it.date) == filterDate }
                                        if (filteredDayEntries.isEmpty()) {
                                            Text("Kein Dienst an diesem Tag.", fontSize = 12.sp, color = primaryTextColor.copy(alpha = 0.5f))
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                filteredDayEntries.forEach { entry ->
                                                    Surface(
                                                        color = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.70f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("${entry.startTime} - ${entry.endTime} (${entry.note}): ${WorkTimeManager.formatMinutesToHours(entry.durationMinutes)}", fontSize = 12.sp, color = primaryTextColor)
                                                            Row {
                                                                IconButton(onClick = { editingWorkEntry = entry; showWorkCalendarDialog = false; showEditEntryDialog = true }, modifier = Modifier.size(24.dp)) {
                                                                    Icon(Icons.Default.Edit, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
                                                                }
                                                                IconButton(onClick = { WorkTimeManager.deleteEntry(context, entry.id); workEntriesList = WorkTimeManager.getEntries(context) }, modifier = Modifier.size(24.dp)) {
                                                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF5252))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- STUNDEN BEARBEITEN / NACHTRAGEN DIALOG (iOS MODAL SHEET) ---
                if (showEditEntryDialog && editingWorkEntry != null) {
                    var editDate by remember { mutableStateOf(WorkTimeManager.formatDateToGerman(editingWorkEntry!!.date)) }
                    var editStart by remember { mutableStateOf(editingWorkEntry!!.startTime) }
                    var editEnd by remember { mutableStateOf(editingWorkEntry!!.endTime) }
                    var editNote by remember { mutableStateOf(editingWorkEntry!!.note) }

                    Dialog(
                        onDismissRequest = {
                            showEditEntryDialog = false
                            showWorkCalendarDialog = true // Restore calendar view cleanly
                        },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        IosGroupedCard(
                            isDarkMode = isDarkMode,
                            cornerRadius = 28.dp,
                            modifier = Modifier
                                .fillMaxWidth(0.90f)
                                .padding(vertical = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // iOS Header Row: Title + Round ✕ Close Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Eintrag bearbeiten / nachtragen",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )

                                    Surface(
                                        onClick = {
                                            showEditEntryDialog = false
                                            showWorkCalendarDialog = true // Restore calendar view cleanly
                                        },
                                        shape = CircleShape,
                                        color = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                                        modifier = Modifier
                                            .size(30.dp)
                                            .tactilePress {
                                                showEditEntryDialog = false
                                                showWorkCalendarDialog = true
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Schließen",
                                                tint = primaryTextColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                TextField(
                                    value = editDate,
                                    onValueChange = { editDate = it },
                                    label = { Text("Datum (TT.MM.JJJJ)") },
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val cal = Calendar.getInstance()
                                            try {
                                                if (editDate.contains(".")) {
                                                    val parts = editDate.split(".")
                                                    if (parts.size == 3) {
                                                        cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
                                                    }
                                                }
                                            } catch (_: Exception) {}

                                            DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    editDate = String.format(Locale.GERMANY, "%02d.%02d.%04d", d, m + 1, y)
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }) {
                                            Icon(Icons.Default.DateRange, contentDescription = "Kalender öffnen", tint = iconTint)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = iosTextFieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                TextField(
                                    value = editStart,
                                    onValueChange = { editStart = it },
                                    label = { Text("Startzeit (HH:mm)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = iosTextFieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                TextField(
                                    value = editEnd,
                                    onValueChange = { editEnd = it },
                                    label = { Text("Endzeit (HH:mm)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = iosTextFieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                TextField(
                                    value = editNote,
                                    onValueChange = { editNote = it },
                                    label = { Text("Bezeichnung / Notiz") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = iosTextFieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                IosButton(
                                    onClick = {
                                        val durMin = try {
                                            val sdf = SimpleDateFormat("HH:mm", Locale.GERMANY)
                                            val d1 = sdf.parse(editStart)
                                            val d2 = sdf.parse(editEnd)
                                            if (d1 != null && d2 != null) {
                                                maxOf(1L, (d2.time - d1.time) / (1000 * 60))
                                            } else 60L
                                        } catch (_: Exception) { 60L }

                                        val updated = editingWorkEntry!!.copy(
                                            date = editDate.trim(),
                                            startTime = editStart.trim(),
                                            endTime = editEnd.trim(),
                                            durationMinutes = durMin,
                                            note = editNote.trim()
                                        )
                                        val currentList = WorkTimeManager.getEntries(context)
                                        if (currentList.any { it.id == updated.id }) {
                                            WorkTimeManager.updateEntry(context, updated)
                                        } else {
                                            WorkTimeManager.addEntry(context, updated)
                                        }
                                        workEntriesList = WorkTimeManager.getEntries(context)
                                        showEditEntryDialog = false
                                        showWorkCalendarDialog = true // Restore calendar view cleanly
                                    },
                                    isDarkMode = isDarkMode,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Speichern", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
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
