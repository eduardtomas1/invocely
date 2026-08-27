package ui;

import i18n.I18n;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LineTableEditingTest {
    @Test
    void validPendingNumericEditIsCommittedBeforeCollection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ItemTablePanel panel = new ItemTablePanel();
            panel.getModel().addEmpty();
            JTable table = firstTable(panel);

            assertTrue(table.editCellAt(0, 1));
            JTextField editor = (JTextField) table.getEditorComponent();
            editor.setText("2,5");

            panel.requireCommittedEdits();

            assertFalse(table.isEditing());
            assertEquals(0, new BigDecimal("2.5").compareTo(
                    panel.getModel().getItemAt(0).getQuantity()));
        });
    }

    @Test
    void invalidPendingNumericEditIsLocalizedAndCannotCommit() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ItemTablePanel panel = new ItemTablePanel();
            panel.getModel().addEmpty();
            JTable table = firstTable(panel);

            assertTrue(table.editCellAt(0, 1));
            JTextField editor = (JTextField) table.getEditorComponent();
            editor.setText("not a number");

            assertThrows(ItemTablePanel.PendingEditException.class, panel::requireCommittedEdits);
            assertTrue(table.isEditing());
            assertEquals(I18n.t("validation.invalid_number"),
                    editor.getClientProperty("validation.message"));
            assertEquals(BigDecimal.ONE, panel.getModel().getItemAt(0).getQuantity());
        });
    }

    @Test
    void extremeAndOutOfRangeValuesNeverEnterTheModel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ItemTablePanel panel = new ItemTablePanel();
            panel.getModel().addEmpty();

            assertTimeout(java.time.Duration.ofSeconds(1), () ->
                    assertThrows(IllegalArgumentException.class,
                            () -> panel.getModel().setValueAt("1E+2147483647", 0, 1)));
            assertThrows(IllegalArgumentException.class,
                    () -> panel.getModel().setValueAt("101", 0, 3));
            assertEquals(BigDecimal.ONE, panel.getModel().getItemAt(0).getQuantity());
            assertEquals(BigDecimal.ZERO, panel.getModel().getItemAt(0).getDiscountPercent());
        });
    }

    @SuppressWarnings("unchecked")
    private JTable firstTable(ItemTablePanel panel) {
        try {
            Field field = ItemTablePanel.class.getDeclaredField("tables");
            field.setAccessible(true);
            return ((List<JTable>) field.get(panel)).get(0);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
}
