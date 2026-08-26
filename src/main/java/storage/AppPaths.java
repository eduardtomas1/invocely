package storage;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppPaths {
    private static final String APP_DIR = ".invocely";

    private AppPaths() { }

    public static Path baseDir() {
        return Paths.get(System.getProperty("user.home"), APP_DIR);
    }

    public static Path dataDir() {
        return baseDir().resolve("data");
    }

    public static Path defaultsDir() {
        return baseDir().resolve("defaults");
    }

    public static Path partnersFile() {
        return dataDir().resolve("partners.json");
    }
}
