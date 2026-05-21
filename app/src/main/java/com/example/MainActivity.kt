package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TvApp
import com.example.data.TvAppDatabase
import com.example.data.TvAppRepository
import com.example.ui.TvAppViewModel
import com.example.ui.TvAppViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Room Database and Repository
        val database = TvAppDatabase.getDatabase(this)
        val repository = TvAppRepository(database.tvAppDao())
        
        setContent {
            MyApplicationTheme {
                val viewModel: TvAppViewModel = viewModel(
                    factory = TvAppViewModelFactory(repository)
                )
                TvLauncherScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TvLauncherScreen(viewModel: TvAppViewModel) {
    val context = LocalContext.current
    val apps by viewModel.filteredApps.collectAsState(initial = emptyList())
    val allAppsRaw by viewModel.allApps.collectAsState(initial = emptyList())
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isMinimalMode by viewModel.isMinimalMode.collectAsState()
    val selectedAppIndex by viewModel.selectedAppIndex.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val selectedAppDetail by viewModel.selectedAppDetail.collectAsState()
    
    var launchingApp by remember { mutableStateOf<TvApp?>(null) }
    var isDefaultLauncher by remember { mutableStateOf(false) }

    // Proactively check if Focus Mode is set as the default device launcher
    LaunchedEffect(Unit) {
        while (true) {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            isDefaultLauncher = (resolveInfo?.activityInfo?.packageName == context.packageName)
            delay(2000) // Poll every 2 seconds to instantly refresh when user sets it and returns
        }
    }

    val onSetDefaultLauncher = {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                    addCategory(android.content.Intent.CATEGORY_HOME)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(
                    context,
                    "Open settings to choose Focus Mode as your default launcher of this device.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // Listen for launcher event to trigger visual simulator overlays
    LaunchedEffect(key1 = true) {
        viewModel.launchEvent.collect { app ->
            launchingApp = app
            delay(2000) // Pulse animation runtime
            launchingApp = null
        }
    }
    
    // Bounds-check selected app index on catalogue database resizing
    val currentApp: TvApp? = if (allAppsRaw.isNotEmpty()) {
        val idx = selectedAppIndex.coerceIn(0, allAppsRaw.size - 1)
        allAppsRaw[idx]
    } else {
        null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF381E72).copy(alpha = 0.2f),
                        Color(0xFF1C1B1F)
                    ),
                    center = Offset(0.5f, -0.2f),
                    radius = 1200f
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Background soft ambient light representation in the Elegant Dark theme palette
        val categoryColor = when (selectedCategory) {
            "Video" -> Color(0xFFD0BCFF).copy(alpha = 0.12f)
            "Audio" -> Color(0xFFD0BCFF).copy(alpha = 0.08f)
            "Games" -> Color(0xFFD0BCFF).copy(alpha = 0.12f)
            "Utility" -> Color(0xFFD0BCFF).copy(alpha = 0.08f)
            else -> Color.Transparent
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(categoryColor, Color.Transparent),
                        center = Offset(0.8f, 0.1f),
                        radius = 800f
                    )
                )
        )

        // Main Animated Content switching between Minimalist and Complicated mode
        AnimatedContent(
            targetState = isMinimalMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "ViewModeTransition"
        ) { minimal ->
            if (minimal) {
                // VIEW 1: Ultra-Clean Textual Mode focusing on ONLY one app is active
                MinimalistTextualView(
                    app = currentApp,
                    onNext = { viewModel.nextApp(allAppsRaw.size) },
                    onPrev = { viewModel.prevApp(allAppsRaw.size) },
                    onLaunch = { viewModel.launchApp(context, it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onSwitchToComplicated = { viewModel.isMinimalMode.value = false },
                    onOpenAddDialog = { viewModel.showAddDialog.value = true },
                    totalAppsCount = allAppsRaw.size,
                    currentIndex = selectedAppIndex,
                    isDefaultLauncher = isDefaultLauncher,
                    onSetDefaultLauncher = onSetDefaultLauncher
                )
            } else {
                // VIEW 2: Complicated Catalog Hub containing rich tabs, bookmarks, details & search
                ComplicatedCatalogView(
                    apps = apps,
                    allApps = allAppsRaw,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    onSearchQueryChange = { viewModel.searchQuery.value = it },
                    onCategorySelect = { viewModel.selectedCategory.value = it },
                    onAppClick = { viewModel.selectedAppDetail.value = it },
                    onLaunchApp = { viewModel.launchApp(context, it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onSwitchToMinimal = { viewModel.isMinimalMode.value = true },
                    onOpenAddDialog = { viewModel.showAddDialog.value = true },
                    onDeleteApp = { viewModel.deleteApp(it) },
                    isDefaultLauncher = isDefaultLauncher,
                    onSetDefaultLauncher = onSetDefaultLauncher
                )
            }
        }

        // --- LAYER OVERLAYS & MODALS ---

        // 1. ADD CUSTOM APP BOOKMARK DIALOG
        if (showAddDialog) {
            AddCustomAppDialog(
                onDismiss = { viewModel.showAddDialog.value = false },
                onAddApp = { name, pkg, cat, desc ->
                    viewModel.addCustomApp(name, pkg, cat, desc)
                    viewModel.showAddDialog.value = false
                    Toast.makeText(context, "$name Added to Custom Bookmarks", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 2. DETAILED APP PROFILE DIRECT DIALOG
        selectedAppDetail?.let { app ->
            DetailedAppModal(
                app = app,
                onDismiss = { viewModel.selectedAppDetail.value = null },
                onLaunch = {
                    viewModel.selectedAppDetail.value = null
                    viewModel.launchApp(context, app)
                },
                onToggleFavorite = { viewModel.toggleFavorite(app) },
                onDelete = {
                    viewModel.deleteApp(app)
                }
            )
        }

        // 3. LAUNCH SIMULATOR ANIMATION OVERLAY
        AnimatedVisibility(
            visible = launchingApp != null,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(400))
        ) {
            launchingApp?.let { app ->
                SimulatedLaunchOverlay(app = app)
            }
        }
    }
}

// ==========================================
// VIEW 1: MINIMALIST TEXTUAL SINGLE-APP VIEW
// ==========================================
@Composable
fun MinimalistTextualView(
    app: TvApp?,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunch: (TvApp) -> Unit,
    onToggleFavorite: (TvApp) -> Unit,
    onSwitchToComplicated: () -> Unit,
    onOpenAddDialog: () -> Unit,
    totalAppsCount: Int,
    currentIndex: Int,
    isDefaultLauncher: Boolean,
    onSetDefaultLauncher: () -> Unit
) {
    if (app == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "No apps in dashboard",
                    color = Color(0xFF938F99),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onOpenAddDialog,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Service")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add App Shortcut", color = Color(0xFF381E72), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    val themeAccentColor = Color(0xFFD0BCFF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP ZEN HEADER WITH LOGO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Precise rounded-full brand gradient from Design HTML with absolute styling
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFD0BCFF), Color(0xFF381E72))
                            )
                        )
                )
                Column {
                    Text(
                        text = "Focus Mode",
                        color = Color(0xFFE6E1E5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "Pure • Textual View",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp
                    )
                }
            }

            // Elegant Actions Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick setting configuration option to make Focus Mode Default Launcher
                TextButton(
                    onClick = onSetDefaultLauncher,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (isDefaultLauncher) Color(0xFF49454F) else Color(0xFFD0BCFF).copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isDefaultLauncher) Color(0xFF2B2930) else Color(0xFF381E72).copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = if (isDefaultLauncher) Icons.Default.CheckCircle else Icons.Default.Home,
                        contentDescription = "Device home launcher setting status",
                        tint = if (isDefaultLauncher) Color(0xFF4AF288) else Color(0xFFD0BCFF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDefaultLauncher) "Default Home" else "Set Default Home",
                        color = if (isDefaultLauncher) Color(0xFFE6E1E5) else Color(0xFFD0BCFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Elegant Quick Toggle
                TextButton(
                    onClick = onSwitchToComplicated,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                        .testTag("switch_to_complicated_view"),
                    colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFF2B2930))
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Complicated View",
                        tint = Color(0xFFE6E1E5),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complicated View", color = Color(0xFFE6E1E5), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // CENTER STAGE: SINGLE GIANT APP TEXT CARD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Swipe Indicator using crisp design typography
            IconButton(
                onClick = onPrev,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2B2930), CircleShape)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF49454F), CircleShape)
            ) {
                Text("<", color = Color(0xFFE6E1E5), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Main typography Focus Block
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Category/Focus Tag with explicit tracked uppercase layout
                Text(
                    text = "CURRENTLY ACTIVE",
                    color = Color(0xFFD0BCFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Giant Title with light weights and elegant details matching the design guide
                Text(
                    text = app.name.uppercase(),
                    color = Color(0xFFE6E1E5),
                    fontSize = 48.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.5).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Divider line explicitly matching HTML border specification: w-12 h-1 bg-[#49454F]
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(48.dp)
                        .height(1.dp)
                        .background(Color(0xFF49454F))
                )

                // Inline Stars/Rating & Clicks Counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${app.rating}",
                        color = Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "•",
                        color = Color(0xFF49454F),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Launches",
                        tint = Color(0xFF938F99),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Launched ${app.clicks} times",
                        color = Color(0xFF938F99),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                // Subtitle/Brief Description in thin italic representation
                Text(
                    text = app.description,
                    color = Color(0xFF938F99),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Swipe Indicator using crisp design typography
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2B2930), CircleShape)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFF49454F), CircleShape)
            ) {
                Text(">", color = Color(0xFFE6E1E5), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // BOTTOM INTEGRATED PILL ACTIONS AND CONTROLS
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Large Open Application Button with elegant primary dark layout specifications
                Button(
                    onClick = { onLaunch(app) },
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .testTag("launch_hero_app_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Launch Action",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF381E72)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Application",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.1).sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Star Button representation using core Star icon color shift
                OutlinedIconButton(
                    onClick = { onToggleFavorite(app) },
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("toggle_favorite_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF49454F)),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = Color(0xFF2B2930),
                        contentColor = if (app.isFavorite) Color(0xFFD0BCFF) else Color(0xFF938F99)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Toggle Favorite",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Plus option shortcut button
                OutlinedIconButton(
                    onClick = onOpenAddDialog,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF49454F)),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        containerColor = Color(0xFF2B2930),
                        contentColor = Color(0xFFE6E1E5)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Service Shortcut",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Carousel Dots representation using elegant violet/gray accents
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until totalAppsCount) {
                    val isActive = i == currentIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isActive) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color(0xFFD0BCFF) else Color(0xFF49454F)
                            )
                    )
                }
            }
        }
    }
}

// ============================================
// VIEW 2: COMPLICATED CINEMATIC CATALOG VIEW
// ============================================
@Composable
fun ComplicatedCatalogView(
    apps: List<TvApp>,
    allApps: List<TvApp>,
    searchQuery: String,
    selectedCategory: String,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onAppClick: (TvApp) -> Unit,
    onLaunchApp: (TvApp) -> Unit,
    onToggleFavorite: (TvApp) -> Unit,
    onSwitchToMinimal: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onDeleteApp: (TvApp) -> Unit,
    isDefaultLauncher: Boolean,
    onSetDefaultLauncher: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    // Top featured active spotlight banner (The "Hero" item)
    val featuredApp = allApps.firstOrNull { it.isFavorite } ?: allApps.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        
        // 1. DASHBOARD NAVIGATION HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HUB DASHBOARD",
                    fontSize = 11.sp,
                    color = Color(0xFFD0BCFF),
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Custom catalog • No Commercials",
                    fontSize = 11.sp,
                    color = Color(0xFF938F99)
                )
            }
            
            // Elegant Settings & Switch Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Set Default Launcher setting launcher shortcut
                TextButton(
                    onClick = onSetDefaultLauncher,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (isDefaultLauncher) Color(0xFF49454F) else Color(0xFFD0BCFF).copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isDefaultLauncher) Color(0xFF2B2930) else Color(0xFF381E72).copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = if (isDefaultLauncher) Icons.Default.CheckCircle else Icons.Default.Home,
                        contentDescription = "Device home setting selector status",
                        tint = if (isDefaultLauncher) Color(0xFF4AF288) else Color(0xFFD0BCFF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDefaultLauncher) "Default Home" else "Set Default Home",
                        color = if (isDefaultLauncher) Color(0xFFE6E1E5) else Color(0xFFD0BCFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Switch back to text focus view with Elegant Dark formatting
                TextButton(
                    onClick = onSwitchToMinimal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                        .testTag("switch_to_minimal_view"),
                    colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFF2B2930))
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Minimal Mode",
                        tint = Color(0xFFE6E1E5),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Minimal Textual View", color = Color(0xFFE6E1E5), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // 2. CINEMATIC GLASS HERO SPOTLIGHT
        featuredApp?.let { app ->
            SpotlightHeroBanner(app = app, onLaunch = { onLaunchApp(app) }, onToggleFavorite = { onToggleFavorite(app) })
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. SEARCH & ADD ACTIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search field container
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(14.dp)),
                placeholder = { Text("Search services...", color = Color(0xFF938F99), fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF938F99), modifier = Modifier.size(20.dp)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF2B2930),
                    unfocusedContainerColor = Color(0xFF2B2930),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color(0xFFE6E1E5),
                    unfocusedTextColor = Color(0xFFE6E1E5)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            // Add Custom Service shortcut button
            IconButton(
                onClick = onOpenAddDialog,
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFF2B2930), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(14.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add custom service", tint = Color(0xFFE6E1E5))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. CATEGORIES FILTER TABS
        val categories = listOf("All", "Video", "Audio", "Games", "Utility")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(cat) },
                    label = { 
                        Text(
                            text = cat, 
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF938F99)
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF2B2930),
                        selectedContainerColor = Color(0xFF381E72),
                        labelColor = Color(0xFF938F99)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFF49454F),
                        selectedBorderColor = Color.Transparent,
                        selectedBorderWidth = 0.dp,
                        enabled = true,
                        selected = isSelected
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. CURRENT SERVICES GRID LIST
        Text(
            text = "Your Services (${apps.size})",
            fontSize = 15.sp,
            color = Color(0xFFE6E1E5),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp)
                    .background(Color(0xFF2B2930), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF49454F), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Empty list",
                        tint = Color(0xFF938F99),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No services match your filters.",
                        color = Color(0xFF938F99),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Flexible responsive layout displaying elegant cards
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                apps.chunked(2).forEach { rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowApps.forEach { app ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                AppShortcutCard(
                                    app = app,
                                    onClick = { onAppClick(app) },
                                    onLaunch = { onLaunchApp(app) }
                                )
                            }
                        }
                        if (rowApps.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. CUSTOM BOOKMARKS ROW
        val customApps = allApps.filter { it.isCustom }
        if (customApps.isNotEmpty()) {
            Text(
                text = "My Custom Bookmarks",
                fontSize = 15.sp,
                color = Color(0xFFE6E1E5),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customApps) { app ->
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .height(80.dp)
                            .clickable { onAppClick(app) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF49454F))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = app.name,
                                color = Color(0xFFE6E1E5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = app.category.uppercase(),
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Launch",
                                    tint = Color(0xFFE6E1E5).copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 7. STATISTICS SUMMARY
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Usage Stat",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Usage Stats Dashboard",
                        color = Color(0xFF938F99),
                        fontSize = 12.sp
                    )
                }
                
                val totalClicks = allApps.sumOf { it.clicks }
                Text(
                    text = "$totalClicks total launches recorded",
                    color = Color(0xFFE6E1E5),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ===================================
// HERO BANNER SPOTLIGHT COMPONENT
// ===================================
@Composable
fun SpotlightHeroBanner(
    app: TvApp,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val categoryColor = Color(0xFFD0BCFF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF381E72).copy(alpha = 0.3f),
                        Color(0xFF2B2930)
                    )
                )
            )
            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF381E72).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "FEATURED",
                            color = Color(0xFFD0BCFF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = app.category.uppercase(),
                        color = Color(0xFF938F99),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = app.name,
                    color = Color(0xFFE6E1E5),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = app.description,
                    color = Color(0xFF938F99),
                    fontSize = 11.sp,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, end = 24.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72)
                    ),
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Launch", modifier = Modifier.size(14.dp), tint = Color(0xFF381E72))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LAUNCH NOW", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Toggle favorite",
                        tint = if (app.isFavorite) Color(0xFFD0BCFF) else Color(0xFF938F99)
                    )
                }
            }
        }
    }
}

// ============================================
// APP SHORTCUT CARDS (COMPLICATED GRID TILES)
// ============================================
@Composable
fun AppShortcutCard(
    app: TvApp,
    onClick: () -> Unit,
    onLaunch: () -> Unit
) {
    val cardColor = Color(0xFFD0BCFF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable { onClick() }
            .testTag("app_card_${app.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF49454F))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        color = Color(0xFFE6E1E5),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Launched: ${app.clicks}x",
                        color = Color(0xFF938F99),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                if (app.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Starred",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(cardColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = app.category.uppercase(),
                        color = Color(0xFF938F99),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Quick Launch visual ripple trigger
                IconButton(
                    onClick = onLaunch,
					modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF381E72).copy(alpha = 0.5f), CircleShape)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Direct Launch",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ============================================
// DETAILS PROFILE MODAL / DIALOG COMPONENT
// ============================================
@Composable
fun DetailedAppModal(
    app: TvApp,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1B1F),
        titleContentColor = Color(0xFFE6E1E5),
        textContentColor = Color(0xFF938F99),
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = app.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (app.isCustom) {
                    IconButton(
                        onClick = { 
                            onDelete()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Bookmark", tint = Color.Red.copy(alpha = 0.7f))
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF381E72).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF49454F), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = app.category.uppercase(),
                            color = Color(0xFFD0BCFF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFD0BCFF), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${app.rating} ★", color = Color(0xFFE6E1E5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Total clicks: ${app.clicks}", color = Color(0xFF938F99), fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = app.description,
                    color = Color(0xFF938F99),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "System package identification:\n${app.packageName}",
                    color = Color(0xFF938F99).copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Launch", modifier = Modifier.size(16.dp), tint = Color(0xFF381E72))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Launch Service", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onToggleFavorite,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE6E1E5))
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star",
                    tint = if (app.isFavorite) Color(0xFFD0BCFF) else Color(0xFF938F99),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (app.isFavorite) "Favorited" else "Add Favorite", fontWeight = FontWeight.Medium)
            }
        }
    )
}

// ============================================
// DIALOG: ADD CUSTOM SHORTCUT BOOKMARK APPS
// ============================================
@Composable
fun AddCustomAppDialog(
    onDismiss: () -> Unit,
    onAddApp: (name: String, pkg: String, cat: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pkg by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Video") }
    var desc by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1B1F),
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "Add TV App Bookmark",
                color = Color(0xFFE6E1E5),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure a commercial-free shortcut. If the application is installed on your TV device, launching it will trigger the native system app.",
                    color = Color(0xFF938F99),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                if (errorMsg.isNotEmpty()) {
                    Text(text = errorMsg, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        if (it.isNotEmpty()) errorMsg = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("App Name *", color = Color(0xFF938F99)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Android Package ID (optional)", color = Color(0xFF938F99)) },
                    placeholder = { Text("e.g. com.disney.disneyplus", color = Color(0xFF49454F)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    label = { Text("Brief Description", color = Color(0xFF938F99)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFE6E1E5),
                        unfocusedTextColor = Color(0xFFE6E1E5),
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF49454F)
                    )
                )

                Text(text = "Category Filter Tag *:", color = Color(0xFFE6E1E5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                val categories = listOf("Video", "Audio", "Games", "Utility")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = selectedCat == cat
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF381E72) else Color(0xFF2B2930))
                                .border(1.dp, if (isSel) Color.Transparent else Color(0xFF49454F), RoundedCornerShape(8.dp))
                                .clickable { selectedCat = cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                color = if (isSel) Color(0xFFD0BCFF) else Color(0xFF938F99),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        errorMsg = "App Name is required"
                    } else {
                        onAddApp(name, pkg, selectedCat, desc)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Shortcut", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF938F99))
            ) {
                Text("Cancel")
            }
        }
    )
}

// ============================================
// SIMULATED LAUNCH FULLSCREEN OVERLAY PULSER
// ============================================
@Composable
fun SimulatedLaunchOverlay(app: TvApp) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE61C1B1F)) // 90% opacity of actual Elegant Dark carbon background
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF381E72).copy(alpha = pulseAlpha * 0.4f))
                    .border(2.dp, Color(0xFFD0BCFF).copy(alpha = pulseAlpha), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "TV launch animation",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Launching commercial-free link...",
                color = Color(0xFF938F99),
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = app.name,
                color = Color(0xFFE6E1E5),
                fontSize = 32.sp,
                fontWeight = FontWeight.Thin,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                color = Color(0xFFD0BCFF),
                trackColor = Color(0xFF49454F),
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "System Package Identifier:\n${app.packageName}",
                color = Color(0xFF938F99).copy(alpha = 0.3f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
