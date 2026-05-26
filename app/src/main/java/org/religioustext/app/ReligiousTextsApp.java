package org.religioustext.app;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Religious Texts Platform — Vaadin application entry point.
 */
@SpringBootApplication
@Theme(value = "religious-texts", variant = Lumo.LIGHT)
public class ReligiousTextsApp implements AppShellConfigurator {

    public static void main(final String[] args) {
        SpringApplication.run(ReligiousTextsApp.class, args);
    }
}
