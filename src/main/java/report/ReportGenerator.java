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

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ReportGenerator {

  private static final String INV_JRXML = "/reports/factura.jrxml";
  private static final String INV_JRXML_ES = "/reports/factura_es.jrxml";
  private static final String INV_JRXML_EN = "/reports/factura_en.jrxml";
  private static final String BUD_JRXML = "/reports/pressupost.jrxml";
  private static final String BUD_JRXML_ES = "/reports/pressupost_es.jrxml";
  private static final String BUD_JRXML_EN = "/reports/pressupost_en.jrxml";
  private final Map<String, JasperReport> compiledCache = new HashMap<>();

  public void exportInvoice(InvoiceData data, String type, Path target) throws JRException {
    JasperPrint print = fill(invoiceTemplate(), buildInvoiceParams(data), lineDataSource(data));
    export(print, target, type);
  }

  public void exportBudget(BudgetData data, String type, Path target) throws JRException {
    JasperPrint print = fill(budgetTemplate(), buildBudgetParams(data), lineDataSource(data));
    export(print, target, type);
  }

  private JasperPrint fill(String jrxmlPath, Map<String,Object> params, JRDataSource ds) throws JRException {
    JasperReport report = compileReport(jrxmlPath);
    return JasperFillManager.fillReport(report, params, ds);
  }

  private JasperReport compileReport(String jrxmlPath) throws JRException {
    JasperReport cached = compiledCache.get(jrxmlPath);
    if (cached != null) return cached;

    try (InputStream jrxml = getClass().getResourceAsStream(jrxmlPath)) {
      if (jrxml == null) {
        throw new JRException(I18n.t("report.template_missing", jrxmlPath));
      }
      JasperReport report = JasperCompileManager.compileReport(jrxml);
      compiledCache.put(jrxmlPath, report);
      return report;
    } catch (JRException e) {
      throw e;
    } catch (Exception e) {
      throw new JRException(I18n.t("report.template_load_error", jrxmlPath), e);
    }
  }

  private void export(JasperPrint print, Path target, String type) throws JRException {
    if (target == null) {
      throw new JRException(I18n.t("report.export_no_target"));
    }
    try {
      Path parent = target.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
    } catch (Exception e) {
      throw new JRException(I18n.t("report.export_dir_error", target.toAbsolutePath()), e);
    }
    switch (type.toLowerCase()) {
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
        xls.exportReport();
        break;
      default:
        throw new JRException(I18n.t("report.export_format_error", type));
    }
  }

  private JRBeanCollectionDataSource lineDataSource(InvoiceData data) {
    if (data == null || data.getLines() == null) {
      return new JRBeanCollectionDataSource(Collections.emptyList());
    }
    return new JRBeanCollectionDataSource(orderedLines(data.getLines(), data.isSplitLines()));
  }

  private JRBeanCollectionDataSource lineDataSource(BudgetData data) {
    if (data == null || data.getLines() == null) {
      return new JRBeanCollectionDataSource(Collections.emptyList());
    }
    return new JRBeanCollectionDataSource(orderedLines(data.getLines(), data.isSplitLines()));
  }

  private java.util.List<LineItem> orderedLines(java.util.List<LineItem> lines, boolean split) {
    java.util.List<LineItem> copy = new java.util.ArrayList<>(lines);
    if (split) {
      copy.sort(java.util.Comparator.comparing(
          LineItem::getCategory,
          java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
    }
    return copy;
  }

  private Map<String,Object> buildInvoiceParams(InvoiceData data) {
    Map<String,Object> params = new HashMap<>();
    if (data == null) return params;
    params.put("LOGO", resolveReportLogo());
    params.put(JRParameter.REPORT_LOCALE, I18n.getLocale());
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

  private Map<String,Object> buildBudgetParams(BudgetData data) {
    Map<String,Object> params = new HashMap<>();
    if (data == null) return params;
    params.put("LOGO", resolveReportLogo());
    params.put(JRParameter.REPORT_LOCALE, I18n.getLocale());
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

  private String invoiceTemplate() {
    if (I18n.isSpanish()) return INV_JRXML_ES;
    if (I18n.isEnglish()) return INV_JRXML_EN;
    return INV_JRXML;
  }

  private String budgetTemplate() {
    if (I18n.isSpanish()) return BUD_JRXML_ES;
    if (I18n.isEnglish()) return BUD_JRXML_EN;
    return BUD_JRXML;
  }

  private Image resolveReportLogo() {
    String customLogoPath = AppPreferences.getReportLogoPath();
    if (customLogoPath != null && !customLogoPath.isBlank()) {
      try {
        Path path = Path.of(customLogoPath).toAbsolutePath().normalize();
        if (Files.exists(path) && Files.isRegularFile(path)) {
          try (InputStream in = Files.newInputStream(path)) {
            Image custom = ImageIO.read(in);
            if (custom != null) return custom;
          }
        }
      } catch (Exception ignored) { }
    }

    try (InputStream in = getClass().getResourceAsStream("/reports/logo.png")) {
      if (in == null) return null;
      return ImageIO.read(in);
    } catch (Exception ignored) {
      return null;
    }
  }
}
