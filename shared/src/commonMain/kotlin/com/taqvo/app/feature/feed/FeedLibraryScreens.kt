package com.taqvo.app.feature.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.content.FeedCard
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.TaqvoBottomSheet
import com.taqvo.app.design.components.TaqvoCard
import com.taqvo.app.design.components.TaqvoRow
import com.taqvo.app.feature.settings.BackChevron
import com.taqvo.app.feature.settings.SettingsNote
import com.taqvo.app.feature.settings.SettingsScaffold
import com.taqvo.app.feed.FeedCollection
import com.taqvo.app.feed.FeedTextStyle
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.feed_collection_delete
import com.taqvo.app.resources.feed_collection_rename
import com.taqvo.app.resources.feed_collections_empty
import com.taqvo.app.resources.feed_collection_empty
import com.taqvo.app.resources.feed_collections_title
import com.taqvo.app.resources.feed_favorites_empty
import com.taqvo.app.resources.feed_favorites_title
import com.taqvo.app.feed.FeedTheme
import com.taqvo.app.design.FeedInk

/** Favorites (feed spec §5): two columns of small cards, newest first. */
@Composable
fun FavoritesScreen(cards: List<FeedCard>?, theme: FeedTheme, onOpen: (FeedCard) -> Unit, onBack: () -> Unit) {
    SettingsScaffold(stringResource(Res.string.feed_favorites_title), onBack) {
        CardGrid(cards, theme, stringResource(Res.string.feed_favorites_empty), onOpen)
    }
}

/** One collection: the same grid as favorites. */
@Composable
fun CollectionScreen(name: String, cards: List<FeedCard>?, theme: FeedTheme, onOpen: (FeedCard) -> Unit, onBack: () -> Unit) {
    SettingsScaffold(name, onBack) {
        CardGrid(cards, theme, stringResource(Res.string.feed_collection_empty), onOpen)
    }
}

/**
 * The grid, built from rows rather than a lazy grid: it sits in the scaffold's own scroll, and a
 * reader's favorites run to dozens, not thousands.
 */
@Composable
private fun CardGrid(cards: List<FeedCard>?, theme: FeedTheme, empty: String, onOpen: (FeedCard) -> Unit) {
    when {
        cards == null -> Unit
        cards.isEmpty() -> SettingsNote(empty)
        else -> Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { card -> MiniCard(card, theme, Modifier.weight(1f)) { onOpen(card) } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniCard(card: FeedCard, theme: FeedTheme, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(168.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Image(painterResource(theme.thumb), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(scrimBrush(theme.scrim + 0.15f)))
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                card.translation ?: card.item.arabic,
                style = TaqvoText.caption,
                color = FeedInk.white,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
            Text(card.reference, style = TaqvoText.caption.copy(fontWeight = FontWeight.SemiBold), color = FeedInk.amber, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The collections (feed spec §5): each opens its grid; a long press renames or deletes it. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionsScreen(
    collections: List<FeedCollection>?,
    onOpen: (FeedCollection) -> Unit,
    onEdit: (FeedCollection) -> Unit,
    onBack: () -> Unit,
) {
    val format = LocalPlatformFormat.current
    SettingsScaffold(stringResource(Res.string.feed_collections_title), onBack) {
        when {
            collections == null -> Unit
            collections.isEmpty() -> SettingsNote(stringResource(Res.string.feed_collections_empty))
            else -> TaqvoCard(Modifier.padding(horizontal = 16.dp)) {
                collections.forEachIndexed { i, c ->
                    if (i > 0) CardDivider()
                    Box(Modifier.combinedClickable(onClick = { onOpen(c) }, onLongClick = { onEdit(c) })) {
                        TaqvoRow(label = c.name, value = format.localizedDigits(c.size))
                    }
                }
            }
        }
    }
}

/** Rename or delete a collection, from a long press. */
@Composable
fun CollectionEditSheet(collection: FeedCollection, onRename: (String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    TaqvoBottomSheet(onDismissRequest = onDismiss, skipPartiallyExpanded = true) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(stringResource(Res.string.feed_collection_rename), style = TaqvoText.screenTitle)
            Spacer(Modifier.height(16.dp))
            NameField(initial = collection.name, onDone = onRename)
            Spacer(Modifier.height(20.dp))
            TaqvoCard { TaqvoRow(label = stringResource(Res.string.feed_collection_delete), onClick = onDelete) }
        }
    }
}

/**
 * One card on its own (feed spec §5), from favorites or a collection: the feed's card over the
 * theme, with the same share, like and style actions, and a back link.
 */
@Composable
fun SingleCardScreen(
    card: FeedCard,
    liked: Boolean,
    format: AyahFormat,
    style: FeedTextStyle,
    theme: FeedTheme,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onStyle: () -> Unit,
    onBack: () -> Unit,
) {
    val fonts = rememberFeedFonts(style)
    Box(Modifier.fillMaxSize().background(FeedInk.deep)) {
        FeedBackground(theme, Modifier.fillMaxSize())
        FeedCardView(
            card = card,
            format = format,
            style = style,
            fonts = fonts,
            topSpace = 72.dp,
            bottomSpace = FeedChromeSizes.BOTTOM_SPACE,
            onDoubleTap = { if (!liked) onLike() },
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        )
        Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) { BackChevron(onBack) }
        FeedActionRow(
            ink = ChromeInk,
            liked = liked,
            onShare = onShare,
            onLike = onLike,
            onStyle = onStyle,
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 16.dp),
        )
    }
}
