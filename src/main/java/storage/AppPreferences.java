package storage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.prefs.Preferences;

public final class AppPreferences {
    public static final String KEY_EXPORT_DIR = "export.dir";
    public static final String KEY_XML_DIR = "xml.dir";
    public static final String KEY_IMPORT_DIR = "import.dir";
    public static final String KEY_REPORT_LOGO_DIR = "report.logo.dir";
    public static final String KEY_LANGUAGE = "ui.language";
    public static final String KEY_REPORT_LOGO_PATH = "report.logo.path";

    private static final Preferences PREFS = Preferences.userRoot().node("/app/invocely");

    private AppPreferences() { }

    public static Path getLastDirectory(String key) {
        String value = PREFS.get(key, null);
        if (value == null || value.isBlank()) {
            return Paths.get(System.getProperty("user.home"));
        }
        return Paths.get(value);
    }

    public static void setLastDirectory(String key, Path dir) {
        if (dir == null) return;
        PREFS.put(key, dir.toString());
    }

    public static String getLanguageTag() {
        return resolveLanguageTag(PREFS.get(KEY_LANGUAGE, null), Locale.getDefault());
    }

    static String resolveLanguageTag(String savedTag, Locale systemLocale) {
        String tag = savedTag;
        if (tag == null || tag.isBlank()) {
            tag = systemLocale != null ? systemLocale.toLanguageTag() : "en-US";
        }
        String normalized = tag.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("ca")) return "ca-ES";
        if (normalized.startsWith("es")) return "es-ES";
        if (normalized.startsWith("en")) return "en-US";
        return "en-US";
    }

    public static void setLanguageTag(String tag) {
        if (tag == null || tag.isBlank()) return;
        PREFS.put(KEY_LANGUAGE, tag);
    }

    public static String getReportLogoPath() {
        String value = PREFS.get(KEY_REPORT_LOGO_PATH, "");
        return value == null ? "" : value.trim();
    }

    public static void setReportLogoPath(Path path) {
        if (path == null) return;
        PREFS.put(KEY_REPORT_LOGO_PATH, path.toAbsolutePath().normalize().toString());
    }

    public static void clearReportLogoPath() {
        PREFS.remove(KEY_REPORT_LOGO_PATH);
    }

}
