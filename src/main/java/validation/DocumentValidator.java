package validation;

import i18n.I18n;
import models.BudgetData;
import models.InvoiceData;
import models.LineItem;

import java.math.BigDecimal;
import java.util.List;

/** Shared safety bounds for editable documents and every persistence/export sink. */
public final class DocumentValidator {
    public static final int MAX_LINES = 10_000;
    public static final int MAX_TEXT_LENGTH = 20_000;
    public static final int MAX_TOTAL_TEXT_LENGTH = 400_000;
    private static final BigDecimal MAX_ABSOLUTE_NUMBER = new BigDecimal("1000000000000000");

    private DocumentValidator() { }

    public static void validateInvoice(InvoiceData data) {
        if (data == null) throw new IllegalArgumentException(I18n.t("validation.document_required"));
        long textLength = validateTexts(data.getInvoiceNumber(), data.getIssuerName(), data.getIssuerNif(),
                data.getIssuerAddress(), data.getIssuerAccount(), data.getCustomerName(),
                data.getCustomerNif(), data.getCustomerAddress());
        validatePercent(data.getVatPercent());
        validateLines(data.getLines(), textLength);
    }

    public static void validateBudget(BudgetData data) {
        if (data == null) throw new IllegalArgumentException(I18n.t("validation.document_required"));
        if (data.getIssueDate() == null || data.getValidUntil() == null) {
            throw new IllegalArgumentException(I18n.t("validation.date_required"));
        }
        long textLength = validateTexts(data.getBudgetNumber(), data.getSupplierName(), data.getSupplierNif(),
                data.getSupplierAddress(), data.getClientName(), data.getClientNif(), data.getClientAddress(),
                data.getPaymentTerms(), data.getNotes(), data.getTaxName());
        validatePercent(data.getTaxPercent());
        if (data.getValidUntil().isBefore(data.getIssueDate())) {
            throw new IllegalArgumentException(I18n.t("validation.date_range"));
        }
        validateLines(data.getLines(), textLength);
    }

    public static void validateLines(List<LineItem> lines) {
        validateLines(lines, 0);
    }

    private static void validateLines(List<LineItem> lines, long textLength) {
        if (lines == null) return;
        if (lines.size() > MAX_LINES) {
            throw new IllegalArgumentException(I18n.t("validation.too_many_lines"));
        }
        for (LineItem line : lines) {
            if (line == null) continue;
            validateText(line.getDescription());
            textLength += length(line.getDescription());
            validateTotalTextLength(textLength);
            validateNumber(line.getQuantity());
            validateNumber(line.getUnitPrice());
            validatePercent(line.getDiscountPercent());
        }
    }

    public static void validateText(String value) {
        if (value != null && value.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(I18n.t("validation.field_too_long"));
        }
    }

    private static long validateTexts(String... values) {
        long total = 0;
        for (String value : values) {
            validateText(value);
            total += length(value);
        }
        validateTotalTextLength(total);
        return total;
    }

    private static int length(String value) {
        return value != null ? value.length() : 0;
    }

    private static void validateTotalTextLength(long length) {
        if (length > MAX_TOTAL_TEXT_LENGTH) {
            throw new IllegalArgumentException(I18n.t("validation.document_too_large"));
        }
    }

    public static void validateNumber(BigDecimal value) {
        if (value == null) return;
        if (value.precision() > 24 || value.scale() < -6 || value.scale() > 8
                || value.abs().compareTo(MAX_ABSOLUTE_NUMBER) > 0) {
            throw new IllegalArgumentException(I18n.t("validation.number_range"));
        }
    }

    public static void validatePercent(BigDecimal value) {
        if (value == null) return;
        validateNumber(value);
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(I18n.t("validation.percent_range"));
        }
    }
}
