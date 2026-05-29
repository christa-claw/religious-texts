package org.religioustext.app.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
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

@Route("")
@PageTitle("Religious Texts Reader")
public class ReaderView extends VerticalLayout {

    private static final int MAX_BOOKS_IN_DOM = 3;

    private final TextQueryService   queryService;
    private final List<SourceColumn> columns = new ArrayList<>();
    private final List<ColState>     states  = new ArrayList<>();
    private final List<String[]>     sources;
    private final Div                columnsLayout;

    private String currentBook    = null;
    private int    currentChapter = 1;

    private static final class ColState {
        List<String[]>  books        = new ArrayList<>();
        int             bookIndex    = 0;
        int             firstChapter = 1;
        int             lastChapter  = 1;
        int             firstBookIdx = 0;
        int             lastBookIdx  = 0;
        Div             scrollRoot;   // the column's scroll container
        Div             content;      // verse content area inside scrollRoot
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
        this.columnsLayout = new Div();

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        columnsLayout.getStyle()
            .set("display", "flex")
            .set("flex-direction", "row")
            .set("flex", "1")
            .set("overflow-x", "auto")
            .set("overflow-y", "hidden")
            .set("min-height", "0");

        add(buildToolbar(), columnsLayout);
        setFlexGrow(1, columnsLayout);
        addColumn();
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────

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

        final Select<OrderMode> orderSelect = new Select<>();
        orderSelect.setItems(OrderMode.values());
        orderSelect.setValue(OrderMode.CANONICAL);
        orderSelect.setItemLabelGenerator(m -> switch (m) {
            case CANONICAL     -> "Canonical";
            case CHRONOLOGICAL -> "Chronological";
            case NARRATIVE     -> "Narrative";
        });
        orderSelect.addValueChangeListener(e -> {
            columns.forEach(col -> col.getDisplayOptions().setOrderMode(e.getValue()));
            refreshAllColumns();
        });

        final Button addBtn = new Button("Add Column", VaadinIcon.PLUS.create(), e -> addColumn());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        toolbar.add(title, aboutLink, orderSelect, addBtn);
        return toolbar;
    }

    // ── Column lifecycle ──────────────────────────────────────────────────────

    private void addColumn() {
        final SourceColumn col   = new SourceColumn(DisplayOptions.defaults());
        final ColState     state = new ColState();
        state.col         = col;
        state.bookSelect  = new Select<>();
        state.chapterSelect = new Select<>();
        columns.add(col);
        states.add(state);

        // scrollRoot is the actual scrolling column div
        final Div scrollRoot = new Div();
        final String colId = "col-" + System.identityHashCode(state);
        scrollRoot.setId(colId);
        scrollRoot.getStyle()
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("width", "33vw")
            .set("min-width", "320px")
            .set("height", "100%")
            .set("overflow-y", "auto")
            .set("overflow-x", "hidden")
            .set("flex-shrink", "0")
            .set("border-right", "1px solid var(--lumo-contrast-10pct)")
            .set("box-sizing", "border-box");
        state.scrollRoot = scrollRoot;

        // sticky header
        final Div header = buildHeader(col, state, scrollRoot);
        header.getStyle()
            .set("position", "sticky")
            .set("top", "0")
            .set("z-index", "10")
            .set("flex-shrink", "0")
            .set("background", "var(--lumo-base-color)");

        // nav bar
        final Div nav = buildNavBar(col, state);
        nav.getStyle().set("flex-shrink", "0");

        // content area
        final Div content = new Div();
        content.getStyle()
            .set("padding", "12px 16px")
            .set("flex", "1")
            .set("box-sizing", "border-box");
        content.add(new Span("Select a source and book to begin reading."));
        state.content = content;

        scrollRoot.add(header, nav, content);
        columnsLayout.add(scrollRoot);

        // set up scroll sentinels after attach
        scrollRoot.addAttachListener(e -> setupScrollObserver(state));
    }

    private void removeColumn(final ColState state) {
        final int idx = states.indexOf(state);
        if (idx >= 0) { columns.remove(idx); states.remove(idx); }
        columnsLayout.remove(state.scrollRoot);
    }

    // ── Column header (source + mode selectors) ────────────────────────────

    private Div buildHeader(final SourceColumn col, final ColState state, final Div scrollRoot) {
        final Div header = new Div();
        header.getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("gap", "4px")
            .set("padding", "6px 8px")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        final ComboBox<String[]> sourceCombo = new ComboBox<>();
        sourceCombo.setPlaceholder("Select source...");
        sourceCombo.setItemLabelGenerator(arr -> arr[2] + " \u2014 " + arr[1]);
        sourceCombo.setItems(sources);
        sourceCombo.getStyle().set("flex", "1").set("min-width", "0");
        sourceCombo.addValueChangeListener(e -> {
            final String[] sel = e.getValue();
            if (sel == null) return;
            col.setSource(sel[0], sel[1], sel[2], sel[3],
                sel.length > 4 ? sel[4] : null,
                sel.length > 5 ? sel[5] : null);
            if (col.isRtl()) scrollRoot.getStyle().set("direction", "rtl");
            else             scrollRoot.getStyle().remove("direction");
            final List<String[]> books = queryService.listBooksWithChapterCounts(col.getSourceId());
            state.books = books;
            state.bookIndex = state.firstBookIdx = state.lastBookIdx = 0;
            final List<String> names = books.stream().map(b -> b[0]).toList();
            state.bookSelect.setItems(names);
            if (!names.isEmpty()) {
                state.bookSelect.setValue(names.get(0));
                populateChapters(state, 0);
                loadBookFromChapter(state, 0, 1);
            }
        });

        final Select<DisplayOptions.DisplayMode> modeSelect = new Select<>();
        modeSelect.setItems(DisplayOptions.DisplayMode.values());
        modeSelect.setValue(DisplayOptions.DisplayMode.CHAPTERS_VERSES);
        modeSelect.setItemLabelGenerator(DisplayOptions.DisplayMode::getLabel);
        modeSelect.getStyle().set("min-width", "175px").set("flex-shrink", "0");
        modeSelect.addValueChangeListener(e -> {
            col.getDisplayOptions().setMode(e.getValue());
            if (col.getSourceId() != null && state.currentBookName() != null) reloadColumn(state);
        });

        final Button syncBtn = new Button(VaadinIcon.LINK.create());
        syncBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        syncBtn.getElement().setAttribute("title", "Synced");
        syncBtn.addClickListener(e -> {
            col.toggleSync();
            syncBtn.setIcon(col.isSynced() ? VaadinIcon.LINK.create() : VaadinIcon.UNLINK.create());
            if (col.isSynced() && col.getSourceId() != null && currentBook != null) reloadColumn(state);
        });

        final Button removeBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        removeBtn.addClickListener(e -> removeColumn(state));

        header.add(sourceCombo, modeSelect, syncBtn, removeBtn);
        return header;
    }

    // ── Nav bar (book + chapter selectors) ───────────────────────────────────

    private Div buildNavBar(final SourceColumn col, final ColState state) {
        final Div nav = new Div();
        nav.getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("gap", "4px")
            .set("padding", "4px 8px")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        state.bookSelect.setPlaceholder("Book...");
        state.bookSelect.getStyle().set("flex", "1").set("min-width", "0");
        state.bookSelect.addValueChangeListener(e -> {
            if (e.getValue() == null || col.getSourceId() == null) return;
            final String bookName = e.getValue();
            for (int i = 0; i < state.books.size(); i++) {
                if (state.books.get(i)[0].equals(bookName)) { state.bookIndex = i; break; }
            }
            populateChapters(state, state.bookIndex);
            if (col.isSynced()) { currentBook = bookName; currentChapter = 1; syncOtherColumns(state); }
            else loadBookFromChapter(state, state.bookIndex, 1);
        });

        state.chapterSelect.setItems(List.of(1));
        state.chapterSelect.setValue(1);
        state.chapterSelect.getStyle().set("min-width", "70px").set("flex-shrink", "0");
        state.chapterSelect.addValueChangeListener(e -> {
            if (e.getValue() == null || !e.isFromClient() || col.getSourceId() == null) return;
            final int chapter = e.getValue();
            if (col.isSynced()) { currentChapter = chapter; syncOtherColumns(state); }
            else scrollToChapter(chapter);
        });

        final Button prev = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> {
            final Integer cur = state.chapterSelect.getValue();
            if (cur == null) return;
            if (cur > 1) state.chapterSelect.setValue(cur - 1);
            else if (state.bookIndex > 0) {
                state.bookSelect.setValue(state.books.get(state.bookIndex - 1)[0]);
                state.chapterSelect.setValue(state.chapterCountForBook(state.bookIndex));
            }
        });
        final Button next = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> {
            final Integer cur = state.chapterSelect.getValue();
            if (cur == null) return;
            if (cur < state.chapterCountForBook(state.bookIndex)) state.chapterSelect.setValue(cur + 1);
            else if (state.bookIndex < state.books.size() - 1) {
                state.bookSelect.setValue(state.books.get(state.bookIndex + 1)[0]);
            }
        });
        prev.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        next.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        nav.add(state.bookSelect, prev, state.chapterSelect, next);
        return nav;
    }

    // ── Content loading ───────────────────────────────────────────────────────

    private void populateChapters(final ColState state, final int bookIdx) {
        final int count = state.chapterCountForBook(bookIdx);
        final List<Integer> chs = new ArrayList<>();
        for (int i = 1; i <= count; i++) chs.add(i);
        state.chapterSelect.setItems(chs);
        state.chapterSelect.setValue(1);
    }

    private void loadBookFromChapter(final ColState state, final int bookIdx, final int fromChapter) {
        if (state.col.getSourceId() == null || state.books.isEmpty()) return;
        if (bookIdx < 0 || bookIdx >= state.books.size()) return;
        state.bookIndex = state.firstBookIdx = state.lastBookIdx = bookIdx;
        state.firstChapter = state.lastChapter = fromChapter;
        // clear content but keep sentinels (first and last child)
        clearContent(state);
        appendChapter(state, state.books.get(bookIdx)[0], fromChapter, false);
    }

    private void clearContent(final ColState state) {
        state.content.getChildren()
            .filter(c -> c.getElement().hasAttribute("data-chapter"))
            .toList()
            .forEach(state.content::remove);
        // also remove placeholder span
        state.content.getChildren()
            .filter(c -> c instanceof Span && !c.getElement().hasAttribute("id"))
            .toList()
            .forEach(state.content::remove);
    }

    private void appendChapter(final ColState state, final String book,
                                final int chapter, final boolean prepend) {
        final Div div = buildChapterDiv(state, book, chapter);
        if (prepend) state.content.getElement().insertChild(0, div.getElement());
        else         state.content.add(div);
    }

    private Div buildChapterDiv(final ColState state, final String bookName, final int chapter) {
        final Div div = new Div();
        div.getElement().setAttribute("data-book", bookName);
        div.getElement().setAttribute("data-chapter", String.valueOf(chapter));
        div.getStyle().set("padding-bottom", "12px");

        final List<VerseRef> verses = queryService.getVerses(
            state.col.getSourceId(), bookName, chapter, state.col.getDisplayOptions());

        if (verses.isEmpty()) {
            div.add(new Span("No verses found."));
            return div;
        }

        final DisplayOptions opts = state.col.getDisplayOptions();

        if (opts.isAllCaps()) {
            final Div block = new Div();
            block.getStyle().set("line-height", "1.7").set("letter-spacing", "0.05em");
            final StringBuilder sb = new StringBuilder();
            for (final VerseRef v : verses)
                sb.append(v.getDisplayContent(true).replaceAll("\\s+", "").replaceAll("[^A-Z0-9]", ""));
            block.add(new Span(sb.toString()));
            div.add(block);
            return div;
        }

        if (opts.isShowChapters()) {
            final String title = !verses.isEmpty()
                && verses.get(0).getChapterTitle() != null
                && !verses.get(0).getChapterTitle().isBlank()
                ? verses.get(0).getChapterTitle() : "Chapter " + chapter;
            final H2 h = new H2(title);
            h.getStyle().set("font-size", "15px").set("margin", "8px 0 4px 0")
             .set("color", "var(--lumo-secondary-text-color)");
            div.add(h);
        }

        final Div text = new Div();
        text.getStyle()
            .set("line-height", "1.8")
            .set("text-align", "justify")
            .set("overflow-wrap", "break-word");

        for (final VerseRef verse : verses) {
            if (opts.isShowVerses()) {
                final Span num = new Span(verse.getVerseNumber() + " ");
                num.getStyle()
                    .set("font-size", "10px")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("vertical-align", "super");
                final String code = verse.getBookCode();
                if (code != null && !code.isBlank())
                    num.setId("v-" + code + "-" + chapter + "-" + verse.getVerseNumber());
                text.add(num);
            }
            text.add(new Span(verse.getDisplayContent(false) + " "));
        }
        div.add(text);
        return div;
    }

    // ── Scroll observer ───────────────────────────────────────────────────────

    private void setupScrollObserver(final ColState state) {
        final String vcId  = "vc-"  + System.identityHashCode(state);
        final String topId = "top-" + System.identityHashCode(state);
        final String botId = "bot-" + System.identityHashCode(state);
        final String clId  = state.scrollRoot.getId().orElse("");

        state.content.setId(vcId);

        // Insert sentinel divs inside content
        final Div top = new Div(); top.setId(topId); top.getStyle().set("height", "1px");
        final Div bot = new Div(); bot.setId(botId); bot.getStyle().set("height", "1px");
        state.content.getElement().insertChild(0, top.getElement());
        state.content.add(bot);

        top.getElement().addEventListener("load-prev", ev ->
            getUI().ifPresent(ui -> ui.access(() -> loadPrev(state))));
        bot.getElement().addEventListener("load-next", ev ->
            getUI().ifPresent(ui -> ui.access(() -> loadNext(state))));

        state.content.getElement().addEventListener("chapter-visible", ev -> {
            final String ch   = ev.getEventData().getString("event.detail.chapter");
            final String book = ev.getEventData().getString("event.detail.book");
            try {
                final int c = Integer.parseInt(ch);
                getUI().ifPresent(ui -> ui.access(() -> {
                    if (!Integer.valueOf(c).equals(state.chapterSelect.getValue()))
                        state.chapterSelect.setValue(c);
                    if (state.col.isSynced()) {
                        currentBook = book; currentChapter = c;
                        notifyOtherColumns(state);
                    }
                }));
            } catch (final NumberFormatException ignored) {}
        }).addEventData("event.detail.chapter").addEventData("event.detail.book");

        getUI().ifPresent(ui -> ui.getPage().executeJs("""
            (function() {
                const root = document.getElementById($0);
                const vc   = document.getElementById($1);
                const top  = document.getElementById($2);
                const bot  = document.getElementById($3);
                if (!root || !vc || !top || !bot) {
                    console.warn('ScrollObserver missing:', $0, $1, $2, $3);
                    return;
                }
                // root is the scrolling element (overflow-y:auto)
                const sentinel = new IntersectionObserver(entries => {
                    entries.forEach(e => {
                        if (!e.isIntersecting) return;
                        if (e.target === top) top.dispatchEvent(new CustomEvent('load-prev',{bubbles:true}));
                        else                  bot.dispatchEvent(new CustomEvent('load-next',{bubbles:true}));
                    });
                }, { root, rootMargin: '200px', threshold: 0 });
                sentinel.observe(top);
                sentinel.observe(bot);

                const chapterVis = new IntersectionObserver(entries => {
                    entries.forEach(e => {
                        if (!e.isIntersecting) return;
                        const ch = e.target.getAttribute('data-chapter');
                        const bk = e.target.getAttribute('data-book');
                        if (ch && bk) vc.dispatchEvent(new CustomEvent('chapter-visible',
                            {bubbles:true, detail:{chapter:ch,book:bk}}));
                    });
                }, { root, threshold: 0.1 });
                vc.querySelectorAll('[data-chapter]').forEach(el => chapterVis.observe(el));
                new MutationObserver(ms => ms.forEach(m => m.addedNodes.forEach(n => {
                    if (n.nodeType !== 1) return;
                    if (n.getAttribute?.('data-chapter')) chapterVis.observe(n);
                    n.querySelectorAll?.('[data-chapter]').forEach(el => chapterVis.observe(el));
                }))).observe(vc, {childList:true, subtree:true});
                console.log('ScrollObserver ready on', $0);
            })();
        """, clId, vcId, topId, botId));
    }

    private void scrollToChapter(final int chapter) {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
            "const el=document.querySelector('[data-chapter=\"'+$0+'\"]');if(el)el.scrollIntoView({behavior:'smooth'});",
            String.valueOf(chapter)));
    }

    private void loadNext(final ColState state) {
        if (state.col.getSourceId() == null || state.books.isEmpty()) return;
        if (state.lastChapter < state.chapterCountForBook(state.lastBookIdx)) {
            appendChapter(state, state.books.get(state.lastBookIdx)[0], ++state.lastChapter, false);
        } else if (state.lastBookIdx < state.books.size() - 1) {
            state.lastBookIdx++;
            appendChapter(state, state.books.get(state.lastBookIdx)[0], 1, false);
            state.lastChapter = 1;
            trimOldest(state);
        }
    }

    private void loadPrev(final ColState state) {
        if (state.col.getSourceId() == null || state.books.isEmpty()) return;
        if (state.firstChapter > 1) {
            appendChapter(state, state.books.get(state.firstBookIdx)[0], --state.firstChapter, true);
        } else if (state.firstBookIdx > 0) {
            state.firstBookIdx--;
            final int last = state.chapterCountForBook(state.firstBookIdx);
            appendChapter(state, state.books.get(state.firstBookIdx)[0], last, true);
            state.firstChapter = last;
            trimNewest(state);
        }
    }

    private void trimOldest(final ColState state) {
        if (state.lastBookIdx - state.firstBookIdx < MAX_BOOKS_IN_DOM) return;
        final String book = state.books.get(state.firstBookIdx++)[0];
        state.content.getChildren()
            .filter(c -> book.equals(c.getElement().getAttribute("data-book")))
            .toList().forEach(state.content::remove);
        state.firstChapter = 1;
    }

    private void trimNewest(final ColState state) {
        if (state.lastBookIdx - state.firstBookIdx < MAX_BOOKS_IN_DOM) return;
        final String book = state.books.get(state.lastBookIdx--)[0];
        state.content.getChildren()
            .filter(c -> book.equals(c.getElement().getAttribute("data-book")))
            .toList().forEach(state.content::remove);
        state.lastChapter = state.chapterCountForBook(state.lastBookIdx);
    }

    // ── Sync ─────────────────────────────────────────────────────────────────

    private void syncOtherColumns(final ColState src) {
        for (final ColState s : states) {
            if (s == src || !s.col.isSynced() || s.col.getSourceId() == null) continue;
            navigateTo(s, currentBook, currentChapter);
        }
    }

    private void notifyOtherColumns(final ColState src) {
        for (final ColState s : states) {
            if (s == src || !s.col.isSynced()) continue;
            if (!Integer.valueOf(currentChapter).equals(s.chapterSelect.getValue()))
                s.chapterSelect.setValue(currentChapter);
        }
    }

    private void navigateTo(final ColState state, final String bookName, final int chapter) {
        if (bookName == null || state.books.isEmpty()) return;
        for (int i = 0; i < state.books.size(); i++) {
            if (state.books.get(i)[0].equals(bookName)) {
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
        for (final ColState state : states)
            if (state.col.getSourceId() != null && !state.books.isEmpty()) reloadColumn(state);
    }
}
