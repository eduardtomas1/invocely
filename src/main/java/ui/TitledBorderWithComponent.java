package ui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;

/**
 * A custom border that displays a component (like a panel with title and button)
 * in the title position of a titled border.
 */
public class TitledBorderWithComponent extends AbstractBorder {
    private final JComponent titleComponent;
    private final Color borderColor;
    private final Insets insets;

    public TitledBorderWithComponent(JComponent titleComponent, Color borderColor) {
        this.titleComponent = titleComponent;
        this.borderColor = borderColor;
        // Calculate insets based on title component height
        int titleHeight = titleComponent.getPreferredSize().height;
        this.insets = new Insets(titleHeight + 4, 1, 1, 1);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int titleHeight = titleComponent.getPreferredSize().height;
            int titleWidth = Math.min(titleComponent.getPreferredSize().width, width - 20);
            int arc = 8;

            // Draw the border (rounded rectangle starting below title area)
            g2.setColor(borderColor);
            int borderY = y + titleHeight / 2;
            int borderHeight = height - titleHeight / 2;

            // Draw border with gap for title
            g2.drawRoundRect(x, borderY, width - 1, borderHeight - 1, arc, arc);

            // Clear the area behind the title
            g2.setColor(c.getBackground());
            g2.fillRect(x + 8, y, titleWidth + 8, titleHeight);

            // Position and paint the title component
            titleComponent.setSize(titleWidth, titleHeight);
            titleComponent.doLayout();

            g2.translate(x + 12, y);
            titleComponent.paint(g2);

        } finally {
            g2.dispose();
        }
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return insets;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.top = this.insets.top;
        insets.left = this.insets.left;
        insets.bottom = this.insets.bottom;
        insets.right = this.insets.right;
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
