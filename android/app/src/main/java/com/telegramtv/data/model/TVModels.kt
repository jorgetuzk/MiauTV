package com.telegramtv.data.model

import com.google.gson.annotations.SerializedName

/**
 * TV-specific API responses optimized for home screen.
 */

/**
 * TV Browse response - returns all data needed for home screen in one call.
 */
data class TVBrowseResponse(
    @SerializedName("continue_watching") val continueWatching: List<FileItem>,
    @SerializedName("recent") val recentFiles: List<FileItem>,
    // The Mídia subtype folders themselves (Séries/Filmes/Animes/Hot/...) —
    // powers the "Menu 1" category pill row. Always the full set, regardless
    // of which one is currently selected.
    @SerializedName("subtype_folders") val subtypeFolders: List<MediaFolderCardItem> = emptyList(),
    // Mídia titles (movie/show folders) — same source as the web app's
    // Mídia landing page Destaques/Recentes. Scoped to the selected subtype
    // (type_id) and genre/sort when those are passed to /tv/browse.
    @SerializedName("featured_folders") val featuredFolders: List<MediaFolderCardItem> = emptyList(),
    @SerializedName("recent_folders") val recentFolders: List<MediaFolderCardItem> = emptyList(),
    // Legacy flat top-level folder list — kept for backward compat, no
    // longer rendered on the TV home screen.
    @SerializedName("folders") val folders: List<Folder>
)

/**
 * Genre + count for a folder's direct children (movies/shows) — powers the
 * "Menu 2" genre pill row, same as the web app's Mídia genre bar.
 */
data class GenreCount(
    @SerializedName("genre") val genre: String,
    @SerializedName("count") val count: Int
)

/**
 * Search result response — scoped to Mídia (see tv_search on the backend).
 */
data class SearchResponse(
    @SerializedName("files") val files: List<FileItem> = emptyList(),
    @SerializedName("folders") val folders: List<MediaFolderCardItem> = emptyList()
)
