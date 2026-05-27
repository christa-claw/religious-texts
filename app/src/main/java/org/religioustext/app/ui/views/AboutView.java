package org.religioustext.app.ui.views;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
            , buildAvailableTextsSection()
            , buildSourcesSection()
            , buildCommentsSection()
            , buildAboutSection()
            , buildFooter());
    }

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
            "Use the book selector and chapter arrows to jump anywhere. Or just scroll — the reader loads the next chapter automatically.");
        addStep(steps, "3", "Add More Columns",
            "Click \"Add Column\" in the toolbar to open a second or third text alongside the first. Compare translations or traditions side by side.");
        addStep(steps, "4", "Sync or Browse Freely",
            "Columns scroll together by default. Click the 🔗 link icon in any column header to unlink it and browse that column independently.");
        addStep(steps, "5", "Change Display Mode",
            "Switch between Original (scriptio continua), Chapters, Verses, and Titles modes per column using the dropdown in the header.");
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

        addSourceRow(sources, "Bible texts (public domain)",  "wldeh/bible-api",             "https://github.com/wldeh/bible-api");
        addSourceRow(sources, "NIV, NASB, NBLA (licensed)",  "API.Bible by American Bible Society", "https://scripture.api.bible");
        addSourceRow(sources, "Quran (coming soon)",          "fawazahmed0/quran-api",        "https://github.com/fawazahmed0/quran-api");
        addSourceRow(sources, "Hadith (coming soon)",         "fawazahmed0/hadith-api",       "https://github.com/fawazahmed0/hadith-api");

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

    private void addSourceRow(final Div container, final String label, final String source, final String url) {
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

    private Div buildAboutSection() {
        final Div s = section("white");
        s.add(sectionTitle("About This Project"));

        final Paragraph p1 = new Paragraph(
            "The Religious Texts Platform is an independent project built to make serious "
            + "comparative study of religious texts accessible to everyone — scholars, students, "
            + "and curious readers alike. It is not affiliated with any religious organisation or publisher.");
        p1.getStyle().set("color", "#555").set("margin", "12px 0 12px")
           .set("max-width", "680px").set("line-height", "1.7");

        final Paragraph p2 = new Paragraph(
            "The platform is built on open-source technology: Java, Vaadin, Spring Boot, BaseX "
            + "(a native XML database), and MySQL (for user accounts and comments). "
            + "Source texts from the public domain are available freely. "
            + "Licensed translations are used with permission from their publishers.");
        p2.getStyle().set("color", "#555").set("margin", "0 0 12px")
           .set("max-width", "680px").set("line-height", "1.7");

        final Paragraph p3 = new Paragraph(
            "New translations, traditions, and features are added continuously. "
            + "Quran translations and Hadith collections are coming next, followed by "
            + "commentary integration and cross-reference links.");
        p3.getStyle().set("color", "#555").set("margin", "0")
           .set("max-width", "680px").set("line-height", "1.7");

        s.add(p1, p2, p3);
        return s;
    }

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
