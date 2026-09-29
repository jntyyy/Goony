package com.example.ui

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.VaultRepository
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

enum class SortMode {
    CARD_NEWEST,      // New (تاريخ الكرت - أحدث)
    CARD_OLDEST,      // Old (تاريخ الكرت - أقدم)
    RECENTLY_ADDED,   // أضيفت مؤخراً (تاريخ الإضافة للتطبيق - أحدث)
    OLDEST_ADDED,     // أضيفت قديماً (تاريخ الإضافة للتطبيق - أقدم)
    NEWEST,           // Backward compatibility (maps to CARD_NEWEST)
    OLDEST,           // Backward compatibility (maps to CARD_OLDEST)
    TITLE_AZ,
    TITLE_ZA
}

enum class StashSearchType {
    ACTORS,
    STUDIO
}

sealed class ScreenState {
    object Home : ScreenState()
    object Bookmarks : ScreenState()
    data class AddEditLink(val linkId: String? = null) : ScreenState()
    object Actors : ScreenState()
    data class AddEditActor(val actorId: String? = null) : ScreenState()
    data class ActorScenes(val actorId: String) : ScreenState()
    object Studios : ScreenState()
    data class AddEditStudio(val studioId: String? = null) : ScreenState()
    data class StudioScenes(val studioId: String) : ScreenState()
    object Coomers : ScreenState()
    data class AddEditCoomer(val coomerId: String? = null) : ScreenState()
    data class CoomerDetail(val coomerId: String) : ScreenState()
    object Hanime : ScreenState()
    data class AddEditHanime(val hanimeId: String? = null) : ScreenState()
    data class HanimeDetail(val hanimeId: String) : ScreenState()
    data class PhotosetViewer(val title: String, val images: List<String>, val initialIndex: Int = 0) : ScreenState()
    object StashDb : ScreenState()
    object Settings : ScreenState()
}

data class ActiveVideoPlayback(
    val title: String,
    val qualities: List<StreamQuality>,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val initialPositionMs: Long = 0L,
    val startInLandscape: Boolean = false
)

data class ActiveInlineVideoPlayback(
    val cardId: String,
    val title: String,
    val qualities: List<StreamQuality>,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val headers: Map<String, String> = emptyMap()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: VaultRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = VaultRepository(db)
        seedInitialDataIfEmpty()
    }

    private fun seedInitialDataIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val existingLinks = repository.allLinks.first()
            
            // Delete obsolete demo IDs if present
            val oldSceneIds = listOf(
                "scene_raissa_stepmom", "scene_raissa_double", "scene_raissa_vip",
                "test_scene_1", "test_scene_2", "test_scene_3", "test_scene_4", "test_scene_5",
                "test_scene_6", "test_scene_7", "test_scene_8", "test_scene_9", "test_scene_10",
                "test_scene_11", "test_scene_12", "test_scene_13", "test_scene_14", "test_scene_15",
                "test_scene_16", "test_scene_17", "test_scene_18", "test_scene_19", "test_scene_20"
            )
            oldSceneIds.forEach { oldId ->
                repository.deleteLinkById(oldId)
            }
            existingLinks.filter { it.id.startsWith("demo_") || it.id.startsWith("test_scene_") }.forEach {
                repository.deleteLinkById(it.id)
            }

            // Ensure Real-Debrid and StashDB API Keys are populated with the requested default keys if currently empty
            val currentSett = repository.settings.first() ?: SettingsEntity()
            var updatedSett = currentSett
            if (updatedSett.realDebridApiKey.isBlank()) {
                updatedSett = updatedSett.copy(realDebridApiKey = "HNR2RHUY4K6JYXNFJCB4QXAJ57TKDQKTQOPYEXZ2VANQO7TN5YJQ")
            }
            if (updatedSett.stashDbApiKey.isBlank()) {
                updatedSett = updatedSett.copy(stashDbApiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOiIwMTlmYmRlYi00MDRlLTdjYmMtOTFhNy00YTA4MjhjMTQ5ZjQiLCJzdWIiOiJBUElLZXkiLCJpYXQiOjE3ODU1OTc3Mzl9.J9ojzjsBP8sBOLZNUACF94EWwren89ql8TDcW3gT7WY")
            }
            if (updatedSett != currentSett) {
                repository.updateSettings(updatedSett)
            }

            // Always insert / update all sample test dataset scenes, actors, and studios into the database
            com.example.data.util.SampleTestDataset.sampleActors.forEach { repository.insertActor(it) }
            com.example.data.util.SampleTestDataset.sampleStudios.forEach { repository.insertStudio(it) }
            repository.insertLinks(com.example.data.util.SampleTestDataset.sampleScenes)
        }
    }

    // Navigation Stack / Current Screen & Direction
    enum class NavigationDirection {
        FORWARD, BACK
    }

    private val _navDirection = MutableStateFlow(NavigationDirection.FORWARD)
    val navDirection: StateFlow<NavigationDirection> = _navDirection.asStateFlow()

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Home)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val screenStack = mutableListOf<ScreenState>(ScreenState.Home)

    // Home Feed Scroll Position Memory
    var homeScrollIndex: Int = 0
    var homeScrollOffset: Int = 0
    var initialSettingsSection: String? = null

    fun navigateTo(screen: ScreenState) {
        if (screen == _screenState.value) return
        val existingIndex = screenStack.indexOf(screen)
        if (existingIndex >= 0 && existingIndex < screenStack.size - 1) {
            _navDirection.value = NavigationDirection.BACK
            while (screenStack.size > existingIndex + 1) {
                screenStack.removeAt(screenStack.size - 1)
            }
        } else {
            _navDirection.value = NavigationDirection.FORWARD
            screenStack.add(screen)
        }
        _screenState.value = screen
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            _navDirection.value = NavigationDirection.BACK
            screenStack.removeAt(screenStack.size - 1)
            _screenState.value = screenStack.last()
            return true
        }
        return false
    }

    // Active Video Player Overlay State (Full Screen / Landscape)
    private val _activeVideo = MutableStateFlow<ActiveVideoPlayback?>(null)
    val activeVideo: StateFlow<ActiveVideoPlayback?> = _activeVideo.asStateFlow()

    // Active Inline Video Card State (Embedded 16:9 Cover Player)
    private val _activeInlineVideo = MutableStateFlow<ActiveInlineVideoPlayback?>(null)
    val activeInlineVideo: StateFlow<ActiveInlineVideoPlayback?> = _activeInlineVideo.asStateFlow()

    // Shared ExoPlayer Manager for seamless transition without stopping video
    val sharedPlayerManager by lazy { SharedPlayerManager(application) }
    private var lastInlineCardId: String? = null

    // Video Resolution Loading & Error States
    private val _resolvingCardId = MutableStateFlow<String?>(null)
    val resolvingCardId: StateFlow<String?> = _resolvingCardId.asStateFlow()

    private val _resolvingVideoStatus = MutableStateFlow<String?>(null)
    val resolvingVideoStatus: StateFlow<String?> = _resolvingVideoStatus.asStateFlow()

    private val _videoResolutionError = MutableStateFlow<String?>(null)
    val videoResolutionError: StateFlow<String?> = _videoResolutionError.asStateFlow()

    private var resolveVideoJob: kotlinx.coroutines.Job? = null

    fun playVideo(rawUrl: String, title: String = "Media Stream", cardId: String? = null) {
        resolveVideoJob?.cancel()
        _videoResolutionError.value = null
        _resolvingCardId.value = cardId

        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            _videoResolutionError.value = "Cannot play empty stream URL."
            _resolvingCardId.value = null
            return
        }

        resolveVideoJob = viewModelScope.launch {
            _resolvingVideoStatus.value = if (trimmed.startsWith("magnet:", ignoreCase = true) || trimmed.matches(Regex("^[a-fA-F0-9]{40}$"))) {
                "Resolving stream..."
            } else {
                "Resolving stream..."
            }

            try {
                val settings = repository.getSettingsOnce()
                val resolved = VideoResolvers.resolve(
                    rawUrl = trimmed,
                    torboxApiKey = settings.torboxApiKey,
                    realDebridApiKey = settings.realDebridApiKey
                )

                if (resolved.qualities.isEmpty()) {
                    _videoResolutionError.value = "No playable media qualities found for this source."
                    _resolvingVideoStatus.value = null
                    return@launch
                }

                val primaryQuality = resolved.qualities.firstOrNull { it.isDefault } ?: resolved.qualities.first()
                _resolvingVideoStatus.value = "Verifying media stream..."

                val validation = MediaUrlValidator.validate(
                    primaryQuality.url,
                    resolved.headers + primaryQuality.headers
                )

                val displayTitle = if (resolved.title.isNotBlank() && resolved.title != "Media Stream") resolved.title else title

                when (validation) {
                    is ValidatedMediaResult.Valid -> {
                        if (cardId != null) {
                            // Play directly inside the 16:9 card cover
                            _activeInlineVideo.value = ActiveInlineVideoPlayback(
                                cardId = cardId,
                                title = displayTitle,
                                qualities = resolved.qualities,
                                subtitles = resolved.subtitles,
                                headers = resolved.headers
                            )
                        } else {
                            _activeVideo.value = ActiveVideoPlayback(
                                title = displayTitle,
                                qualities = resolved.qualities,
                                subtitles = resolved.subtitles,
                                headers = resolved.headers
                            )
                        }
                    }
                    is ValidatedMediaResult.Invalid -> {
                        // If it's a valid HTTP/HTTPS URL, don't abort playback on network probe failure
                        if (primaryQuality.url.startsWith("http://", ignoreCase = true) ||
                            primaryQuality.url.startsWith("https://", ignoreCase = true)
                        ) {
                            if (cardId != null) {
                                _activeInlineVideo.value = ActiveInlineVideoPlayback(
                                    cardId = cardId,
                                    title = displayTitle,
                                    qualities = resolved.qualities,
                                    subtitles = resolved.subtitles,
                                    headers = resolved.headers
                                )
                            } else {
                                _activeVideo.value = ActiveVideoPlayback(
                                    title = displayTitle,
                                    qualities = resolved.qualities,
                                    subtitles = resolved.subtitles,
                                    headers = resolved.headers
                                )
                            }
                        } else {
                            _videoResolutionError.value = validation.reason
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _videoResolutionError.value = e.message ?: "Failed to resolve media stream"
            } finally {
                _resolvingVideoStatus.value = null
                _resolvingCardId.value = null
            }
        }
    }

    fun dismissVideoError() {
        _videoResolutionError.value = null
    }

    fun closeVideo() {
        val currentVideo = _activeVideo.value
        val inlineId = lastInlineCardId
        _activeVideo.value = null

        if (inlineId != null && currentVideo != null) {
            // Smoothly return to Inline Video Player mode without stopping playback
            _activeInlineVideo.value = ActiveInlineVideoPlayback(
                cardId = inlineId,
                title = currentVideo.title,
                qualities = currentVideo.qualities,
                subtitles = currentVideo.subtitles,
                headers = currentVideo.headers
            )
            lastInlineCardId = null
        } else {
            sharedPlayerManager.stopPlayer()
        }
    }

    fun closeInlineVideo(cardId: String? = null) {
        if (cardId == null || _activeInlineVideo.value?.cardId == cardId) {
            _activeInlineVideo.value = null
            lastInlineCardId = null
            sharedPlayerManager.stopPlayer()
        }
    }

    fun openFullscreenFromInline(cardId: String, currentPositionMs: Long = 0L) {
        val inline = _activeInlineVideo.value ?: return
        if (inline.cardId == cardId) {
            lastInlineCardId = cardId
            _activeInlineVideo.value = null
            _activeVideo.value = ActiveVideoPlayback(
                title = inline.title,
                qualities = inline.qualities,
                subtitles = inline.subtitles,
                headers = inline.headers,
                initialPositionMs = currentPositionMs,
                startInLandscape = true
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        sharedPlayerManager.release()
    }

    // Active Photoset Lightbox State
    private val _activeLightbox = MutableStateFlow<Pair<List<String>, Int>?>(null)
    val activeLightbox: StateFlow<Pair<List<String>, Int>?> = _activeLightbox.asStateFlow()

    fun openLightbox(images: List<String>, startIndex: Int = 0) {
        _activeLightbox.value = images to startIndex
    }

    fun closeLightbox() {
        _activeLightbox.value = null
    }

    // ==========================================
    // STASHDB PERSISTENT STATE & OPERATIONS
    // ==========================================
    private val _stashSearchQuery = MutableStateFlow("")
    val stashSearchQuery: StateFlow<String> = _stashSearchQuery.asStateFlow()

    private val _stashActiveType = MutableStateFlow(StashSearchType.ACTORS)
    val stashActiveType: StateFlow<StashSearchType> = _stashActiveType.asStateFlow()

    private val _stashPerformerResults = MutableStateFlow<List<StashPerformer>>(emptyList())
    val stashPerformerResults: StateFlow<List<StashPerformer>> = _stashPerformerResults.asStateFlow()

    private val _stashStudioResults = MutableStateFlow<List<StashStudio>>(emptyList())
    val stashStudioResults: StateFlow<List<StashStudio>> = _stashStudioResults.asStateFlow()

    private val _stashScenesList = MutableStateFlow<List<StashScene>>(emptyList())
    val stashScenesList: StateFlow<List<StashScene>> = _stashScenesList.asStateFlow()

    private val _stashSelectedPerformer = MutableStateFlow<StashPerformer?>(null)
    val stashSelectedPerformer: StateFlow<StashPerformer?> = _stashSelectedPerformer.asStateFlow()

    private val _stashSelectedStudio = MutableStateFlow<StashStudio?>(null)
    val stashSelectedStudio: StateFlow<StashStudio?> = _stashSelectedStudio.asStateFlow()

    private val _stashSelectedSceneIds = MutableStateFlow<Set<String>>(emptySet())
    val stashSelectedSceneIds: StateFlow<Set<String>> = _stashSelectedSceneIds.asStateFlow()

    private val _stashTotalScenesCount = MutableStateFlow(0)
    val stashTotalScenesCount: StateFlow<Int> = _stashTotalScenesCount.asStateFlow()

    private val _stashCurrentPage = MutableStateFlow(1)
    val stashCurrentPage: StateFlow<Int> = _stashCurrentPage.asStateFlow()

    private val _stashCanLoadMore = MutableStateFlow(false)
    val stashCanLoadMore: StateFlow<Boolean> = _stashCanLoadMore.asStateFlow()

    private val _isStashLoadingEntities = MutableStateFlow(false)
    val isStashLoadingEntities: StateFlow<Boolean> = _isStashLoadingEntities.asStateFlow()

    private val _isStashLoadingScenes = MutableStateFlow(false)
    val isStashLoadingScenes: StateFlow<Boolean> = _isStashLoadingScenes.asStateFlow()

    private val _isStashLoadingMore = MutableStateFlow(false)
    val isStashLoadingMore: StateFlow<Boolean> = _isStashLoadingMore.asStateFlow()

    private val _stashSearchError = MutableStateFlow<String?>(null)
    val stashSearchError: StateFlow<String?> = _stashSearchError.asStateFlow()

    private val _isStashSearchExpanded = MutableStateFlow(false)
    val isStashSearchExpanded: StateFlow<Boolean> = _isStashSearchExpanded.asStateFlow()

    private var stashSearchJob: kotlinx.coroutines.Job? = null
    private var stashScenesJob: kotlinx.coroutines.Job? = null

    fun setStashSearchQuery(query: String) {
        _stashSearchQuery.value = query
    }

    fun setStashSearchExpanded(expanded: Boolean) {
        _isStashSearchExpanded.value = expanded
    }

    fun setStashActiveType(type: StashSearchType, apiKey: String) {
        if (_stashActiveType.value == type) return
        _stashActiveType.value = type
        _stashSelectedPerformer.value = null
        _stashSelectedStudio.value = null
        _stashScenesList.value = emptyList()
        _stashSearchError.value = null
        _stashCurrentPage.value = 1
        _stashCanLoadMore.value = false
        if (_stashSearchQuery.value.trim().isNotBlank()) {
            performStashSearch(apiKey, _stashSearchQuery.value.trim())
        }
    }

    fun toggleStashSceneSelection(sceneId: String) {
        val current = _stashSelectedSceneIds.value
        _stashSelectedSceneIds.value = if (current.contains(sceneId)) current - sceneId else current + sceneId
    }

    fun clearStashSelection() {
        _stashSelectedSceneIds.value = emptySet()
    }

    fun resetStashState() {
        stashSearchJob?.cancel()
        stashScenesJob?.cancel()
        _stashSearchQuery.value = ""
        _isStashSearchExpanded.value = false
        _stashPerformerResults.value = emptyList()
        _stashStudioResults.value = emptyList()
        _stashScenesList.value = emptyList()
        _stashSelectedPerformer.value = null
        _stashSelectedStudio.value = null
        _stashSelectedSceneIds.value = emptySet()
        _stashSearchError.value = null
        _stashCurrentPage.value = 1
        _stashCanLoadMore.value = false
        _isStashLoadingEntities.value = false
        _isStashLoadingScenes.value = false
        _isStashLoadingMore.value = false
    }

    fun performStashSearch(apiKey: String, query: String? = null) {
        val q = (query ?: _stashSearchQuery.value).trim()
        if (q.isBlank()) return

        stashSearchJob?.cancel()
        stashSearchJob = viewModelScope.launch(Dispatchers.IO) {
            _isStashLoadingEntities.value = true
            _stashSearchError.value = null
            _stashSelectedPerformer.value = null
            _stashSelectedStudio.value = null
            _stashScenesList.value = emptyList()
            _stashCurrentPage.value = 1
            _stashCanLoadMore.value = false

            if (_stashActiveType.value == StashSearchType.ACTORS) {
                val res = StashDbApiService.searchPerformers(q, apiKey)
                res.onSuccess { rawPerformers ->
                    val sorted = rawPerformers.sortedWith(
                        compareByDescending<StashPerformer> { performer ->
                            val name = performer.name.trim().lowercase()
                            val aliases = performer.aliases.map { it.trim().lowercase() }
                            when {
                                name == q.lowercase() -> 100
                                aliases.contains(q.lowercase()) -> 90
                                name.startsWith(q.lowercase()) -> 80
                                aliases.any { it.startsWith(q.lowercase()) } -> 70
                                name.contains(q.lowercase()) -> 60
                                aliases.any { it.contains(q.lowercase()) } -> 50
                                else -> 10
                            }
                        }.thenByDescending {
                            if (!it.imageUrl.isNullOrBlank()) 1 else 0
                        }.thenBy {
                            it.name.lowercase()
                        }
                    )
                    _stashPerformerResults.value = sorted
                    _isStashLoadingEntities.value = false
                    if (sorted.isNotEmpty()) {
                        selectStashPerformer(sorted.first(), apiKey)
                    }
                }.onFailure { err ->
                    _stashSearchError.value = err.message ?: "Failed to search actors"
                    _isStashLoadingEntities.value = false
                }
            } else {
                val res = StashDbApiService.searchStudios(q, apiKey)
                res.onSuccess { rawStudios ->
                    val sorted = rawStudios.sortedWith(
                        compareByDescending<StashStudio> { studio ->
                            val name = studio.name.trim().lowercase()
                            when {
                                name == q.lowercase() -> 100
                                name.startsWith(q.lowercase()) -> 80
                                name.contains(q.lowercase()) -> 60
                                else -> 10
                            }
                        }.thenByDescending {
                            if (!it.logoUrl.isNullOrBlank()) 1 else 0
                        }.thenBy {
                            it.name.lowercase()
                        }
                    )
                    _stashStudioResults.value = sorted
                    _isStashLoadingEntities.value = false
                    if (sorted.isNotEmpty()) {
                        selectStashStudio(sorted.first(), apiKey)
                    }
                }.onFailure { err ->
                    _stashSearchError.value = err.message ?: "Failed to search studios"
                    _isStashLoadingEntities.value = false
                }
            }
        }
    }

    fun selectStashPerformer(performer: StashPerformer, apiKey: String) {
        _stashSelectedPerformer.value = performer
        _stashSelectedStudio.value = null
        stashScenesJob?.cancel()
        stashScenesJob = viewModelScope.launch(Dispatchers.IO) {
            _isStashLoadingScenes.value = true
            _stashCurrentPage.value = 1
            _stashScenesList.value = emptyList()
            _stashSearchError.value = null

            val res = StashDbApiService.queryPerformerScenes(
                performerId = performer.id,
                apiKey = apiKey,
                page = 1,
                perPage = 30
            )
            res.onSuccess { queryResult ->
                _stashTotalScenesCount.value = queryResult.count
                _stashScenesList.value = queryResult.scenes
                _stashCanLoadMore.value = queryResult.scenes.isNotEmpty() && (1 * 30 < queryResult.count)
                _isStashLoadingScenes.value = false
            }.onFailure { err ->
                _stashSearchError.value = err.message ?: "Failed to load scenes"
                _isStashLoadingScenes.value = false
            }
        }
    }

    fun selectStashStudio(studio: StashStudio, apiKey: String) {
        _stashSelectedStudio.value = studio
        _stashSelectedPerformer.value = null
        stashScenesJob?.cancel()
        stashScenesJob = viewModelScope.launch(Dispatchers.IO) {
            _isStashLoadingScenes.value = true
            _stashCurrentPage.value = 1
            _stashScenesList.value = emptyList()
            _stashSearchError.value = null

            val res = StashDbApiService.queryStudioScenes(
                studioId = studio.id,
                apiKey = apiKey,
                page = 1,
                perPage = 30,
                providedChildIds = studio.childIds
            )
            res.onSuccess { queryResult ->
                _stashTotalScenesCount.value = queryResult.count
                _stashScenesList.value = queryResult.scenes
                _stashCanLoadMore.value = queryResult.scenes.isNotEmpty() && (1 * 30 < queryResult.count)
                _isStashLoadingScenes.value = false
            }.onFailure { err ->
                _stashSearchError.value = err.message ?: "Failed to load studio scenes"
                _isStashLoadingScenes.value = false
            }
        }
    }

    fun loadMoreStashScenes(apiKey: String) {
        if (_isStashLoadingMore.value || !_stashCanLoadMore.value) return
        val nextPage = _stashCurrentPage.value + 1

        viewModelScope.launch(Dispatchers.IO) {
            _isStashLoadingMore.value = true
            val selectedPerf = _stashSelectedPerformer.value
            val selectedStud = _stashSelectedStudio.value

            if (selectedPerf != null) {
                val res = StashDbApiService.queryPerformerScenes(
                    performerId = selectedPerf.id,
                    apiKey = apiKey,
                    page = nextPage,
                    perPage = 30
                )
                res.onSuccess { queryResult ->
                    _stashCurrentPage.value = nextPage
                    val current = _stashScenesList.value
                    val newUnique = queryResult.scenes.filter { ns -> current.none { it.id == ns.id } }
                    val updated = current + newUnique
                    _stashScenesList.value = updated
                    _stashCanLoadMore.value = queryResult.scenes.isNotEmpty() && (nextPage * 30 < queryResult.count)
                }
            } else if (selectedStud != null) {
                val res = StashDbApiService.queryStudioScenes(
                    studioId = selectedStud.id,
                    apiKey = apiKey,
                    page = nextPage,
                    perPage = 30,
                    providedChildIds = selectedStud.childIds
                )
                res.onSuccess { queryResult ->
                    _stashCurrentPage.value = nextPage
                    val current = _stashScenesList.value
                    val newUnique = queryResult.scenes.filter { ns -> current.none { it.id == ns.id } }
                    val updated = current + newUnique
                    _stashScenesList.value = updated
                    _stashCanLoadMore.value = queryResult.scenes.isNotEmpty() && (nextPage * 30 < queryResult.count)
                }
            }
            _isStashLoadingMore.value = false
        }
    }

    fun saveSelectedStashScenes(onComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedIds = _stashSelectedSceneIds.value
            if (selectedIds.isEmpty()) return@launch

            val scenesToSave = _stashScenesList.value.filter { selectedIds.contains(it.id) }
            if (scenesToSave.isEmpty()) return@launch

            val allExistingActors = repository.allActors.first()
            val allExistingStudios = repository.allStudios.first()
            val allExistingLinks = repository.allLinks.first()

            val newActorsMap = mutableMapOf<String, ActorEntity>()
            val newStudiosMap = mutableMapOf<String, StudioEntity>()
            val linksToInsert = mutableListOf<LinkEntity>()

            for (scene in scenesToSave) {
                // 1. Process female performers
                val actorIds = mutableListOf<String>()
                for (perf in scene.femalePerformers) {
                    val pName = perf.name.trim()
                    if (pName.isBlank()) continue

                    val existing = allExistingActors.find {
                        (it.stashDbId != null && it.stashDbId == perf.id) ||
                        it.name.trim().equals(pName, ignoreCase = true)
                    } ?: newActorsMap.values.find {
                        (it.stashDbId != null && it.stashDbId == perf.id) ||
                        it.name.trim().equals(pName, ignoreCase = true)
                    }

                    if (existing != null) {
                        actorIds.add(existing.id)
                    } else {
                        val newActorId = UUID.randomUUID().toString()
                        val actor = ActorEntity(
                            id = newActorId,
                            stashDbId = perf.id,
                            name = perf.name,
                            imageUrl = perf.imageUrl ?: "",
                            originalImageUrl = perf.imageUrl
                        )
                        newActorsMap[perf.id] = actor
                        actorIds.add(newActorId)
                    }
                }

                // 2. Process Studio
                val studioIds = mutableListOf<String>()
                if (!scene.studioName.isNullOrBlank()) {
                    val sName = scene.studioName.trim()
                    val existingStudio = allExistingStudios.find {
                        (scene.studioId != null && it.stashDbId == scene.studioId) ||
                        it.name.trim().equals(sName, ignoreCase = true)
                    } ?: newStudiosMap.values.find {
                        (scene.studioId != null && it.stashDbId == scene.studioId) ||
                        it.name.trim().equals(sName, ignoreCase = true)
                    }

                    if (existingStudio != null) {
                        studioIds.add(existingStudio.id)
                    } else {
                        val newStudioId = UUID.randomUUID().toString()
                        val studio = StudioEntity(
                            id = newStudioId,
                            stashDbId = scene.studioId,
                            name = scene.studioName,
                            logoUrl = scene.studioLogo,
                            imageUrl = scene.studioLogo
                        )
                        newStudiosMap[scene.studioId ?: sName] = studio
                        studioIds.add(newStudioId)
                    }
                }

                // 3. Parse date
                val parsedDate = StashDbApiService.parseDateToMillis(scene.date)

                // 4. Check if Link already exists
                val existingLink = allExistingLinks.find {
                    (it.stashDbId != null && it.stashDbId == scene.id) ||
                    (it.title.trim().equals(scene.title.trim(), ignoreCase = true) && it.assignedDate == parsedDate)
                }

                val linkToSave = if (existingLink != null) {
                    existingLink.copy(
                        stashDbId = scene.id,
                        title = scene.title,
                        coverImage = if (existingLink.coverImage.isBlank()) (scene.coverUrl ?: "") else existingLink.coverImage,
                        actorIds = (existingLink.actorIds + actorIds).distinct(),
                        studioIds = (existingLink.studioIds + studioIds).distinct(),
                        assignedDate = existingLink.assignedDate ?: parsedDate
                    )
                } else {
                    LinkEntity(
                        id = UUID.randomUUID().toString(),
                        stashDbId = scene.id,
                        title = scene.title,
                        coverImage = scene.coverUrl ?: "",
                        actorIds = actorIds.distinct(),
                        studioIds = studioIds.distinct(),
                        assignedDate = parsedDate
                    )
                }

                linksToInsert.add(linkToSave)
            }

            // Batch insert everything into Room
            if (newActorsMap.isNotEmpty()) {
                repository.insertActors(newActorsMap.values.toList())
            }
            if (newStudiosMap.isNotEmpty()) {
                repository.insertStudios(newStudiosMap.values.toList())
            }
            if (linksToInsert.isNotEmpty()) {
                repository.insertLinks(linksToInsert)
            }

            _stashSelectedSceneIds.value = emptySet()
            withContext(Dispatchers.Main) {
                onComplete(linksToInsert.size)
            }
        }
    }

    // Search, Tabs, Filter and Sort
    val searchQuery = MutableStateFlow("")
    val sortMode = MutableStateFlow(SortMode.NEWEST)
    val homeTab = MutableStateFlow(0) // 0: Videos, 1: Channels/Studios, 2: Bookmarks
    val bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val lastFeedRefresh = MutableStateFlow(System.currentTimeMillis())
    val viewFilter = MutableStateFlow("ALL") // ALL, 4K, HD

    fun toggleBookmark(id: String) {
        val curr = bookmarkedIds.value
        bookmarkedIds.value = if (curr.contains(id)) curr - id else curr + id
    }

    fun refreshFeed() {
        lastFeedRefresh.value = System.currentTimeMillis()
    }

    // Data Flows
    val allLinks = repository.allLinks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allActors = repository.allActors.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allStudios = repository.allStudios.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allHanime = repository.allHanime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCoomers = repository.allCoomers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _settingsState = MutableStateFlow<SettingsEntity?>(null)
    val settings: StateFlow<SettingsEntity> = repository.settings
        .map { it ?: SettingsEntity() }
        .onEach { dbSettings ->
            if (_settingsState.value == null) {
                _settingsState.value = dbSettings
            }
        }
        .combine(_settingsState) { dbSettings, localOverride ->
            localOverride ?: dbSettings
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsEntity())

    // Filtered scenes based on search, tabs, filter and sort
    val filteredLinks: StateFlow<List<LinkEntity>> = combine(
        combine(allLinks, searchQuery, allActors, allStudios) { links, query, actors, studios ->
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                val matchingActorIds = actors.filter { it.name.lowercase().contains(q) }.map { it.id }.toSet()
                val matchingStudioIds = studios.filter { it.name.lowercase().contains(q) }.map { it.id }.toSet()
                links.filter { link ->
                    link.title.lowercase().contains(q) ||
                    link.actorIds.any { matchingActorIds.contains(it) } ||
                    link.studioIds.any { matchingStudioIds.contains(it) }
                }
            } else {
                links
            }
        },
        combine(sortMode, viewFilter) { sort, filter -> sort to filter },
        combine(homeTab, bookmarkedIds) { tab, bookmarks -> tab to bookmarks }
    ) { searchedLinks, (sort, filter), (tab, bookmarks) ->
        var list: List<LinkEntity> = searchedLinks

        if (filter == "4K") {
            list = list.filter { it.url4K != null || it.magnet4K != null }
        } else if (filter == "HD") {
            list = list.filter { it.urlHD != null || it.magnet != null }
        }

        if (tab == 2) {
            list = list.filter { bookmarks.contains(it.id) }
        }

        when (sort) {
            SortMode.CARD_NEWEST, SortMode.NEWEST -> list.sortedByDescending { it.assignedDate ?: it.createdAt }
            SortMode.CARD_OLDEST, SortMode.OLDEST -> list.sortedBy { it.assignedDate ?: it.createdAt }
            SortMode.RECENTLY_ADDED -> list.sortedByDescending { it.createdAt }
            SortMode.OLDEST_ADDED -> list.sortedBy { it.createdAt }
            SortMode.TITLE_AZ -> list.sortedBy { it.title.lowercase() }
            SortMode.TITLE_ZA -> list.sortedByDescending { it.title.lowercase() }
        }
    }.distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // CRUD operations
    fun saveLink(link: LinkEntity) {
        viewModelScope.launch {
            repository.insertLink(link)
        }
    }

    fun deleteLink(id: String) {
        viewModelScope.launch {
            repository.deleteLinkById(id)
        }
    }

    fun saveActor(actor: ActorEntity) {
        viewModelScope.launch {
            repository.insertActor(actor)
        }
    }

    fun deleteActor(id: String) {
        viewModelScope.launch {
            repository.deleteActorById(id)
        }
    }

    fun deleteActorWithCascade(actorId: String) {
        viewModelScope.launch {
            try {
                val links = repository.allLinks.first()
                links.forEach { link ->
                    if (link.actorIds.contains(actorId)) {
                        if (link.actorIds.size > 1) {
                            // Link has other actors tagged: keep link, un-tag this actor
                            val updatedActors = link.actorIds.filter { it != actorId }
                            repository.updateLink(link.copy(actorIds = updatedActors))
                        } else {
                            // Sole actor: delete link completely
                            repository.deleteLinkById(link.id)
                        }
                    }
                }
                repository.deleteActorById(actorId)
            } catch (e: Exception) {
                repository.deleteActorById(actorId)
            }
        }
    }

    fun saveStudio(studio: StudioEntity) {
        viewModelScope.launch {
            repository.insertStudio(studio)
        }
    }

    fun deleteStudio(id: String) {
        viewModelScope.launch {
            repository.deleteStudioById(id)
        }
    }

    fun deleteStudioWithCascade(studioId: String) {
        viewModelScope.launch {
            try {
                val links = repository.allLinks.first()
                links.filter { it.studioIds.contains(studioId) }.forEach { link ->
                    repository.deleteLinkById(link.id)
                }
                repository.deleteStudioById(studioId)
            } catch (e: Exception) {
                repository.deleteStudioById(studioId)
            }
        }
    }

    fun saveHanime(hanime: HanimeEntity) {
        viewModelScope.launch {
            repository.insertHanime(hanime)
        }
    }

    fun deleteHanime(id: String) {
        viewModelScope.launch {
            repository.deleteHanimeById(id)
        }
    }

    fun saveCoomer(coomer: CoomerEntity) {
        viewModelScope.launch {
            repository.insertCoomer(coomer)
        }
    }

    fun deleteCoomer(id: String) {
        viewModelScope.launch {
            repository.deleteCoomerById(id)
        }
    }

    fun updateSettings(newSettings: SettingsEntity) {
        _settingsState.value = newSettings
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }

    // Auto-match actors & studios by scene title (System Check)
    fun runSystemCheck(onComplete: (matched: Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val links = repository.allLinks.first()
            val actors = repository.allActors.first()
            val studios = repository.allStudios.first()
            var matchCount = 0

            for (link in links) {
                val foundActors = actors.filter {
                    it.name.isNotBlank() && link.title.contains(it.name, ignoreCase = true)
                }.map { it.id }

                val foundStudios = studios.filter {
                    it.name.isNotBlank() && link.title.contains(it.name, ignoreCase = true)
                }.map { it.id }

                val newActorIds = (link.actorIds + foundActors).distinct()
                val newStudioIds = (link.studioIds + foundStudios).distinct()

                if (newActorIds != link.actorIds || newStudioIds != link.studioIds) {
                    repository.updateLink(
                        link.copy(actorIds = newActorIds, studioIds = newStudioIds)
                    )
                    matchCount++
                }
            }
            onComplete(matchCount)
        }
    }

    // Export JSON string
    suspend fun exportDataJson(): String {
        val root = JSONObject()
        val linksArr = JSONArray()
        repository.allLinks.first().forEach { l ->
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("title", l.title)
            obj.put("coverImage", l.coverImage)
            obj.put("urlHD", l.urlHD ?: JSONObject.NULL)
            obj.put("url4K", l.url4K ?: JSONObject.NULL)
            obj.put("aspectRatio", l.aspectRatio)
            linksArr.put(obj)
        }
        root.put("links", linksArr)
        return root.toString(2)
    }

    suspend fun importJsonData(jsonString: String): Result<Int> {
        return try {
            val root = JSONObject(jsonString.trim())
            val linksArr = root.optJSONArray("links") ?: JSONArray()
            val importedList = mutableListOf<com.example.data.local.entity.LinkEntity>()
            for (i in 0 until linksArr.length()) {
                val obj = linksArr.getJSONObject(i)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val title = obj.optString("title", "Imported Scene")
                val coverImage = obj.optString("coverImage", "")
                val urlHD = if (obj.isNull("urlHD")) null else obj.optString("urlHD")
                val url4K = if (obj.isNull("url4K")) null else obj.optString("url4K")
                val aspectRatio = obj.optString("aspectRatio", "16:9")
                importedList.add(
                    com.example.data.local.entity.LinkEntity(
                        id = id,
                        title = title,
                        coverImage = coverImage,
                        urlHD = urlHD,
                        url4K = url4K,
                        aspectRatio = aspectRatio
                    )
                )
            }
            if (importedList.isNotEmpty()) {
                repository.insertLinks(importedList)
            }
            Result.success(importedList.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importSampleDataset(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            com.example.data.util.SampleTestDataset.sampleActors.forEach { repository.insertActor(it) }
            com.example.data.util.SampleTestDataset.sampleStudios.forEach { repository.insertStudio(it) }
            repository.insertLinks(com.example.data.util.SampleTestDataset.sampleScenes)
            onDone(com.example.data.util.SampleTestDataset.sampleScenes.size)
        }
    }

    fun clearSampleDataset(onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val sceneIds = com.example.data.util.SampleTestDataset.sampleScenes.map { it.id }
            var count = 0
            sceneIds.forEach { id ->
                repository.deleteLinkById(id)
                count++
            }
            onDone(count)
        }
    }

    private fun compressGzip(str: String): ByteArray {
        val byteOut = ByteArrayOutputStream()
        GZIPOutputStream(byteOut).use { it.write(str.toByteArray(Charsets.UTF_8)) }
        return byteOut.toByteArray()
    }
}

class SharedPlayerManager(private val context: android.content.Context) {
    private var _exoPlayer: androidx.media3.exoplayer.ExoPlayer? = null

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getPlayer(): androidx.media3.exoplayer.ExoPlayer {
        val existing = _exoPlayer
        if (existing != null) return existing

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 10_000,
                /* maxBufferMs = */ 45_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val newPlayer = androidx.media3.exoplayer.ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build().apply {
                playWhenReady = true
            }

        _exoPlayer = newPlayer
        return newPlayer
    }

    fun stopPlayer() {
        _exoPlayer?.stop()
        _exoPlayer?.clearMediaItems()
    }

    fun release() {
        _exoPlayer?.release()
        _exoPlayer = null
    }
}
