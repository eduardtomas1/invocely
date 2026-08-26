package ui;

import i18n.I18n;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import models.LineItem;

/** Strict parsing shared by the main forms and the defaults editor. */
public final class InputParser {
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final BigDecimal MAX_ABSOLUTE_NUMBER = new BigDecimal("1000000000000000");

    private InputParser() { }

    public static LocalDate requiredDate(String value) {
        LocalDate parsed = optionalDate(value);
        if (parsed == null) throw new IllegalArgumentException(I18n.t("validation.date_required"));
        return parsed;
    }

    public static LocalDate optionalDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(value.trim(), DISPLAY_DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(I18n.t("validation.invalid_date"), ex);
        }
    }

    public static BigDecimal percent(String value) {
        if (value == null || value.trim().isEmpty()) return BigDecimal.ZERO;
        final BigDecimal parsed;
        try {
            parsed = new BigDecimal(value.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(I18n.t("validation.invalid_percent"), ex);
        }
        if (parsed.precision() > 8 || parsed.scale() < 0 || parsed.scale() > 4) {
            throw new IllegalArgumentException(I18n.t("validation.invalid_percent"));
        }
        if (parsed.compareTo(BigDecimal.ZERO) < 0 || parsed.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(I18n.t("validation.percent_range"));
        }
        return parsed;
    }

    public static void validDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException(I18n.t("validation.date_range"));
        }
    }

    public static void validLineItems(List<LineItem> lines) {
        if (lines == null) return;
        for (LineItem line : lines) {
            if (line == null) continue;
            boundedNumber(line.getQuantity());
            boundedNumber(line.getUnitPrice());
            percent(line.getDiscountPercent() != null ? line.getDiscountPercent().toString() : "0");
            if (line.getDescription() != null && line.getDescription().length() > 20_000) {
                throw new IllegalArgumentException(I18n.t("validation.line_too_long"));
            }
        }
    }

    private static void boundedNumber(BigDecimal value) {
        if (value == null) return;
        if (value.precision() > 24 || value.scale() < -6 || value.scale() > 8
                || value.abs().compareTo(MAX_ABSOLUTE_NUMBER) > 0) {
            throw new IllegalArgumentException(I18n.t("validation.number_range"));
        }
    }
}
