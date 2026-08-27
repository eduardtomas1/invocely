package ui;

import i18n.I18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AccessibilityHooksTest {
    @AfterEach
    void restoreLocale() {
        I18n.setLocale(Locale.forLanguageTag("en-US"));
    }

    @Test
    void invoiceLabelsNameAndTargetTheirFields() throws Exception {
        I18n.setLocale(Locale.forLanguageTag("en-US"));
        SwingUtilities.invokeAndWait(() -> {
            InvoicePanel panel = new InvoicePanel();
            JTextComponent date = field(panel, "tfDate");
            JLabel label = labels(panel).stream()
                .filter(candidate -> I18n.t("label.issue_date").equals(candidate.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Issue date label not found"));

            assertEquals(date, label.getLabelFor());
            assertEquals(I18n.t("label.issue_date"), date.getAccessibleContext().getAccessibleName());
            assertNotNull(date.getAccessibleContext().getAccessibleDescription());
        });
    }

    @Test
    void everyLineTableHasAnAccessibleNameAndDescription() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ItemTablePanel panel = new ItemTablePanel();
            List<JTable> tables = components(panel, JTable.class);

            assertEquals(3, tables.size());
            for (JTable table : tables) {
                assertFalse(table.getAccessibleContext().getAccessibleName().isBlank());
                assertFalse(table.getAccessibleContext().getAccessibleDescription().isBlank());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> List<T> components(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) found.add((T) component);
            if (component instanceof Container) found.addAll(components((Container) component, type));
        }
        return found;
    }

    private List<JLabel> labels(Container root) {
        return components(root, JLabel.class);
    }

    private JTextComponent field(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (JTextComponent) field.get(target);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
}
