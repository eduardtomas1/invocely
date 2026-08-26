package ui;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LanguageDraftStateTest {
    @Test
    void invoiceDraftStatePreservesInvalidWorkInProgressWithoutValidation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InvoicePanel original = new InvoicePanel();
            field(original, "tfDate").setText("31/02/2026");
            field(original, "tfVatPercent").setText("still typing");

            InvoicePanel rebuilt = new InvoicePanel();
            rebuilt.restoreDraft(original.snapshotDraft());

            assertEquals("31/02/2026", field(rebuilt, "tfDate").getText());
            assertEquals("still typing", field(rebuilt, "tfVatPercent").getText());
            assertThrows(IllegalArgumentException.class, rebuilt::collect);
        });
    }

    @Test
    void quoteDraftStatePreservesInvalidWorkInProgressWithoutValidation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BudgetPanel original = new BudgetPanel();
            field(original, "tfValid").setText("unfinished date");
            field(original, "tfTaxPercent").setText("unfinished tax");

            BudgetPanel rebuilt = new BudgetPanel();
            rebuilt.restoreDraft(original.snapshotDraft());

            assertEquals("unfinished date", field(rebuilt, "tfValid").getText());
            assertEquals("unfinished tax", field(rebuilt, "tfTaxPercent").getText());
            assertThrows(IllegalArgumentException.class, rebuilt::collect);
        });
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
