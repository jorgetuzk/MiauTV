package com.telegramtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.AsyncImage
import com.telegramtv.data.model.FileItem
import com.telegramtv.ui.theme.*

/**
 * TV-optimized media card ("Add Recente" row) — a vertical poster card
 * (2:3 cover, title below, genre chip) matching the web app's Mídia card
 * layout (MediaFolderCard/MediaFileCard.tsx).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaCard(
    file: FileItem,
    thumbnailUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .width(180.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = TVAccentGlow,
                    spotColor = TVPrimary.copy(alpha = 0.25f)
                ) else Modifier
            ),
        colors = CardDefaults.colors(
            containerColor = if (isFocused) TVCardFocused else TVCardBackground
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Poster (2:3, like a TMDB cover)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TVSurfaceVariant)
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = file.displayTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                FileTypeBadge(
                    fileName = file.fileName,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )

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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = file.displayTitle,
                style = MaterialTheme.typography.titleSmall,
                color = TVTextPrimary,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Divider, same visual break as the web card's title/meta split
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TVSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val genre = file.primaryGenre
            if (genre != null) {
                Text(
                    text = genre,
                    style = MaterialTheme.typography.labelSmall,
                    color = TVPrimaryLight,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    file.formattedDuration?.let { duration ->
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = TVTextSecondary
                        )
                    }
                    Text(
                        text = file.formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = TVTextSecondary
                    )
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
 * Large media card variant for featured content ("Voltar a Ver") — kept as
 * the original landscape thumbnail with overlaid title/metadata, since
 * that's the one row whose existing look the user explicitly wants to keep.
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
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .width(320.dp)
            .height(220.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = TVAccentGlow,
                    spotColor = TVPrimary.copy(alpha = 0.3f)
                ) else Modifier
            ),
        colors = CardDefaults.colors(
            containerColor = if (isFocused) TVCardFocused else TVCardBackground
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = file.fileName,
                modifier = Modifier.fillMaxSize(),
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
