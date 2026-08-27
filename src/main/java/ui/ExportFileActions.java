package ui;

import i18n.I18n;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;

/** Native open/reveal actions for a completed export. */
public final class ExportFileActions {
    private ExportFileActions() { }

    public static boolean canOpen() {
        return supports(Desktop.Action.OPEN);
    }

    public static boolean canReveal() {
        return supports(Desktop.Action.BROWSE_FILE_DIR);
    }

    public static void open(Path file) throws IOException {
        if (file == null || !canOpen()) throw new IOException(I18n.t("msg.open_export_unavailable"));
        try {
            Desktop.getDesktop().open(file.toFile());
        } catch (IOException | RuntimeException ex) {
            throw new IOException(I18n.t("msg.open_export_error"), ex);
        }
    }

    public static void reveal(Path file) throws IOException {
        if (file == null || !canReveal()) throw new IOException(I18n.t("msg.reveal_export_unavailable"));
        try {
            Desktop.getDesktop().browseFileDirectory(file.toFile());
        } catch (RuntimeException ex) {
            throw new IOException(I18n.t("msg.reveal_export_error"), ex);
        }
    }

    private static boolean supports(Desktop.Action action) {
        if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported()) return false;
        try {
            return Desktop.getDesktop().isSupported(action);
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
