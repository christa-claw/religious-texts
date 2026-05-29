package org.religioustext.app.ui.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.religioustext.app.model.DisplayOptions;
import org.religioustext.app.model.DisplayOptions.OrderMode;
import org.religioustext.app.model.SourceColumn;
import org.religioustext.app.model.VerseRef;
import org.religioustext.app.service.TextQueryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Route("")
@PageTitle("Religious Texts Reader")
public class ReaderView extends VerticalLayout {

    private static final int MAX_BOOKS_IN_DOM = 3;

    private final TextQueryService   queryService;
    private final List<SourceColumn> columns = new ArrayList<>();
    private final List<ColState>     states  = new ArrayList<>();
    private final List<String[]>     sources;
    private final HorizontalLayout   columnsLayout;

    private String currentBook    = null;
    private int    currentChapter = 1;

    private static final class ColState {
        List<String[]>  books         = new ArrayList<>();
        int             bookIndex     = 0;
        int             firstChapter  = 1;
        int             lastChapter   = 1;
        int             firstBookIdx  = 0;
        int             lastBookIdx   = 0;
        Div             verseContainer;
        Select<String>  bookSelect;
        Select<Integer> chapterSelect;
        SourceColumn    col;

        String currentBookName() {
            return books.isEmpty() ? null : books.get(bookIndex)[0];
        }

        int chapterCountForBook(final int idx) {
            if (idx < 0 || idx >= books.size()) return 0;
            try { return Integer.parseInt(books.get(idx)[1]); }
            catch (final Exception e) { return 1; }
        }
    }

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
        addColumn();
    }

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
        title.getStyle().set("font-weight", "bold").set("font-size", "18px").set("flex-grow", "1");

        final RouterLink aboutLink = new RouterLink("About & Help", AboutView.class);
        aboutLink.getStyle()
            .set("font-size", "14px")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("text-decoration", "none");

        final Select<OrderMode> orderSelect = buildOrderModeSelector();

        final Button addColumnBtn = new Button("Add Column", VaadinIcon.PLUS.create(), e -> addColumn());
        addColumnBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        toolbar.add(title, aboutLink, orderSelect, addColumnBtn);
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

    private void addColumn() {
        final SourceColumn col   = new SourceColumn(DisplayOptions.defaults());
        final ColState     state = new ColState();
        state.col            = col;
        state.verseContainer = buildVerseContainer();
        state.bookSelect     = new Select<>();
        state.chapterSelect  = new Select<>();
        columns.add(col);
        states.add(state);
        buildColumnComponent(col, state); // adds itself to columnsLayout
    }

    private void removeColumn(final SourceColumn col, final ColState state, final VerticalLayout colLayout) {
        final int idx = columns.indexOf(col);
        if (idx >= 0) { columns.remove(idx); states.remove(idx); }
        columnsLayout.remove(colLayout);
    }

    private VerticalLayout buildColumnComponent(final SourceColumn col, final ColState state) {
        final VerticalLayout colLayout = new VerticalLayout();
        colLayout.getStyle()
            .set("width", "33vw").set("min-width", "320px").set("height", "100%")
            .set("border-right", "1px solid var(--lumo-contrast-10pct)")
            .set("overflow-y", "auto").set("padding", "0").set("flex-shrink", "0");
        colLayout.add(buildColumnHeader(col, state, colLayout), buildNavBar(col, state), state.verseContainer);
        colLayout.setFlexGrow(1, state.verseContainer);
        // Set up scroll observer after adding to layout, not via addAttachListener
        columnsLayout.add(colLayout);
        colLayout.getElement().executeJs(
            "$0._scrollSetupDone = true;", colLayout.getElement());
        setupScrollObserver(colLayout, state);
        return colLayout;
    }

    private HorizontalLayout buildColumnHeader(final SourceColumn col, final ColState state,
                                               final VerticalLayout colLayout) {
        final HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
            .set("padding", "6px 8px")
            .set("background", "var(--lumo-contrast-5pct)")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
            .set("position", "sticky").set("top", "0").set("z-index", "1").set("flex-shrink", "0");

        final ComboBox<String[]> sourceCombo = new ComboBox<>();
        sourceCombo.setPlaceholder("Select source...");
        sourceCombo.setItemLabelGenerator(arr -> arr[2] + " — " + arr[1]);
        sourceCombo.setItems(sources);
        sourceCombo.setWidthFull();
        sourceCombo.addValueChangeListener(e -> {
            final String[] sel = e.getValue();
            if (sel == null) return;
            col.setSource(sel[0], sel[1], sel[2], sel[3],
                sel.length > 4 ? sel[4] : null, sel.length > 5 ? sel[5] : null);
            if (col.isRtl()) colLayout.getStyle().set("direction", "rtl");
            else             colLayout.getStyle().remove("direction");
            final List<String[]> books = queryService.listBooksWithChapterCounts(col.getSourceId());
            state.books = books;
            state.bookIndex = state.firstBookIdx = state.lastBookIdx = 0;
            final List<String> names = books.stream().map(b -> b[0]).toList();
            state.bookSelect.setItems(names);
            if (!names.isEmpty()) {
                // Defer value set to avoid Vaadin off-by-one on first render
                getUI().ifPresent(ui -> ui.access(() ->
                    state.bookSelect.setValue(names.get(0))));
            }
            if (sel.length > 5 && sel[5] != null && !sel[5].isBlank()) {
                attributionLinkFor(header).ifPresent(a -> {
                    a.setHref(sel[5]);
                    a.getStyle().set("display", "inline");
                });
            }
        });

        final Select<DisplayOptions.DisplayMode> modeSelect = new Select<>();
        modeSelect.setItems(DisplayOptions.DisplayMode.values());
        modeSelect.setValue(DisplayOptions.DisplayMode.CHAPTERS_VERSES);
        modeSelect.setItemLabelGenerator(DisplayOptions.DisplayMode::getLabel);
        modeSelect.setItemEnabledProvider(mode -> true);
        modeSelect.getStyle().set("min-width", "180px").set("max-width", "190px");
        modeSelect.addValueChangeListener(e -> {
            col.getDisplayOptions().setMode(e.getValue());
            if (col.getSourceId() != null && state.currentBookName() != null) reloadColumn(state);
        });

        final Anchor attributionLink = new Anchor();
        attributionLink.setTarget("_blank");
        attributionLink.getElement().setAttribute("title", "View source & attribution");
        attributionLink.getElement().setProperty("innerHTML", "&#x2139;");
        attributionLink.getStyle()
            .set("font-size", "16px").set("color", "var(--lumo-secondary-text-color)")
            .set("text-decoration", "none").set("padding", "0 4px").set("display", "none");

        final Button syncBtn = new Button(VaadinIcon.LINK.create());
        syncBtn.getElement().setAttribute("title", "Synced — click to break sync");
        syncBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        syncBtn.addClickListener(e -> {
            col.toggleSync();
            if (col.isSynced()) {
                syncBtn.setIcon(VaadinIcon.LINK.create());
                syncBtn.getElement().setAttribute("title", "Synced — click to break sync");
                if (col.getSourceId() != null && currentBook != null) reloadColumn(state);
            } else {
                syncBtn.setIcon(VaadinIcon.UNLINK.create());
                syncBtn.getElement().setAttribute("title", "Unsynced — click to re-sync");
            }
        });

        final Button removeBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        removeBtn.addClickListener(e -> removeColumn(col, state, colLayout));

        header.add(sourceCombo, modeSelect, attributionLink, syncBtn, removeBtn);
        header.setFlexGrow(1, sourceCombo);
        return header;
    }

    private Optional<Anchor> attributionLinkFor(final HorizontalLayout header) {
        return header.getChildren().filter(c -> c instanceof Anchor).map(c -> (Anchor) c).findFirst();
    }

    private HorizontalLayout buildNavBar(final SourceColumn col, final ColState state) {
        final HorizontalLayout nav = new HorizontalLayout();
        nav.setWidthFull();
        nav.setAlignItems(Alignment.CENTER);
        nav.getStyle()
            .set("padding", "4px 8px")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
            .set("flex-shrink", "0");

        state.bookSelect.setPlaceholder("Book...");
        state.bookSelect.setWidthFull();
        state.bookSelect.addValueChangeListener(e -> {
            if (e.getValue() == null || col.getSourceId() == null) return;
            final String bookName = e.getValue();
            state.bookIndex = 0;
            for (int i = 0; i < state.books.size(); i++) {
                if (state.books.get(i)[0].equals(bookName)) { state.bookIndex = i; break; }
            }
            final int chapterCount = state.chapterCountForBook(state.bookIndex);
            final List<Integer> chapters = new ArrayList<>();
            for (int i = 1; i <= chapterCount; i++) chapters.add(i);
            state.chapterSelect.setItems(chapters);
            state.chapterSelect.setValue(1);
            if (col.isSynced()) { currentBook = bookName; currentChapter = 1; navigateSyncedColumns(state); }
            else loadBookFromChapter(state, state.bookIndex, 1);
        });

        state.chapterSelect.setItems(1);
        state.chapterSelect.setValue(1);
        state.chapterSelect.getStyle().set("min-width", "65px");
        state.chapterSelect.addValueChangeListener(e -> {
            if (e.getValue() == null || col.getSourceId() == null
                    || state.bookSelect.getValue() == null || !e.isFromClient()) return;
            final int chapter = e.getValue();
            if (col.isSynced()) { currentChapter = chapter; navigateSyncedColumns(state); }
            else scrollToChapter(state, chapter);
        });

        final Button prevBtn = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> {
            final Integer cur = state.chapterSelect.getValue();
            if (cur == null) return;
            if (cur > 1) { state.chapterSelect.setValue(cur - 1); }
            else if (state.bookIndex > 0) {
                final int prev = state.bookIndex - 1;
                state.bookSelect.setValue(state.books.get(prev)[0]);
                state.chapterSelect.setValue(state.chapterCountForBook(prev));
            }
        });
        final Button nextBtn = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> {
            final Integer cur = state.chapterSelect.getValue();
            if (cur == null) return;
            if (cur < state.chapterCountForBook(state.bookIndex)) { state.chapterSelect.setValue(cur + 1); }
            else if (state.bookIndex < state.books.size() - 1) {
                state.bookSelect.setValue(state.books.get(state.bookIndex + 1)[0]);
                state.chapterSelect.setValue(1);
            }
        });
        prevBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        nextBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        nav.add(state.bookSelect, prevBtn, state.chapterSelect, nextBtn);
        nav.setFlexGrow(1, state.bookSelect);
        return nav;
    }

    private Div buildVerseContainer() {
        final Div container = new Div();
        container.setWidthFull();
        container.getStyle().set("padding", "12px 16px");
        container.add(new Paragraph("Select a source and book to begin reading."));
        return container;
    }

    private void loadBookFromChapter(final ColState state, final int bookIdx, final int fromChapter) {
        if (state.col.getSourceId() == null || state.books.isEmpty()) return;
        if (bookIdx < 0 || bookIdx >= state.books.size()) return;
        state.bookIndex = state.firstBookIdx = state.lastBookIdx = bookIdx;
        state.firstChapter = fromChapter;
        state.lastChapter  = Math.min(fromChapter, state.chapterCountForBook(bookIdx));
        state.verseContainer.removeAll();
        appendChapterContent(state, state.books.get(bookIdx)[0], fromChapter, false);
    }

    private void appendChapterContent(final ColState state, final String bookName,
                                      final int chapter, final boolean prepend) {
        final Div div = buildChapterDiv(state, bookName, chapter);
        if (prepend) state.verseContainer.getElement().insertChild(0, div.getElement());
        else         state.verseContainer.add(div);
    }

    private Div buildChapterDiv(final ColState state, final String bookName, final int chapter) {
        final Div div = new Div();
        div.getStyle().set("padding-bottom", "8px");
        div.getElement().setAttribute("data-book", bookName);
        div.getElement().setAttribute("data-chapter", String.valueOf(chapter));

        final List<VerseRef> verses = queryService.getVerses(
            state.col.getSourceId(), bookName, chapter, state.col.getDisplayOptions());

        if (verses.isEmpty()) { div.add(new Paragraph("No verses found.")); return div; }

        final DisplayOptions opts = state.col.getDisplayOptions();

        if (opts.isAllCaps()) {
            final Div block = new Div();
            block.getStyle().set("line-height", "1.7").set("word-break", "break-all")
                 .set("letter-spacing", "0.05em");
            final StringBuilder sb = new StringBuilder();
            for (final VerseRef v : verses)
                sb.append(v.getDisplayContent(true).replaceAll("[\\s]+", "").replaceAll("[^A-Z0-9]", ""));
            block.add(new Span(sb.toString()));
            div.add(block);
            return div;
        }

        if (opts.isShowChapters()) {
            final H2 heading = new H2("Chapter " + chapter);
            heading.getStyle().set("font-size", "16px").set("margin", "12px 0 4px 0")
                   .set("color", "var(--lumo-secondary-text-color)");
            div.add(heading);
            if (opts.isShowChapterTitles() && !verses.isEmpty()
                    && verses.get(0).getChapterTitle() != null
                    && !verses.get(0).getChapterTitle().isBlank()) {
                final Span ts = new Span(verses.get(0).getChapterTitle());
                ts.getStyle().set("font-style", "italic").set("font-size", "13px")
                  .set("color", "var(--lumo-secondary-text-color)")
                  .set("display", "block").set("margin-bottom", "8px");
                div.add(ts);
            }
        }

        for (final VerseRef verse : verses) {
            final Div line = new Div();
            final String code = verse.getBookCode();
            if (code != null && !code.isBlank())
                line.setId("verse-" + code + "-" + chapter + "-" + verse.getVerseNumber());
            line.getStyle().set("margin-bottom", "4px").set("line-height", "1.7");
            if (opts.isShowVerses()) {
                final Span num = new Span(verse.getVerseNumber() + " ");
                num.getStyle().set("font-size", "10px")
                   .set("color", "var(--lumo-secondary-text-color)")
                   .set("vertical-align", "super").set("margin-right", "2px");
                line.add(num);
            }
            line.add(new Span(verse.getDisplayContent(false)));
            div.add(line);
        }
        return div;
    }

    private void setupScrollObserver(final VerticalLayout colLayout, final ColState state) {
        final Div top    = new Div(); top.setId("top-" + System.identityHashCode(state));
        final Div bottom = new Div(); bottom.setId("bot-" + System.identityHashCode(state));
        top.getStyle().set("height", "1px");
        bottom.getStyle().set("height", "1px");
        state.verseContainer.setId("vc-" + System.identityHashCode(state));
        state.verseContainer.getElement().insertChild(0, top.getElement());
        state.verseContainer.add(bottom);

        top.getElement().addEventListener("load-prev", ev ->
            getUI().ifPresent(ui -> ui.access(() -> loadPreviousChapter(state))));
        bottom.getElement().addEventListener("load-next", ev ->
            getUI().ifPresent(ui -> ui.access(() -> loadNextChapter(state))));
        state.verseContainer.getElement().addEventListener("chapter-visible", ev -> {
            final String ch   = ev.getEventData().getString("event.detail.chapter");
            final String book = ev.getEventData().getString("event.detail.book");
            try {
                final int chapter = Integer.parseInt(ch);
                getUI().ifPresent(ui -> ui.access(() -> {
                    if (state.chapterSelect.getValue() == null || state.chapterSelect.getValue() != chapter)
                        state.chapterSelect.setValue(chapter);
                    if (state.col.isSynced()) {
                        currentBook = book; currentChapter = chapter;
                        notifySyncedColumns(state);
                    }
                }));
            } catch (final NumberFormatException ignored) {}
        }).addEventData("event.detail.chapter").addEventData("event.detail.book");

        getUI().ifPresent(ui -> ui.getPage().executeJs("""
            (function() {
                const vc  = document.getElementById($0);
                const top = document.getElementById($1);
                const bot = document.getElementById($2);
                if (!vc || !top || !bot) return;
                const sObs = new IntersectionObserver(entries => {
                    entries.forEach(e => {
                        if (!e.isIntersecting) return;
                        if (e.target === top) top.dispatchEvent(new CustomEvent('load-prev', {bubbles:true}));
                        else                  bot.dispatchEvent(new CustomEvent('load-next', {bubbles:true}));
                    });
                }, { root: vc, threshold: 0.1 });
                sObs.observe(top); sObs.observe(bot);
                const cObs = new IntersectionObserver(entries => {
                    entries.forEach(e => {
                        if (!e.isIntersecting) return;
                        const ch = e.target.getAttribute('data-chapter');
                        const bk = e.target.getAttribute('data-book');
                        if (ch && bk) vc.dispatchEvent(new CustomEvent('chapter-visible',
                            { bubbles:true, detail: { chapter:ch, book:bk } }));
                    });
                }, { root: vc, threshold: 0.1 });
                vc.querySelectorAll('[data-chapter]').forEach(el => cObs.observe(el));
                new MutationObserver(ms => ms.forEach(m => m.addedNodes.forEach(n => {
                    if (n.nodeType !== 1) return;
                    if (n.hasAttribute?.('data-chapter')) cObs.observe(n);
                    n.querySelectorAll?.('[data-chapter]').forEach(el => cObs.observe(el));
                }))).observe(vc, { childList:true, subtree:true });
            })();
        """, state.verseContainer.getId().orElse(""), top.getId().orElse(""), bottom.getId().orElse("")));
    }

    private void scrollToChapter(final ColState state, final int chapter) {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
            "const el = document.querySelector('[data-chapter=\"' + $0 + '\"]'); if(el) el.scrollIntoView({behavior:'smooth'});",
            String.valueOf(chapter)));
    }

    private void loadNextChapter(final ColState state) {
        if (state.col.getSourceId() == null || state.books.isEmpty()) return;
        final int chapCount = state.chapterCountForBook(state.lastBookIdx);
        if (state.lastChapter < chapCount) {
            appendChapterContent(state, state.books.get(state.lastBookIdx)[0], ++state.lastChapter, false);
        } else if (state.lastBookIdx < state.books.size() - 1) {
            appendChapterContent(state, state.books.get(++state.lastBookIdx)[0], 1, false);
            state.lastChapter = 1;
            trimOldestBook(state);
        }
    }

    private void loadPreviousChapter(final ColState state) {
        if (state.col.getSourceId() == null || state.books.isEmpty()) return;
        if (state.firstChapter > 1) {
            appendChapterContent(state, state.books.get(state.firstBookIdx)[0], --state.firstChapter, true);
        } else if (state.firstBookIdx > 0) {
            final int prevLastChap = state.chapterCountForBook(--state.firstBookIdx);
            appendChapterContent(state, state.books.get(state.firstBookIdx)[0], prevLastChap, true);
            state.firstChapter = prevLastChap;
            trimNewestBook(state);
        }
    }

    private void trimOldestBook(final ColState state) {
        if (state.lastBookIdx - state.firstBookIdx + 1 <= MAX_BOOKS_IN_DOM) return;
        final String old = state.books.get(state.firstBookIdx++)[0];
        state.verseContainer.getChildren()
            .filter(c -> old.equals(c.getElement().getAttribute("data-book")))
            .toList().forEach(state.verseContainer::remove);
        state.firstChapter = 1;
    }

    private void trimNewestBook(final ColState state) {
        if (state.lastBookIdx - state.firstBookIdx + 1 <= MAX_BOOKS_IN_DOM) return;
        final String newest = state.books.get(state.lastBookIdx--)[0];
        state.verseContainer.getChildren()
            .filter(c -> newest.equals(c.getElement().getAttribute("data-book")))
            .toList().forEach(state.verseContainer::remove);
        state.lastChapter = state.chapterCountForBook(state.lastBookIdx);
    }

    private void navigateSyncedColumns(final ColState src) {
        for (final ColState s : states) {
            if (s == src || !s.col.isSynced() || s.col.getSourceId() == null) continue;
            navigateTo(s, currentBook, currentChapter);
        }
    }

    private void notifySyncedColumns(final ColState src) {
        for (final ColState s : states) {
            if (s == src || !s.col.isSynced()) continue;
            if (s.chapterSelect.getValue() == null || s.chapterSelect.getValue() != currentChapter)
                s.chapterSelect.setValue(currentChapter);
        }
    }

    private void navigateTo(final ColState state, final String bookName, final int chapter) {
        if (bookName == null || state.books.isEmpty()) return;
        for (int i = 0; i < state.books.size(); i++) {
            if (state.books.get(i)[0].equals(bookName)) {
                state.bookIndex = i;
                loadBookFromChapter(state, i, chapter);
                state.bookSelect.setValue(bookName);
                state.chapterSelect.setValue(chapter);
                return;
            }
        }
    }

    private void reloadColumn(final ColState state) {
        if (state.col.getSourceId() == null || state.books.isEmpty()) return;
        loadBookFromChapter(state, state.bookIndex, state.firstChapter);
    }

    private void refreshAllColumns() {
        for (final ColState state : states) {
            if (state.col.getSourceId() != null && !state.books.isEmpty()) reloadColumn(state);
        }
    }
}
