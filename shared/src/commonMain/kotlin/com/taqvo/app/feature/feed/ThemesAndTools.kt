package com.taqvo.app.feature.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.FeedInk
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.drawClose
import com.taqvo.app.feed.FeedTheme
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.feed_nav_themes
import com.taqvo.app.resources.feed_nav_tools
import com.taqvo.app.resources.feed_share_close
import com.taqvo.app.resources.feed_theme_a11y
import com.taqvo.app.resources.feed_theme_edit

/** The Mavzular and Ilovalar screens' header: a round close button and the title, centred. */
@Composable
private fun ModalHeader(title: String, onClose: () -> Unit) {
    val closeLabel = stringResource(Res.string.feed_share_close)
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(FeedInk.tile)
                .clickable(onClick = onClose)
                .semantics { role = Role.Button; contentDescription = closeLabel },
            contentAlignment = Alignment.Center,
        ) { Canvas(Modifier.size(18.dp).clearAndSetSemantics {}) { drawClose(FeedInk.white) } }
        Text(
            title,
            color = FeedInk.white,
            style = TaqvoText.rowLabel.copy(fontSize = 20.sp),
            modifier = Modifier.align(Alignment.Center).semantics { heading() },
        )
    }
}

/**
 * Mavzular: every theme as a tall tile, three to a row. A tap chooses the theme; the chosen one
 * carries "Tahrirlash", which opens the text style editor over it.
 */
@Composable
fun ThemesScreen(themes: List<FeedTheme>, selectedId: String, onSelect: (FeedTheme) -> Unit, onEdit: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().background(FeedInk.deep).windowInsetsPadding(WindowInsets.safeDrawing)) {
        ModalHeader(stringResource(Res.string.feed_nav_themes), onClose)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(themes, key = { it.id }) { theme ->
                ThemeTile(theme, selected = theme.id == selectedId, number = themes.indexOf(theme) + 1, onSelect = { onSelect(theme) }, onEdit = onEdit)
            }
        }
    }
}

@Composable
private fun ThemeTile(theme: FeedTheme, selected: Boolean, number: Int, onSelect: () -> Unit, onEdit: () -> Unit) {
    val label = stringResource(Res.string.feed_theme_a11y, number)
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .aspectRatio(0.56f)
            .clip(shape)
            .then(if (selected) Modifier.border(3.dp, FeedInk.white, shape) else Modifier)
            .clickable(onClick = onSelect)
            .semantics { contentDescription = label; role = Role.RadioButton; this.selected = selected },
    ) {
        Image(painterResource(theme.thumb), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (selected) {
            Text(
                stringResource(Res.string.feed_theme_edit),
                color = FeedInk.white,
                style = TaqvoText.caption.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FeedInk.glass)
                    .clickable(onClick = onEdit)
                    .semantics { role = Role.Button }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/** One tile on Ilovalar: a large line drawing and its name under it. */
class ToolTile(val label: String, val glyph: DrawScope.(Color) -> Unit, val onClick: () -> Unit)

/** Ilovalar: every part of the app other than the feed, two to a row. */
@Composable
fun ToolsScreen(tiles: List<ToolTile>, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().background(FeedInk.deep).windowInsetsPadding(WindowInsets.safeDrawing)) {
        ModalHeader(stringResource(Res.string.feed_nav_tools), onClose)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // By position: two labels can read alike in some language, and a lazy grid's keys must not.
            items(tiles.size) { index ->
                val tile = tiles[index]
                Column(
                    Modifier
                        .aspectRatio(0.95f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(FeedInk.tile)
                        .clickable(onClick = tile.onClick)
                        .semantics(mergeDescendants = true) { role = Role.Button }
                        .padding(18.dp),
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(64.dp).clearAndSetSemantics {}) { tile.glyph(this, FeedInk.white) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(tile.label, color = FeedInk.white, style = TaqvoText.rowLabel)
                }
            }
        }
    }
}
