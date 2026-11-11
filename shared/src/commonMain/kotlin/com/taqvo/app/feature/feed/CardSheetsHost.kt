package com.taqvo.app.feature.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.analytics.Analytics
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.content.FeedCard
import com.taqvo.app.feed.FeedCollection
import com.taqvo.app.feed.FeedEvents
import com.taqvo.app.feed.FeedStore
import com.taqvo.app.feed.FeedTextStyle
import com.taqvo.app.feed.ShareAction
import com.taqvo.app.feed.copyText
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.app_name
import com.taqvo.app.resources.feed_added_to
import com.taqvo.app.resources.feed_copied
import com.taqvo.app.resources.feed_save_failed
import com.taqvo.app.resources.feed_saved
import com.taqvo.app.share.SaveOutcome
import com.taqvo.app.share.saveImageToGallery
import com.taqvo.app.share.shareImage
import com.taqvo.app.feed.FeedTheme
import org.jetbrains.compose.resources.imageResource
import com.taqvo.app.wallpaper.renderWallpaper
import kotlin.random.Random
import kotlin.time.Clock

/** Which of a card's sheets is open: share (with the card), the text style, or the collection picker. */
@Stable
class CardSheets {
    var share by mutableStateOf<FeedCard?>(null)
    var style by mutableStateOf(false)
    var collectionFor by mutableStateOf<FeedCard?>(null)
    var note by mutableStateOf<FeedCard?>(null)
    var message by mutableStateOf<String?>(null)
}

@Composable
fun rememberCardSheets(): CardSheets = remember { CardSheets() }

/** Everything a card's sheets read and write; the same for the feed and for a single card. */
class CardSheetDeps(
    val store: FeedStore,
    val analytics: Analytics,
    val format: AyahFormat,
    val style: FeedTextStyle,
    val theme: FeedTheme,
    val watermark: Boolean,
    val onFormat: (AyahFormat) -> Unit,
    val onStyle: (FeedTextStyle) -> Unit,
    val onWatermark: (Boolean) -> Unit,
    /** A save or a share counts the card as seen (feed spec §9). */
    val onShared: (FeedCard) -> Unit,
)

/**
 * The share sheet, the text style editor and the collection picker, for whichever card [sheets]
 * names (feed spec §7, §9). The picture saved and the picture sent are both drawn here by
 * [drawFeedCard], the same drawing as the preview.
 */
@Composable
fun CardSheetsHost(sheets: CardSheets, styleCard: FeedCard?, deps: CardSheetDeps) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val fonts = rememberFeedFonts(deps.style)
    val measurer = rememberTextMeasurer()
    val appName = stringResource(Res.string.app_name)
    val saved = stringResource(Res.string.feed_saved)
    val saveFailed = stringResource(Res.string.feed_save_failed)
    val copied = stringResource(Res.string.feed_copied)
    var collections by remember { mutableStateOf<List<FeedCollection>>(emptyList()) }
    val addedTo = stringResource(Res.string.feed_added_to)

    val photo = imageResource(deps.theme.image)

    fun render(card: FeedCard) = renderWallpaper(SHARE_WIDTH_PX, SHARE_HEIGHT_PX) {
        drawFeedCard(card, deps.format, deps.style, photo, deps.theme.scrim, fonts, measurer, deps.watermark, appName)
    }

    fun track(card: FeedCard, action: ShareAction) = deps.analytics.track(
        FeedEvents.SHARE,
        mapOf("item_id" to card.item.id, "action" to action.id, "watermark" to deps.watermark),
    )

    sheets.share?.let { card ->
        ShareSheet(
            card = card,
            format = deps.format,
            style = deps.style,
            theme = deps.theme,
            watermark = deps.watermark,
            message = sheets.message,
            onMessageShown = { sheets.message = null },
            actions = ShareActions(
                onSave = {
                    track(card, ShareAction.SAVE_IMAGE)
                    deps.onShared(card)
                    scope.launch {
                        val image = render(card)
                        when (saveImageToGallery(image)) {
                            SaveOutcome.SAVED -> sheets.message = saved
                            // An Android 9 phone has no permission-free gallery; its share sheet saves instead.
                            SaveOutcome.UNSUPPORTED -> shareImage(image, card.copyText(deps.format.showsArabic()))
                            SaveOutcome.REFUSED, SaveOutcome.FAILED -> sheets.message = saveFailed
                        }
                    }
                },
                onToggleWatermark = { deps.onWatermark(!deps.watermark) },
                onCollection = {
                    track(card, ShareAction.COLLECTION)
                    scope.launch {
                        collections = deps.store.collections()
                        sheets.collectionFor = card
                    }
                },
                onCopy = {
                    track(card, ShareAction.COPY)
                    clipboard.setText(AnnotatedString(card.copyText(deps.format.showsArabic())))
                    sheets.message = copied
                },
                onStyle = {
                    track(card, ShareAction.STYLE)
                    sheets.style = true
                },
                onShare = {
                    track(card, ShareAction.SHARE)
                    deps.onShared(card)
                    scope.launch { shareImage(render(card), card.copyText(deps.format.showsArabic())) }
                },
                onDismiss = { sheets.share = null },
            ),
        )
    }

    if (sheets.style) {
        TextStyleSheet(
            card = sheets.share ?: styleCard,
            format = deps.format,
            style = deps.style,
            theme = deps.theme,
            onFormat = deps.onFormat,
            onStyle = deps.onStyle,
            onDismiss = { sheets.style = false },
        )
    }

    sheets.note?.let { card -> NoteSheet(card, onDismiss = { sheets.note = null }) }

    sheets.collectionFor?.let { card ->
        CollectionPickerSheet(
            collections = collections,
            onPick = { collection ->
                scope.launch {
                    deps.store.addToCollection(collection.id, card.item.id, Clock.System.now().toEpochMilliseconds())
                    sheets.collectionFor = null
                    sheets.message = addedTo.replace("%1\$s", collection.name)
                }
            },
            onCreate = { name ->
                scope.launch {
                    val id = newCollectionId()
                    val now = Clock.System.now().toEpochMilliseconds()
                    deps.store.createCollection(id, name, now)
                    deps.store.addToCollection(id, card.item.id, now)
                    sheets.collectionFor = null
                    sheets.message = addedTo.replace("%1\$s", name)
                }
            },
            onDismiss = { sheets.collectionFor = null },
        )
    }
}

/** A collection's id: local only, so time and a random tail are unique enough. */
fun newCollectionId(): String = "c${Clock.System.now().toEpochMilliseconds()}-${Random.nextInt(0, Int.MAX_VALUE)}"
