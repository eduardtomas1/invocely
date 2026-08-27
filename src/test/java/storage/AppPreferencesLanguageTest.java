package storage;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppPreferencesLanguageTest {
    @Test
    void firstRunUsesSupportedSystemLanguage() {
        assertEquals("es-ES", AppPreferences.resolveLanguageTag(null, Locale.forLanguageTag("es-MX")));
        assertEquals("ca-ES", AppPreferences.resolveLanguageTag("", Locale.forLanguageTag("ca-FR")));
        assertEquals("en-US", AppPreferences.resolveLanguageTag(null, Locale.forLanguageTag("fr-FR")));
    }

    @Test
    void savedLanguageOverridesSystemLanguage() {
        assertEquals("en-US", AppPreferences.resolveLanguageTag("en-GB", Locale.forLanguageTag("es-ES")));
        assertEquals("ca-ES", AppPreferences.resolveLanguageTag("ca", Locale.forLanguageTag("en-US")));
    }
}
