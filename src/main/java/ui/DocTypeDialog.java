package ui;

import i18n.I18n;
import models.DocumentType;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DocTypeDialog extends JDialog {
    private DocumentType chosen = null;

    public DocTypeDialog(Frame owner) {
        super(owner, I18n.t("doc_type.title"), true);
        setLayout(new BorderLayout());
        ThemePalette palette = ThemeManager.palette();

        JPanel content = new JPanel(new BorderLayout(18, 12)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                ThemePalette current = ThemeManager.palette();
                Color top = mix(current.background(), current.card(), 0.35f);
                Color bottom = mix(current.background(), current.card(), 0.12f);
                g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                g2.dispose();
            }
        };
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        add(content, BorderLayout.CENTER);

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel badge = new JLabel(I18n.t("app.badge"));
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 12f));
        badge.setForeground(accentColor());
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 0)),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));

        JLabel header = new JLabel(I18n.t("doc_type.header"), SwingConstants.CENTER);
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 22f));
        JLabel sub = new JLabel(I18n.t("doc_type.sub"), SwingConstants.CENTER);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setForeground(palette.muted());
        headerPanel.add(badge);
        headerPanel.add(Box.createVerticalStrut(6));
        headerPanel.add(header);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(sub);

        JPanel headerWrap = new JPanel(new BorderLayout());
        headerWrap.setOpaque(false);
        headerWrap.add(headerPanel, BorderLayout.CENTER);
        content.add(headerWrap, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 16, 16));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        buttons.add(createOptionCard(
                I18n.t("doc_type.option.invoice"),
                I18n.t("doc_type.option.invoice.sub"),
                AppIcons.invoiceIcon(),
                DocumentType.INVOICE
        ));
        buttons.add(createOptionCard(
                I18n.t("doc_type.option.budget"),
                I18n.t("doc_type.option.budget.sub"),
                AppIcons.budgetIcon(),
                DocumentType.BUDGET
        ));
        buttons.add(createOptionCard(
                I18n.t("doc_type.option.cancel"),
                I18n.t("doc_type.option.cancel.sub"),
                AppIcons.cancelIcon(),
                null
        ));

        content.add(buttons, BorderLayout.CENTER);

        JLabel hint = new JLabel(I18n.t("doc_type.hint"), SwingConstants.CENTER);
        hint.setForeground(palette.muted());
        hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        content.add(hint, BorderLayout.SOUTH);

        setSize(720, 360);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                chosen = null;
            }
        });
    }

    private JButton createOptionCard(String title, String subtitle, Icon icon, DocumentType type) {
        JButton btn = new JButton();
        ThemePalette palette = ThemeManager.palette();
        btn.setLayout(new BorderLayout(0, 10));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setVerticalAlignment(SwingConstants.CENTER);
        btn.setOpaque(true);
        btn.setBorder(cardBorder(false));
        btn.setBackground(cardColor());
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("JComponent.sizeVariant", "large");
        btn.setPreferredSize(new Dimension(210, 180));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        iconLbl.setOpaque(false);
        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 16f));
        titleLbl.setForeground(palette.text());
        JLabel subLbl = new JLabel(subtitle, SwingConstants.CENTER);
        subLbl.setForeground(palette.muted());

        btn.add(iconLbl, BorderLayout.NORTH);
        btn.add(titleLbl, BorderLayout.CENTER);
        btn.add(subLbl, BorderLayout.SOUTH);

        btn.getModel().addChangeListener(e -> {
            ButtonModel m = btn.getModel();
            boolean hover = m.isRollover();
            boolean press = m.isArmed() && m.isPressed();
            Color base = cardColor();
            Color hoverCol = mix(base, accentColor(), 0.10f);
            Color pressCol = mix(base, accentColor(), 0.16f);
            btn.setBackground(press ? pressCol : (hover ? hoverCol : base));
            btn.setBorder(cardBorder(hover || press));
        });

        btn.addActionListener(e -> {
            chosen = type;
            dispose();
        });
        return btn;
    }

    private Border cardBorder(boolean accent) {
        Color base = ThemeManager.palette().border();
        Color stroke = accent
                ? accentColor()
                : new Color(base.getRed(), base.getGreen(), base.getBlue(), 150);
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(stroke, 1, true),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        );
    }

    public DocumentType getChosen() {
        return chosen;
    }

    private Color cardColor() {
        Color card = UIManager.getColor("App.cardColor");
        if (card != null) return card;
        Color c = UIManager.getColor("Panel.background");
        return c != null ? c : Color.WHITE;
    }

    private Color accentColor() {
        Color c = UIManager.getColor("Component.focusColor");
        if (c != null) return c;
        return new Color(52, 120, 246);
    }

    private Color mix(Color base, Color accent, float ratio) {
        float inv = 1f - ratio;
        return new Color(
                Math.round(base.getRed() * inv + accent.getRed() * ratio),
                Math.round(base.getGreen() * inv + accent.getGreen() * ratio),
                Math.round(base.getBlue() * inv + accent.getBlue() * ratio)
        );
    }

}
