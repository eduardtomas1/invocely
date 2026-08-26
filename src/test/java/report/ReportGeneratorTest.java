package report;

import models.BudgetData;
import models.InvoiceData;
import models.LineCategory;
import models.LineItem;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintFrame;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JasperPrint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {
    @TempDir Path tempDir;
    private final ReportGenerator generator = new ReportGenerator();

    @ParameterizedTest
    @ValueSource(strings = {"en-US", "es-ES", "ca-ES"})
    void everyInvoiceTemplateCompilesAndRendersBusinessValues(String languageTag) throws Exception {
        JasperPrint print = generator.prepareInvoice(
                invoice(Arrays.asList(new LineItem("INVOICE-LINE-42", BigDecimal.ONE,
                        new BigDecimal("10.50"), BigDecimal.ZERO, LineCategory.SERVEI)),
                        "INVOICE-ISSUER-84"),
                Locale.forLanguageTag(languageTag));
        String text = allText(print);

        assertTrue(text.contains("INVOICE-LINE-42"), text);
        assertTrue(text.contains("INVOICE-ISSUER-84"), text);
        assertTrue(text.contains("12.71") || text.contains("12,71"), text);
    }

    @ParameterizedTest
    @ValueSource(strings = {"en-US", "es-ES", "ca-ES"})
    void everyQuoteTemplateRendersTermsNotesAndExplicitValidity(String languageTag) throws Exception {
        JasperPrint print = generator.prepareBudget(quote(), Locale.forLanguageTag(languageTag));
        String text = allText(print);

        assertTrue(text.contains("TERMS-UNIQUE-42"), text);
        assertTrue(text.contains("NOTES-UNIQUE-84"), text);
        assertTrue(occurrences(text, "31/12/2026") >= 2,
                "The entered validity date should appear in the header and summary: " + text);
        assertFalse(text.contains("15/02/2026"), "Validity must not be inferred from the issue date");
    }

    @Test
    void invoiceTotalsRenderOnlyOnTheFinalPage() throws Exception {
        List<LineItem> lines = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            lines.add(new LineItem("Line " + i, BigDecimal.ONE, BigDecimal.TEN,
                    BigDecimal.ZERO, LineCategory.SERVEI));
        }
        InvoiceData invoice = invoice(lines, "Issuer");
        JasperPrint print = generator.prepareInvoice(invoice, Locale.forLanguageTag("en-US"));
        assertTrue(print.getPages().size() > 1, "fixture must produce a multi-page report");

        for (int i = 0; i < print.getPages().size() - 1; i++) {
            assertFalse(pageText(print.getPages().get(i)).contains("1,210.00"),
                    "grand total appeared before the final page");
        }
        assertTrue(pageText(print.getPages().get(print.getPages().size() - 1)).contains("1,210.00"));
    }

    @Test
    void realExportersProduceReadableFilesAndCsvNeutralizesFormulas() throws Exception {
        InvoiceData invoice = invoice(Arrays.asList(new LineItem("=SUM(1,1)", BigDecimal.ONE,
                new BigDecimal("25"), BigDecimal.ZERO, LineCategory.MATERIAL)), " @HYPERLINK");
        Path pdf = tempDir.resolve("invoice.pdf");
        Path xlsx = tempDir.resolve("invoice.xlsx");
        Path csv = tempDir.resolve("invoice.csv");

        generator.exportInvoice(invoice, "pdf", pdf, Locale.forLanguageTag("en-US"));
        generator.exportInvoice(invoice, "xlsx", xlsx, Locale.forLanguageTag("en-US"));
        generator.exportInvoice(invoice, "csv", csv, Locale.forLanguageTag("en-US"));

        assertTrue(Files.size(pdf) > 1_000);
        assertEquals("%PDF", Files.readString(pdf, StandardCharsets.ISO_8859_1).substring(0, 4));
        try (ZipFile zip = new ZipFile(xlsx.toFile())) {
            assertNotNull(zip.getEntry("[Content_Types].xml"));
            java.util.zip.ZipEntry sheet = zip.getEntry("xl/worksheets/sheet1.xml");
            assertNotNull(sheet);
            String sheetXml = new String(zip.getInputStream(sheet).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(Pattern.compile("<c\\b[^>]*\\bt=\"n\"[^>]*><v>25(?:\\.0+)?</v></c>")
                            .matcher(sheetXml).find(),
                    "The XLSX unit price should be a numeric cell, not formatted text");
        }
        String csvText = Files.readString(csv, StandardCharsets.UTF_8);
        assertTrue(csvText.contains("'=SUM(1,1)"), csvText);
        assertTrue(csvText.contains("' @HYPERLINK"), csvText);
    }

    @Test
    void compiledTemplatesAreSharedAcrossGeneratorInstances() throws Exception {
        generator.prepareInvoice(invoice(Arrays.asList(), "Issuer"), Locale.forLanguageTag("en-US"));
        int afterFirst = ReportGenerator.cachedTemplateCount();
        new ReportGenerator().prepareInvoice(invoice(Arrays.asList(), "Issuer"), Locale.forLanguageTag("en-US"));
        assertEquals(afterFirst, ReportGenerator.cachedTemplateCount());
    }

    @Test
    void spanishTemplatesRenderNaturalColumnLabels() throws Exception {
        String invoiceText = allText(generator.prepareInvoice(
                invoice(Arrays.asList(new LineItem("Work", BigDecimal.ONE, BigDecimal.TEN,
                        BigDecimal.ZERO, LineCategory.SERVEI)), "Issuer"),
                Locale.forLanguageTag("es-ES")));
        String quoteText = allText(generator.prepareBudget(quote(), Locale.forLanguageTag("es-ES")));

        assertTrue(invoiceText.contains("Descripción"), invoiceText);
        assertTrue(invoiceText.contains("Cant."), invoiceText);
        assertTrue(quoteText.contains("Descripción"), quoteText);
        assertTrue(quoteText.contains("Cant."), quoteText);
        assertFalse(invoiceText.contains("\\u00f3") || invoiceText.contains("Cant\\."), invoiceText);
        assertFalse(quoteText.contains("Cant\\."), quoteText);
    }

    private InvoiceData invoice(List<LineItem> lines, String issuer) {
        return new InvoiceData("INV-2026-001", LocalDate.of(2026, 1, 15), issuer, "ID-1",
                "Issuer address", "ES00 0000", "Customer", "ID-2", "Customer address",
                new BigDecimal("21"), false, lines);
    }

    private BudgetData quote() {
        return new BudgetData("Q-1", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 12, 31),
                "Supplier", "ID-1", "Address", "Client", "ID-2", "Address 2",
                "TERMS-UNIQUE-42", "NOTES-UNIQUE-84", true, "VAT", new BigDecimal("21"),
                false, Arrays.asList(new LineItem("Work", BigDecimal.ONE, BigDecimal.TEN,
                        BigDecimal.ZERO, LineCategory.SERVEI)));
    }

    private String allText(JasperPrint print) {
        StringBuilder text = new StringBuilder();
        for (JRPrintPage page : print.getPages()) text.append(pageText(page)).append('\n');
        return text.toString();
    }

    private String pageText(JRPrintPage page) {
        StringBuilder text = new StringBuilder();
        appendElements(page.getElements(), text);
        return text.toString();
    }

    private void appendElements(List<JRPrintElement> elements, StringBuilder text) {
        for (JRPrintElement element : elements) {
            if (element instanceof JRPrintText) {
                text.append(((JRPrintText) element).getFullText()).append('\n');
            } else if (element instanceof JRPrintFrame) {
                appendElements(((JRPrintFrame) element).getElements(), text);
            }
        }
    }

    private int occurrences(String value, String needle) {
        int count = 0;
        for (int index = value.indexOf(needle); index >= 0; index = value.indexOf(needle, index + needle.length())) {
            count++;
        }
        return count;
    }
}
