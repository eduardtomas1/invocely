package ui;

import i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExportFileActionsTest {
    @AfterEach
    void restoreLocale() {
        I18n.setLocale(Locale.forLanguageTag("en-US"));
    }

    @Test
    void headlessModeHidesNativeActionsAndUsesLocalizedFallback() {
        I18n.setLocale(Locale.forLanguageTag("es-ES"));

        assertFalse(ExportFileActions.canOpen());
        assertFalse(ExportFileActions.canReveal());
        IOException error = assertThrows(IOException.class, () -> ExportFileActions.open(null));
        assertEquals(I18n.t("msg.open_export_unavailable"), error.getMessage());
    }
}
