package com.taqvo.app.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.TaqvoBottomSheet
import com.taqvo.app.design.components.TaqvoCard
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.design.components.TaqvoRow
import com.taqvo.app.feed.FeedCollection
import com.taqvo.app.feed.FeedPremium
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.feed_collection_create
import com.taqvo.app.resources.feed_collection_name
import com.taqvo.app.resources.feed_collection_new
import com.taqvo.app.resources.feed_share_collection

/** The longest a collection's name may be (feed spec §5). */
const val COLLECTION_NAME_MAX = 30

/**
 * "Add to collection" (feed spec §5): the reader's collections, and a new one named on the spot. A
 * tap on a collection adds the card and closes the sheet; a new collection is created with the
 * card already in it.
 */
@Composable
fun CollectionPickerSheet(
    collections: List<FeedCollection>,
    onPick: (FeedCollection) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalTaqvoColors.current
    val format = LocalPlatformFormat.current
    var naming by remember { mutableStateOf(collections.isEmpty()) }
    TaqvoBottomSheet(onDismissRequest = onDismiss, skipPartiallyExpanded = true) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(stringResource(Res.string.feed_share_collection), style = TaqvoText.screenTitle)
            Spacer(Modifier.height(16.dp))
            if (collections.isNotEmpty()) {
                TaqvoCard {
                    collections.forEachIndexed { i, collection ->
                        if (i > 0) CardDivider()
                        TaqvoRow(label = collection.name, value = format.localizedDigits(collection.size), onClick = { onPick(collection) })
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            val canCreate = collections.size < FeedPremium.collectionLimit()
            if (naming && canCreate) {
                NameField(initial = "", onDone = onCreate)
            } else if (canCreate) {
                TaqvoCard { TaqvoRow(label = stringResource(Res.string.feed_collection_new), onClick = { naming = true }) }
            }
        }
    }
}

/** A collection's name, typed: trimmed, at most [COLLECTION_NAME_MAX] characters, never empty. */
@Composable
fun NameField(initial: String, onDone: (String) -> Unit) {
    val colors = LocalTaqvoColors.current
    var name by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val placeholder = stringResource(Res.string.feed_collection_name)
    val clean = name.trim().take(COLLECTION_NAME_MAX)
    Column {
        BasicTextField(
            value = name,
            onValueChange = { if (it.length <= COLLECTION_NAME_MAX) name = it },
            singleLine = true,
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 18.sp),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (clean.isNotEmpty()) onDone(clean) }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = placeholder },
            decorationBox = { field ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.hairline, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (name.isEmpty()) Text(placeholder, color = colors.textTertiary, fontSize = 18.sp)
                    field()
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        TaqvoPrimaryButton(stringResource(Res.string.feed_collection_create), onClick = { onDone(clean) }, enabled = clean.isNotEmpty())
    }
}
