package ui;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;

/** Small helpers that make existing Swing labels and controls useful to assistive technology. */
final class AccessibilitySupport {
    private AccessibilitySupport() { }

    static void bindLabel(JLabel label, JComponent control) {
        if (label == null || control == null) return;
        JComponent target = focusTarget(control);
        label.setLabelFor(target);
        name(target, label.getText());
    }

    static void name(JComponent component, String name) {
        if (component == null || name == null || name.isBlank()) return;
        AccessibleContext context = component.getAccessibleContext();
        if (context != null) context.setAccessibleName(name);
    }

    static void describe(JComponent component, String description) {
        if (component == null || description == null || description.isBlank()) return;
        AccessibleContext context = focusTarget(component).getAccessibleContext();
        if (context != null) context.setAccessibleDescription(description);
    }

    static JComponent focusTarget(JComponent component) {
        if (component instanceof JScrollPane) {
            Component view = ((JScrollPane) component).getViewport().getView();
            if (view instanceof JComponent) return (JComponent) view;
        }
        return component;
    }
}
