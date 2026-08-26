package ui;

import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Applies a single light theme plus custom palette defaults.
 */
public final class ThemeManager {

    private static final ThemePalette LIGHT = new ThemePalette(
            new Color(245, 246, 248),
            new Color(252, 253, 255),
            new Color(214, 220, 228),
            new Color(32, 35, 39),
            new Color(118, 126, 136),
            new Color(130, 136, 145),
            new Color(64, 126, 255),
            new Color(248, 250, 252),
            new Color(252, 253, 255),
            new Color(211, 224, 253),
            new Color(236, 240, 244),
            new Color(32, 35, 39)
    );

    private ThemeManager() { }

    /**
     * Call once before building the UI.
     */
    public static void bootstrap() {
        installLookAndFeel();
        applyDefaults();
    }

    /**
     * Refreshes look and feel defaults and updates a root component tree.
     */
    public static void apply(JFrame root) {
        installLookAndFeel();
        applyDefaults();
        if (root != null) {
            SwingUtilities.updateComponentTreeUI(root);
        }
        refreshOpenWindows();
    }

    public static ThemePalette palette() { return LIGHT; }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatMacLightLaf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void applyDefaults() {
        ThemePalette p = palette();
        Color accent = p.accent();
        Color card = p.card();
        Color background = p.background();
        Color menuBar = blend(background, card, 0.18f);
        Color menuSelection = blend(menuBar, accent, 0.18f);
        Color fieldSelection = blend(card, accent, 0.26f);
        Color listSelection = blend(card, accent, 0.22f);
        UIManager.put("defaultFont", resolveBaseFont());
        UIManager.put("Component.arc", 16);
        UIManager.put("Button.arc", 16);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Panel.arc", 16);
        UIManager.put("ScrollBar.thumbArc", 14);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.innerFocusWidth", 1);
        UIManager.put("Component.focusColor", accent);
        UIManager.put("App.accent", accent);
        UIManager.put("ScrollBar.width", 12);

        UIManager.put("Panel.background", background);
        UIManager.put("App.cardColor", p.card());
        UIManager.put("Component.borderColor", p.border());
        UIManager.put("Label.foreground", p.text());
        UIManager.put("Label.disabledForeground", p.muted());
        UIManager.put("Menu.foreground", p.text());
        UIManager.put("MenuItem.foreground", p.text());
        UIManager.put("CheckBoxMenuItem.foreground", p.text());
        UIManager.put("MenuBar.background", menuBar);
        UIManager.put("MenuBar.selectionBackground", menuSelection);
        UIManager.put("MenuBar.selectionForeground", p.text());
        UIManager.put("MenuBar.margin", new Insets(6, 10, 6, 10));
        UIManager.put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, p.border()));
        UIManager.put("Menu.background", card);
        UIManager.put("MenuItem.background", card);
        UIManager.put("CheckBoxMenuItem.background", card);
        UIManager.put("MenuItem.selectionBackground", menuSelection);
        UIManager.put("MenuItem.selectionForeground", p.text());
        UIManager.put("Menu.selectionBackground", menuSelection);
        UIManager.put("Menu.selectionForeground", p.text());
        UIManager.put("MenuItem.acceleratorForeground", p.muted());
        UIManager.put("MenuItem.acceleratorSelectionForeground", p.text());
        UIManager.put("MenuItem.disabledForeground", p.muted());
        UIManager.put("MenuItem.margin", new Insets(6, 12, 6, 12));
        UIManager.put("Menu.margin", new Insets(4, 10, 4, 10));
        UIManager.put("PopupMenu.background", card);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(p.border()));
        UIManager.put("Button.foreground", p.text());
        UIManager.put("Button.background", card);
        UIManager.put("Button.hoverBackground", blend(card, accent, 0.12f));
        UIManager.put("Button.pressedBackground", blend(card, accent, 0.18f));
        UIManager.put("Button.disabledText", p.muted());
        UIManager.put("ToolBar.background", p.background());
        UIManager.put("ToolBar.foreground", p.text());
        UIManager.put("ToolBar.borderColor", p.border());
        UIManager.put("Separator.foreground", p.border());
        UIManager.put("Separator.background", p.border());
        UIManager.put("CheckBox.foreground", p.text());
        UIManager.put("CheckBox.background", p.background());
        UIManager.put("RadioButton.foreground", p.text());
        UIManager.put("RadioButton.background", p.background());
        UIManager.put("ComboBox.foreground", p.text());
        UIManager.put("ComboBox.background", p.card());
        UIManager.put("ComboBox.selectionForeground", p.text());
        UIManager.put("ComboBox.selectionBackground", listSelection);
        UIManager.put("ComboBox.buttonBackground", p.card());
        UIManager.put("ComboBox.buttonArrowColor", p.muted());
        UIManager.put("Spinner.foreground", p.text());
        UIManager.put("Spinner.background", p.card());
        UIManager.put("FormattedTextField.foreground", p.text());
        UIManager.put("FormattedTextField.background", p.card());
        UIManager.put("PasswordField.foreground", p.text());
        UIManager.put("PasswordField.background", p.card());
        UIManager.put("Table.foreground", p.text());
        UIManager.put("TableHeader.foreground", p.headerForeground());
        UIManager.put("TableHeader.background", p.headerBackground());
        UIManager.put("Table.background", p.card());
        UIManager.put("Table.alternateRowColor", p.tableEven());
        UIManager.put("Table.selectionForeground", p.text());
        UIManager.put("Table.gridColor", p.border());
        UIManager.put("TextComponent.foreground", p.text());
        UIManager.put("TextComponent.background", p.card());
        UIManager.put("TextComponent.caretForeground", p.text());
        UIManager.put("TextComponent.selectionBackground", fieldSelection);
        UIManager.put("TextComponent.selectionForeground", p.text());
        UIManager.put("TextComponent.inactiveForeground", p.muted());
        UIManager.put("TextComponent.placeholderForeground", p.placeholder());
        UIManager.put("TextArea.background", p.card());
        UIManager.put("TextArea.foreground", p.text());
        UIManager.put("TextField.background", p.card());
        UIManager.put("TextField.foreground", p.text());
        UIManager.put("TextField.placeholderForeground", p.placeholder());
        UIManager.put("List.background", p.card());
        UIManager.put("List.foreground", p.text());
        UIManager.put("List.selectionBackground", listSelection);
        UIManager.put("List.selectionForeground", p.text());
        UIManager.put("ScrollPane.background", p.card());
        UIManager.put("Viewport.background", p.card());
        UIManager.put("SplitPane.background", p.background());
        UIManager.put("SplitPaneDivider.background", p.background());
        UIManager.put("TitledBorder.titleColor", p.text());
        UIManager.put("App.table.odd", p.tableOdd());
        UIManager.put("App.table.even", p.tableEven());
        UIManager.put("App.table.selection", p.tableSelected());
        UIManager.put("Table.selectionBackground", p.tableSelected());
    }

    public static void refreshOpenWindows() {
        for (Window window : Window.getWindows()) {
            if (window == null || !window.isDisplayable()) continue;
            SwingUtilities.updateComponentTreeUI(window);
            window.invalidate();
            window.validate();
            window.repaint();
        }
    }

    private static Color blend(Color base, Color accent, float ratio) {
        float inv = 1f - ratio;
        return new Color(
                Math.round(base.getRed() * inv + accent.getRed() * ratio),
                Math.round(base.getGreen() * inv + accent.getGreen() * ratio),
                Math.round(base.getBlue() * inv + accent.getBlue() * ratio)
        );
    }

    private static Font resolveBaseFont() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String[] candidates;
        if (os.contains("mac")) {
            candidates = new String[] {"SF Pro Text", "SF Pro Display", "Helvetica Neue", "Helvetica"};
        } else if (os.contains("win")) {
            candidates = new String[] {"Segoe UI", "Arial"};
        } else {
            candidates = new String[] {"Noto Sans", "DejaVu Sans", "SansSerif"};
        }
        Set<String> available = new HashSet<>();
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            available.add(name);
        }
        for (String candidate : candidates) {
            if (available.contains(candidate)) {
                return new Font(candidate, Font.PLAIN, 13);
            }
        }
        return new JLabel().getFont().deriveFont(Font.PLAIN, 13f);
    }
}
