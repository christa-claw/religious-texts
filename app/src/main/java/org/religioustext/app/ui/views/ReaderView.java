package org.religioustext.app.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.religioustext.app.model.DisplayOptions;
import org.religioustext.app.model.DisplayOptions.OrderMode;
import org.religioustext.app.model.SourceColumn;
import org.religioustext.app.model.VerseRef;
import org.religioustext.app.service.TextQueryService;

import java.util.ArrayList;
import java.util.List;

/**
 * Main reader view — multi-column religious text display.
 *
 * Layout:
 *   [Toolbar: title | Order Mode | Add Column]
 *   [Column 1 | Column 2 | Column 3 ... → horizontal scroll]
 *
 * Each column is 33vw wide and has its own source selector in the header.
 * All synced columns share the same book/chapter reference.
 * Individual columns can break sync to browse independently.
 */
@Route("")
@PageTitle("Religious Texts Reader")
public class ReaderView extends VerticalLayout {

    private final TextQueryService   queryService;
    private final List<SourceColumn> columns = new ArrayList<>();
    private final List<String[]>     sources;
    private final HorizontalLayout   columnsLayout;

    // Shared navigation state (for synced columns)
    private String currentBook    = null;
    private int    currentChapter = 1;

    public ReaderView(final TextQueryService queryService) {
        this.queryService  = queryService;
        this.sources       = queryService.listSources();
        this.columnsLayout = new HorizontalLayout();

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        columnsLayout.setSizeFull();
        columnsLayout.setSpacing(false);
        columnsLayout.getStyle()
            .set("overflow-x", "auto")
            .set("align-items", "stretch");

        add(buildToolbar(), columnsLayout);
        setFlexGrow(1, columnsLayout);

        // Start with one empty column
        addColumn();
    }

    // ── Toolbar ───────────────────────────────────────────────────────

    private HorizontalLayout buildToolbar() {
        final HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.getStyle()
            .set("padding", "8px 16px")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
            .set("flex-shrink", "0");

        final Span title = new Span("Religious Texts Reader");
        title.getStyle()
            .set("font-weight", "bold")
            .set("font-size", "18px")
            .set("flex-grow", "1");

        final Select<OrderMode> orderSelect = buildOrderModeSelector();

        final Button addColumnBtn = new Button(
             "Add Column"
            , VaadinIcon.PLUS.create()
            , e -> addColumn());
        addColumnBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        toolbar.add(title, orderSelect, addColumnBtn);
        return toolbar;
    }

    private Select<OrderMode> buildOrderModeSelector() {
        final Select<OrderMode> select = new Select<>();
        select.setLabel("Order");
        select.setItems(OrderMode.values());
        select.setValue(OrderMode.CANONICAL);
        select.setItemLabelGenerator(mode -> switch (mode) {
            case CANONICAL     -> "Canonical";
            case CHRONOLOGICAL -> "Chronological";
            case NARRATIVE     -> "Narrative";
        });
        select.addValueChangeListener(e -> {
            columns.forEach(col -> col.getDisplayOptions().setOrderMode(e.getValue()));
            refreshAllColumns();
        });
        return select;
    }

    // ── Column management ─────────────────────────────────────────────

    private void addColumn() {
        final SourceColumn     col       = new SourceColumn(DisplayOptions.defaults());
        final Div              verses    = buildVerseContainer();
        final Select<String>   bookSel   = new Select<>();
        final Select<Integer>  chapSel   = new Select<>();

        columns.add(col);
        columnsLayout.add(buildColumnComponent(col, verses, bookSel, chapSel));
    }

    private void removeColumn(final SourceColumn col, final VerticalLayout colLayout) {
        columns.remove(col);
        columnsLayout.remove(colLayout);
    }

    // ── Column component ──────────────────────────────────────────────

    private VerticalLayout buildColumnComponent(
             final SourceColumn    col
            , final Div             verseContainer
            , final Select<String>  bookSelect
            , final Select<Integer> chapterSelect) {

        final VerticalLayout colLayout = new VerticalLayout();
        colLayout.getStyle()
            .set("width", "33vw")
            .set("min-width", "320px")
            .set("height", "100%")
            .set("border-right", "1px solid var(--lumo-contrast-10pct)")
            .set("overflow-y", "auto")
            .set("padding", "0")
            .set("flex-shrink", "0");

        colLayout.add(
             buildColumnHeader(col, colLayout, verseContainer, bookSelect, chapterSelect)
            , buildNavBar(col, verseContainer, bookSelect, chapterSelect)
            , verseContainer);

        colLayout.setFlexGrow(1, verseContainer);
        return colLayout;
    }

    // ── Column header ─────────────────────────────────────────────────

    private HorizontalLayout buildColumnHeader(
             final SourceColumn    col
            , final VerticalLayout  colLayout
            , final Div             verseContainer
            , final Select<String>  bookSelect
            , final Select<Integer> chapterSelect) {

        final HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
            .set("padding", "6px 8px")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
            .set("position", "sticky")
            .set("top", "0")
            .set("z-index", "1")
            .set("flex-shrink", "0");

        // Source selector
        final ComboBox<String[]> sourceCombo = new ComboBox<>();
        sourceCombo.setPlaceholder("Select source...");
        sourceCombo.setItemLabelGenerator(arr -> arr[2] + " — " + arr[1]);
        sourceCombo.setItems(sources);
        sourceCombo.setWidthFull();
        sourceCombo.addValueChangeListener(e -> {
            final String[] selected = e.getValue();
            if (selected == null) return;
            col.setSource(selected[0], selected[1], selected[2], selected[3]);

            // RTL direction
            if (col.isRtl()) {
                colLayout.getStyle().set("direction", "rtl");
            } else {
                colLayout.getStyle().remove("direction");
            }

            // Load books for this source
            final List<String> books = queryService.listBooks(col.getSourceId());
            bookSelect.setItems(books);
            if (!books.isEmpty()) {
                bookSelect.setValue(books.get(0));
                // bookSelect value change listener will trigger verse load
            }
        });

        // Display mode selector
        final Select<DisplayOptions.DisplayMode> modeSelect = new Select<>();
        modeSelect.setItems(DisplayOptions.DisplayMode.values());
        modeSelect.setValue(DisplayOptions.DisplayMode.CHAPTERS_VERSES);
        modeSelect.setItemLabelGenerator(DisplayOptions.DisplayMode::getLabel);
        modeSelect.setItemEnabledProvider(mode -> !mode.isDisabled());
        modeSelect.getStyle().set("min-width", "130px");
        modeSelect.getElement().setAttribute("title",
            DisplayOptions.DisplayMode.CHAPTERS_VERSES.getTooltip());
        modeSelect.addValueChangeListener(e -> {
            col.getDisplayOptions().setMode(e.getValue());
            modeSelect.getElement().setAttribute("title", e.getValue().getTooltip());
            if (col.getSourceId() != null && currentBook != null) {
                populateVerses(verseContainer, col);
            }
        });

        // Sync toggle
        final Button[] syncBtnHolder = new Button[1];
        final Button syncBtn = new Button(VaadinIcon.LINK.create());
        syncBtnHolder[0] = syncBtn;
        syncBtn.getElement().setAttribute("title", "Synced — click to break sync");
        syncBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        syncBtn.addClickListener(e -> {
            col.toggleSync();
            if (col.isSynced()) {
                syncBtn.setIcon(VaadinIcon.LINK.create());
                syncBtn.getElement().setAttribute("title", "Synced — click to break sync");
                if (col.getSourceId() != null && currentBook != null) {
                    populateVerses(verseContainer, col);
                }
            } else {
                syncBtn.setIcon(VaadinIcon.UNLINK.create());
                syncBtn.getElement().setAttribute("title", "Unsynced — click to re-sync");
            }
        });

        // Remove column
        final Button removeBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        removeBtn.addClickListener(e -> removeColumn(col, colLayout));

        header.add(sourceCombo, modeSelect, syncBtn, removeBtn);
        header.setFlexGrow(1, sourceCombo);
        return header;
    }

    // ── Navigation bar ────────────────────────────────────────────────

    private HorizontalLayout buildNavBar(
             final SourceColumn    col
            , final Div             verseContainer
            , final Select<String>  bookSelect
            , final Select<Integer> chapterSelect) {

        final HorizontalLayout nav = new HorizontalLayout();
        nav.setWidthFull();
        nav.setAlignItems(Alignment.CENTER);
        nav.getStyle()
            .set("padding", "4px 8px")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
            .set("flex-shrink", "0");

        bookSelect.setPlaceholder("Book...");
        bookSelect.setWidthFull();
        bookSelect.addValueChangeListener(e -> {
            if (e.getValue() == null || col.getSourceId() == null) return;
            chapterSelect.setItems(1);
            chapterSelect.setValue(1);
            if (col.isSynced()) {
                currentBook    = e.getValue();
                currentChapter = 1;
                refreshSyncedColumns();
            } else {
                populateVerses(verseContainer, col);
            }
        });

        chapterSelect.setItems(1);
        chapterSelect.setValue(1);
        chapterSelect.getStyle().set("min-width", "65px");
        chapterSelect.addValueChangeListener(e -> {
            if (e.getValue() == null || col.getSourceId() == null
                    || bookSelect.getValue() == null) return;
            if (col.isSynced()) {
                currentChapter = e.getValue();
                refreshSyncedColumns();
            } else {
                populateVerses(verseContainer, col);
            }
        });

        final Button prevBtn = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> {
            final Integer cur = chapterSelect.getValue();
            if (cur != null && cur > 1) chapterSelect.setValue(cur - 1);
        });
        final Button nextBtn = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> {
            final Integer cur = chapterSelect.getValue();
            if (cur != null) chapterSelect.setValue(cur + 1);
        });
        prevBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        nextBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        nav.add(bookSelect, prevBtn, chapterSelect, nextBtn);
        nav.setFlexGrow(1, bookSelect);
        return nav;
    }

    // ── Verse container ───────────────────────────────────────────────

    private Div buildVerseContainer() {
        final Div container = new Div();
        container.setWidthFull();
        container.getStyle().set("padding", "12px 16px");
        container.add(new Paragraph("Select a source and book to begin reading."));
        return container;
    }

    private void populateVerses(final Div container, final SourceColumn col) {
        if (col.getSourceId() == null || currentBook == null) return;

        container.removeAll();

        final List<VerseRef> verses = queryService.getVerses(
             col.getSourceId()
            , currentBook
            , currentChapter
            , col.getDisplayOptions());

        if (verses.isEmpty()) {
            container.add(new Paragraph("No verses found."));
            return;
        }

        final DisplayOptions opts = col.getDisplayOptions();

        for (final VerseRef verse : verses) {

            if (opts.isShowChapters() && verse.getVerseNumber() == 1) {
                final H2 chapterHeading = new H2("Chapter " + verse.getChapterNumber());
                chapterHeading.getStyle()
                    .set("font-size", "16px")
                    .set("margin", "12px 0 4px 0")
                    .set("color", "var(--lumo-secondary-text-color)");
                container.add(chapterHeading);

                if (opts.isShowChapterTitles()
                        && verse.getChapterTitle() != null
                        && !verse.getChapterTitle().isBlank()) {
                    final Span chapterTitle = new Span(verse.getChapterTitle());
                    chapterTitle.getStyle()
                        .set("font-style", "italic")
                        .set("font-size", "13px")
                        .set("color", "var(--lumo-secondary-text-color)")
                        .set("display", "block")
                        .set("margin-bottom", "8px");
                    container.add(chapterTitle);
                }
            }

            final Div verseLine = new Div();
            verseLine.getStyle().set("margin-bottom", "4px").set("line-height", "1.7");

            if (opts.isShowVerses()) {
                final Span verseNum = new Span(verse.getVerseNumber() + " ");
                verseNum.getStyle()
                    .set("font-size", "10px")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("vertical-align", "super")
                    .set("margin-right", "2px");
                verseLine.add(verseNum);
            }

            verseLine.add(new Span(verse.getDisplayContent(opts.isAllCaps())));
            container.add(verseLine);
        }
    }

    // ── Sync helpers ──────────────────────────────────────────────────

    private void refreshSyncedColumns() {
        final List<com.vaadin.flow.component.Component> kids =
            columnsLayout.getChildren().toList();
        for (int i = 0; i < kids.size() && i < columns.size(); i++) {
            final SourceColumn col = columns.get(i);
            if (!col.isSynced() || col.getSourceId() == null) continue;
            final VerticalLayout colLayout = (VerticalLayout) kids.get(i);
            findVerseContainer(colLayout).ifPresent(div -> populateVerses(div, col));
        }
    }

    private void refreshAllColumns() {
        final List<com.vaadin.flow.component.Component> kids =
            columnsLayout.getChildren().toList();
        for (int i = 0; i < kids.size() && i < columns.size(); i++) {
            final SourceColumn col = columns.get(i);
            if (col.getSourceId() == null || currentBook == null) continue;
            final VerticalLayout colLayout = (VerticalLayout) kids.get(i);
            findVerseContainer(colLayout).ifPresent(div -> populateVerses(div, col));
        }
    }

    private java.util.Optional<Div> findVerseContainer(final VerticalLayout colLayout) {
        return colLayout.getChildren()
            .filter(c -> c instanceof Div)
            .map(c -> (Div) c)
            .findFirst();
    }
}
