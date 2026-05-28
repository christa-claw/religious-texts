package org.religioustext.app.ui.views;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("about")
@PageTitle("About & Help — Religious Texts Platform")
public class AboutView extends VerticalLayout {

    public AboutView() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("overflow-y", "auto");

        add(
             buildNavBar()
            , buildHero()
            , buildHowToSection()
            , buildDisplayModesSection()
            , buildChapterVerseProblemsSection()
            , buildAvailableTextsSection()
            , buildSourcesSection()
            , buildCommentsSection()
            , buildAboutSection()
            , buildFooter());
    }

    // ── Nav bar ───────────────────────────────────────────────────────

    private HorizontalLayout buildNavBar() {
        final HorizontalLayout nav = new HorizontalLayout();
        nav.setWidthFull();
        nav.setAlignItems(Alignment.CENTER);
        nav.getStyle()
            .set("padding", "12px 32px")
            .set("background", "var(--lumo-base-color)")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
            .set("position", "sticky").set("top", "0").set("z-index", "100");

        final Span logo = new Span("✦ Religious Texts Platform");
        logo.getStyle()
            .set("font-weight", "700").set("font-size", "16px")
            .set("color", "var(--lumo-primary-color)").set("flex-grow", "1");

        final RouterLink readerLink = new RouterLink("Open Reader", ReaderView.class);
        readerLink.getStyle()
            .set("font-size", "14px").set("color", "var(--lumo-primary-color)")
            .set("font-weight", "600").set("text-decoration", "none");

        nav.add(logo, readerLink);
        return nav;
    }

    // ── Hero ──────────────────────────────────────────────────────────

    private Div buildHero() {
        final Div hero = new Div();
        hero.setWidthFull();
        hero.getStyle()
            .set("background", "linear-gradient(135deg, #1a3a5c 0%, #2e6da4 100%)")
            .set("padding", "80px 10% 60px").set("text-align", "center");

        final H1 title = new H1("Study the Texts That Shaped the World");
        title.getStyle()
            .set("color", "white").set("font-size", "clamp(24px, 4vw, 42px)")
            .set("font-weight", "700").set("margin", "0 0 16px 0").set("line-height", "1.2");

        final Paragraph sub = new Paragraph(
            "A free, open reader for exploring Bible translations, the Quran, Hadith, "
            + "and commentaries — side by side, in any language, at any depth.");
        sub.getStyle()
            .set("color", "rgba(255,255,255,0.85)").set("font-size", "clamp(14px, 2vw, 18px)")
            .set("max-width", "640px").set("margin", "0 auto 32px").set("line-height", "1.6");

        final RouterLink openReader = new RouterLink("Open the Reader →", ReaderView.class);
        openReader.getStyle()
            .set("display", "inline-block").set("background", "#c9a84c").set("color", "#1a3a5c")
            .set("font-weight", "700").set("font-size", "16px")
            .set("padding", "12px 28px").set("border-radius", "4px").set("text-decoration", "none");

        hero.add(title, sub, openReader);
        return hero;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Div section(final String background) {
        final Div s = new Div();
        s.setWidthFull();
        s.getStyle().set("padding", "56px 10%").set("background", background);
        return s;
    }

    private H2 sectionTitle(final String text) {
        final H2 h = new H2(text);
        h.getStyle()
            .set("font-size", "26px").set("font-weight", "700").set("color", "#1a3a5c")
            .set("margin", "0 0 8px 0").set("border-bottom", "3px solid #c9a84c")
            .set("padding-bottom", "8px").set("display", "inline-block");
        return h;
    }

    private Paragraph prose(final String text) {
        final Paragraph p = new Paragraph(text);
        p.getStyle().set("color", "#444").set("line-height", "1.8")
         .set("font-size", "15px").set("margin", "0 0 14px 0").set("max-width", "760px");
        return p;
    }

    // ── How to use ────────────────────────────────────────────────────

    private Div buildHowToSection() {
        final Div s = section("white");
        final Div header = new Div();
        header.add(sectionTitle("How to Use the Reader"));
        final Paragraph intro = new Paragraph(
            "The reader is built around columns. Each column shows one text — "
            + "a Bible translation, the Quran, a commentary, or your own notes. "
            + "You can open as many columns as you like and mix traditions freely.");
        intro.getStyle().set("color", "#555").set("margin", "12px 0 32px")
             .set("max-width", "680px").set("line-height", "1.7");
        header.add(intro);
        s.add(header);

        final Div steps = new Div();
        steps.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fit, minmax(220px, 1fr))")
            .set("gap", "24px");

        addStep(steps, "1", "Choose a Source",
            "Click \"Select source...\" in the column header and pick any translation or text from the dropdown.");
        addStep(steps, "2", "Navigate",
            "Use the book selector and chapter arrows to jump anywhere. Or just scroll — the reader loads the next chapter automatically as you reach the bottom.");
        addStep(steps, "3", "Add More Columns",
            "Click \"Add Column\" in the toolbar to open a second or third text alongside the first. Compare translations or traditions side by side.");
        addStep(steps, "4", "Sync or Browse Freely",
            "Columns scroll together by default. Click the 🔗 link icon in any column header to unlink it and browse that column independently.");
        addStep(steps, "5", "Change Display Mode",
            "Switch between Original, Chapters (1227), Verses (1551), and Titles modes per column using the dropdown in the header. See below for what these mean.");
        addStep(steps, "6", "Follow Commentary Links",
            "When a commentary references a verse, click the link — all open Bible columns jump to that passage automatically.");

        s.add(steps);
        return s;
    }

    private void addStep(final Div container, final String num, final String title, final String body) {
        final Div step = new Div();
        step.getStyle()
            .set("background", "#f8fafc").set("border-radius", "8px")
            .set("padding", "20px").set("border-left", "4px solid #1a3a5c");

        final Span number = new Span(num);
        number.getStyle()
            .set("display", "inline-block").set("background", "#1a3a5c").set("color", "white")
            .set("font-weight", "700").set("font-size", "13px")
            .set("width", "24px").set("height", "24px").set("border-radius", "50%")
            .set("text-align", "center").set("line-height", "24px").set("margin-bottom", "10px");

        final H3 stepTitle = new H3(title);
        stepTitle.getStyle()
            .set("font-size", "15px").set("font-weight", "700")
            .set("color", "#1a3a5c").set("margin", "0 0 6px 0");

        final Paragraph stepBody = new Paragraph(body);
        stepBody.getStyle()
            .set("font-size", "13px").set("color", "#666").set("margin", "0").set("line-height", "1.6");

        step.add(number, stepTitle, stepBody);
        container.add(step);
    }

    // ── Display modes ─────────────────────────────────────────────────

    private Div buildDisplayModesSection() {
        final Div s = section("#f0f4f8");
        s.add(sectionTitle("Understanding the Display Modes"));

        final Paragraph intro = prose(
            "The display mode selector in each column header controls how the text is presented. "
            + "Each mode reflects a different layer of editorial history — some ancient, some surprisingly recent. "
            + "Understanding where these divisions come from changes how you read.");
        intro.getStyle().set("margin", "12px 0 32px");
        s.add(intro);

        final Div grid = new Div();
        grid.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fit, minmax(300px, 1fr))")
            .set("gap", "24px");

        addModeCard(grid, "Original",
            "Scriptio Continua",
            "The earliest biblical manuscripts — whether Greek papyri of the New Testament "
            + "or the great Hebrew codices — were written in continuous uppercase script with no spaces, "
            + "no punctuation, no chapter breaks, and no verse numbers. Readers were expected to already "
            + "know the text well enough to parse it.\n\n"
            + "This mode recreates that experience. It is deliberately challenging. Reading it forces you "
            + "to engage with the text as the earliest readers did — as a continuous argument or narrative, "
            + "not a collection of isolated quotable fragments. Many scholars argue this is the most "
            + "honest way to encounter the original intent of the authors.",
            "#8b4513");

        addModeCard(grid, "Chapters (1227)",
            "Added by Stephen Langton",
            "Stephen Langton, an English scholar who became Archbishop of Canterbury, divided "
            + "the Bible into the chapter system still in use today around 1227 AD — more than a "
            + "thousand years after the texts were written.\n\n"
            + "His motivation was practical: scholars needed a consistent way to cite passages in "
            + "theological debates. The chapter divisions were never intended as inspired boundaries. "
            + "Langton worked quickly across an enormous text, and his divisions are sometimes "
            + "excellent and sometimes jarring — cutting mid-argument, mid-poem, or even mid-sentence "
            + "in ways that obscure the author's original flow.",
            "#1a3a5c");

        addModeCard(grid, "Verses (1551)",
            "Added by Robert Estienne",
            "Verse numbers were added to the New Testament in 1551 by Robert Estienne, a French "
            + "printer also known as Stephanus. According to his son, he numbered the verses while "
            + "travelling on horseback from Paris to Lyon — which may explain some of the more "
            + "puzzling divisions.\n\n"
            + "The Old Testament verse system followed shortly after. Like chapters, verses were "
            + "added purely for reference convenience. They have since shaped how billions of people "
            + "read and quote scripture — often in ways that isolate fragments from their context "
            + "in ways the original authors never intended or anticipated.",
            "#2e6da4");

        addModeCard(grid, "Titles",
            "Editorial Section Headings",
            "Section titles — sometimes called pericopes — are editorial headings added by "
            + "publishers and translators to help readers navigate. They are not part of any "
            + "original manuscript.\n\n"
            + "They vary significantly between translations: the NIV uses them extensively, "
            + "while the KJV has none at all. Some titles are helpful summaries; others impose "
            + "a theological interpretation on a passage before you have read a single word. "
            + "Reading without titles first, then with, can be a revealing exercise.",
            "#c9a84c");

        s.add(grid);
        return s;
    }

    private void addModeCard(final Div container, final String mode, final String subtitle,
                             final String body, final String accentColor) {
        final Div card = new Div();
        card.getStyle()
            .set("background", "white").set("border-radius", "8px")
            .set("padding", "24px").set("border-top", "4px solid " + accentColor)
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.06)");

        final Span modeLabel = new Span(mode);
        modeLabel.getStyle()
            .set("display", "inline-block").set("background", accentColor).set("color", "white")
            .set("font-weight", "700").set("font-size", "12px").set("padding", "3px 10px")
            .set("border-radius", "20px").set("margin-bottom", "8px");

        final H3 sub = new H3(subtitle);
        sub.getStyle()
            .set("font-size", "16px").set("font-weight", "700").set("color", "#1a3a5c")
            .set("margin", "0 0 12px 0");

        card.add(modeLabel, sub);

        // Split body on \n\n into paragraphs
        for (final String para : body.split("\n\n")) {
            final Paragraph p = new Paragraph(para.trim());
            p.getStyle().set("font-size", "13px").set("color", "#555")
             .set("line-height", "1.7").set("margin", "0 0 10px 0");
            card.add(p);
        }

        container.add(card);
    }

    // ── Chapter & verse problems ──────────────────────────────────────

    private Div buildChapterVerseProblemsSection() {
        final Div s = section("white");
        s.add(sectionTitle("When Divisions Distort the Text"));

        s.add(prose(
            "Chapter and verse divisions have shaped how billions of people read scripture — "
            + "but they were added by human editors centuries after the texts were written, "
            + "and they sometimes cut across the original argument in ways that genuinely "
            + "change the meaning. The following are among the most significant examples."));

        // Isaiah
        addProblemExample(s,
            "Isaiah 52:13 – 53:12",
            "A Prophecy Split in Two",
            "The chapter break at Isaiah 53 cuts through what is actually one continuous poem — "
            + "often called the fourth Servant Song. The poem begins at Isaiah 52:13 with "
            + "\"See, my servant will act wisely; he will be raised and lifted up and highly exalted.\" "
            + "This opening stanza (52:13–15) sets up the entire passage that follows in chapter 53, "
            + "which Christians read as a prophecy of the crucifixion.\n\n"
            + "Because the chapter division places 52:13–15 in what readers think of as \"Isaiah 52\" "
            + "and the main body in \"Isaiah 53\", the opening verses are frequently overlooked. "
            + "The poem is routinely cited beginning at 53:1, losing its dramatic opening. "
            + "Reading it as one unbroken passage — as the original text presents it — restores "
            + "the full arc of the prophecy.");

        // Romans 7-8
        addProblemExample(s,
            "Romans 7 – 8",
            "The \"Therefore\" That Lost Its \"Because\"",
            "Romans 8 opens with one of the most famous lines in the New Testament: "
            + "\"Therefore, there is now no condemnation for those who are in Christ Jesus.\" "
            + "The word \"therefore\" is doing critical work — it signals that Paul is drawing "
            + "a conclusion from an argument he has just made.\n\n"
            + "But the chapter break separates the conclusion from the argument. "
            + "Readers who begin at chapter 8 encounter a declaration without its proof. "
            + "Paul's argument about the struggle between flesh and spirit in chapter 7 "
            + "is the direct foundation for the assurance in chapter 8. The chapter division "
            + "has contributed to Romans 8:1 being read as a standalone promise rather than "
            + "the climax of a sustained theological argument.");

        // Philippians 4:13
        addProblemExample(s,
            "Philippians 4:13",
            "The Most Misquoted Verse in Scripture",
            "\"I can do all things through Christ who strengthens me\" is one of the most "
            + "widely quoted verses in the Bible — printed on merchandise, cited before "
            + "sporting events, and invoked as a general promise of divine empowerment.\n\n"
            + "In context, Paul is talking about something far more specific and far more "
            + "challenging: contentment in poverty. The surrounding verses read: \"I have "
            + "learned, in whatever state I am, to be content. I know how to be abased, "
            + "and I know how to abound.\" The \"all things\" Paul can do through Christ is "
            + "endure any circumstance — abundance or deprivation — with equanimity. "
            + "The verse division stripped the verse from this context and turned a teaching "
            + "about suffering gracefully into a slogan about achievement.");

        // Jeremiah 29:11
        addProblemExample(s,
            "Jeremiah 29:11",
            "A Promise to Exiles, Not Individuals",
            "\"For I know the plans I have for you, declares the Lord, plans to prosper you "
            + "and not to harm you, plans to give you hope and a future\" is one of the most "
            + "popular verses in contemporary Christian culture, frequently applied as a "
            + "personal divine promise.\n\n"
            + "In context, God is addressing the entire community of Israelites exiled in "
            + "Babylon — and he is telling them something they did not want to hear: that "
            + "the exile will last seventy years. The hopeful verse is embedded in a letter "
            + "telling the exiles to settle in Babylon, build houses, plant gardens, and "
            + "accept that they will not return in their lifetimes. The promise is real — "
            + "but it is collective, long-term, and preceded by decades of suffering. "
            + "Verse division made it easy to extract the comfort while leaving behind the context.");

        // John 11:35
        addProblemExample(s,
            "John 11:35",
            "The Shortest Verse and What It Lost",
            "\"Jesus wept\" is the shortest verse in the Bible — a distinction that has made "
            + "it famous, frequently cited, and widely memorised in isolation.\n\n"
            + "In the original Greek, the verse is part of a sustained emotional scene: "
            + "Jesus arriving at the tomb of Lazarus, seeing Mary weeping, seeing the crowd "
            + "weeping, and being \"deeply moved in spirit and troubled\" — a phrase that in "
            + "the Greek (embrimaomai) carries a sense of anger or agitation, not just sadness. "
            + "Scholars debate whether Jesus wept from grief, from compassion for those grieving, "
            + "or from something more complex. The verse division has reduced a theologically "
            + "rich moment to a touching anecdote about Jesus sharing human emotion.");

        // Broader point
        final Div callout = new Div();
        callout.getStyle()
            .set("background", "#f8fafc").set("border-left", "4px solid #c9a84c")
            .set("border-radius", "4px").set("padding", "20px 24px")
            .set("margin-top", "32px").set("max-width", "760px");

        final H3 calloutTitle = new H3("Proof-Texting and the Verse System");
        calloutTitle.getStyle()
            .set("font-size", "15px").set("font-weight", "700")
            .set("color", "#1a3a5c").set("margin", "0 0 10px 0");

        final Paragraph calloutBody = new Paragraph(
            "The verse system inadvertently created the conditions for \"proof-texting\" — "
            + "the practice of citing an isolated verse to support a theological or moral position "
            + "without reference to its surrounding argument. Both scholarly debate and popular "
            + "culture have been shaped by this. A verse divorced from its context can appear "
            + "to say almost the opposite of what the author intended.\n\n"
            + "This is why the Original and Chapters display modes exist. Reading a chapter or "
            + "book as continuous prose — without the visual interruption of verse numbers — "
            + "often reveals an argument, narrative, or poem that the verse grid has made "
            + "almost invisible.");
        calloutBody.getStyle()
            .set("font-size", "14px").set("color", "#555").set("line-height", "1.8").set("margin", "0");

        callout.add(calloutTitle, calloutBody);
        s.add(callout);

        return s;
    }

    private void addProblemExample(final Div container, final String reference,
                                   final String title, final String body) {
        final Div example = new Div();
        example.getStyle()
            .set("margin-bottom", "32px").set("padding-bottom", "32px")
            .set("border-bottom", "1px solid #e8edf2").set("max-width", "760px");

        final Div header = new Div();
        header.getStyle().set("display", "flex").set("align-items", "baseline")
              .set("gap", "12px").set("margin-bottom", "8px").set("flex-wrap", "wrap");

        final Span ref = new Span(reference);
        ref.getStyle()
            .set("font-family", "monospace").set("font-size", "13px")
            .set("background", "#eef2f7").set("color", "#1a3a5c")
            .set("padding", "2px 8px").set("border-radius", "4px")
            .set("font-weight", "600").set("white-space", "nowrap");

        final H3 h = new H3(title);
        h.getStyle()
            .set("font-size", "17px").set("font-weight", "700")
            .set("color", "#1a3a5c").set("margin", "0");

        header.add(ref, h);
        example.add(header);

        for (final String para : body.split("\n\n")) {
            final Paragraph p = new Paragraph(para.trim());
            p.getStyle().set("font-size", "14px").set("color", "#444")
             .set("line-height", "1.8").set("margin", "0 0 12px 0");
            example.add(p);
        }

        container.add(example);
    }

    // ── Available texts ───────────────────────────────────────────────

    private Div buildAvailableTextsSection() {
        final Div s = section("#f0f4f8");
        s.add(sectionTitle("Available Texts"));

        final Paragraph intro = new Paragraph(
            "The platform currently includes the following texts. More translations and traditions are being added regularly.");
        intro.getStyle().set("color", "#555").set("margin", "12px 0 28px").set("line-height", "1.6");
        s.add(intro);

        final H3 bibleHeader = new H3("Bible Translations");
        bibleHeader.getStyle().set("color", "#1a3a5c").set("margin", "0 0 12px 0").set("font-size", "17px");
        s.add(bibleHeader);

        final Div grid = new Div();
        grid.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fit, minmax(280px, 1fr))")
            .set("gap", "12px").set("margin-bottom", "32px");

        addTextCard(grid, "NIV",   "New International Version", "English",           "2011", "Licensed",      "✅");
        addTextCard(grid, "KJV",   "King James Version",         "English",           "1611", "Public Domain", "✅");
        addTextCard(grid, "ASV",   "American Standard Version",  "English",           "1901", "Public Domain", "✅");
        addTextCard(grid, "WEB",   "World English Bible",        "English",           "—",    "Public Domain", "✅");
        addTextCard(grid, "DRA",   "Douay-Rheims",               "English (Catholic)","1899", "Public Domain", "✅");
        addTextCard(grid, "RVR09", "Reina Valera",               "Spanish",           "1909", "Public Domain", "✅");
        s.add(grid);

        final H3 comingHeader = new H3("Coming Soon");
        comingHeader.getStyle().set("color", "#1a3a5c").set("margin", "0 0 12px 0").set("font-size", "17px");
        s.add(comingHeader);

        final Div coming = new Div();
        coming.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "10px");
        for (final String text : new String[]{
                "Quran (multiple translations)", "Sahih al-Bukhari", "Sahih Muslim",
                "NASB 2020", "NBLA (Spanish)", "Luther Bibel 1912",
                "Westminster Leningrad Codex (Hebrew)", "Greek Textus Receptus",
                "200+ additional translations"}) {
            final Span badge = new Span("⏳ " + text);
            badge.getStyle()
                .set("background", "white").set("border", "1px solid #c9a84c")
                .set("color", "#1a3a5c").set("padding", "4px 12px")
                .set("border-radius", "20px").set("font-size", "13px");
            coming.add(badge);
        }
        s.add(coming);
        return s;
    }

    private void addTextCard(final Div container, final String abbr, final String name,
                             final String language, final String year,
                             final String license, final String status) {
        final Div card = new Div();
        card.getStyle()
            .set("background", "white").set("border-radius", "6px")
            .set("padding", "14px 16px").set("border", "1px solid #e0e8f0")
            .set("display", "flex").set("align-items", "center").set("gap", "12px");

        final Span abbrSpan = new Span(abbr);
        abbrSpan.getStyle()
            .set("background", "#1a3a5c").set("color", "white")
            .set("font-weight", "700").set("font-size", "13px")
            .set("padding", "4px 10px").set("border-radius", "4px").set("white-space", "nowrap");

        final Div info = new Div();
        info.getStyle().set("flex-grow", "1");
        final Span nameSpan = new Span(name);
        nameSpan.getStyle().set("font-weight", "600").set("font-size", "14px")
                .set("color", "#1a3a5c").set("display", "block");
        final Span meta = new Span(language + (year.equals("—") ? "" : " · " + year) + " · " + license);
        meta.getStyle().set("font-size", "12px").set("color", "#888");
        info.add(nameSpan, meta);

        final Span statusSpan = new Span(status);
        statusSpan.getStyle().set("font-size", "16px");

        card.add(abbrSpan, info, statusSpan);
        container.add(card);
    }

    // ── Sources & Attribution ─────────────────────────────────────────

    private Div buildSourcesSection() {
        final Div s = section("white");
        s.add(sectionTitle("Sources & Attribution"));

        final Paragraph intro = new Paragraph(
            "All texts are sourced from reputable public domain or licensed providers. "
            + "We are committed to proper attribution for every text on this platform. "
            + "Where a license permits, the full text is provided freely for personal study.");
        intro.getStyle().set("color", "#555").set("margin", "12px 0 24px")
             .set("max-width", "680px").set("line-height", "1.7");
        s.add(intro);

        final Div sources = new Div();
        sources.getStyle()
            .set("display", "flex").set("flex-direction", "column")
            .set("gap", "12px").set("max-width", "720px");

        addSourceRow(sources, "Bible texts (public domain)",  "wldeh/bible-api",                     "https://github.com/wldeh/bible-api");
        addSourceRow(sources, "NIV, NASB, NBLA (licensed)",  "API.Bible by American Bible Society",  "https://scripture.api.bible");
        addSourceRow(sources, "Quran (coming soon)",          "fawazahmed0/quran-api",                "https://github.com/fawazahmed0/quran-api");
        addSourceRow(sources, "Hadith (coming soon)",         "fawazahmed0/hadith-api",               "https://github.com/fawazahmed0/hadith-api");

        s.add(sources);

        final Paragraph note = new Paragraph(
            "Licensed translations (NIV, NASB, NBLA) are used with permission. "
            + "Reproduction or redistribution of licensed content outside this platform is not permitted. "
            + "Public domain texts may be freely used and shared.");
        note.getStyle()
            .set("color", "#888").set("font-size", "13px")
            .set("margin-top", "24px").set("max-width", "680px").set("line-height", "1.6")
            .set("border-left", "3px solid #e0e8f0").set("padding-left", "12px");
        s.add(note);
        return s;
    }

    private void addSourceRow(final Div container, final String label,
                              final String source, final String url) {
        final Div row = new Div();
        row.getStyle()
            .set("display", "flex").set("align-items", "center")
            .set("gap", "16px").set("padding", "12px 16px")
            .set("background", "#f8fafc").set("border-radius", "6px");

        final Span labelSpan = new Span(label);
        labelSpan.getStyle()
            .set("font-size", "14px").set("color", "#333")
            .set("font-weight", "500").set("flex-grow", "1");

        final Anchor link = new Anchor(url, source);
        link.setTarget("_blank");
        link.getStyle()
            .set("font-size", "13px").set("color", "#2e6da4")
            .set("text-decoration", "none").set("white-space", "nowrap");

        row.add(labelSpan, link);
        container.add(row);
    }

    // ── Comments CTA ──────────────────────────────────────────────────

    private Div buildCommentsSection() {
        final Div s = section("#f0f4f8");

        final Div inner = new Div();
        inner.getStyle()
            .set("background", "linear-gradient(135deg, #1a3a5c, #2e6da4)")
            .set("border-radius", "12px").set("padding", "40px 48px")
            .set("display", "flex").set("align-items", "center")
            .set("gap", "32px").set("flex-wrap", "wrap");

        final Div text = new Div();
        text.getStyle().set("flex-grow", "1");

        final H2 title = new H2("Share Your Insights");
        title.getStyle()
            .set("color", "white").set("font-size", "22px")
            .set("font-weight", "700").set("margin", "0 0 10px 0");

        final Paragraph body = new Paragraph(
            "Create a free account to leave comments and notes on any verse. "
            + "Your annotations stay private by default — share them if you choose. "
            + "Reading and studying the texts is always free, no account needed.");
        body.getStyle()
            .set("color", "rgba(255,255,255,0.85)").set("margin", "0")
            .set("font-size", "14px").set("line-height", "1.6").set("max-width", "480px");

        text.add(title, body);

        final Div buttons = new Div();
        buttons.getStyle().set("display", "flex").set("gap", "12px").set("flex-wrap", "wrap");

        final Anchor register = new Anchor("/register", "Create Free Account");
        register.getStyle()
            .set("display", "inline-block").set("background", "#c9a84c").set("color", "#1a3a5c")
            .set("font-weight", "700").set("font-size", "14px")
            .set("padding", "10px 22px").set("border-radius", "4px")
            .set("text-decoration", "none").set("white-space", "nowrap");

        final Anchor login = new Anchor("/login", "Sign In");
        login.getStyle()
            .set("display", "inline-block").set("background", "transparent").set("color", "white")
            .set("font-weight", "600").set("font-size", "14px")
            .set("padding", "10px 22px").set("border-radius", "4px")
            .set("text-decoration", "none").set("white-space", "nowrap")
            .set("border", "1px solid rgba(255,255,255,0.4)");

        buttons.add(register, login);
        inner.add(text, buttons);
        s.add(inner);
        return s;
    }

    // ── About the project ─────────────────────────────────────────────

    private Div buildAboutSection() {
        final Div s = section("white");
        s.add(sectionTitle("About This Project"));

        s.add(prose(
            "The Religious Texts Platform is an independent project built to make serious "
            + "comparative study of religious texts accessible to everyone — scholars, students, "
            + "and curious readers alike. It is not affiliated with any religious organisation or publisher."));

        s.add(prose(
            "The platform is built on open-source technology: Java, Vaadin, Spring Boot, BaseX "
            + "(a native XML database), and MySQL (for user accounts and comments). "
            + "Source texts from the public domain are available freely. "
            + "Licensed translations are used with permission from their publishers."));

        s.add(prose(
            "New translations, traditions, and features are added continuously. "
            + "Quran translations and Hadith collections are coming next, followed by "
            + "commentary integration and cross-reference links."));

        return s;
    }

    // ── Footer ────────────────────────────────────────────────────────

    private Div buildFooter() {
        final Div footer = new Div();
        footer.setWidthFull();
        footer.getStyle()
            .set("background", "#1a3a5c").set("padding", "24px 10%")
            .set("display", "flex").set("align-items", "center")
            .set("justify-content", "space-between").set("flex-wrap", "wrap").set("gap", "12px");

        final Span copy = new Span("© 2026 Religious Texts Platform · Free to use for personal study");
        copy.getStyle().set("color", "rgba(255,255,255,0.6)").set("font-size", "13px");

        final Div links = new Div();
        links.getStyle().set("display", "flex").set("gap", "20px");
        for (final String[] link : new String[][]{{"Open Reader", "/"}, {"About & Help", "/about"}}) {
            final Anchor a = new Anchor(link[1], link[0]);
            a.getStyle().set("color", "rgba(255,255,255,0.7)").set("font-size", "13px")
             .set("text-decoration", "none");
            links.add(a);
        }

        footer.add(copy, links);
        return footer;
    }
}
