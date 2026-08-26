package app.invocely;

import models.InvoiceData;
import models.LineCategory;
import models.LineItem;
import report.ReportGenerator;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.ZipFile;

/** Noninteractive release smoke test used by CI against the packaged application. */
final class DistributionVerifier {
    private DistributionVerifier() { }

    static void verify(Path outputDirectory) throws Exception {
        Files.createDirectories(outputDirectory);
        InvoiceData invoice = new InvoiceData(
                "CI-VERIFY-1", LocalDate.of(2026, 1, 15),
                "Invoicely Verification", "ID-1", "Offline", "ES00 0000",
                "Example Customer", "ID-2", "Offline",
                new BigDecimal("21"), false,
                Arrays.asList(new LineItem("=SAFE-CSV", BigDecimal.ONE,
                        new BigDecimal("10.50"), BigDecimal.ZERO, LineCategory.SERVEI)));

        Path pdf = outputDirectory.resolve("verification.pdf");
        Path xlsx = outputDirectory.resolve("verification.xlsx");
        Path csv = outputDirectory.resolve("verification.csv");
        ReportGenerator generator = new ReportGenerator();
        Locale locale = Locale.forLanguageTag("en-US");
        generator.exportInvoice(invoice, "pdf", pdf, locale);
        generator.exportInvoice(invoice, "xlsx", xlsx, locale);
        generator.exportInvoice(invoice, "csv", csv, locale);

        String pdfHeader = Files.readString(pdf, StandardCharsets.ISO_8859_1).substring(0, 4);
        if (!"%PDF".equals(pdfHeader)) {
            throw new IllegalStateException("Packaged PDF export is not readable.");
        }
        try (ZipFile workbook = new ZipFile(xlsx.toFile())) {
            if (workbook.getEntry("xl/worksheets/sheet1.xml") == null) {
                throw new IllegalStateException("Packaged XLSX export is not readable.");
            }
        }
        if (!Files.readString(csv, StandardCharsets.UTF_8).contains("'=SAFE-CSV")) {
            throw new IllegalStateException("Packaged CSV export did not preserve formula safety.");
        }
    }
}
