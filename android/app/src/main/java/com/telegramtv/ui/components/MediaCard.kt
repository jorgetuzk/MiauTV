package com.telegramtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.AsyncImage
import com.telegramtv.data.model.FileItem
import com.telegramtv.data.model.MediaFolderCardItem
import com.telegramtv.ui.theme.*

/**
 * TV-optimized folder poster card ("Destaques"/"Recentes" rows) — a
 * cover-only poster (2:3) with title/genre as a fade-in overlay on focus,
 * matching the web app's Mídia page which shows folders, not files, in
 * these rows. File cards use the landscape [LargeMediaCard] design instead.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderPosterCard(
    item: MediaFolderCardItem,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val imageScale by animateFloatAsState(
        targetValue = if (isFocused) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "imageScale"
    )
    val overlayAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(200),
        label = "overlayAlpha"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .aspectRatio(2f / 3f)
            .zIndex(if (isFocused) 1f else 0f)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = TVAccentGlow,
                    spotColor = TVPrimary.copy(alpha = 0.25f)
                ) else Modifier
            ),
        colors = CardDefaults.colors(containerColor = TVCardBackground),
        shape = CardDefaults.shape(shape = RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = item.folder.displayTitle,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(imageScale),
                contentScale = ContentScale.Crop
            )

            // Hover overlay: title + genre fade in over the cover, on focus.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.48f)
                    .align(Alignment.BottomCenter)
                    .alpha(overlayAlpha)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
                ) {
                    Text(
                        text = item.folder.displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = TVTextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val genre = item.folder.primaryGenre
                    if (genre != null) {
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.labelSmall,
                            color = TVPrimaryLight,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * File type badge showing 🎬 video, 🎵 audio, etc.
 */
@Composable
private fun FileTypeBadge(
    fileName: String,
    modifier: Modifier = Modifier
) {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    val (icon, color) = when (extension) {
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v" ->
            Icons.Default.Movie to TVPrimary
        "mp3", "flac", "aac", "ogg", "wav", "m4a", "wma" ->
            Icons.Default.MusicNote to TVSecondary
        "srt", "ass", "sub", "ssa", "vtt" ->
            Icons.Default.Subtitles to TVWarning
        "jpg", "jpeg", "png", "gif", "bmp", "webp" ->
            Icons.Default.Image to TVSuccess
        else -> return // no badge for unknown types
    }

    Box(
        modifier = modifier
            .size(28.dp)
            .background(
                color = Color.Black.copy(alpha = 0.65f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Landscape file card with overlaid title/metadata — used everywhere a
 * file shows as a card (Voltar a Ver, folder browsing, search results).
 * Like [FolderPosterCard], only the image scales on focus — the card's own
 * bounds, the gradient overlay, badge and progress bar stay put.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LargeMediaCard(
    file: FileItem,
    thumbnailUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val imageScale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "imageScale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .width(320.dp)
            .height(220.dp)
            .zIndex(if (isFocused) 1f else 0f)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = TVAccentGlow,
                    spotColor = TVPrimary.copy(alpha = 0.3f)
                ) else Modifier
            ),
        colors = CardDefaults.colors(containerColor = TVCardBackground),
        shape = CardDefaults.shape(shape = RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = file.fileName,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(imageScale),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // File type badge
            FileTypeBadge(
                fileName = file.fileName,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            )

            // File info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TVTextPrimary,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = file.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = TVTextSecondary
                    )
                    file.formattedDuration?.let { duration ->
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = TVTextSecondary
                        )
                    }
                    file.resolution?.let { res ->
                        Text(
                            text = res,
                            style = MaterialTheme.typography.bodySmall,
                            color = TVPrimaryLight
                        )
                    }
                }
            }

            // Progress bar
            if (file.progressPercent > 0f) {
                LinearProgressIndicator(
                    progress = { file.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = TVPrimary,
                    trackColor = TVProgressBackground
                )
            }
        }
    }
}
