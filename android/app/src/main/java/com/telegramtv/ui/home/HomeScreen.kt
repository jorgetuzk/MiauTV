package com.telegramtv.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.telegramtv.data.model.GenreCount
import com.telegramtv.data.model.MediaFolderCardItem
import com.telegramtv.ui.components.*
import com.telegramtv.ui.theme.*

/**
 * Modern home screen with enhanced aesthetics for TV.
 */
@Composable
fun HomeScreen(
    onFileClick: (Int) -> Unit,
    onFolderClick: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TVBackground,
                        TVSurface,
                        TVBackground
                    )
                )
            )
    ) {
        when {
            uiState.isLoading -> {
                ModernLoadingState()
            }

            uiState.error != null -> {
                ModernErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.refresh() },
                    onSettings = onSettingsClick
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Modern top bar
                    ModernTopBar(
                        onSearchClick = onSearchClick,
                        onSettingsClick = onSettingsClick
                    )

                    // Content rows
                    TvLazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Continue Watching section
                        if (uiState.continueWatching.isNotEmpty()) {
                            item {
                                ContentSection(title = "Voltar a Ver") {
                                    ContentRow(
                                        title = "",
                                        files = uiState.continueWatching,
                                        serverUrl = uiState.serverUrl,
                                        onFileClick = onFileClick
                                    )
                                }
                            }
                        }

                        // "Menu 1" — Mídia subtype categories (Séries/
                        // Filmes/Animes/Hot/...), same logic as the web
                        // app's Mídia page category pill bar.
                        if (uiState.subtypeFolders.isNotEmpty()) {
                            item {
                                CategoryPillRow(
                                    subtypeFolders = uiState.subtypeFolders,
                                    selectedId = uiState.selectedSubtypeId,
                                    onSelect = { viewModel.selectSubtype(it) }
                                )
                            }
                        }

                        // "Menu 2" — genres, only once a subtype is
                        // selected (mirrors the web app's second-level menu
                        // that opens after picking Séries/Filmes/Animes).
                        if (uiState.selectedSubtypeId != null && uiState.genreOptions.isNotEmpty()) {
                            item {
                                GenrePillRow(
                                    genres = uiState.genreOptions,
                                    selectedGenre = uiState.selectedGenre,
                                    onGenreSelect = { viewModel.selectGenre(it) }
                                )
                            }
                        }

                        // Destaques section — Mídia title folders, same
                        // pooling logic as the web app's Mídia landing page.
                        if (uiState.featuredFolders.isNotEmpty()) {
                            item {
                                ContentSection(title = "Destaques") {
                                    FolderPosterRow(
                                        title = "",
                                        items = uiState.featuredFolders,
                                        serverUrl = uiState.serverUrl,
                                        onFolderClick = onFolderClick
                                    )
                                }
                            }
                        }

                        // Recentes section — Mídia title folders (not files).
                        if (uiState.recentFolders.isNotEmpty()) {
                            item {
                                ContentSection(title = "Recentes") {
                                    FolderPosterRow(
                                        title = "",
                                        items = uiState.recentFolders,
                                        serverUrl = uiState.serverUrl,
                                        onFolderClick = onFolderClick
                                    )
                                }
                            }
                        }

                        // Empty state
                        if (uiState.continueWatching.isEmpty() &&
                            uiState.featuredFolders.isEmpty() &&
                            uiState.recentFolders.isEmpty()) {
                            item {
                                ModernEmptyState()
                            }
                        }
                    }
                }
            }
        }
    }

    // Request focus on first load
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            kotlinx.coroutines.delay(100)
            try {
                focusRequester.requestFocus()
            } catch (e: IllegalStateException) {
                // Ignore
            }
        }
    }
}

/**
 * Modern top bar with gradient and refined styling.
 */
@Composable
private fun ModernTopBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo
                Surface(
                    color = TVPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = com.telegramtv.R.drawable.app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column {
                    Text(
                        text = "MiauTV",
                        style = MaterialTheme.typography.titleLarge,
                        color = TVTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Transmita seus arquivos",
                        style = MaterialTheme.typography.bodySmall,
                        color = TVTextSecondary.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModernActionButton(
                    icon = Icons.Outlined.Search,
                    label = "Buscar",
                    onClick = onSearchClick
                )

                ModernActionButton(
                    icon = Icons.Outlined.Settings,
                    label = "Configurações",
                    onClick = onSettingsClick
                )
            }
        }
    }
}

/**
 * Modern action button for top bar.
 */
@Composable
private fun ModernActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        color = if (isFocused) TVPrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) TVPrimary else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isFocused) TVPrimary else TVTextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFocused) TVTextPrimary else TVTextSecondary,
                fontWeight = if (isFocused) FontWeight.Medium else FontWeight.Normal,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Content section with header — just the title, tightly spaced above the
 * cards below it.
 */
@Composable
private fun ContentSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TVTextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = 0.sp,
            modifier = Modifier.padding(start = 40.dp, end = 40.dp, bottom = 26.dp)
        )

        content()
    }
}

/**
 * "Menu 1" — the Mídia subtype category pill bar (Séries/Filmes/Animes/
 * Hot/...), same navigation logic as the web app's Mídia page category
 * bar: "Visão geral" pools every subtype, picking one scopes Destaques/
 * Recentes to just that subtype. No header label — the pills are the row.
 */
@Composable
private fun CategoryPillRow(
    subtypeFolders: List<MediaFolderCardItem>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit
) {
    TvLazyRow(
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FilterPill(
                label = "Visão geral",
                selected = selectedId == null,
                onClick = { onSelect(null) }
            )
        }
        items(subtypeFolders, key = { it.folder.id }) { entry ->
            FilterPill(
                label = "${entry.folder.displayTitle} (${entry.itemCount})",
                selected = selectedId == entry.folder.id,
                onClick = { onSelect(entry.folder.id) }
            )
        }
    }
}

/**
 * "Menu 2" — genre pills, shown once a Mídia subtype is selected in
 * [CategoryPillRow]. Sort/format are left out for now (kept on the backend
 * for later — see /tv/browse's sort param — just not exposed here yet).
 */
@Composable
private fun GenrePillRow(
    genres: List<GenreCount>,
    selectedGenre: String?,
    onGenreSelect: (String) -> Unit
) {
    if (genres.isEmpty()) return
    TvLazyRow(
        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(genres, key = { it.genre }) { g ->
            FilterPill(
                label = "${g.genre} (${g.count})",
                selected = selectedGenre == g.genre,
                onClick = { onGenreSelect(g.genre) }
            )
        }
    }
}

/**
 * Shared focusable pill used by both [CategoryPillRow] and [GenrePillRow].
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.colors(
            containerColor = when {
                isFocused -> TVPrimary
                selected -> TVPrimary.copy(alpha = 0.22f)
                else -> TVCardBackground
            }
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(20.dp))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = when {
                isFocused -> Color.Black
                selected -> TVPrimaryLight
                else -> TVTextSecondary
            },
            fontWeight = if (selected || isFocused) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

/**
 * Modern loading state with animation.
 */
@Composable
private fun ModernLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 80.dp)
    ) {
        // Fake top bar shimmer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBlock(width = 200.dp, height = 40.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ShimmerBlock(width = 44.dp, height = 44.dp, shape = CircleShape)
                ShimmerBlock(width = 44.dp, height = 44.dp, shape = CircleShape)
            }
        }

        // Section title shimmer
        ShimmerBlock(width = 180.dp, height = 20.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // Row of shimmer cards
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(5) {
                ShimmerCard()
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Second section
        ShimmerBlock(width = 150.dp, height = 20.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(5) {
                ShimmerCard()
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Loading message
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Text(
                text = "Carregando sua biblioteca...",
                style = MaterialTheme.typography.bodyMedium,
                color = TVTextSecondary.copy(alpha = alpha * 0.7f)
            )
        }
    }
}

/**
 * Shimmer block helper for various shapes.
 */
@Composable
private fun ShimmerBlock(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val shimmerColors = listOf(TVCardBackground, TVCardFocused, TVCardBackground)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "offset"
    )
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = shimmerColors,
                    startX = translateAnim - 200f,
                    endX = translateAnim + 200f
                )
            )
    )
}

/**
 * Modern error state.
 */
@Composable
private fun ModernErrorState(
    message: String,
    onRetry: () -> Unit,
    onSettings: () -> Unit
) {
    var isRetryFocused by remember { mutableStateOf(false) }
    var isSettingsFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            Surface(
                color = TVError.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = TVError,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Erro de Conexão",
                style = MaterialTheme.typography.titleLarge,
                color = TVTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TVTextSecondary,
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Retry Button
                Surface(
                    onClick = onRetry,
                    color = if (isRetryFocused) TVPrimary else TVPrimary.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .onFocusChanged { isRetryFocused = it.isFocused }
                        .border(
                            width = if (isRetryFocused) 2.dp else 0.dp,
                            color = if (isRetryFocused) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tentar Novamente",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }

                // Settings Button
                Surface(
                    onClick = onSettings,
                    color = if (isSettingsFocused) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .onFocusChanged { isSettingsFocused = it.isFocused }
                        .border(
                            width = 1.dp,
                            color = if (isSettingsFocused) TVPrimary else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = TVTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Configurações",
                            color = TVTextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern empty state.
 */
@Composable
private fun ModernEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = TVPrimary.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = null,
                        tint = TVPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Sua Biblioteca Está Vazia",
                style = MaterialTheme.typography.titleLarge,
                color = TVTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Envie arquivos pelo bot do Telegram para começar",
                style = MaterialTheme.typography.bodyMedium,
                color = TVTextSecondary.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
    }
}
