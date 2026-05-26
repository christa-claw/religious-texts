package org.religioustext.app.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
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
 *   [Toolbar: source selector, display toggles, order mode]
 *   [Column 1 | Column 2 | Column 3 ... | Commentary]
 *
 * Each column is independent but all sync to the same
 * current book/chapter reference.
 */
@Route("")
@PageTitle("Religious Texts Reader")
public class ReaderView extends VerticalLayout {

    private final TextQueryService       queryService;
    private final List<SourceColumn>     columns      = new ArrayList<>();
    private final HorizontalLayout       columnsLayout;
    private final ComboBox<String[]>     sourceSelector;
    private       String                 currentBook;
    private       int                    currentChapter = 1;

    public ReaderView(final TextQueryService queryService) {
        this.queryService  = queryService;
        this.columnsLayout = new HorizontalLayout();
        this.sourceSelector = buildSourceSelector();

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        add(buildToolbar(), columnsLayout);

        columnsLayout.setSizeFull();
        columnsLayout.setSpacing(true);
        columnsLayout.getStyle().set("overflow-x", "auto");
    }

    // ── Toolbar ───────────────────────────────────────────────────────

    private HorizontalLayout buildToolbar() {
        final HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.getStyle()
            .set("padding", "8px 16px")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        final Button addColumnBtn = new Button(
             "Add Column"
            , VaadinIcon.PLUS.create()
            , e -> addColumn());

        toolbar.add(
             sourceSelector
            , addColumnBtn
            , buildOrderModeSelector());

        return toolbar;
    }

    private ComboBox<String[]> buildSourceSelector() {
        final ComboBox<String[]> combo = new ComboBox<>("Source");
        combo.setItemLabelGenerator(arr -> arr[2] + " — " + arr[1]);
        combo.setPlaceholder("Select a translation...");

        final List<String[]> sources = queryService.listSources();
        combo.setItems(sources);

        return combo;
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
        select.addValueChangeListener(e -> refreshAllColumns(e.getValue()));
        return select;
    }

    // ── Column management ─────────────────────────────────────────────

    private void addColumn() {
        final String[] selected = sourceSelector.getValue();
        if (selected == null) {
            Notification.show("Please select a source first");
            return;
        }

        final DisplayOptions opts = DisplayOptions.defaults();
        final SourceColumn col = new SourceColumn(
             selected[0]   // sourceId
            , selected[1]   // translation
            , selected[2]   // abbreviation
            , selected[3]   // direction
            , opts);

        columns.add(col);
        columnsLayout.add(buildColumnComponent(col));
    }

    private VerticalLayout buildColumnComponent(final SourceColumn col) {
        final VerticalLayout colLayout = new VerticalLayout();
        colLayout.setWidth("380px");
        colLayout.setHeightFull();
        colLayout.getStyle()
            .set("border-right", "1px solid var(--lumo-contrast-10pct)")
            .set("overflow-y", "auto");

        if (col.isRtl()) {
            colLayout.getStyle().set("direction", "rtl");
        }

        // Column header with translation name and toggles
        final Div verseContainer = buildVerseContainer(col);
        colLayout.add(
             buildColumnHeader(col, colLayout, verseContainer)
            , buildBookChapterSelector(col, verseContainer)
            , verseContainer);

        return colLayout;
    }

    private HorizontalLayout buildColumnHeader(
             final SourceColumn col
            , final VerticalLayout colLayout
            , final Div verseContainer) {

        final HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
            .set("padding", "4px 8px")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("position", "sticky")
            .set("top", "0");

        final H3 title = new H3(col.getAbbreviation());
        title.getStyle().set("margin", "0").set("font-size", "14px");

        // Single dropdown controls all display structure + CAPS
        final Select<DisplayOptions.DisplayMode> modeSelect = new Select<>();
        modeSelect.setItems(DisplayOptions.DisplayMode.values());
        modeSelect.setValue(DisplayOptions.DisplayMode.CHAPTERS_VERSES);
        modeSelect.setItemLabelGenerator(DisplayOptions.DisplayMode::getLabel);
        modeSelect.setItemEnabledProvider(
            mode -> !mode.isDisabled());
        modeSelect.addValueChangeListener(e -> {
            col.getDisplayOptions().setMode(e.getValue());
            refreshColumn(col, colLayout);
        });

        // Tooltip on the select element showing historical context for current mode
        modeSelect.getElement().setAttribute("title",
            DisplayOptions.DisplayMode.CHAPTERS_VERSES.getTooltip());
        modeSelect.addValueChangeListener(e ->
            modeSelect.getElement().setAttribute("title", e.getValue().getTooltip()));

        final Button removeBtn = new Button(
             VaadinIcon.CLOSE_SMALL.create()
            , e -> removeColumn(col
                , colLayout.getParent().map(p -> (HorizontalLayout) p).orElse(null)
                , colLayout));
        removeBtn.getStyle().set("margin-left", "auto");

        header.add(title, modeSelect, removeBtn);
        return header;
    }

    private HorizontalLayout buildBookChapterSelector(
             final SourceColumn col
            , final Div          verseContainer) {

        final HorizontalLayout nav = new HorizontalLayout();
        nav.setWidthFull();
        nav.setAlignItems(Alignment.CENTER);
        nav.getStyle().set("padding", "4px 8px");

        final List<String> books = queryService.listBooks(col.getSourceId());

        final Select<String> bookSelect = new Select<>();
        bookSelect.setItems(books);
        bookSelect.setPlaceholder("Book...");
        if (!books.isEmpty()) {
            bookSelect.setValue(books.get(0));
            if (currentBook == null) {
                currentBook = books.get(0);
            }
        }

        final Select<Integer> chapterSelect = new Select<>();
        chapterSelect.setItems(1);
        chapterSelect.setValue(1);

        bookSelect.addValueChangeListener(e -> {
            currentBook    = e.getValue();
            currentChapter = 1;
            chapterSelect.setValue(1);
            populateVerses(verseContainer, col);
        });

        chapterSelect.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                currentChapter = e.getValue();
                populateVerses(verseContainer, col);
            }
        });

        final Button prevBtn = new Button(
             VaadinIcon.ANGLE_LEFT.create()
            , e -> {
                if (currentChapter > 1) {
                    currentChapter--;
                    chapterSelect.setValue(currentChapter);
                }
            });

        final Button nextBtn = new Button(
             VaadinIcon.ANGLE_RIGHT.create()
            , e -> {
                currentChapter++;
                chapterSelect.setValue(currentChapter);
            });

        nav.add(bookSelect, prevBtn, chapterSelect, nextBtn);

        // Populate initial content
        if (currentBook != null) {
            populateVerses(verseContainer, col);
        }

        return nav;
    }

    private Div buildVerseContainer(final SourceColumn col) {
        final Div container = new Div();
        container.setWidthFull();
        container.getStyle().set("padding", "8px");
        container.setId("verses-" + col.getSourceId());

        if (currentBook != null) {
            populateVerses(container, col);
        } else {
            container.add(new Paragraph("Select a book to begin reading."));
        }

        return container;
    }

    private void populateVerses(final Div container, final SourceColumn col) {
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

        final DisplayOptions opts    = col.getDisplayOptions();
        String               lastBook = null;

        for (final VerseRef verse : verses) {

            // Book boundary marker (always shown, even in streaming mode)
            if (verse.getBookName() != null && !verse.getBookName().equals(lastBook)) {
                final Div bookMarker = new Div();
                bookMarker.getStyle()
                    .set("border-top", "2px solid var(--lumo-primary-color)")
                    .set("padding-top", "8px")
                    .set("margin-top", "16px")
                    .set("font-weight", "bold")
                    .set("color", "var(--lumo-primary-color)");
                bookMarker.add(new Span(verse.getBookName()));
                container.add(bookMarker);
                lastBook = verse.getBookName();
            }

            // Chapter heading: number always shown if chapters on,
            // title shown below only if showChapterTitles is on
            if (opts.isShowChapters() && verse.getVerseNumber() == 1) {
                final H2 chapterHeading = new H2("Chapter " + verse.getChapterNumber());
                chapterHeading.getStyle()
                    .set("font-size", "16px")
                    .set("margin", "12px 0 0 0")
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
                        .set("margin-bottom", "6px");
                    container.add(chapterTitle);
                }
            }

            // Verse content
            final Div verseLine = new Div();
            verseLine.getStyle().set("margin-bottom", "4px").set("line-height", "1.6");

            if (opts.isShowVerses()) {
                final Span verseNum = new Span(verse.getVerseNumber() + " ");
                verseNum.getStyle()
                    .set("font-size", "10px")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("vertical-align", "super")
                    .set("margin-right", "2px");
                verseLine.add(verseNum);
            }

            final Span verseText = new Span(verse.getDisplayContent(opts.isAllCaps()));
            verseLine.add(verseText);
            container.add(verseLine);
        }
    }

    private void refreshColumn(final SourceColumn col, final VerticalLayout colLayout) {
        colLayout.getChildren()
            .filter(c -> c instanceof Div)
            .map(c -> (Div) c)
            .filter(d -> d.getId().isPresent())
            .findFirst()
            .ifPresent(div -> populateVerses(div, col));
    }

    private void refreshAllColumns(final OrderMode orderMode) {
        columns.forEach(col -> col.getDisplayOptions().setOrderMode(orderMode));
        columnsLayout.getChildren()
            .filter(c -> c instanceof VerticalLayout)
            .forEach(colLayout -> {
                final SourceColumn col = columns.stream()
                    .filter(c -> colLayout.getId()
                        .map(id -> id.equals(c.getSourceId()))
                        .orElse(false))
                    .findFirst()
                    .orElse(null);
                if (col != null) {
                    refreshColumn(col, (VerticalLayout) colLayout);
                }
            });
    }

    private void removeColumn(
             final SourceColumn col
            , final HorizontalLayout parent
            , final VerticalLayout colLayout) {

        columns.remove(col);
        if (parent != null) {
            parent.remove(colLayout);
        }
    }
}
