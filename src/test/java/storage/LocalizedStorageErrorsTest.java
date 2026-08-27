package storage;

import i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalizedStorageErrorsTest {
    @TempDir Path tempDir;

    @AfterEach
    void restoreLocale() {
        I18n.setLocale(Locale.forLanguageTag("en-US"));
    }

    @Test
    void draftTypeErrorsUseTheActiveLanguage() throws Exception {
        I18n.setLocale(Locale.forLanguageTag("ca-ES"));
        Path quote = tempDir.resolve("quote.xml");
        Files.writeString(quote, "<?xml version=\"1.0\"?><pressupost/>", StandardCharsets.UTF_8);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> new XmlSaver(tempDir).loadInvoice(quote));

        assertEquals(I18n.t("xml.not_invoice"), error.getMessage());
    }
}
