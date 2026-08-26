package models;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentTotalsTest {
    private static LineItem line(String quantity, String price, String discount) {
        return new LineItem("Work", new BigDecimal(quantity), new BigDecimal(price),
                new BigDecimal(discount), LineCategory.SERVEI);
    }

    private static void decimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
    }

    @Test
    void invoiceCalculatesDiscountTaxAndGrandTotal() {
        InvoiceData invoice = new InvoiceData("INV-1", LocalDate.of(2026, 1, 2),
                "Issuer", "ID", "Address", "Account", "Customer", "ID2", "Address2",
                new BigDecimal("21"), false,
                Arrays.asList(line("2", "100", "10"), line("1", "50", "0"), null));

        decimalEquals("250", invoice.getSubtotal());
        decimalEquals("20", invoice.getDiscountTotal());
        decimalEquals("230", invoice.getTaxableBase());
        decimalEquals("48.3", invoice.getVatAmount());
        decimalEquals("278.3", invoice.getGrandTotal());
    }

    @Test
    void quoteRoundsDisplayedMoneyConsistently() {
        BudgetData quote = new BudgetData("Q-1", LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 2, 2), "Supplier", "ID", "Address", "Client", "ID2",
                "Address2", "30 days", "Notes", true, "VAT", new BigDecimal("7.5"),
                false, Arrays.asList(line("3", "19.99", "5")));

        decimalEquals("59.97", quote.getSubtotal());
        decimalEquals("3.00", quote.getDiscountTotal());
        decimalEquals("56.97", quote.getTaxableBase());
        decimalEquals("4.27", quote.getTaxAmount());
        decimalEquals("61.24", quote.getGrandTotal());
    }

    @Test
    void subtotalMatchesTheSumOfDisplayedLineTotals() {
        InvoiceData invoice = new InvoiceData("INV-2", LocalDate.of(2026, 1, 2),
                "Issuer", "ID", "Address", "Account", "Customer", "ID2", "Address2",
                BigDecimal.ZERO, false,
                Arrays.asList(line("1", "0.335", "0"), line("1", "0.335", "0"),
                        line("1", "0.335", "0")));

        decimalEquals("1.02", invoice.getSubtotal());
        decimalEquals("1.02", invoice.getGrandTotal());
        decimalEquals("0.34", invoice.getLines().get(0).getTotal());
    }
}
