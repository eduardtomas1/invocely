package report;

import i18n.I18n;
import models.*;
import storage.AppPreferences;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import storage.SafeFiles;
import validation.DocumentValidator;

import java.awt.Image;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class ReportGenerator {

  private static final String INV_JRXML = "/reports/factura.jrxml";
  private static final String INV_JRXML_ES = "/reports/factura_es.jrxml";
  private static final String INV_JRXML_EN = "/reports/factura_en.jrxml";
  private static final String BUD_JRXML = "/reports/pressupost.jrxml";
  private static final String BUD_JRXML_ES = "/reports/pressupost_es.jrxml";
  private static final String BUD_JRXML_EN = "/reports/pressupost_en.jrxml";
  private static final Map<String, JasperReport> COMPILED_CACHE = new ConcurrentHashMap<>();
  private static final Object COMPILE_LOCK = new Object();
  private static volatile CachedLogo cachedLogo;

  public void exportInvoice(InvoiceData data, String type, Path target) throws JRException {
    exportInvoice(data, type, target, I18n.getLocale());
  }

  public void exportInvoice(InvoiceData data, String type, Path target, Locale locale) throws JRException {
    DocumentValidator.validateInvoice(data);
    boolean csv = "csv".equalsIgnoreCase(type);
    Map<String, Object> params = buildInvoiceParams(data, locale);
    JasperPrint print = fill(invoiceTemplate(locale), csvSafeParams(params, csv), lineDataSource(data, csv));
    export(print, target, type);
  }

  public void exportBudget(BudgetData data, String type, Path target) throws JRException {
    exportBudget(data, type, target, I18n.getLocale());
  }

  public void exportBudget(BudgetData data, String type, Path target, Locale locale) throws JRException {
    DocumentValidator.validateBudget(data);
    boolean csv = "csv".equalsIgnoreCase(type);
    Map<String, Object> params = buildBudgetParams(data, locale);
    JasperPrint print = fill(budgetTemplate(locale), csvSafeParams(params, csv), lineDataSource(data, csv));
    export(print, target, type);
  }

  JasperPrint prepareInvoice(InvoiceData data, Locale locale) throws JRException {
    DocumentValidator.validateInvoice(data);
    return fill(invoiceTemplate(locale), buildInvoiceParams(data, locale), lineDataSource(data, false));
  }

  JasperPrint prepareBudget(BudgetData data, Locale locale) throws JRException {
    DocumentValidator.validateBudget(data);
    return fill(budgetTemplate(locale), buildBudgetParams(data, locale), lineDataSource(data, false));
  }

  private JasperPrint fill(String jrxmlPath, Map<String,Object> params, JRDataSource ds) throws JRException {
    JasperReport report = compileReport(jrxmlPath);
    return JasperFillManager.fillReport(report, params, ds);
  }

  private JasperReport compileReport(String jrxmlPath) throws JRException {
    JasperReport cached = COMPILED_CACHE.get(jrxmlPath);
    if (cached != null) return cached;

    synchronized (COMPILE_LOCK) {
      cached = COMPILED_CACHE.get(jrxmlPath);
      if (cached != null) return cached;
      try (InputStream jrxml = getClass().getResourceAsStream(jrxmlPath)) {
        if (jrxml == null) {
          throw new JRException(I18n.t("report.template_missing", jrxmlPath));
        }
        JasperReport report = JasperCompileManager.compileReport(jrxml);
        COMPILED_CACHE.put(jrxmlPath, report);
        return report;
      } catch (JRException e) {
        throw e;
      } catch (Exception e) {
        throw new JRException(I18n.t("report.template_load_error", jrxmlPath), e);
      }
    }
  }

  private void export(JasperPrint print, Path target, String type) throws JRException {
    if (target == null) {
      throw new JRException(I18n.t("report.export_no_target"));
    }
    try {
      SafeFiles.writePathAtomically(target, false, temporary -> exportDirect(print, temporary, type));
    } catch (JRException e) {
      throw e;
    } catch (Exception e) {
      throw new JRException(I18n.t("report.export_dir_error", target.toAbsolutePath()), e);
    }
  }

  private void exportDirect(JasperPrint print, Path target, String type) throws JRException {
    switch (type.toLowerCase(Locale.ROOT)) {
      case "pdf":
        JasperExportManager.exportReportToPdfFile(print, target.toString());
        break;
      case "csv":
        JRCsvExporter csv = new JRCsvExporter();
        csv.setExporterInput(new SimpleExporterInput(print));
        csv.setExporterOutput(new SimpleWriterExporterOutput(target.toString()));
        csv.exportReport();
        break;
      case "xlsx":
        JRXlsxExporter xls = new JRXlsxExporter();
        xls.setExporterInput(new SimpleExporterInput(print));
        xls.setExporterOutput(new SimpleOutputStreamExporterOutput(target.toString()));
        SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
        configuration.setDetectCellType(true);
        configuration.setRemoveEmptySpaceBetweenRows(true);
        xls.setConfiguration(configuration);
        xls.exportReport();
        break;
      default:
        throw new JRException(I18n.t("report.export_format_error", type));
    }
  }

  private JRBeanCollectionDataSource lineDataSource(InvoiceData data, boolean csvSafe) {
    if (data == null || data.getLines() == null) {
      return new JRBeanCollectionDataSource(Collections.emptyList());
    }
    return new JRBeanCollectionDataSource(orderedLines(data.getLines(), data.isSplitLines(), csvSafe));
  }

  private JRBeanCollectionDataSource lineDataSource(BudgetData data, boolean csvSafe) {
    if (data == null || data.getLines() == null) {
      return new JRBeanCollectionDataSource(Collections.emptyList());
    }
    return new JRBeanCollectionDataSource(orderedLines(data.getLines(), data.isSplitLines(), csvSafe));
  }

  private java.util.List<LineItem> orderedLines(java.util.List<LineItem> lines, boolean split, boolean csvSafe) {
    java.util.List<LineItem> copy = new java.util.ArrayList<>();
    for (LineItem line : lines) {
      if (line == null) continue;
      copy.add(csvSafe
          ? new LineItem(sanitizeCsvValue(line.getDescription()), line.getQuantity(), line.getUnitPrice(),
              line.getDiscountPercent(), line.getCategory())
          : line);
    }
    if (split) {
      copy.sort(java.util.Comparator.comparing(
          LineItem::getCategory,
          java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
    }
    return copy;
  }

  private Map<String,Object> buildInvoiceParams(InvoiceData data, Locale locale) {
    Map<String,Object> params = new HashMap<>();
    if (data == null) return params;
    params.put("LOGO", resolveReportLogo());
    params.put(JRParameter.REPORT_LOCALE, normalizedLocale(locale));
    params.put("INVOICE_NUMBER", data.getInvoiceNumber());
    params.put("ISSUE_DATE", data.getIssueDate());
    params.put("ISSUER_NAME", data.getIssuerName());
    params.put("ISSUER_NIF", data.getIssuerNif());
    params.put("ISSUER_ADDRESS", data.getIssuerAddress());
    params.put("ISSUER_ACCOUNT", data.getIssuerAccount());
    params.put("CUSTOMER_NAME", data.getCustomerName());
    params.put("CUSTOMER_NIF", data.getCustomerNif());
    params.put("CUSTOMER_ADDRESS", data.getCustomerAddress());
    params.put("VAT_PERCENT", data.getVatPercent());
    params.put("SUBTOTAL", data.getSubtotal());
    params.put("DISCOUNT_TOTAL", data.getDiscountTotal());
    params.put("VAT_AMOUNT", data.getVatAmount());
    params.put("GRAND_TOTAL", data.getGrandTotal());
    params.put("HAS_DISCOUNTS", data.hasDiscounts());
    params.put("SPLIT_LINES", data.isSplitLines());
    return params;
  }

  private Map<String,Object> buildBudgetParams(BudgetData data, Locale locale) {
    Map<String,Object> params = new HashMap<>();
    if (data == null) return params;
    params.put("LOGO", resolveReportLogo());
    params.put(JRParameter.REPORT_LOCALE, normalizedLocale(locale));
    params.put("BUDGET_NUMBER", data.getBudgetNumber());
    params.put("ISSUE_DATE", data.getIssueDate());
    params.put("VALID_UNTIL", data.getValidUntil());
    params.put("SUPPLIER_NAME", data.getSupplierName());
    params.put("SUPPLIER_NIF", data.getSupplierNif());
    params.put("SUPPLIER_ADDRESS", data.getSupplierAddress());
    params.put("CLIENT_NAME", data.getClientName());
    params.put("CLIENT_NIF", data.getClientNif());
    params.put("CLIENT_ADDRESS", data.getClientAddress());
    params.put("PAYMENT_TERMS", data.getPaymentTerms());
    params.put("NOTES", data.getNotes());
    params.put("SUBTOTAL", data.getSubtotal());
    params.put("DISCOUNT_TOTAL", data.getDiscountTotal());
    params.put("GRAND_TOTAL", data.getGrandTotal());
    params.put("HAS_DISCOUNTS", data.hasDiscounts());
    params.put("INCLUDE_TOTALS", data.isIncludeTotals());
    params.put("TAX_NAME", data.getTaxName());
    params.put("TAX_PERCENT", data.getTaxPercent());
    params.put("TAX_AMOUNT", data.getTaxAmount());
    params.put("SPLIT_LINES", data.isSplitLines());
    return params;
  }

  private String invoiceTemplate(Locale locale) {
    String language = normalizedLocale(locale).getLanguage();
    if ("es".equalsIgnoreCase(language)) return INV_JRXML_ES;
    if ("en".equalsIgnoreCase(language)) return INV_JRXML_EN;
    return INV_JRXML;
  }

  private String budgetTemplate(Locale locale) {
    String language = normalizedLocale(locale).getLanguage();
    if ("es".equalsIgnoreCase(language)) return BUD_JRXML_ES;
    if ("en".equalsIgnoreCase(language)) return BUD_JRXML_EN;
    return BUD_JRXML;
  }

  private Image resolveReportLogo() {
    String customLogoPath = AppPreferences.getReportLogoPath();
    if (customLogoPath != null && !customLogoPath.isBlank()) {
      try {
        Path path = Path.of(customLogoPath).toAbsolutePath().normalize();
        if (Files.exists(path) && Files.isRegularFile(path)) {
          long modified = Files.getLastModifiedTime(path).toMillis();
          long size = Files.size(path);
          CachedLogo current = cachedLogo;
          if (current != null && current.matches(path, modified, size)) return current.image;
          Image custom = SafeImageLoader.read(path);
          cachedLogo = new CachedLogo(path, modified, size, custom);
          return custom;
        }
      } catch (Exception ignored) { }
    }

    try (InputStream in = getClass().getResourceAsStream("/reports/logo.png")) {
      if (in == null) return null;
      return javax.imageio.ImageIO.read(in);
    } catch (Exception ignored) {
      return null;
    }
  }

  private Map<String, Object> csvSafeParams(Map<String, Object> params, boolean csvSafe) {
    if (!csvSafe) return params;
    Map<String, Object> copy = new HashMap<>(params.size());
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      Object value = entry.getValue();
      copy.put(entry.getKey(), value instanceof String ? sanitizeCsvValue((String) value) : value);
    }
    return copy;
  }

  static String sanitizeCsvValue(String value) {
    if (value == null || value.isEmpty()) return value;
    int index = 0;
    while (index < value.length() && Character.isWhitespace(value.charAt(index))) index++;
    if (index >= value.length()) return value;
    char first = value.charAt(index);
    if (first == '=' || first == '+' || first == '-' || first == '@') return "'" + value;
    return value;
  }

  static int cachedTemplateCount() {
    return COMPILED_CACHE.size();
  }

  private Locale normalizedLocale(Locale locale) {
    return locale != null ? locale : Locale.forLanguageTag("en-US");
  }

  private static final class CachedLogo {
    private final Path path;
    private final long modified;
    private final long size;
    private final Image image;

    private CachedLogo(Path path, long modified, long size, Image image) {
      this.path = path;
      this.modified = modified;
      this.size = size;
      this.image = image;
    }

    private boolean matches(Path candidate, long candidateModified, long candidateSize) {
      return path.equals(candidate) && modified == candidateModified && size == candidateSize;
    }
  }
}
