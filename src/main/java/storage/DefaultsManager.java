package storage;

import i18n.I18n;
import models.BudgetData;
import models.InvoiceData;
import models.LineItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Simple persistence for default field values (without lines).
 */
public class DefaultsManager {
    private final Path baseDir;

    public DefaultsManager() {
        this(AppPaths.defaultsDir());
    }

    DefaultsManager(Path baseDir) {
        this.baseDir = baseDir;
    }

    public void saveInvoiceDefaults(InvoiceData data) throws IOException {
        Properties p = new Properties();
        p.setProperty("invoiceNumber", nz(data.getInvoiceNumber()));
        p.setProperty("issueDate", formatDate(data.getIssueDate()));
        p.setProperty("issuerName", nz(data.getIssuerName()));
        p.setProperty("issuerNif", nz(data.getIssuerNif()));
        p.setProperty("issuerAddress", nz(data.getIssuerAddress()));
        p.setProperty("issuerAccount", nz(data.getIssuerAccount()));
        p.setProperty("customerName", nz(data.getCustomerName()));
        p.setProperty("customerNif", nz(data.getCustomerNif()));
        p.setProperty("customerAddress", nz(data.getCustomerAddress()));
        p.setProperty("vatPercent", toStr(data.getVatPercent()));
        p.setProperty("splitLines", String.valueOf(data.isSplitLines()));
        write(p, baseDir.resolve("invoice.xml"));
    }

    public InvoiceData loadInvoiceDefaults() throws IOException {
        Properties p = readDefaults("invoice");
        if (p == null) return null;
        return new InvoiceData(
                p.getProperty("invoiceNumber", ""),
                parseDate(p.getProperty("issueDate")),
                p.getProperty("issuerName", ""),
                p.getProperty("issuerNif", ""),
                p.getProperty("issuerAddress", ""),
                p.getProperty("issuerAccount", ""),
                p.getProperty("customerName", ""),
                p.getProperty("customerNif", ""),
                p.getProperty("customerAddress", ""),
                parseBD(p.getProperty("vatPercent")),
                parseBoolean(p, "splitLines", false),
                new ArrayList<LineItem>()
        );
    }

    public void saveBudgetDefaults(BudgetData data) throws IOException {
        validateDateRange(data.getIssueDate(), data.getValidUntil());
        Properties p = new Properties();
        p.setProperty("budgetNumber", nz(data.getBudgetNumber()));
        p.setProperty("issueDate", formatDate(data.getIssueDate()));
        p.setProperty("validUntil", formatDate(data.getValidUntil()));
        p.setProperty("supplierName", nz(data.getSupplierName()));
        p.setProperty("supplierNif", nz(data.getSupplierNif()));
        p.setProperty("supplierAddress", nz(data.getSupplierAddress()));
        p.setProperty("clientName", nz(data.getClientName()));
        p.setProperty("clientNif", nz(data.getClientNif()));
        p.setProperty("clientAddress", nz(data.getClientAddress()));
        p.setProperty("paymentTerms", nz(data.getPaymentTerms()));
        p.setProperty("notes", nz(data.getNotes()));
        p.setProperty("includeTotals", String.valueOf(data.isIncludeTotals()));
        p.setProperty("taxName", nz(data.getTaxName()));
        p.setProperty("taxPercent", toStr(data.getTaxPercent()));
        p.setProperty("splitLines", String.valueOf(data.isSplitLines()));
        write(p, baseDir.resolve("budget.xml"));
    }

    public BudgetData loadBudgetDefaults() throws IOException {
        Properties p = readDefaults("budget");
        if (p == null) return null;
        LocalDate issueDate = parseDate(p.getProperty("issueDate"));
        LocalDate validUntil = parseDate(p.getProperty("validUntil"));
        validateDateRange(issueDate, validUntil);
        return new BudgetData(
                p.getProperty("budgetNumber", ""),
                issueDate,
                validUntil,
                p.getProperty("supplierName", ""),
                p.getProperty("supplierNif", ""),
                p.getProperty("supplierAddress", ""),
                p.getProperty("clientName", ""),
                p.getProperty("clientNif", ""),
                p.getProperty("clientAddress", ""),
                p.getProperty("paymentTerms", ""),
                p.getProperty("notes", ""),
                parseBoolean(p, "includeTotals", false),
                p.getProperty("taxName", "IVA"),
                parseBD(p.getProperty("taxPercent")),
                parseBoolean(p, "splitLines", false),
                new ArrayList<LineItem>()
        );
    }

    private void write(Properties p, Path target) throws IOException {
        try {
            Path parent = target.toAbsolutePath().normalize().getParent();
            if (parent == null) throw new IOException(I18n.t("validation.invalid_file_name"));
            Path appBase = AppPaths.baseDir().toAbsolutePath().normalize();
            if (parent.startsWith(appBase)) {
                AppPaths.ensurePrivateDirectory(parent);
            } else {
                SafeFiles.createDirectories(parent, true);
            }
            SafeFiles.writeAtomically(target, true,
                    out -> p.storeToXML(out, "Default values", "UTF-8"));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Could not save default values.", e);
        }
    }

    private Properties read(Path file) throws IOException {
        SafeFiles.requireReadableFile(file, 1024L * 1024L);
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.loadFromXML(in);
        }
        return p;
    }

    private Properties readLegacy(Path file) throws IOException {
        SafeFiles.requireReadableFile(file, 1024L * 1024L);
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        }
        return p;
    }

    private Properties readDefaults(String baseName) throws IOException {
        try {
            Path xml = baseDir.resolve(baseName + ".xml");
            if (Files.exists(xml)) return read(xml);
            Path legacy = baseDir.resolve(baseName + ".properties");
            if (Files.exists(legacy)) return readLegacy(legacy);
            return null;
        } catch (IOException | RuntimeException ex) {
            throw new IOException(I18n.t("storage.defaults_read_error"), ex);
        }
    }

    private String nz(String s) { return s == null ? "" : s; }

    private String formatDate(LocalDate d) {
        if (d == null) return "";
        return String.format("%04d-%02d-%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }

    private LocalDate parseDate(String s) throws IOException {
        try {
            if (s == null || s.isBlank()) return null;
            return LocalDate.parse(s);
        } catch (Exception e) {
            throw new IOException(I18n.t("storage.defaults_read_error"), e);
        }
    }

    private BigDecimal parseBD(String s) throws IOException {
        try {
            if (s == null || s.isBlank()) return BigDecimal.ZERO;
            BigDecimal value = new BigDecimal(s.trim().replace(',', '.'));
            if (value.precision() > 8 || value.scale() < 0 || value.scale() > 4
                    || value.compareTo(BigDecimal.ZERO) < 0
                    || value.compareTo(new BigDecimal("100")) > 0) {
                throw new NumberFormatException("percentage outside supported range");
            }
            return value;
        } catch (Exception e) {
            throw new IOException(I18n.t("storage.defaults_read_error"), e);
        }
    }

    private boolean parseBoolean(Properties properties, String key, boolean defaultValue) throws IOException {
        String raw = properties.getProperty(key);
        if (raw == null) return defaultValue;
        String value = raw.trim();
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IOException(I18n.t("storage.defaults_read_error"));
    }

    private void validateDateRange(LocalDate issueDate, LocalDate validUntil) throws IOException {
        if ((issueDate == null) != (validUntil == null)) {
            throw new IOException(I18n.t("validation.default_dates_pair"));
        }
        if (issueDate != null && validUntil != null && validUntil.isBefore(issueDate)) {
            throw new IOException(I18n.t("validation.date_range"));
        }
    }

    private String toStr(BigDecimal bd) {
        return bd != null ? bd.toPlainString() : "";
    }
}
