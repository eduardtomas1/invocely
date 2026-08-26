package storage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/** Small, dependency-free helpers for bounded reads and crash-safe writes. */
public final class SafeFiles {
    private static final Set<PosixFilePermission> PRIVATE_DIR =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> PRIVATE_FILE =
            PosixFilePermissions.fromString("rw-------");

    private SafeFiles() { }

    @FunctionalInterface
    public interface OutputWriter {
        void write(OutputStream output) throws Exception;
    }

    @FunctionalInterface
    public interface PathWriter {
        void write(Path temporaryFile) throws Exception;
    }

    public static void requireReadableFile(Path file, long maxBytes) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IOException("The selected file does not exist or is not a regular file.");
        }
        long size = Files.size(file);
        if (size > maxBytes) {
            throw new IOException("The selected file is too large (maximum " + humanSize(maxBytes) + ").");
        }
    }

    public static void writeAtomically(Path target, boolean privateFile, OutputWriter writer) throws Exception {
        writePathAtomically(target, privateFile, temporary -> {
            try (FileChannel channel = FileChannel.open(temporary,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                 OutputStream output = Channels.newOutputStream(channel)) {
                writer.write(output);
                output.flush();
                channel.force(true);
            }
        });
    }

    public static void writePathAtomically(Path target, boolean privateFile, PathWriter writer) throws Exception {
        if (target == null) throw new IOException("No target file was selected.");
        Path normalized = target.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        if (parent == null) throw new IOException("The target file has no parent directory.");
        createDirectories(parent, false);

        Path fileNamePath = normalized.getFileName();
        if (fileNamePath == null) throw new IOException("The target file has no file name.");
        String fileName = fileNamePath.toString();
        String prefix = fileName.length() >= 3 ? fileName : "inv" + fileName;
        Path temporary = Files.createTempFile(parent, "." + prefix + "-", ".tmp");
        boolean committed = false;
        try {
            writer.write(temporary);
            if (!Files.isRegularFile(temporary)) {
                throw new IOException("The temporary output file was not created.");
            }
            if (privateFile) setPrivateFilePermissions(temporary);
            moveReplacing(temporary, normalized);
            committed = true;
        } finally {
            if (!committed) Files.deleteIfExists(temporary);
        }
    }

    public static void createDirectories(Path directory, boolean privateDirectory) throws IOException {
        Files.createDirectories(directory);
        if (privateDirectory) {
            try {
                Files.setPosixFilePermissions(directory, PRIVATE_DIR);
            } catch (UnsupportedOperationException ignored) {
                // Windows and some mounted volumes do not expose POSIX permissions.
            }
        }
    }

    public static void protectDirectory(Path directory) throws IOException {
        if (directory == null) return;
        try {
            Files.setPosixFilePermissions(directory, PRIVATE_DIR);
        } catch (UnsupportedOperationException ignored) {
            // Windows and some mounted volumes do not expose POSIX permissions.
        }
    }

    private static void setPrivateFilePermissions(Path file) throws IOException {
        try {
            Files.setPosixFilePermissions(file, PRIVATE_FILE);
        } catch (UnsupportedOperationException ignored) {
            // Windows and some mounted volumes do not expose POSIX permissions.
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String humanSize(long bytes) {
        if (bytes >= 1024L * 1024L) return (bytes / (1024L * 1024L)) + " MB";
        if (bytes >= 1024L) return (bytes / 1024L) + " KB";
        return bytes + " bytes";
    }
}
