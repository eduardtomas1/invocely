package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public final class AppIcons {
    private static final int SIZE = 28;

    private AppIcons() { }

    public static Icon invoiceIcon() {
        Icon icon = UIManager.getIcon("FileView.fileIcon");
        return icon != null ? icon : new DocumentIcon();
    }

    public static Icon budgetIcon() {
        Icon icon = UIManager.getIcon("FileView.directoryIcon");
        return icon != null ? icon : new FolderIcon();
    }

    public static Icon cancelIcon() {
        Icon icon = UIManager.getIcon("OptionPane.errorIcon");
        return icon != null ? icon : new CancelIcon();
    }

    public static Icon saveIcon() {
        Icon icon = UIManager.getIcon("FileView.floppyDriveIcon");
        return icon != null ? icon : new SaveIcon();
    }

    public static Icon pdfIcon() {
        return pdfIcon(SIZE);
    }

    public static Icon excelIcon() {
        return excelIcon(SIZE);
    }

    public static Icon draftIcon() {
        return draftIcon(SIZE);
    }

    public static Icon pdfIcon(int size) {
        Icon fallback = UIManager.getIcon("FileView.fileIcon");
        if (fallback == null) {
            fallback = new DocumentIcon();
        }
        return loadResourceIcon("/icon/pdfIcon.png", size, fallback);
    }

    public static Icon excelIcon(int size) {
        Icon fallback = UIManager.getIcon("FileView.fileIcon");
        if (fallback == null) {
            fallback = new DocumentIcon();
        }
        return loadResourceIcon("/icon/excelIcon.png", size, fallback);
    }

    public static Icon draftIcon(int size) {
        Icon fallback = UIManager.getIcon("FileView.floppyDriveIcon");
        if (fallback == null) {
            fallback = new SaveIcon();
        }
        return loadResourceIcon("/icon/draftIcon.png", size, fallback);
    }

    public static Icon saveIcon(int size, Color color) {
        return new SaveIcon(size, color);
    }

    private static Color iconColor() {
        Color c = UIManager.getColor("Label.foreground");
        return c != null ? c : new Color(60, 60, 60);
    }

    private static Icon loadResourceIcon(String path, Icon fallback) {
        return loadResourceIcon(path, SIZE, fallback);
    }

    private static Icon loadResourceIcon(String path, int size, Icon fallback) {
        try (InputStream in = AppIcons.class.getResourceAsStream(path)) {
            if (in == null) return fallback;
            BufferedImage raw = ImageIO.read(in);
            if (raw == null) return fallback;
            Image scaled = scaleImage(raw, size, size);
            return new ImageIcon(scaled);
        } catch (IOException ex) {
            return fallback;
        }
    }

    private static Image scaleImage(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(source, 0, 0, width, height, null);
        g2.dispose();
        return scaled;
    }

    private static class DocumentIcon implements Icon {
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(iconColor());
            int w = getIconWidth();
            int h = getIconHeight();
            g2.drawRoundRect(x + 4, y + 3, w - 8, h - 6, 4, 4);
            g2.drawLine(x + 8, y + 9, x + w - 8, y + 9);
            g2.drawLine(x + 8, y + 14, x + w - 8, y + 14);
            g2.dispose();
        }

        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }
    }

    private static class FolderIcon implements Icon {
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(iconColor());
            int w = getIconWidth();
            int h = getIconHeight();
            g2.drawRoundRect(x + 3, y + 7, w - 6, h - 10, 4, 4);
            g2.drawLine(x + 6, y + 7, x + 12, y + 4);
            g2.drawLine(x + 12, y + 4, x + w - 6, y + 4);
            g2.dispose();
        }

        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }
    }

    private static class CancelIcon implements Icon {
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(200, 66, 58));
            int w = getIconWidth();
            int h = getIconHeight();
            g2.drawOval(x + 4, y + 4, w - 8, h - 8);
            g2.drawLine(x + 8, y + 8, x + w - 8, y + h - 8);
            g2.drawLine(x + w - 8, y + 8, x + 8, y + h - 8);
            g2.dispose();
        }

        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }
    }

    private static class SaveIcon implements Icon {
        private final int size;
        private final Color color;

        SaveIcon() {
            this(SIZE, null);
        }

        SaveIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color != null ? color : iconColor());
            int w = size;
            int h = size;
            g2.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 4, 4);
            g2.drawRect(x + 8, y + 6, w - 16, h / 3);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }
}
