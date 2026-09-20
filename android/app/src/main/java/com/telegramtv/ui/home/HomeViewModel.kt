package com.telegramtv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegramtv.data.model.FileItem
import com.telegramtv.data.model.Folder
import com.telegramtv.data.model.GenreCount
import com.telegramtv.data.model.MediaFolderCardItem
import com.telegramtv.data.model.TVBrowseResponse
import com.telegramtv.data.repository.FilesRepository
import com.telegramtv.data.repository.FoldersRepository
import com.telegramtv.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home screen UI state.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val continueWatching: List<FileItem> = emptyList(),
    // "Menu 1": the Mídia subtype folders themselves (Séries/Filmes/Animes/
    // Hot/...) and which one (if any) is currently selected.
    val subtypeFolders: List<MediaFolderCardItem> = emptyList(),
    val selectedSubtypeId: Int? = null,
    // "Menu 2": genres for the selected subtype, which one is active, and
    // the current sort — only meaningful once a subtype is selected.
    val genreOptions: List<GenreCount> = emptyList(),
    val selectedGenre: String? = null,
    val sort: String = "recent",
    val featuredFolders: List<MediaFolderCardItem> = emptyList(),
    val recentFolders: List<MediaFolderCardItem> = emptyList(),
    val recentFiles: List<FileItem> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val serverUrl: String = "",
    val error: String? = null
)

/**
 * ViewModel for the home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val filesRepository: FilesRepository,
    private val foldersRepository: FoldersRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    /**
     * Load all data for the home screen, scoped to the current subtype/
     * genre/sort selection. [showLoading] is false for filter changes
     * (subtype/genre/sort pills) so picking one doesn't flash the
     * full-screen shimmer over content that's already on screen.
     */
    fun loadHomeData(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            val serverUrl = settingsRepository.getServerUrl()
            val state = _uiState.value

            val browseResult = filesRepository.getTVBrowse(
                typeId = state.selectedSubtypeId,
                genre = state.selectedGenre,
                sort = state.sort
            )

            browseResult.fold(
                onSuccess = { browse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        serverUrl = serverUrl,
                        continueWatching = browse.continueWatching,
                        subtypeFolders = browse.subtypeFolders,
                        featuredFolders = browse.featuredFolders,
                        recentFolders = browse.recentFolders,
                        recentFiles = browse.recentFiles,
                        folders = browse.folders
                    )
                },
                onFailure = { _ ->
                    if (showLoading) {
                        // Fallback to individual calls if TV browse fails
                        loadDataFallback()
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            )
        }
    }

    /**
     * Fallback when TV browse endpoint is not available.
     */
    private suspend fun loadDataFallback() {
        // Load continue watching
        val continueResult = filesRepository.getContinueWatching()
        val continueWatching = continueResult.getOrDefault(emptyList())

        // Load recent files
        val recentResult = filesRepository.getRecentFiles(20)
        val recentFiles = recentResult.getOrDefault(emptyList())

        // Load folders
        val foldersResult = foldersRepository.getFolders()
        val folders = foldersResult.getOrDefault(emptyList())

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            continueWatching = continueWatching,
            recentFiles = recentFiles,
            folders = folders,
            error = if (recentFiles.isEmpty() && folders.isEmpty()) {
                "Falha ao carregar conteúdo"
            } else null
        )
    }

    /**
     * "Menu 1" — select (or clear, if it's already selected) a Mídia
     * subtype folder (Séries/Filmes/Animes/Hot/...). Clears the genre
     * selection, since it's scoped to the previous subtype, and (re)loads
     * that subtype's genre pills ("Menu 2").
     */
    fun selectSubtype(id: Int?) {
        val newId = if (_uiState.value.selectedSubtypeId == id) null else id
        _uiState.value = _uiState.value.copy(
            selectedSubtypeId = newId,
            selectedGenre = null,
            genreOptions = emptyList()
        )
        loadHomeData(showLoading = false)
        if (newId != null) {
            loadGenres(newId)
        }
    }

    private fun loadGenres(folderId: Int) {
        viewModelScope.launch {
            foldersRepository.getFolderGenres(folderId).onSuccess { genres ->
                _uiState.value = _uiState.value.copy(genreOptions = genres)
            }
        }
    }

    /**
     * "Menu 2" — select (or clear) a genre filter within the selected
     * subtype.
     */
    fun selectGenre(genre: String) {
        val newGenre = if (_uiState.value.selectedGenre == genre) null else genre
        _uiState.value = _uiState.value.copy(selectedGenre = newGenre)
        loadHomeData(showLoading = false)
    }

    /**
     * "Menu 2" — change the sort order (mais recentes/mais antigos/nome).
     */
    fun selectSort(sort: String) {
        if (_uiState.value.sort == sort) return
        _uiState.value = _uiState.value.copy(sort = sort)
        loadHomeData(showLoading = false)
    }

    /**
     * Refresh home data.
     */
    fun refresh() {
        loadHomeData()
    }
}
