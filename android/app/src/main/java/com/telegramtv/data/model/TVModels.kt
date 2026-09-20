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
    // Mídia titles (movie/show folders) — same source as the web app's
    // Mídia landing page Destaques/Recentes.
    @SerializedName("featured_folders") val featuredFolders: List<MediaFolderCardItem> = emptyList(),
    @SerializedName("recent_folders") val recentFolders: List<MediaFolderCardItem> = emptyList(),
    // Legacy flat top-level folder list — kept for backward compat, no
    // longer rendered on the TV home screen.
    @SerializedName("folders") val folders: List<Folder>
)

/**
 * Search result response — scoped to Mídia (see tv_search on the backend).
 */
data class SearchResponse(
    @SerializedName("files") val files: List<FileItem> = emptyList(),
    @SerializedName("folders") val folders: List<MediaFolderCardItem> = emptyList()
)
