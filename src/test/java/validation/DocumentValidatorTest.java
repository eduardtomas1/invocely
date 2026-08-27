package validation;

import models.InvoiceData;
import models.BudgetData;
import models.LineCategory;
import models.LineItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DocumentValidatorTest {
    @Test
    void acceptsExactTextAndLineCountLimits() {
        LineItem line = new LineItem("",
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, LineCategory.MATERIAL);
        InvoiceData invoice = invoice("x".repeat(DocumentValidator.MAX_TEXT_LENGTH),
                Collections.nCopies(DocumentValidator.MAX_LINES, line));

        assertDoesNotThrow(() -> DocumentValidator.validateInvoice(invoice));
    }

    @Test
    void rejectsDocumentsWhoseCombinedTextExceedsTheSafeLimit() {
        LineItem maximumField = new LineItem("x".repeat(DocumentValidator.MAX_TEXT_LENGTH),
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, LineCategory.MATERIAL);
        InvoiceData unsafe = invoice("",
                Collections.nCopies(DocumentValidator.MAX_TOTAL_TEXT_BYTES
                        / DocumentValidator.MAX_TEXT_LENGTH + 1, maximumField));

        assertThrows(IllegalArgumentException.class,
                () -> DocumentValidator.validateInvoice(unsafe));
    }

    @Test
    void rejectsTextAndLineCountBeyondLimits() {
        InvoiceData longText = invoice("x".repeat(DocumentValidator.MAX_TEXT_LENGTH + 1),
                Collections.emptyList());
        assertThrows(IllegalArgumentException.class,
                () -> DocumentValidator.validateInvoice(longText));

        LineItem line = new LineItem("line", BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.ZERO, LineCategory.MATERIAL);
        InvoiceData tooManyLines = invoice("INV",
                Collections.nCopies(DocumentValidator.MAX_LINES + 1, line));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentValidator.validateInvoice(tooManyLines));
    }

    @Test
    void enforcesNumericBoundsAndPercentSemantics() {
        assertDoesNotThrow(() -> DocumentValidator.validateNumber(new BigDecimal("1000000000000000")));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentValidator.validateNumber(new BigDecimal("1000000000000001")));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentValidator.validateNumber(new BigDecimal("0.000000001")));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentValidator.validatePercent(new BigDecimal("100.01")));
    }

    @Test
    void requiresExplicitQuoteDatesAtSaveAndExportBoundaries() {
        BudgetData missingValidity = new BudgetData("Q-1", LocalDate.of(2026, 8, 27), null,
                "Supplier", "ID", "Address", "Customer", "ID2", "Address2", "Terms", "Notes",
                true, "VAT", new BigDecimal("21"), false, Collections.emptyList());

        assertThrows(IllegalArgumentException.class,
                () -> DocumentValidator.validateBudget(missingValidity));
    }

    private InvoiceData invoice(String number, java.util.List<LineItem> lines) {
        return new InvoiceData(number, LocalDate.of(2026, 8, 27), "Issuer", "ID", "Address",
                "Account", "Customer", "ID2", "Address2", new BigDecimal("21"), false, lines);
    }
}
