package app.invocely;

import i18n.I18n;
import models.BudgetData;
import models.InvoiceData;
import models.LineCategory;
import models.LineItem;
import report.ReportGenerator;
import storage.XmlSaver;
import ui.BudgetPanel;
import ui.InvoicePanel;
import ui.ThemeManager;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dependency-free performance probe for release engineering. It reports evidence rather than
 * enforcing machine-dependent timing thresholds.
 *
 * Run with:
 * ./mvnw -B -ntp -Dexec.classpathScope=test -Dexec.executable=java \
 *   -Dexec.args="-cp %classpath app.invocely.PerformanceBenchmark" test-compile exec:exec
 */
public final class PerformanceBenchmark {
    private static final int XML_LINE_COUNT = 10_000;
    private static final int REPORT_LINE_COUNT = 500;

    private PerformanceBenchmark() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path directory = Files.createTempDirectory("invocely-performance-");
        try {
            benchmarkStartupInitialization();
            benchmarkXml(directory);
            benchmarkReports(directory);
            benchmarkUiResponsiveness(directory);
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) { }
                });
            }
        }
    }

    private static void benchmarkStartupInitialization() throws Exception {
        long start = System.nanoTime();
        SwingUtilities.invokeAndWait(() -> { });
        print("startup.awt-event-thread", start, "first event dispatch in this JVM");

        start = System.nanoTime();
        SwingUtilities.invokeAndWait(I18n::init);
        print("startup.i18n", start, "preferences and resource bundle");

        start = System.nanoTime();
        SwingUtilities.invokeAndWait(ThemeManager::bootstrap);
        print("startup.theme", start, "look and feel plus UI defaults");

        start = System.nanoTime();
        SwingUtilities.invokeAndWait(InvoicePanel::new);
        print("startup.invoice-panel", start, "first document panel");

        start = System.nanoTime();
        SwingUtilities.invokeAndWait(BudgetPanel::new);
        print("startup.quote-panel", start, "additional document panel");
    }

    private static void benchmarkXml(Path directory) throws Exception {
        InvoiceData invoice = invoice(XML_LINE_COUNT);
        BudgetData quote = quote(XML_LINE_COUNT);
        XmlSaver saver = new XmlSaver(directory);
        Path invoiceTarget = directory.resolve("large-invoice.xml");
        Path quoteTarget = directory.resolve("large-quote.xml");

        long start = System.nanoTime();
        saver.saveInvoice(invoice, invoiceTarget);
        print("xml.invoice-save-" + XML_LINE_COUNT, start, Files.size(invoiceTarget) + " bytes");

        start = System.nanoTime();
        InvoiceData loadedInvoice = saver.loadInvoice(invoiceTarget);
        print("xml.invoice-load-" + XML_LINE_COUNT, start, loadedInvoice.getLines().size() + " lines");

        start = System.nanoTime();
        saver.saveBudget(quote, quoteTarget);
        print("xml.quote-save-" + XML_LINE_COUNT, start, Files.size(quoteTarget) + " bytes");

        start = System.nanoTime();
        BudgetData loadedQuote = saver.loadBudget(quoteTarget);
        print("xml.quote-load-" + XML_LINE_COUNT, start, loadedQuote.getLines().size() + " lines");

        requireLineCount("invoice XML", loadedInvoice.getLines().size(), XML_LINE_COUNT);
        requireLineCount("quote XML", loadedQuote.getLines().size(), XML_LINE_COUNT);
    }

    private static void benchmarkReports(Path directory) throws Exception {
        ReportGenerator generator = new ReportGenerator();
        Locale locale = Locale.forLanguageTag("en-US");
        InvoiceData invoice = invoice(REPORT_LINE_COUNT);
        BudgetData quote = quote(REPORT_LINE_COUNT);

        long start = System.nanoTime();
        generator.exportInvoice(invoice, "pdf", directory.resolve("invoice-cold.pdf"), locale);
        print("report.invoice-cold-" + REPORT_LINE_COUNT, start, "compile, fill, PDF export");

        start = System.nanoTime();
        generator.exportInvoice(invoice, "pdf", directory.resolve("invoice-warm.pdf"), locale);
        print("report.invoice-warm-" + REPORT_LINE_COUNT, start, "cached template, fill, PDF export");

        start = System.nanoTime();
        generator.exportBudget(quote, "pdf", directory.resolve("quote-cold.pdf"), locale);
        print("report.quote-cold-" + REPORT_LINE_COUNT, start, "compile, fill, PDF export");

        start = System.nanoTime();
        generator.exportBudget(quote, "pdf", directory.resolve("quote-warm.pdf"), locale);
        print("report.quote-warm-" + REPORT_LINE_COUNT, start, "cached template, fill, PDF export");
    }

    private static void benchmarkUiResponsiveness(Path directory) throws Exception {
        CountDownLatch workStarted = new CountDownLatch(1);
        CountDownLatch eventProcessed = new CountDownLatch(1);
        CountDownLatch saveCompleted = new CountDownLatch(1);
        AtomicLong workStart = new AtomicLong();
        AtomicLong eventTime = new AtomicLong();
        AtomicBoolean workFinished = new AtomicBoolean();
        AtomicBoolean workHadFinishedAtEvent = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        XmlSaver saver = new XmlSaver(directory);
        InvoiceData invoice = invoice(XML_LINE_COUNT);
        Path target = directory.resolve("responsive-save.xml");

        SwingUtilities.invokeAndWait(() -> BackgroundTaskRunner.run(() -> {
            workStart.set(System.nanoTime());
            workStarted.countDown();
            SwingUtilities.invokeLater(() -> {
                eventTime.set(System.nanoTime());
                workHadFinishedAtEvent.set(workFinished.get());
                eventProcessed.countDown();
            });
            try {
                return saver.saveInvoice(invoice, target);
            } finally {
                workFinished.set(true);
            }
        }, saved -> saveCompleted.countDown(), error -> {
            failure.set(error);
            saveCompleted.countDown();
        }));

        if (!workStarted.await(5, TimeUnit.SECONDS)
                || !eventProcessed.await(5, TimeUnit.SECONDS)
                || !saveCompleted.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("UI responsiveness probe timed out.");
        }
        if (failure.get() != null) throw new IllegalStateException("UI save probe failed.", failure.get());
        double milliseconds = (eventTime.get() - workStart.get()) / 1_000_000.0;
        System.out.printf(Locale.ROOT, "%-34s %9.2f ms  processed before save completed: %s%n",
                "ui.event-delay-during-save", milliseconds, !workHadFinishedAtEvent.get());
    }

    private static InvoiceData invoice(int lineCount) {
        return new InvoiceData("PERF-1", LocalDate.of(2026, 1, 15),
                "Benchmark Issuer", "ID-1", "Issuer address", "ES00 0000",
                "Benchmark Customer", "ID-2", "Customer address", new BigDecimal("21"), true,
                lines(lineCount));
    }

    private static BudgetData quote(int lineCount) {
        return new BudgetData("PERF-Q-1", LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 12, 31), "Benchmark Supplier", "ID-1", "Address",
                "Benchmark Client", "ID-2", "Address 2", "30 days", "Benchmark notes",
                true, "VAT", new BigDecimal("21"), true, lines(lineCount));
    }

    private static List<LineItem> lines(int count) {
        List<LineItem> lines = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            lines.add(new LineItem("Benchmark line " + index + " with reproducible content",
                    new BigDecimal("2.5"), new BigDecimal("80.25"), new BigDecimal("10"),
                    index % 2 == 0 ? LineCategory.SERVEI : LineCategory.MATERIAL));
        }
        return lines;
    }

    private static void requireLineCount(String operation, int actual, int expected) {
        if (actual != expected) {
            throw new IllegalStateException(operation + " lost line items: " + actual + " of " + expected);
        }
    }

    private static void print(String operation, long startNanos, String detail) {
        double milliseconds = (System.nanoTime() - startNanos) / 1_000_000.0;
        System.out.printf(Locale.ROOT, "%-34s %9.2f ms  %s%n", operation, milliseconds, detail);
    }
}
