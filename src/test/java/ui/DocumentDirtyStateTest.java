package ui;

import i18n.I18n;
import models.BudgetData;
import models.InvoiceData;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentDirtyStateTest {
    @Test
    void invoiceTracksSemanticChangesAndPreservesBaselineAcrossLanguageRebuild() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InvoicePanel original = new InvoicePanel();
            assertFalse(original.isDirty());

            field(original, "tfNumber").setText("INV-42");
            assertTrue(original.isDirty());

            InvoicePanel rebuilt = new InvoicePanel();
            rebuilt.restoreDraft(original.snapshotDraft());
            assertTrue(rebuilt.isDirty(), "language rebuild must preserve the dirty baseline");

            field(rebuilt, "tfNumber").setText("");
            assertFalse(rebuilt.isDirty(), "reverting to the baseline should clear the dirty state");

            member(rebuilt, "itemsPanel", ItemTablePanel.class).getModel().addEmpty();
            assertTrue(rebuilt.isDirty());
            rebuilt.markClean();
            assertFalse(rebuilt.isDirty());

            JTable table = firstTable(member(rebuilt, "itemsPanel", ItemTablePanel.class));
            assertTrue(table.editCellAt(0, 1));
            assertFalse(rebuilt.isDirty(), "opening an unchanged editor is not an unsaved change");
            ((JTextField) table.getEditorComponent()).setText("2");
            assertTrue(rebuilt.isDirty(), "a pending changed value must be protected on close");
        });
    }

    @Test
    void importedInvoiceBecomesCleanAndApplyingDefaultsChangesTheDraft() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InvoicePanel panel = new InvoicePanel();
            field(panel, "tfNumber").setText("work in progress");
            assertTrue(panel.isDirty());

            InvoiceData imported = new InvoiceData("INV-1", LocalDate.of(2026, 8, 27),
                    "Issuer", "ID", "Address", "Account", "Customer", "ID2", "Address2",
                    new BigDecimal("21"), false, Collections.emptyList());
            panel.fillFromData(imported);
            assertFalse(panel.isDirty());

            InvoiceData defaults = new InvoiceData("DEFAULT", LocalDate.of(2026, 8, 27),
                    "Other issuer", "ID", "Address", "Account", "Customer", "ID2", "Address2",
                    new BigDecimal("21"), true, Collections.emptyList());
            panel.applyDefaults(defaults);
            assertTrue(panel.isDirty());
        });
    }

    @Test
    void backgroundSaveOnlyCleansTheSnapshotThatWasActuallyWritten() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InvoicePanel panel = new InvoicePanel();
            field(panel, "tfNumber").setText("saved snapshot");
            InvoicePanel.DraftState savedDraft = panel.snapshotDraft();

            field(panel, "tfNumber").setText("edited while saving");
            panel.markCleanIfUnchanged(savedDraft);
            assertTrue(panel.isDirty(), "later edits must remain unsaved after background completion");

            field(panel, "tfNumber").setText("saved snapshot");
            panel.markCleanIfUnchanged(savedDraft);
            assertFalse(panel.isDirty());
        });
    }

    @Test
    void quoteRequiresAnExplicitValidityChoiceAndTracksNonTextControls() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BudgetPanel panel = new BudgetPanel();
            JTextComponent validity = field(panel, "tfValid");

            assertFalse(panel.isDirty());
            assertEquals(Boolean.TRUE, validity.getClientProperty("placeholder.active"));
            IllegalArgumentException missing = assertThrows(IllegalArgumentException.class, panel::collect);
            assertEquals(I18n.t("validation.date_required"), missing.getMessage());

            JTextComponent issue = field(panel, "tfDate");
            validity.setText(issue.getText());
            assertNotNull(panel.collect(), "same-day validity remains an explicit valid choice");
            assertTrue(panel.isDirty());

            panel.markClean();
            checkbox(panel, "cbIncludeTotals").setSelected(true);
            assertTrue(panel.isDirty());
        });
    }

    private JTextComponent field(Object target, String name) {
        return member(target, name, JTextComponent.class);
    }

    private JCheckBox checkbox(Object target, String name) {
        return member(target, name, JCheckBox.class);
    }

    private <T> T member(Object target, String name, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
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
