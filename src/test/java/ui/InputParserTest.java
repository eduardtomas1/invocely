package ui;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class InputParserTest {
    @Test
    void parsesStrictLeapDayAndDecimalComma() {
        assertEquals(LocalDate.of(2024, 2, 29), InputParser.requiredDate("29/02/2024"));
        assertEquals(0, new BigDecimal("21.5").compareTo(InputParser.percent("21,5")));
    }

    @Test
    void rejectsPlausibleButWrongFinancialInput() {
        assertThrows(IllegalArgumentException.class, () -> InputParser.requiredDate("31/02/2026"));
        assertThrows(IllegalArgumentException.class, () -> InputParser.percent("21%"));
        assertThrows(IllegalArgumentException.class, () -> InputParser.percent("-1"));
        assertThrows(IllegalArgumentException.class, () -> InputParser.percent("101"));
    }

    @Test
    void rejectsTinyExponentValuesBeforeTheyCanExpandToHugeStrings() {
        assertThrows(IllegalArgumentException.class, () -> InputParser.percent("1E-2147483647"));
        assertTimeout(java.time.Duration.ofSeconds(1),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> InputParser.percent("1E+2147483647")));
    }

    @Test
    void rejectsValidityBeforeIssueDate() {
        assertThrows(IllegalArgumentException.class, () -> InputParser.validDateRange(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 31)));
    }

    @Test
    void parsesAndBoundsLineNumbersImmediately() {
        assertEquals(0, new BigDecimal("2.5").compareTo(InputParser.lineNumber("2,5")));
        assertNull(InputParser.lineNumber(" "));
        assertThrows(IllegalArgumentException.class, () -> InputParser.lineNumber("12 units"));
        assertThrows(IllegalArgumentException.class, () -> InputParser.linePercent("-1"));
        assertThrows(IllegalArgumentException.class, () -> InputParser.linePercent("101"));
        assertTimeout(java.time.Duration.ofSeconds(1), () ->
                assertThrows(IllegalArgumentException.class,
                        () -> InputParser.lineNumber("1E+2147483647")));
    }
}
