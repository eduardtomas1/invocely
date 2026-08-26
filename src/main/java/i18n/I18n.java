package i18n;

import storage.AppPreferences;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class I18n {
    private static final String BASE_NAME = "i18n.messages";
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("en-US");
    private static final ResourceBundle.Control UTF8_CONTROL = new Utf8Control();
    private static Locale locale = DEFAULT_LOCALE;
    private static ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, DEFAULT_LOCALE, UTF8_CONTROL);

    private I18n() { }

    public static void init() {
        setLocale(Locale.forLanguageTag(AppPreferences.getLanguageTag()));
    }

    public static void setLocale(Locale newLocale) {
        locale = normalizeLocale(newLocale);
        Locale.setDefault(locale);
        bundle = ResourceBundle.getBundle(BASE_NAME, locale, UTF8_CONTROL);
    }

    public static Locale getLocale() {
        return locale;
    }

    public static boolean isSpanish() {
        return "es".equalsIgnoreCase(locale.getLanguage());
    }

    public static boolean isEnglish() {
        return "en".equalsIgnoreCase(locale.getLanguage());
    }

    private static Locale normalizeLocale(Locale candidate) {
        if (candidate == null) return DEFAULT_LOCALE;
        String lang = candidate.getLanguage();
        if ("ca".equalsIgnoreCase(lang)) return Locale.forLanguageTag("ca-ES");
        if ("es".equalsIgnoreCase(lang)) return Locale.forLanguageTag("es-ES");
        if ("en".equalsIgnoreCase(lang)) return Locale.forLanguageTag("en-US");
        return DEFAULT_LOCALE;
    }

    public static String t(String key) {
        if (key == null) return "";
        try {
            return bundle.getString(key);
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    public static String t(String key, Object... args) {
        String pattern = t(key);
        if (args == null || args.length == 0) return pattern;
        return String.format(locale, pattern, args);
    }

    private static class Utf8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload) {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                if (stream == null) return null;
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    return new java.util.PropertyResourceBundle(reader);
                }
            } catch (Exception e) {
                return null;
            }
        }
    }
}
