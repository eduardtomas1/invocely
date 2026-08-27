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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackgroundSaveSnapshotTest {
    @Test
    void collectedInvoiceRemainsStableWhileTheFormKeepsChanging() throws Exception {
        LineItem displayedLine = line("Original invoice line");
        AtomicReference<InvoiceData> snapshot = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            InvoicePanel panel = new InvoicePanel();
            panel.fillFromData(invoice(displayedLine));
            snapshot.set(panel.collect());
            displayedLine.setDescription("Edited after save started");
        });

        assertEquals("Original invoice line", snapshot.get().getLines().get(0).getDescription());
    }

    @Test
    void collectedQuoteRemainsStableWhileTheFormKeepsChanging() throws Exception {
        LineItem displayedLine = line("Original quote line");
        AtomicReference<BudgetData> snapshot = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            BudgetPanel panel = new BudgetPanel();
            panel.fillFromData(quote(displayedLine));
            snapshot.set(panel.collect());
            displayedLine.setDescription("Edited after save started");
        });

        assertEquals("Original quote line", snapshot.get().getLines().get(0).getDescription());
    }

    private InvoiceData invoice(LineItem line) {
        return new InvoiceData("INV", LocalDate.of(2026, 1, 1), "Issuer", "ID", "Address",
                "Account", "Customer", "ID2", "Address 2", new BigDecimal("21"), false,
                Arrays.asList(line));
    }

    private BudgetData quote(LineItem line) {
        return new BudgetData("Q", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                "Supplier", "ID", "Address", "Client", "ID2", "Address 2", "30 days",
                "Notes", true, "VAT", new BigDecimal("21"), false, Arrays.asList(line));
    }

    private LineItem line(String description) {
        return new LineItem(description, BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.ZERO, LineCategory.SERVEI);
    }
}
