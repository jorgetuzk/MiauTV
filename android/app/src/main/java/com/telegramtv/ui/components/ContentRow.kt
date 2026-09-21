package com.telegramtv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import com.telegramtv.data.model.FileItem
import com.telegramtv.data.model.Folder
import com.telegramtv.data.model.MediaFolderCardItem
import com.telegramtv.ui.theme.TVTextPrimary

/**
 * Horizontal content row for the home screen ("Voltar a Ver"). Always uses
 * the landscape [LargeMediaCard] design — every file card in the app looks
 * the same way now, not just this row.
 */
@Composable
fun ContentRow(
    title: String,
    files: List<FileItem>,
    serverUrl: String,
    onFileClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Row title — only rendered when the caller actually wants an
        // inline title (HomeScreen's own ContentSection header covers this
        // normally, and passes "").
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TVTextPrimary,
                modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
            )
        }

        // Horizontal scrollable items. Cards cast a shadow on focus, so
        // rows get extra breathing room below to avoid looking cramped —
        // the top only needs a little, since the section title above
        // already provides separation.
        TvLazyRow(
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 4.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            items(files, key = { it.id }) { file ->
                // Prefer the API's own resolved thumbnail_url (covers a
                // custom/TMDb cover when set, cache-busted) — falls back to
                // the plain stream endpoint for older responses without it.
                val thumbnailUrl = file.thumbnailUrl?.let { "$serverUrl$it" }
                    ?: "$serverUrl/api/stream/${file.id}/thumbnail"

                LargeMediaCard(
                    file = file,
                    thumbnailUrl = thumbnailUrl,
                    onClick = { onFileClick(file) }
                )
            }
        }
    }
}

/**
 * Horizontal row of title-folder poster cards ("Destaques"/"Recentes" on the
 * TV home screen and Mídia-scoped folder search results) — mirrors
 * [ContentRow] but for [MediaFolderCardItem] instead of [FileItem].
 */
@Composable
fun FolderPosterRow(
    title: String,
    items: List<MediaFolderCardItem>,
    serverUrl: String,
    onFolderClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TVTextPrimary,
                modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
            )
        }

        TvLazyRow(
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 4.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(items, key = { it.folder.id }) { item ->
                val thumbnailUrl = item.coverUrl?.let { "$serverUrl$it" }
                FolderPosterCard(
                    item = item,
                    thumbnailUrl = thumbnailUrl,
                    onClick = { onFolderClick(item.folder.id) }
                )
            }
        }
    }
}

/**
 * Horizontal folder row for the home screen.
 */
@Composable
fun FolderRow(
    title: String,
    folders: List<Folder>,
    onFolderClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Row title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TVTextPrimary,
            modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
        )

        // Horizontal scrollable folders
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(folders, key = { it.id }) { folder ->
                FolderCard(
                    folder = folder,
                    onClick = { onFolderClick(folder.id) }
                )
            }
        }
    }
}
