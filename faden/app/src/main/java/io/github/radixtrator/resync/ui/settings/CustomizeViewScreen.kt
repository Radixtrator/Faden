@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.radixtrator.resync.ui.settings

import io.github.radixtrator.resync.ui.common.Symbols
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.radixtrator.resync.AppContainer
import io.github.radixtrator.resync.data.ImageSource
import io.github.radixtrator.resync.data.MediaPreviewSize
import io.github.radixtrator.resync.data.Post
import io.github.radixtrator.resync.data.PostColumns
import io.github.radixtrator.resync.data.isCoverScreen
import io.github.radixtrator.resync.data.PostLayout
import io.github.radixtrator.resync.data.PostMedia
import io.github.radixtrator.resync.data.TitleSize
import io.github.radixtrator.resync.data.ViewAutoplay
import io.github.radixtrator.resync.data.ViewOptions
import io.github.radixtrator.resync.ui.common.HeaderFilterChip
import io.github.radixtrator.resync.ui.common.LargeHeader
import io.github.radixtrator.resync.ui.common.PostActions
import io.github.radixtrator.resync.ui.common.PostLayoutItem
import io.github.radixtrator.resync.ui.common.SwipeBack
import io.github.radixtrator.resync.ui.common.feedColumns
import io.github.radixtrator.resync.ui.common.gutter
import io.github.radixtrator.resync.ui.common.spacing
import io.github.radixtrator.resync.ui.navigation.LocalNavigator
import io.github.radixtrator.resync.ui.theme.FullSizeText
import io.github.radixtrator.resync.i18n.t

/**
 * Customize view, the one page for every view setting (Settings > View type opens it, as does the Change view
 * sheet's "Customize view" button): a large "View type" title, a chip per layout in Sync's order (Slides … List), a
 * live preview of example posts in the chosen layout, the settings shared by all layouts (default layout, media
 * preview size) and Swipe mode's Autoplay, then the chosen layout's options. Changes apply to every feed using the
 * layout at once; the toolbar's reset icon puts the chosen layout back to its defaults. Of Sync's font pickers (Title, Description, Selftext) only
 * the size half is here, as Small / Normal / Large.
 */
@Composable
fun CustomizeViewScreen(initial: PostLayout, container: AppContainer, highlight: String? = null) {
    val nav = LocalNavigator.current
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    var layout by rememberSaveable { mutableStateOf(initial) }
    val options = settings.viewOptionsFor(layout)
    val customized = layout in settings.viewOptions

    fun change(edit: (ViewOptions) -> ViewOptions) = container.settings.update { s ->
        s.copy(viewOptions = s.viewOptions + (layout to edit(s.viewOptionsFor(layout))))
    }
    // The preview tiles like the feed: the Columns choice, else as many as this window fits.
    val columns = layout.feedColumns(options)
    // A foldable's cover screen (or any phone-sized window) has its own Columns choice.
    val coverScreen = isCoverScreen(LocalConfiguration.current.smallestScreenWidthDp)

    SwipeBack(enabled = settings.swipeBack, onBack = nav::pop) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = nav::pop) { Icon(Symbols.ArrowBack, t("Back")) } },
                    title = {},
                    actions = {
                        IconButton(
                            onClick = { container.settings.update { it.copy(viewOptions = it.viewOptions - layout) } },
                            enabled = customized,
                        ) { Icon(Symbols.Refresh, t("Reset {0} to defaults", layout.label)) }
                    },
                )
            },
        ) { padding ->
            val scroll = rememberScrollState()
            ProvideSettingsJump(highlight, scroll) { viewport ->
            Column(Modifier.padding(padding).fillMaxSize().then(viewport).verticalScroll(scroll)) {
                // Sync's chip row runs from the richest layout to the plainest.
                LargeHeader(title = t("View type")) {
                    PostLayout.entries.reversed().forEach { option ->
                        HeaderFilterChip(
                            label = option.label,
                            selected = option == layout,
                            onClick = { layout = option },
                        )
                    }
                }
                // The preview is drawn at feed size (with the feed's base font scale), so it looks as it will.
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = layout.gutter, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(layout.spacing),
                ) {
                    SamplePosts.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(layout.gutter)) {
                            row.forEach { post ->
                                Box(Modifier.weight(1f)) { PostLayoutItem(layout, post, showSubreddit = true, actions = NoActions) }
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                FullSizeText {
                    Column(Modifier.padding(bottom = 16.dp)) {
                        // The view settings that aren't per layout, so this screen is the one place for all of them.
                        Section(t("All layouts"))
                        Choice(t("Default post layout"), PostLayout.entries, settings.layout, PostLayout::label) { v ->
                            container.settings.update { it.copy(layout = v) }
                        }
                        Choice(t("Post media preview size"), MediaPreviewSize.entries, settings.postMediaPreview, MediaPreviewSize::label) { v ->
                            container.settings.update { it.copy(postMediaPreview = v) }
                        }
                        // Swipe mode isn't a layout, so its one view option sits with the shared ones.
                        Section(t("Swipe mode"))
                        Choice(t("Autoplay"), ViewAutoplay.entries, settings.swipeAutoplay, ViewAutoplay::label) { v ->
                            container.settings.update { it.copy(swipeAutoplay = v) }
                        }
                        sectionsFor(layout).forEach { (title, rows) ->
                            Section(title)
                            rows.forEach { row ->
                                when (row) {
                                    is ViewRow.Switch -> Toggle(row.option.title, row.option.summary, row.option.read(options)) { v ->
                                        change { row.option.write(it, v) }
                                    }
                                    ViewRow.Columns -> {
                                        // Labelled so it's clear which of the two this screen is using right now.
                                        fun title(name: String, here: Boolean) = if (here) t("{0} (this screen)", name) else name
                                        Choice(title(t("Columns on main screen"), !coverScreen), PostColumns.entries, options.columns, PostColumns::label) { v ->
                                            change { it.copy(columns = v) }
                                        }
                                        Choice(title(t("Columns on cover screen"), coverScreen), PostColumns.entries, options.coverColumns, PostColumns::label) { v ->
                                            change { it.copy(coverColumns = v) }
                                        }
                                    }
                                    is ViewRow.FontSize -> Choice(row.size.title, TitleSize.entries, row.size.read(options), TitleSize::label) { v ->
                                        change { row.size.write(it, v) }
                                    }
                                    ViewRow.Autoplay -> Choice(t("Autoplay"), ViewAutoplay.entries, options.autoplay, ViewAutoplay::label) { v ->
                                        change { it.copy(autoplay = v) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

/** One switch on a View type screen, with the catalogue's title and the [ViewOptions] field it flips. */
internal enum class ViewOption(
    private val enTitle: String,
    private val enSummary: String?,
    val read: (ViewOptions) -> Boolean,
    val write: (ViewOptions, Boolean) -> ViewOptions,
) {
    IMAGE_PREVIEWS("Show image previews", null, { it.imagePreviews }, { o, v -> o.copy(imagePreviews = v) }),
    SELFTEXT_PREVIEWS("Show selftext previews", null, { it.selftextPreviews }, { o, v -> o.copy(selftextPreviews = v) }),
    SUBREDDIT_ICONS("Show subreddit icons", "Shown on Frontpage, All, search etc", { it.subredditIcons }, { o, v -> o.copy(subredditIcons = v) }),
    SELFTEXT_BORDER("Show a border around selftext", null, { it.selftextBorder }, { o, v -> o.copy(selftextBorder = v) }),
    IMAGE_ABOVE_TITLE("Image above title", null, { it.imageAboveTitle }, { o, v -> o.copy(imageAboveTitle = v) }),
    THUMBNAIL_LEFT("Align thumbnail left", null, { it.thumbnailLeft }, { o, v -> o.copy(thumbnailLeft = v) }),
    THUMBNAILS("Thumbnails", null, { it.thumbnails }, { o, v -> o.copy(thumbnails = v) }),
    TYPE_INDICATORS("Type indicators", null, { it.typeIndicators }, { o, v -> o.copy(typeIndicators = v) }),
    SINGLE_LINE_TITLE("Limit title to a single line", null, { it.singleLineTitle }, { o, v -> o.copy(singleLineTitle = v) }),
    THUMBNAIL_RIGHT("Align thumbnail right", null, { it.thumbnailRight }, { o, v -> o.copy(thumbnailRight = v) }),
    SLIDE_FULL_HEIGHT("Preview full height", null, { it.slideFullHeight }, { o, v -> o.copy(slideFullHeight = v) }),
    SLIDE_FULL_WIDTH("Preview full width", null, { it.slideFullWidth }, { o, v -> o.copy(slideFullWidth = v) }),
    SLIDE_IMAGE_TOP("Preview before title", null, { it.slideImageTop }, { o, v -> o.copy(slideImageTop = v) }),
    THICK_DIVIDER("Show a thick divider between posts", "Recommended as this improves post viewability", { it.thickDivider }, { o, v -> o.copy(thickDivider = v) }),
    CARD_FULL_WIDTH("Full width cards", "Single column only", { it.cardFullWidth }, { o, v -> o.copy(cardFullWidth = v) }),
    CARD_FULL_HEIGHT("Full height", null, { it.cardFullHeight }, { o, v -> o.copy(cardFullHeight = v) }),
    SAVE_BUTTON("Save", null, { it.saveButton }, { o, v -> o.copy(saveButton = v) }),
    CARD_SAVE_BUTTON("Save", null, { it.cardSaveButton }, { o, v -> o.copy(cardSaveButton = v) }),
    HIDE_BUTTON("Hide", null, { it.hideButton }, { o, v -> o.copy(hideButton = v) }),
    SHARE_BUTTON("Share", null, { it.shareButton }, { o, v -> o.copy(shareButton = v) }),
    MOD_BUTTON("Mod tools", "This is only shown for subs you moderate", { it.modButton }, { o, v -> o.copy(modButton = v) }),
    VOTE_RIGHT("Right aligned voting", null, { it.voteRight }, { o, v -> o.copy(voteRight = v) }),
    HIDE_ALL_BUTTONS("Hide all buttons", null, { it.hideAllButtons }, { o, v -> o.copy(hideAllButtons = v) });

    val title: String get() = t(enTitle)
    val summary: String? get() = enSummary?.let { t(it) }
}

/** One font size choice on a View type screen: the size half of one of Sync's font pickers. */
internal enum class ViewFontSize(
    private val enTitle: String,
    val read: (ViewOptions) -> TitleSize,
    val write: (ViewOptions, TitleSize) -> ViewOptions,
) {
    /** [ViewOptions.titleSize], Sync's "Title font". */
    TITLE("Title font size", { it.titleSize }, { o, v -> o.copy(titleSize = v) }),

    /** [ViewOptions.descriptionSize], Sync's "Description font". */
    DESCRIPTION("Description font size", { it.descriptionSize }, { o, v -> o.copy(descriptionSize = v) }),

    /** [ViewOptions.selftextSize], Sync's "Selftext font". */
    SELFTEXT("Selftext font size", { it.selftextSize }, { o, v -> o.copy(selftextSize = v) });

    val title: String get() = t(enTitle)
}

/** A row on a View type screen: a [ViewOption] switch, a [ViewFontSize] choice, Columns, or Autoplay. */
private sealed interface ViewRow {
    class Switch(val option: ViewOption) : ViewRow

    class FontSize(val size: ViewFontSize) : ViewRow

    /** [ViewOptions.columns] and [ViewOptions.coverColumns]: main and cover screen. */
    object Columns : ViewRow

    /** [ViewOptions.autoplay]: this layout's override of Data options › "Autoplay". */
    object Autoplay : ViewRow
}

private fun switches(vararg options: ViewOption): List<ViewRow> = options.map { ViewRow.Switch(it) }

private fun fontSizes(vararg sizes: ViewFontSize): List<ViewRow> = sizes.map { ViewRow.FontSize(it) }

/**
 * Each layout's options under Sync's section names and in its order (sync-reference/settings-catalogue.md, "View
 * type"). Small cards get Columns too, since they tile like Cards here.
 */
private fun sectionsFor(layout: PostLayout): List<Pair<String, List<ViewRow>>> = when (layout) {
    PostLayout.SLIDES -> listOf(
        t("Preview options") to switches(ViewOption.IMAGE_PREVIEWS, ViewOption.SELFTEXT_PREVIEWS, ViewOption.SUBREDDIT_ICONS) + ViewRow.Autoplay,
        t("Image options") to switches(ViewOption.SLIDE_FULL_HEIGHT, ViewOption.SLIDE_FULL_WIDTH, ViewOption.SLIDE_IMAGE_TOP),
        t("Fonts and sizing") to fontSizes(ViewFontSize.TITLE, ViewFontSize.DESCRIPTION, ViewFontSize.SELFTEXT),
        t("Buttons") to switches(
            ViewOption.SAVE_BUTTON,
            ViewOption.HIDE_BUTTON,
            ViewOption.SHARE_BUTTON,
            ViewOption.MOD_BUTTON,
            ViewOption.VOTE_RIGHT,
            ViewOption.HIDE_ALL_BUTTONS,
        ),
        t("Experimental options") to switches(ViewOption.THICK_DIVIDER, ViewOption.SELFTEXT_BORDER),
    )
    PostLayout.CARD -> listOf(
        t("Image options") to switches(ViewOption.IMAGE_ABOVE_TITLE, ViewOption.CARD_FULL_HEIGHT) + ViewRow.Autoplay,
        t("Selftext options") to switches(ViewOption.SELFTEXT_PREVIEWS),
        t("Card options") to listOf(ViewRow.Switch(ViewOption.CARD_FULL_WIDTH), ViewRow.Columns),
        t("Fonts and sizing") to fontSizes(ViewFontSize.TITLE, ViewFontSize.DESCRIPTION, ViewFontSize.SELFTEXT),
        t("Buttons") to switches(ViewOption.CARD_SAVE_BUTTON, ViewOption.HIDE_BUTTON, ViewOption.SHARE_BUTTON, ViewOption.MOD_BUTTON),
    )
    PostLayout.SMALL_CARDS -> listOf(
        t("Image options") to switches(ViewOption.THUMBNAIL_LEFT),
        t("Card options") to listOf(ViewRow.Columns),
        t("Fonts and sizing") to fontSizes(ViewFontSize.TITLE, ViewFontSize.DESCRIPTION),
        t("Buttons") to switches(ViewOption.CARD_SAVE_BUTTON, ViewOption.HIDE_BUTTON, ViewOption.SHARE_BUTTON, ViewOption.MOD_BUTTON),
    )
    PostLayout.SMALLER_CARDS, PostLayout.COMPACT, PostLayout.LIST -> listOf(
        t("Image options") to if (layout == PostLayout.SMALLER_CARDS) {
            switches(ViewOption.TYPE_INDICATORS, ViewOption.THUMBNAIL_RIGHT)
        } else {
            switches(ViewOption.THUMBNAILS, ViewOption.TYPE_INDICATORS, ViewOption.THUMBNAIL_RIGHT)
        },
        // Sync's order: Title font, the one-line switch, then Description font.
        t("Fonts and sizing") to listOf(
            ViewRow.FontSize(ViewFontSize.TITLE),
            ViewRow.Switch(ViewOption.SINGLE_LINE_TITLE),
            ViewRow.FontSize(ViewFontSize.DESCRIPTION),
        ),
        t("Buttons") to switches(ViewOption.CARD_SAVE_BUTTON, ViewOption.HIDE_BUTTON, ViewOption.SHARE_BUTTON, ViewOption.MOD_BUTTON),
    )
}

/** The preview only shows; taps go nowhere. Hide, Share and Mod tools are set so their buttons show too. */
private val NoActions = PostActions(
    onOpen = {},
    onMedia = {},
    onMore = {},
    onVote = {},
    onToggleSave = {},
    onHide = {},
    onShare = {},
    onModTools = {},
)

// A Wikimedia Commons photo of a golden retriever, standing in for Sync's dog photo.
private const val SAMPLE_IMAGE = "https://upload.wikimedia.org/wikipedia/commons/6/6e/Golde33443.jpg"

/**
 * Sync's "Example post" by ljdawson (an upvoted, saved photo at 9999 points), plus a text post so the selftext
 * options show too. Ages are fixed offsets from now.
 */
private val SamplePosts: List<Post>
    get() {
        val now = System.currentTimeMillis() / 1000
        return listOf(
            Post(
                id = "faden_example_image",
                fullname = "t3_faden_example_image",
                subreddit = "pics",
                author = "ljdawson",
                title = t("Example post"),
                selfText = "",
                selfTextHtml = null,
                url = SAMPLE_IMAGE,
                domain = "upload.wikimedia.org",
                permalink = "/r/pics/comments/faden_example_image/example_post/",
                score = 9999,
                numComments = 154,
                createdUtc = now - 4 * 3600,
                over18 = false,
                spoiler = false,
                stickied = false,
                isSelf = false,
                likes = true,
                saved = true,
                flair = t("Example"),
                media = PostMedia.Image(ImageSource(SAMPLE_IMAGE, 1600, 1200), SAMPLE_IMAGE, isGif = false),
            ),
            Post(
                id = "faden_example_text",
                fullname = "t3_faden_example_text",
                subreddit = "faden",
                author = "ljdawson",
                title = t("Example text post"),
                selfText = t("This is how a text post's preview looks in this view.\n\nThe rest of the post stays in the thread."),
                selfTextHtml = null,
                url = "https://www.reddit.com/r/faden/comments/faden_example_text/example_text_post/",
                domain = "self.faden",
                permalink = "/r/faden/comments/faden_example_text/example_text_post/",
                score = 42,
                numComments = 7,
                createdUtc = now - 26 * 3600,
                over18 = false,
                spoiler = false,
                stickied = false,
                isSelf = true,
                likes = null,
                saved = false,
                flair = null,
                media = PostMedia.None,
            ),
        )
    }
