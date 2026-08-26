package storage;

import models.BudgetData;
import models.InvoiceData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultsManagerTest {
    @TempDir Path tempDir;

    @Test
    void corruptedFinancialDefaultsAreReportedInsteadOfBecomingZero() throws Exception {
        Properties values = new Properties();
        values.setProperty("issueDate", "2026-01-01");
        values.setProperty("vatPercent", "not-a-percentage");
        try (OutputStream output = Files.newOutputStream(tempDir.resolve("invoice.xml"))) {
            values.storeToXML(output, "test", "UTF-8");
        }

        assertThrows(IOException.class, () -> new DefaultsManager(tempDir).loadInvoiceDefaults());
    }

    @Test
    void invoiceDefaultsRoundTripInAnIsolatedDirectory() throws Exception {
        DefaultsManager manager = new DefaultsManager(tempDir.resolve("invoice-defaults"));
        InvoiceData input = new InvoiceData("INV-DEFAULT", LocalDate.of(2026, 2, 3),
                "Issuer", "NIF-1", "Address", "ES00", "Customer", "NIF-2", "Address 2",
                new BigDecimal("21"), true, Collections.emptyList());

        manager.saveInvoiceDefaults(input);
        InvoiceData loaded = manager.loadInvoiceDefaults();

        assertEquals(input.getInvoiceNumber(), loaded.getInvoiceNumber());
        assertEquals(input.getIssueDate(), loaded.getIssueDate());
        assertEquals(input.getIssuerName(), loaded.getIssuerName());
        assertEquals(input.getIssuerAccount(), loaded.getIssuerAccount());
        assertEquals(input.getCustomerAddress(), loaded.getCustomerAddress());
        assertEquals(0, input.getVatPercent().compareTo(loaded.getVatPercent()));
        assertTrue(loaded.isSplitLines());
    }

    @Test
    void quoteDefaultsRoundTripAndRejectCorruptFlags() throws Exception {
        DefaultsManager manager = new DefaultsManager(tempDir.resolve("quote-defaults"));
        BudgetData input = new BudgetData("Q-DEFAULT", LocalDate.of(2026, 2, 3),
                LocalDate.of(2026, 3, 4), "Supplier", "NIF-1", "Address", "Client", "NIF-2",
                "Address 2", "30 days", "Notes", true, "VAT", new BigDecimal("7.5"),
                false, Collections.emptyList());

        manager.saveBudgetDefaults(input);
        BudgetData loaded = manager.loadBudgetDefaults();

        assertEquals(input.getBudgetNumber(), loaded.getBudgetNumber());
        assertEquals(input.getIssueDate(), loaded.getIssueDate());
        assertEquals(input.getValidUntil(), loaded.getValidUntil());
        assertEquals(input.getPaymentTerms(), loaded.getPaymentTerms());
        assertEquals(input.getNotes(), loaded.getNotes());
        assertTrue(loaded.isIncludeTotals());
        assertFalse(loaded.isSplitLines());

        Properties values = new Properties();
        values.setProperty("includeTotals", "sometimes");
        try (OutputStream output = Files.newOutputStream(
                tempDir.resolve("quote-defaults").resolve("budget.xml"))) {
            values.storeToXML(output, "test", "UTF-8");
        }
        assertThrows(IOException.class, manager::loadBudgetDefaults);
    }

    @Test
    void quoteDefaultsRequireBothDatesOrNeither() {
        DefaultsManager manager = new DefaultsManager(tempDir.resolve("paired-dates"));
        BudgetData missingIssue = new BudgetData("Q", null, LocalDate.of(2026, 3, 4),
                "Supplier", "ID", "Address", "Client", "ID2", "Address 2", "", "",
                false, "VAT", BigDecimal.ZERO, false, Collections.emptyList());
        BudgetData missingValidity = new BudgetData("Q", LocalDate.of(2026, 2, 3), null,
                "Supplier", "ID", "Address", "Client", "ID2", "Address 2", "", "",
                false, "VAT", BigDecimal.ZERO, false, Collections.emptyList());

        assertThrows(IOException.class, () -> manager.saveBudgetDefaults(missingIssue));
        assertThrows(IOException.class, () -> manager.saveBudgetDefaults(missingValidity));
    }
}
