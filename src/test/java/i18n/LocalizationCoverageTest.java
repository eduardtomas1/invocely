package i18n;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocalizationCoverageTest {
    @Test
    void spanishAndCatalanContainEveryEnglishUiKey() throws Exception {
        Properties english = load("messages_en.properties");
        Properties spanish = load("messages_es.properties");
        Properties catalan = load("messages_ca.properties");

        assertEquals(english.stringPropertyNames(), spanish.stringPropertyNames());
        assertEquals(english.stringPropertyNames(), catalan.stringPropertyNames());
    }

    private Properties load(String name) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/i18n/" + name)) {
            assertNotNull(stream);
            Properties properties = new Properties();
            properties.load(new java.io.InputStreamReader(stream, StandardCharsets.UTF_8));
            return properties;
        }
    }
}
