package storage;

import java.io.IOException;
import java.nio.file.Files;
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

    public static void ensurePrivateDirectory(Path directory) throws IOException {
        Path base = baseDir().toAbsolutePath().normalize();
        Path target = directory.toAbsolutePath().normalize();
        if (!target.startsWith(base)) {
            throw new IOException("Refusing to change permissions outside the Invoicely data directory.");
        }
        Files.createDirectories(target);
        Path current = base;
        SafeFiles.protectDirectory(current);
        Path relative = base.relativize(target);
        for (Path part : relative) {
            current = current.resolve(part);
            SafeFiles.protectDirectory(current);
        }
    }
}
