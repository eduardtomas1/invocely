package ui;

import java.awt.Color;

/**
 * Simple value object describing the app color palette.
 */
public final class ThemePalette {
    private final Color background;
    private final Color card;
    private final Color border;
    private final Color text;
    private final Color muted;
    private final Color placeholder;
    private final Color accent;
    private final Color tableOdd;
    private final Color tableEven;
    private final Color tableSelected;
    private final Color headerBackground;
    private final Color headerForeground;

    public ThemePalette(
            Color background,
            Color card,
            Color border,
            Color text,
            Color muted,
            Color placeholder,
            Color accent,
            Color tableOdd,
            Color tableEven,
            Color tableSelected,
            Color headerBackground,
            Color headerForeground
    ) {
        this.background = background;
        this.card = card;
        this.border = border;
        this.text = text;
        this.muted = muted;
        this.placeholder = placeholder;
        this.accent = accent;
        this.tableOdd = tableOdd;
        this.tableEven = tableEven;
        this.tableSelected = tableSelected;
        this.headerBackground = headerBackground;
        this.headerForeground = headerForeground;
    }

    public Color background() { return background; }
    public Color card() { return card; }
    public Color border() { return border; }
    public Color text() { return text; }
    public Color muted() { return muted; }
    public Color placeholder() { return placeholder; }
    public Color accent() { return accent; }
    public Color tableOdd() { return tableOdd; }
    public Color tableEven() { return tableEven; }
    public Color tableSelected() { return tableSelected; }
    public Color headerBackground() { return headerBackground; }
    public Color headerForeground() { return headerForeground; }
}
