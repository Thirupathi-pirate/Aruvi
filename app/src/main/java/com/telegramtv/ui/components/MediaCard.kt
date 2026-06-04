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
 * TV-optimized media card with focus glow, spring animation, and type badges.
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
            .width(220.dp)
            .height(180.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = TVAccentGlow,
                    spotColor = TVPrimary.copy(alpha = 0.25f)
                ) else Modifier
            ),
        colors = CardDefaults.colors(
            containerColor = if (isFocused) TVCardFocused else TVCardBackground
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = file.fileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // File type badge (top-right)
            FileTypeBadge(
                fileName = file.fileName,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )

            // File info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TVTextPrimary,
                    fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = file.formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = TVTextSecondary
                    )
                    file.formattedDuration?.let { duration ->
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = TVTextSecondary
                        )
                    }
                }
            }

            // Watch progress bar
            if (file.progressPercent > 0f) {
                LinearProgressIndicator(
                    progress = { file.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = TVPrimary,
                    trackColor = TVProgressBackground
                )
            }

            // Focus glow border
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Transparent)
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        TVGradientStart.copy(alpha = 0.2f),
                                        TVGradientEnd.copy(alpha = 0.2f)
                                    )
                                )
                            )
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
 * Large media card variant for featured content.
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
