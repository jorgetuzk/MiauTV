package com.telegramtv.data.model

import com.google.gson.annotations.SerializedName

/**
 * Folder data model.
 */
data class Folder(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("parent_id") val parentId: Int?,
    @SerializedName("name") val name: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    // Optional: file count and preview files for home screen
    @SerializedName("file_count") val fileCount: Int? = null,
    @SerializedName("preview_files") val previewFiles: List<FileItem>? = null,
    // Display metadata (Mídia titles) — a movie/show's own folder carries
    // these the same way a file does (see web app's FolderResponse).
    @SerializedName("title") val title: String? = null,
    @SerializedName("genres") val genres: List<String> = emptyList(),
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null
) {
    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: name

    val primaryGenre: String?
        get() = genres.firstOrNull()
}

/**
 * A title (movie/show folder) card as returned by the Destaques/Recentes/
 * search endpoints — same shape as the web app's CategoryOverviewFolder.
 */
data class MediaFolderCardItem(
    @SerializedName("folder") val folder: Folder,
    @SerializedName("item_count") val itemCount: Int,
    @SerializedName("cover_url") val coverUrl: String?
)

/**
 * Folder detail response including files.
 */
data class FolderDetail(
    @SerializedName("folder") val folder: Folder,
    @SerializedName("subfolders") val subfolders: List<Folder>,
    @SerializedName("files") val files: List<FileItem>,
    @SerializedName("parent_path") val parentPath: List<Folder>?
)
/**
 * Folder with children for recursive tree views.
 */
data class FolderWithChildren(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("parent_id") val parentId: Int?,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("file_count") val fileCount: Int,
    @SerializedName("children") val children: List<FolderWithChildren>
)
