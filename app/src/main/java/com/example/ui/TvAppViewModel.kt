package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TvApp
import com.example.data.TvAppRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TvAppViewModel(private val repository: TvAppRepository) : ViewModel() {

    // Main ui states
    val allApps = repository.allApps

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val isMinimalMode = MutableStateFlow(true) // Default to Minimalist textual single-app focus
    val selectedAppIndex = MutableStateFlow(0)
    val showAddDialog = MutableStateFlow(false)
    val selectedAppDetail = MutableStateFlow<TvApp?>(null)

    // Event flow for launching simulation animations
    private val _launchEvent = MutableSharedFlow<TvApp>()
    val launchEvent: SharedFlow<TvApp> = _launchEvent.asSharedFlow()

    init {
        // Prepopulate default apps if completely empty
        viewModelScope.launch {
            if (repository.getCount() == 0) {
                val defaults = listOf(
                    TvApp(
                        name = "YouTube TV",
                        packageName = "com.google.android.youtube.tv",
                        category = "Video",
                        description = "The premier global video sharing and live broadcast stream platform designed fully for television.",
                        rating = 4.8f,
                        isFavorite = true,
                        clicks = 50
                    ),
                    TvApp(
                        name = "Netflix",
                        packageName = "com.netflix.ninja",
                        category = "Video",
                        description = "Nostalgic cinema classics, award-winning original shows, blockbusters, and binge-worthy documentaries.",
                        rating = 4.7f,
                        isFavorite = true,
                        clicks = 42
                    ),
                    TvApp(
                        name = "Spotify Music",
                        packageName = "com.spotify.tv.android",
                        category = "Audio",
                        description = "Unleash millions of background songs, personalized curation playlists, and global podcast content.",
                        rating = 4.6f,
                        isFavorite = true,
                        clicks = 30
                    ),
                    TvApp(
                        name = "Prime Video",
                        packageName = "com.amazon.amazonvideo.livingroom",
                        category = "Video",
                        description = "Stream stunning Amazon Studio Originals, popular Hollywood blockbusters, and live premium athletic channels.",
                        rating = 4.3f,
                        isFavorite = false,
                        clicks = 18
                    ),
                    TvApp(
                        name = "Disney+",
                        packageName = "com.disney.disneyplus",
                        category = "Video",
                        description = "The home of animated and live-action storytelling from Disney, IP films from Pixar, Marvel, and Star Wars.",
                        rating = 4.5f,
                        isFavorite = false,
                        clicks = 12
                    ),
                    TvApp(
                        name = "Twitch TV",
                        packageName = "tv.twitch.android.app",
                        category = "Video",
                        description = "Connect live with content creators, watch top-tier gaming gameplay tournaments, and chat in world communities.",
                        rating = 4.4f,
                        isFavorite = false,
                        clicks = 8
                    ),
                    TvApp(
                        name = "Plex Client",
                        packageName = "com.plexapp.android",
                        category = "Video",
                        description = "Seamlessly index and cast your personal photo albums, downloaded movies, and live local networks in one media hub.",
                        rating = 4.2f,
                        isFavorite = false,
                        clicks = 5
                    ),
                    TvApp(
                        name = "VLC Player",
                        packageName = "org.videolan.vlc",
                        category = "Utility",
                        description = "The ultimate open-source cross platform file parser suited for streaming network links or complex video formats.",
                        rating = 4.7f,
                        isFavorite = false,
                        clicks = 3
                    ),
                    TvApp(
                        name = "TED Talks",
                        packageName = "com.ted.android.tv",
                        category = "Utility",
                        description = "Nourish your analytical thinking with mind-expanding lectures on global tech, natural sciences, and humanities.",
                        rating = 4.1f,
                        isFavorite = false,
                        clicks = 1
                    ),
                    TvApp(
                        name = "RetroArch",
                        packageName = "com.retroarch",
                        category = "Games",
                        description = "Integrate classic 8-bit, 16-bit, and 3D retro arcade game emulators using a unified, clean frontend panel.",
                        rating = 4.6f,
                        isFavorite = false,
                        clicks = 0
                    )
                )
                repository.insertAll(defaults)
            }
        }
    }

    // Filter apps based on search query and category
    val filteredApps: StateFlow<List<TvApp>> = combine(allApps, searchQuery, selectedCategory) { list, search, cat ->
        list.filter { app ->
            val matchesSearch = app.name.contains(search, ignoreCase = true) ||
                    app.description.contains(search, ignoreCase = true) ||
                    app.packageName.contains(search, ignoreCase = true)
            val matchesCategory = (cat == "All") || (app.category == cat)
            matchesSearch && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun launchApp(context: Context, app: TvApp) {
        viewModelScope.launch {
            repository.incrementClicks(app.id)
            _launchEvent.emit(app)
            
            // Try actual device package launch, otherwise redirect to Google Play Store to install it!
            try {
                val intent: Intent? = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    context.startActivity(intent)
                } else {
                    Toast.makeText(
                        context,
                        "App '${app.name}' is not installed. Opening Google Play Store to download...",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    try {
                        val playStoreIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${app.packageName}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(playStoreIntent)
                    } catch (e: Exception) {
                        val playStoreWebIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=${app.packageName}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(playStoreWebIntent)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Failed to launch or open '${app.name}': ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun scanAndImportInstalledApps(context: Context) {
        viewModelScope.launch {
            try {
                val pm = context.packageManager ?: return@launch
                
                // Query standard launcher apps
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val list = try {
                    pm.queryIntentActivities(mainIntent, 0) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                
                // Query Android TV Leanback launcher apps
                val leanbackIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                }
                val tvList = try {
                    pm.queryIntentActivities(leanbackIntent, 0) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                
                val allResolved = (list + tvList)
                    .filter { it?.activityInfo != null }
                    .distinctBy { it.activityInfo.packageName }
                
                // Fetch existing packages currently in database
                val currentApps = allApps.first()
                val existingPkgs = currentApps.map { it.packageName }.toSet()
                
                val appsToInsert = mutableListOf<TvApp>()
                
                for (resolveInfo in allResolved) {
                    val pkgName = resolveInfo.activityInfo?.packageName ?: continue
                    // Skip ourselves
                    if (pkgName == context.packageName) continue
                    
                    if (!existingPkgs.contains(pkgName)) {
                        val label = resolveInfo.loadLabel(pm).toString()
                        
                        // Categorize app based on package/label keywords
                        val cat = when {
                            pkgName.contains("video", ignoreCase = true) || pkgName.contains("movie", ignoreCase = true) || pkgName.contains("tv", ignoreCase = true) || pkgName.contains("netflix", ignoreCase = true) || pkgName.contains("disney", ignoreCase = true) || pkgName.contains("youtube", ignoreCase = true) || pkgName.contains("hulu", ignoreCase = true) || pkgName.contains("hbo", ignoreCase = true) || label.contains("TV", ignoreCase = true) || label.contains("Stream", ignoreCase = true) || label.contains("Video", ignoreCase = true) -> "Video"
                            
                            pkgName.contains("music", ignoreCase = true) || pkgName.contains("audio", ignoreCase = true) || pkgName.contains("sound", ignoreCase = true) || pkgName.contains("spotify", ignoreCase = true) || pkgName.contains("radio", ignoreCase = true) || label.contains("Music", ignoreCase = true) || label.contains("Radio", ignoreCase = true) || label.contains("Audio", ignoreCase = true) -> "Audio"
                            
                            pkgName.contains("game", ignoreCase = true) || pkgName.contains("arcade", ignoreCase = true) || pkgName.contains("retroarc", ignoreCase = true) || pkgName.contains("play", ignoreCase = true) || label.contains("Game", ignoreCase = true) || label.contains("Play", ignoreCase = true) -> "Games"
                            
                            else -> "Utility"
                        }
                        
                        appsToInsert.add(
                            TvApp(
                                name = label,
                                packageName = pkgName,
                                category = cat,
                                description = "Installed system application shortcut.",
                                isCustom = true
                            )
                        )
                    }
                }
                
                if (appsToInsert.isNotEmpty()) {
                    repository.insertAll(appsToInsert)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFavorite(app: TvApp) {
        viewModelScope.launch {
            repository.toggleFavorite(app.id, !app.isFavorite)
        }
    }

    fun addCustomApp(name: String, packageName: String, category: String, description: String) {
        viewModelScope.launch {
            val finalPkg = if (packageName.trim().isEmpty()) "com.custom.${name.lowercase().replace(" ", "")}" else packageName.trim()
            val finalDesc = if (description.trim().isEmpty()) "A custom configured textual service bookmark." else description.trim()
            val newApp = TvApp(
                name = name.trim(),
                packageName = finalPkg,
                category = category,
                description = finalDesc,
                isCustom = true
            )
            repository.insert(newApp)
        }
    }

    fun deleteApp(app: TvApp) {
        viewModelScope.launch {
            repository.deleteById(app.id)
            if (selectedAppDetail.value?.id == app.id) {
                selectedAppDetail.value = null
            }
        }
    }

    fun nextApp(maxSize: Int) {
        if (maxSize <= 1) return
        val current = selectedAppIndex.value
        selectedAppIndex.value = (current + 1) % maxSize
    }

    fun prevApp(maxSize: Int) {
        if (maxSize <= 1) return
        val current = selectedAppIndex.value
        selectedAppIndex.value = if (current - 1 < 0) maxSize - 1 else current - 1
    }
}

class TvAppViewModelFactory(private val repository: TvAppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TvAppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TvAppViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
