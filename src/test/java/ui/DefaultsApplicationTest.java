package ui;

import models.BudgetData;
import models.InvoiceData;
import models.LineCategory;
import models.LineItem;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultsApplicationTest {
    @Test
    void blankInvoiceDefaultDateAndEmptyDefaultLinesPreserveTheDraft() throws Exception {
        AtomicReference<InvoiceData> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            InvoicePanel panel = new InvoicePanel();
            panel.fillFromData(invoice(LocalDate.of(2026, 4, 5), Arrays.asList(line())));
            panel.applyDefaults(invoice(null, Collections.emptyList()));
            result.set(panel.collect());
        });

        assertEquals(LocalDate.of(2026, 4, 5), result.get().getIssueDate());
        assertEquals(1, result.get().getLines().size());
    }

    @Test
    void blankQuoteDefaultDatesAndEmptyDefaultLinesPreserveTheDraft() throws Exception {
        AtomicReference<BudgetData> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            BudgetPanel panel = new BudgetPanel();
            panel.fillFromData(quote(LocalDate.of(2026, 4, 5), LocalDate.of(2026, 5, 6),
                    Arrays.asList(line())));
            panel.applyDefaults(quote(null, null, Collections.emptyList()));
            result.set(panel.collect());
        });

        assertEquals(LocalDate.of(2026, 4, 5), result.get().getIssueDate());
        assertEquals(LocalDate.of(2026, 5, 6), result.get().getValidUntil());
        assertEquals(1, result.get().getLines().size());
    }

    @Test
    void partialQuoteDateDefaultsAreRejectedBeforeChangingTheDraft() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BudgetPanel panel = new BudgetPanel();
            panel.fillFromData(quote(LocalDate.of(2026, 4, 5), LocalDate.of(2026, 5, 6),
                    Arrays.asList(line())));
            BudgetData partial = quote(null, LocalDate.of(2026, 1, 1), Collections.emptyList());

            assertThrows(IllegalArgumentException.class, () -> panel.applyDefaults(partial));

            BudgetData unchanged = panel.collect();
            assertEquals(LocalDate.of(2026, 4, 5), unchanged.getIssueDate());
            assertEquals(LocalDate.of(2026, 5, 6), unchanged.getValidUntil());
            assertEquals(1, unchanged.getLines().size());
        });
    }

    private InvoiceData invoice(LocalDate date, java.util.List<LineItem> lines) {
        return new InvoiceData("INV", date, "Issuer", "ID", "Address", "Account",
                "Customer", "ID2", "Address 2", new BigDecimal("21"), false, lines);
    }

    private BudgetData quote(LocalDate issue, LocalDate valid, java.util.List<LineItem> lines) {
        return new BudgetData("Q", issue, valid, "Supplier", "ID", "Address", "Client", "ID2",
                "Address 2", "30 days", "Notes", true, "VAT", new BigDecimal("21"), false, lines);
    }

    private LineItem line() {
        return new LineItem("Work", BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.ZERO, LineCategory.SERVEI);
    }
}
