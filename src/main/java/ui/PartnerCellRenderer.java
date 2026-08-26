package ui;

import models.BusinessPartner;

import javax.swing.*;
import java.awt.*;

public class PartnerCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof BusinessPartner) {
            BusinessPartner partner = (BusinessPartner) value;
            ThemePalette palette = ThemeManager.palette();
            String name = safe(partner.getName());
            String nif = safe(partner.getNif());
            if (name.isEmpty()) {
                name = nif.isEmpty() ? "-" : nif;
            }
            String secondary = nif.isEmpty() ? "-" : nif;
            String secondaryColor = toHex(isSelected ? palette.text() : palette.muted());
            String html = String.format(
                    "<html><div style='font-weight:600'>%s</div><div style='color:%s;font-size:90%%'>%s</div></html>",
                    escapeHtml(name),
                    secondaryColor,
                    escapeHtml(secondary)
            );
            setText(html);
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            setForeground(palette.text());
        }
        if (isSelected) {
            setBackground(ThemeManager.palette().tableSelected());
        } else {
            setBackground(index % 2 == 0
                    ? ThemeManager.palette().tableEven()
                    : ThemeManager.palette().tableOdd());
        }
        return this;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
