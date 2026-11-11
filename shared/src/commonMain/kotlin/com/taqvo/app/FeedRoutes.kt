package com.taqvo.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.content.ContentLang
import com.taqvo.app.content.FeedCard
import com.taqvo.app.content.LanguageResolver
import com.taqvo.app.i18n.AppLanguage
import com.taqvo.app.di.AppContainer
import com.taqvo.app.feature.feed.CardSheetDeps
import com.taqvo.app.feature.feed.CardSheetsHost
import com.taqvo.app.feature.feed.CollectionEditSheet
import com.taqvo.app.feature.feed.CollectionScreen
import com.taqvo.app.feature.feed.CollectionsScreen
import com.taqvo.app.feature.feed.FavoritesScreen
import com.taqvo.app.feature.feed.FeedActions
import com.taqvo.app.feature.feed.FeedScreen
import com.taqvo.app.feature.feed.GoalSheet
import com.taqvo.app.feature.feed.SingleCardScreen
import com.taqvo.app.feature.feed.StreakSheet
import com.taqvo.app.feature.feed.rememberCardSheets
import com.taqvo.app.feed.FeedCollection
import com.taqvo.app.feed.FeedEvents
import com.taqvo.app.feed.FeedTextStyle
import com.taqvo.app.feed.LikeVia
import com.taqvo.app.nav.Navigator
import com.taqvo.app.nav.Screen
import com.taqvo.app.notifications.RescheduleTrigger
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.components.drawBell
import com.taqvo.app.design.components.drawBook
import com.taqvo.app.design.components.drawBookmark
import com.taqvo.app.design.components.drawHeart
import com.taqvo.app.design.components.drawHorizon
import com.taqvo.app.design.components.drawKaaba
import com.taqvo.app.design.components.drawMihrab
import com.taqvo.app.design.components.drawMisbaha
import com.taqvo.app.design.components.drawPhone
import com.taqvo.app.design.components.drawPicture
import com.taqvo.app.design.components.drawSliders
import com.taqvo.app.feature.feed.FeedTopActions
import com.taqvo.app.feature.feed.HelpSheet
import com.taqvo.app.feature.feed.TextStyleSheet
import com.taqvo.app.feature.feed.ThemesScreen
import com.taqvo.app.feature.feed.ToolTile
import com.taqvo.app.feature.feed.ToolsScreen
import com.taqvo.app.feature.settings.BackChevron
import com.taqvo.app.feed.FeedTheme
import com.taqvo.app.feed.FeedThemes
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.feed_favorites_title
import com.taqvo.app.resources.feed_tools_duas
import com.taqvo.app.resources.qibla_title
import com.taqvo.app.resources.settings_daily_ayahs
import com.taqvo.app.resources.settings_notifications
import com.taqvo.app.resources.settings_widgets
import com.taqvo.app.resources.tab_prayer
import com.taqvo.app.resources.tab_quran
import com.taqvo.app.resources.tab_settings
import com.taqvo.app.resources.tasbeeh_title
import kotlin.time.Clock

/** The app's language setting as the feed's translation language (content spec §2). */
@Composable
private fun feedLanguage(container: AppContainer): ContentLang {
    val language by container.settingsRepository.appLanguage.collectAsState(AppLanguage.DEFAULT)
    return LanguageResolver.forApp(language)
}

/** The reader's look for every feed surface: the ayah format, the text style, the theme, the watermark. */
private class FeedLook(val format: AyahFormat, val style: FeedTextStyle, val theme: FeedTheme, val watermark: Boolean)

@Composable
private fun rememberFeedLook(container: AppContainer): FeedLook {
    val format by container.onboardingStore.ayahFormat.collectAsState(AyahFormat.ARABIC_TRANSLATION)
    val style by container.feedPrefsStore.textStyle.collectAsState(FeedTextStyle())
    val themeId by container.feedPrefsStore.themeId.collectAsState(FeedThemes.DEFAULT_ID)
    val watermark by container.feedPrefsStore.watermark.collectAsState(true)
    return FeedLook(format, style, FeedThemes.of(themeId), watermark)
}

private fun sheetDeps(container: AppContainer, scope: CoroutineScope, look: FeedLook, onShared: (FeedCard) -> Unit) = CardSheetDeps(
    store = container.feedStore,
    analytics = container.analytics,
    format = look.format,
    style = look.style,
    theme = look.theme,
    watermark = look.watermark,
    onFormat = { chosen ->
        container.analytics.track(FeedEvents.STYLE_CHANGE, mapOf("field" to "format", "value" to chosen.name))
        scope.launch {
            container.onboardingStore.setAyahFormat(chosen)
            // The daily reminders carry the ayah in this same format (onboarding screen 17).
            container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
        }
    },
    onStyle = { next ->
        trackStyleChange(container, look.style, next)
        scope.launch { container.feedPrefsStore.setTextStyle(next) }
    },
    onWatermark = { shown -> scope.launch { container.feedPrefsStore.setWatermark(shown) } },
    onShared = onShared,
)

private fun trackStyleChange(container: AppContainer, before: FeedTextStyle, after: FeedTextStyle) {
    val changes = listOfNotNull(
        ("font" to after.translationFont.id).takeIf { before.translationFont != after.translationFont },
        ("size" to after.sizePct.toString()).takeIf { before.sizePct != after.sizePct },
        ("color" to after.color.id).takeIf { before.color != after.color },
        ("align" to after.align.id).takeIf { before.align != after.align },
    )
    changes.forEach { (field, value) -> container.analytics.track(FeedEvents.STYLE_CHANGE, mapOf("field" to field, "value" to value)) }
}

@Composable
internal fun FeedRoute(container: AppContainer, navigator: Navigator) {
    val vm = container.feedViewModel
    val state by vm.state.collectAsState()
    val lang = feedLanguage(container)
    val look = rememberFeedLook(container)
    val scope = rememberCoroutineScope()
    val sheets = rememberCardSheets()
    var goalOpen by remember { mutableStateOf(false) }
    var streakOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (vm.state.value.cards.isEmpty()) vm.start(lang) else vm.refresh()
    }
    LaunchedEffect(lang) { vm.setLanguage(lang) }
    // Leaving the tab ends the card on screen the way leaving the app does.
    DisposableEffect(Unit) {
        vm.onForeground()
        onDispose { vm.onBackground() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.onBackground() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.onForeground() }

    val guideClosed by container.feedPrefsStore.guideChipClosed.collectAsState(true)
    val helpClosed by container.feedPrefsStore.helpChipClosed.collectAsState(true)
    var helpOpen by remember { mutableStateOf(false) }
    FeedScreen(
        state = state,
        format = look.format,
        style = look.style,
        theme = look.theme,
        showGuideChip = state.showGuideChip && !guideClosed,
        showHelpChip = !helpClosed,
        actions = FeedActions(
            onSettled = vm::onSettled,
            onLike = { vm.toggleLike(it.item.id) },
            onDoubleTap = { vm.like(it.item.id, LikeVia.DOUBLE_TAP) },
            onShare = { card ->
                container.analytics.track(FeedEvents.SHARE_OPEN, mapOf("item_id" to card.item.id))
                sheets.share = card
            },
            onStyle = { sheets.style = true },
            onNote = { sheets.note = it },
            top = FeedTopActions(
                onReminders = { navigator.push(Screen.DailyAyahs) },
                onGuide = { navigator.push(Screen.Widgets) },
                onGuideDone = { scope.launch { container.feedPrefsStore.closeGuideChip() } },
                onHelp = { helpOpen = true },
                onHelpClose = { scope.launch { container.feedPrefsStore.closeHelpChip() } },
                onGoal = { goalOpen = true },
                onStreak = { streakOpen = true },
            ),
            onThemes = { navigator.push(Screen.Themes) },
            onTools = { navigator.push(Screen.Tools) },
            onRetry = { vm.start(lang) },
            onCelebrated = vm::consumeCelebration,
        ),
    )
    if (helpOpen) HelpSheet(onAbout = { helpOpen = false; navigator.push(Screen.About) }, onDismiss = { helpOpen = false })

    val today = remember(state.likesToday, state.streak) { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    if (goalOpen) {
        GoalSheet(state, today, onFavorites = { goalOpen = false; navigator.push(Screen.Favorites) }, onDismiss = { goalOpen = false })
    }
    if (streakOpen) StreakSheet(state, today, onDismiss = { streakOpen = false })
    CardSheetsHost(sheets, styleCard = state.current, deps = sheetDeps(container, scope, look) { vm.onShared(it.item.id) })
}

@Composable
internal fun FavoritesRoute(container: AppContainer, navigator: Navigator) {
    val lang = feedLanguage(container)
    val look = rememberFeedLook(container)
    val revision by container.feedStore.revision.collectAsState()
    val cards by produceState<List<FeedCard>?>(null, revision, lang) {
        value = container.feedLibrary.cards(container.feedStore.favorites(), lang)
    }
    FavoritesScreen(cards, look.theme, onOpen = { navigator.push(Screen.FeedItem(it.item.id)) }, onBack = { navigator.pop() })
}

@Composable
internal fun CollectionsRoute(container: AppContainer, navigator: Navigator) {
    val scope = rememberCoroutineScope()
    val revision by container.feedStore.revision.collectAsState()
    val collections by produceState<List<FeedCollection>?>(null, revision) { value = container.feedStore.collections() }
    var editing by remember { mutableStateOf<FeedCollection?>(null) }
    CollectionsScreen(
        collections = collections,
        onOpen = { navigator.push(Screen.Collection(it.id)) },
        onEdit = { editing = it },
        onBack = { navigator.pop() },
    )
    editing?.let { collection ->
        CollectionEditSheet(
            collection = collection,
            onRename = { name -> scope.launch { container.feedStore.renameCollection(collection.id, name); editing = null } },
            onDelete = { scope.launch { container.feedStore.deleteCollection(collection.id); editing = null } },
            onDismiss = { editing = null },
        )
    }
}

@Composable
internal fun CollectionRoute(screen: Screen.Collection, container: AppContainer, navigator: Navigator) {
    val lang = feedLanguage(container)
    val look = rememberFeedLook(container)
    val revision by container.feedStore.revision.collectAsState()
    val collection by produceState<FeedCollection?>(null, revision) {
        value = container.feedStore.collections().firstOrNull { it.id == screen.id }
    }
    val cards by produceState<List<FeedCard>?>(null, revision, lang) {
        value = container.feedLibrary.cards(container.feedStore.collectionItems(screen.id), lang)
    }
    CollectionScreen(collection?.name.orEmpty(), cards, look.theme, onOpen = { navigator.push(Screen.FeedItem(it.item.id)) }, onBack = { navigator.pop() })
}

@Composable
internal fun FeedItemRoute(screen: Screen.FeedItem, container: AppContainer, navigator: Navigator) {
    val lang = feedLanguage(container)
    val look = rememberFeedLook(container)
    val scope = rememberCoroutineScope()
    val sheets = rememberCardSheets()
    val vm = container.feedViewModel
    val state by vm.state.collectAsState()
    val revision by container.feedStore.revision.collectAsState()
    val card by produceState<FeedCard?>(null, screen.itemId, lang) { value = container.feedLibrary.card(screen.itemId, lang) }
    val liked by produceState(false, revision, state.favorites) { value = screen.itemId in container.feedStore.favorites() }
    val current = card ?: return
    SingleCardScreen(
        card = current,
        liked = liked,
        format = look.format,
        style = look.style,
        theme = look.theme,
        onLike = { if (liked) vm.unlike(current.item.id) else vm.like(current.item.id, LikeVia.TAP) },
        onShare = {
            container.analytics.track(FeedEvents.SHARE_OPEN, mapOf("item_id" to current.item.id))
            sheets.share = current
        },
        onStyle = { sheets.style = true },
        onNote = { sheets.note = current },
        onBack = { navigator.pop() },
    )
    CardSheetsHost(sheets, styleCard = current, deps = sheetDeps(container, scope, look) { vm.onShared(it.item.id) })
}

/** Mavzular: choose the theme; "Tahrirlash" on the chosen one opens the text style editor. */
@Composable
internal fun ThemesRoute(container: AppContainer, navigator: Navigator) {
    val look = rememberFeedLook(container)
    val scope = rememberCoroutineScope()
    var styleOpen by remember { mutableStateOf(false) }
    val state by container.feedViewModel.state.collectAsState()
    ThemesScreen(
        themes = FeedThemes.ALL,
        selectedId = look.theme.id,
        onSelect = { theme ->
            container.analytics.track(FeedEvents.STYLE_CHANGE, mapOf("field" to "theme", "value" to theme.id))
            scope.launch { container.feedPrefsStore.setThemeId(theme.id) }
        },
        onEdit = { styleOpen = true },
        onClose = { navigator.pop() },
    )
    if (styleOpen) {
        val deps = sheetDeps(container, scope, look) { }
        TextStyleSheet(
            card = state.current,
            format = look.format,
            style = look.style,
            theme = look.theme,
            onFormat = deps.onFormat,
            onStyle = deps.onStyle,
            onDismiss = { styleOpen = false },
        )
    }
}

/** Ilovalar: every part of the app other than the feed, each opened over this screen. */
@Composable
internal fun ToolsRoute(navigator: Navigator) {
    fun open(screen: Screen): () -> Unit = { navigator.push(screen) }
    val tiles = listOf(
        ToolTile(stringResource(Res.string.settings_widgets), { drawPhone(it) }, open(Screen.Widgets)),
        ToolTile(stringResource(Res.string.settings_daily_ayahs), { drawBell(it) }, open(Screen.DailyAyahs)),
        ToolTile(stringResource(Res.string.tab_prayer), { drawMihrab(it) }, open(Screen.Prayer)),
        ToolTile(stringResource(Res.string.tab_quran), { drawBook(it) }, open(Screen.Quran)),
        ToolTile(stringResource(Res.string.feed_tools_duas), { drawHorizon(it) }, open(Screen.Adhkar())),
        ToolTile(stringResource(Res.string.qibla_title), { drawKaaba(it) }, open(Screen.Qibla)),
        ToolTile(stringResource(Res.string.tasbeeh_title), { drawMisbaha(it) }, open(Screen.Tasbeeh)),
        ToolTile(stringResource(Res.string.feed_favorites_title), { drawHeart(it, false) }, open(Screen.Favorites)),
        ToolTile(stringResource(Res.string.settings_notifications), { drawBell(it) }, open(Screen.NotificationSettings)),
        ToolTile(stringResource(Res.string.tab_settings), { drawSliders(it) }, open(Screen.Settings)),
    )
    ToolsScreen(tiles, onClose = { navigator.pop() })
}

/**
 * A screen that was a tab root (Prayer, Quran, Settings), opened from Ilovalar: a back bar above
 * it, so the screen keeps its own header as it was and still has a way home. The bar takes the
 * status bar's inset; the screen under it is told the top is already cleared.
 */
@Composable
internal fun RootWithBack(navigator: Navigator, content: @Composable () -> Unit) {
    val colors = LocalTaqvoColors.current
    Column(Modifier.fillMaxSize().background(colors.background)) {
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) { BackChevron { navigator.pop() } }
        Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) { content() }
    }
}
