package com.telegramtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.telegramtv.data.model.Folder
import com.telegramtv.ui.theme.*

/**
 * TV-optimized folder card with gradient background and animated icon.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderCard(
    folder: Folder,
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
        label = "folderCardScale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .width(180.dp)
            .height(120.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = TVAccentGlow,
                    spotColor = TVPrimary.copy(alpha = 0.2f)
                ) else Modifier
            )
            .then(
                if (isFocused) Modifier.border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(TVGradientStart, TVGradientEnd)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors = CardDefaults.colors(
            containerColor = if (isFocused) TVCardFocused else TVCardBackground
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isFocused) Brush.linearGradient(
                        listOf(
                            TVPrimary.copy(alpha = 0.08f),
                            TVSecondary.copy(alpha = 0.04f)
                        )
                    ) else Brush.linearGradient(
                        listOf(
                            TVCardBackground,
                            TVCardBackground
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isFocused) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = folder.name,
                    modifier = Modifier.size(48.dp),
                    tint = if (isFocused) TVSecondary else TVTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TVTextPrimary,
                    fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )

                folder.fileCount?.let { count ->
                    Text(
                        text = "$count files",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFocused) TVSecondary else TVTextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Horizontal folder card for list view.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FolderListItem(
    folder: Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "folderItemScale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (isFocused) Modifier.border(
                    width = 2.dp,
                    color = TVFocusRing,
                    shape = RoundedCornerShape(8.dp)
                ) else Modifier
            ),
        colors = CardDefaults.colors(
            containerColor = if (isFocused) TVCardFocused else TVCardBackground
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isFocused) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = folder.name,
                modifier = Modifier.size(32.dp),
                tint = if (isFocused) TVSecondary else TVTextSecondary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TVTextPrimary,
                    fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal
                )
                folder.fileCount?.let { count ->
                    Text(
                        text = "$count files",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFocused) TVSecondary else TVTextSecondary
                    )
                }
            }
        }
    }
}
