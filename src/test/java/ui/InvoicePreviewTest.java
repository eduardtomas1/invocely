package ui;

import models.InvoiceData;
import models.LineCategory;
import models.LineItem;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoicePreviewTest {
    @Test
    void halfCentVatPreviewMatchesTheExportedModelTotals() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InvoiceData invoice = new InvoiceData("INV", LocalDate.of(2026, 1, 1),
                    "Issuer", "ID", "Address", "Account", "Customer", "ID2", "Address 2",
                    new BigDecimal("1"), false,
                    Arrays.asList(new LineItem("Work", BigDecimal.ONE, new BigDecimal("0.50"),
                            BigDecimal.ZERO, LineCategory.SERVEI)));
            InvoicePanel panel = new InvoicePanel();
            panel.fillFromData(invoice);

            assertAmount("0.01", label(panel, "lblTotalVat").getText());
            assertAmount("0.51", label(panel, "lblGrandTotal").getText());
            assertAmount("0.01", invoice.getVatAmount().toPlainString());
            assertAmount("0.51", invoice.getGrandTotal().toPlainString());
        });
    }

    private void assertAmount(String expected, String actual) {
        assertTrue(actual.endsWith(expected) || actual.endsWith(expected.replace('.', ',')),
                () -> "Expected " + expected + " but saw " + actual);
    }

    private JLabel label(InvoicePanel panel, String name) {
        try {
            Field field = InvoicePanel.class.getDeclaredField(name);
            field.setAccessible(true);
            return (JLabel) field.get(panel);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
}
