package storage;

import models.BudgetData;
import models.InvoiceData;
import models.LineCategory;
import models.LineItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import validation.DocumentValidator;

import static org.junit.jupiter.api.Assertions.*;

class XmlSaverTest {
    @TempDir Path tempDir;

    private LineItem line() {
        return new LineItem("Design & build\nPhase <one>", new BigDecimal("2.5"),
                new BigDecimal("80.25"), new BigDecimal("10"), LineCategory.SERVEI);
    }

    @Test
    void roundTripsInvoiceAndQuoteWithoutLosingBusinessFields() throws Exception {
        XmlSaver saver = new XmlSaver(tempDir);
        InvoiceData invoice = new InvoiceData("INV/2026-1", LocalDate.of(2024, 2, 29),
                "A & B", "NIF-1", "Street <1>", "ES12 3456", "Client", "NIF-2",
                "Line 1\nLine 2", new BigDecimal("21"), true, Arrays.asList(line()));
        Path invoiceFile = tempDir.resolve("invoice.xml");
        saver.saveInvoice(invoice, invoiceFile);
        InvoiceData loadedInvoice = saver.loadInvoice(invoiceFile);

        assertEquals(invoice.getInvoiceNumber(), loadedInvoice.getInvoiceNumber());
        assertEquals(invoice.getIssueDate(), loadedInvoice.getIssueDate());
        assertEquals(invoice.getIssuerName(), loadedInvoice.getIssuerName());
        assertEquals(invoice.getIssuerAccount(), loadedInvoice.getIssuerAccount());
        assertEquals(LineCategory.SERVEI, loadedInvoice.getLines().get(0).getCategory());
        assertEquals(invoice.getLines().get(0).getDescription(), loadedInvoice.getLines().get(0).getDescription());

        BudgetData quote = new BudgetData("Q-1", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31), "Supplier", "S1", "Address", "Client", "C1",
                "Address 2", "30 days", "A note\nwith details", true, "VAT",
                new BigDecimal("7.5"), true, Arrays.asList(line()));
        Path quoteFile = tempDir.resolve("quote.xml");
        saver.saveBudget(quote, quoteFile);
        BudgetData loadedQuote = saver.loadBudget(quoteFile);

        assertEquals(quote.getValidUntil(), loadedQuote.getValidUntil());
        assertEquals(quote.getPaymentTerms(), loadedQuote.getPaymentTerms());
        assertEquals(quote.getNotes(), loadedQuote.getNotes());
        assertEquals(0, quote.getTaxPercent().compareTo(loadedQuote.getTaxPercent()));
    }

    @Test
    void rejectsExternalEntitiesWithoutReadingLocalFiles() throws Exception {
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "SHOULD-NOT-BE-READ", StandardCharsets.UTF_8);
        Path malicious = tempDir.resolve("malicious.xml");
        String xml = "<?xml version=\"1.0\"?><!DOCTYPE factura [<!ENTITY xxe SYSTEM \""
                + secret.toUri() + "\">]><factura><numero>&xxe;</numero></factura>";
        Files.writeString(malicious, xml, StandardCharsets.UTF_8);

        Exception error = assertThrows(Exception.class, () -> new XmlSaver(tempDir).loadInvoice(malicious));
        assertFalse(String.valueOf(error.getMessage()).contains("SHOULD-NOT-BE-READ"));
    }

    @Test
    void rejectsMalformedAndExponentNumbersInsteadOfSilentlyImportingZero() throws Exception {
        Path malformed = tempDir.resolve("malformed.xml");
        Files.writeString(malformed, invoiceXml("not-a-number"), StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> new XmlSaver(tempDir).loadInvoice(malformed));

        Path exponent = tempDir.resolve("exponent.xml");
        Files.writeString(exponent, invoiceXml("1E-2147483647"), StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> new XmlSaver(tempDir).loadInvoice(exponent));
    }

    @Test
    void rejectsOversizedDraftBeforeParsing() throws Exception {
        Path oversized = tempDir.resolve("oversized.xml");
        Files.write(oversized, new byte[5 * 1024 * 1024 + 1]);
        assertThrows(java.io.IOException.class, () -> new XmlSaver(tempDir).loadInvoice(oversized));
    }

    @Test
    void rejectsDeeplyNestedDraftWithoutOverflowingTheStack() throws Exception {
        Path deeplyNested = tempDir.resolve("deeply-nested.xml");
        String nested = "<x>".repeat(250) + "1" + "</x>".repeat(250);
        Files.writeString(deeplyNested,
                "<?xml version=\"1.0\"?><factura><numero>" + nested
                        + "</numero><emissor/><client/><linies/></factura>",
                StandardCharsets.UTF_8);

        assertThrows(Exception.class, () -> new XmlSaver(tempDir).loadInvoice(deeplyNested));
    }

    @Test
    void readsLineItemsOnlyFromTheDocumentLineContainer() throws Exception {
        Path nested = tempDir.resolve("nested.xml");
        String xml = "<?xml version=\"1.0\"?><factura><numero>1</numero><data>2026-01-01</data>"
                + "<iva_percent>21</iva_percent><emissor><linies><linia><descripcio>hidden</descripcio>"
                + "</linia></linies></emissor><client/><linies/></factura>";
        Files.writeString(nested, xml, StandardCharsets.UTF_8);

        assertTrue(new XmlSaver(tempDir).loadInvoice(nested).getLines().isEmpty());
    }

    @Test
    void rejectsQuoteWhoseValidityPredatesItsIssueDate() throws Exception {
        Path invalid = tempDir.resolve("invalid-dates.xml");
        Files.writeString(invalid,
                "<?xml version=\"1.0\"?><pressupost><numero>Q-1</numero>"
                        + "<data>2026-12-31</data><valid_fins>2026-01-01</valid_fins>"
                        + "<proveidor/><client/><linies/></pressupost>",
                StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> new XmlSaver(tempDir).loadBudget(invalid));
    }

    @Test
    void skipsNullLineItemsWhenSaving() throws Exception {
        InvoiceData invoice = new InvoiceData("INV-NULL", LocalDate.of(2026, 1, 1),
                "Issuer", "ID", "Address", "Account", "Customer", "ID2", "Address2",
                BigDecimal.ZERO, false, Arrays.asList(line(), null));
        Path target = tempDir.resolve("null-line.xml");

        new XmlSaver(tempDir).saveInvoice(invoice, target);

        assertEquals(1, new XmlSaver(tempDir).loadInvoice(target).getLines().size());
    }

    @Test
    void rejectsInvalidBooleanAndCategoryValues() throws Exception {
        Path invalidBoolean = tempDir.resolve("invalid-boolean.xml");
        Files.writeString(invalidBoolean,
                "<?xml version=\"1.0\"?><factura><numero>1</numero>"
                        + "<linies_desglossades>sometimes</linies_desglossades>"
                        + "<emissor/><client/><linies/></factura>",
                StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> new XmlSaver(tempDir).loadInvoice(invalidBoolean));

        Path invalidCategory = tempDir.resolve("invalid-category.xml");
        Files.writeString(invalidCategory,
                "<?xml version=\"1.0\"?><factura><numero>1</numero><emissor/><client/><linies>"
                        + "<linia><descripcio>Work</descripcio><categoria>unknown</categoria></linia>"
                        + "</linies></factura>",
                StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> new XmlSaver(tempDir).loadInvoice(invalidCategory));
    }

    @Test
    void rejectsUnsafeDocumentBeforeWritingXml() {
        InvoiceData unsafe = new InvoiceData("x".repeat(20_001), LocalDate.of(2026, 1, 1),
                "Issuer", "ID", "Address", "Account", "Customer", "ID2", "Address2",
                BigDecimal.ZERO, false, Arrays.asList(line()));
        Path target = tempDir.resolve("unsafe.xml");

        assertThrows(IllegalArgumentException.class,
                () -> new XmlSaver(tempDir).saveInvoice(unsafe, target));
        assertFalse(Files.exists(target));
    }

    @Test
    void rejectsQuoteWithoutExplicitValidityBeforeWritingXml() {
        BudgetData missingValidity = new BudgetData("Q-1", LocalDate.of(2026, 1, 1), null,
                "Supplier", "ID", "Address", "Client", "ID2", "Address2", "Terms", "Notes",
                true, "VAT", new BigDecimal("21"), false, Collections.emptyList());
        Path target = tempDir.resolve("missing-validity.xml");

        assertThrows(IllegalArgumentException.class,
                () -> new XmlSaver(tempDir).saveBudget(missingValidity, target));
        assertFalse(Files.exists(target));
    }

    @Test
    void convenienceSaveValidatesBeforeCreatingStorageDirectories() {
        XmlSaver saver = new XmlSaver(tempDir);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> saver.saveInvoice(null));

        assertEquals(i18n.I18n.t("validation.document_required"), error.getMessage());
        assertFalse(Files.exists(tempDir.resolve("factures")));
    }

    @Test
    void largeAllowedDraftCanBeSavedAndOpenedAgain() throws Exception {
        LineItem repeated = new LineItem("x".repeat(30), BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.ZERO, LineCategory.MATERIAL);
        InvoiceData invoice = new InvoiceData("LARGE", LocalDate.of(2026, 1, 1),
                "Issuer", "ID", "Address", "Account", "Customer", "ID2", "Address2",
                BigDecimal.ZERO, false,
                Collections.nCopies(DocumentValidator.MAX_LINES, repeated));
        Path target = tempDir.resolve("large.xml");
        XmlSaver saver = new XmlSaver(tempDir);

        saver.saveInvoice(invoice, target);
        InvoiceData loaded = saver.loadInvoice(target);

        assertEquals(DocumentValidator.MAX_LINES, loaded.getLines().size());
        assertTrue(Files.size(target) < 5L * 1024L * 1024L);
    }

    @Test
    void xmlExpansionCannotCreateADraftThatTheImporterRejects() {
        LineItem escaped = new LineItem("&".repeat(90),
                BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, LineCategory.MATERIAL);
        InvoiceData invoice = new InvoiceData("EXPANDED", LocalDate.of(2026, 1, 1),
                "Issuer", "ID", "Address", "Account", "Customer", "ID2", "Address2",
                BigDecimal.ZERO, false, Collections.nCopies(DocumentValidator.MAX_LINES, escaped));
        Path target = tempDir.resolve("expanded.xml");

        assertDoesNotThrow(() -> DocumentValidator.validateInvoice(invoice));
        assertThrows(IllegalArgumentException.class,
                () -> new XmlSaver(tempDir).saveInvoice(invoice, target));
        assertFalse(Files.exists(target));
    }

    private String invoiceXml(String vat) {
        return "<?xml version=\"1.0\"?><factura><numero>1</numero><data>2026-01-01</data>"
                + "<iva_percent>" + vat + "</iva_percent><emissor/><client/><linies/></factura>";
    }
}
