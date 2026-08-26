package models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Eduard Tomàs
 */

public class BudgetData {
    private static final int MONEY_SCALE = 2;
    private String budgetNumber;
    private LocalDate issueDate;
    private LocalDate validUntil;
    private String supplierName;
    private String supplierNif;
    private String supplierAddress;
    private String clientName;
    private String clientNif;
    private String clientAddress;
    private String paymentTerms;
    private String notes;
    private boolean includeTotals;
    private String taxName;
    private BigDecimal taxPercent;
    private boolean splitLines;
    private List<LineItem> lines;

    public BudgetData() {

    }

    public BudgetData(String budgetNumber, LocalDate issueDate, LocalDate validUntil,
                    String supplierName, String supplierNif, String supplierAddress,
                    String clientName, String clientNif, String clientAddress,
                    String paymentTerms, String notes,
                    boolean includeTotals, String taxName, BigDecimal taxPercent,
                    boolean splitLines, List<LineItem> lines)
    {
        this.budgetNumber = budgetNumber;
        this.issueDate = issueDate;
        this.validUntil = validUntil;
        this.supplierName = supplierName;
        this.supplierNif = supplierNif;
        this.supplierAddress = supplierAddress;
        this.clientName = clientName;
        this.clientNif = clientNif;
        this.clientAddress = clientAddress;
        this.paymentTerms = paymentTerms;
        this.notes = notes;
        this.includeTotals = includeTotals;
        this.taxName = (taxName == null || taxName.isBlank()) ? "IVA" : taxName;
        this.taxPercent = taxPercent != null ? taxPercent : BigDecimal.ZERO;
        this.splitLines = splitLines;
        this.lines = lines == null ? null : Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public BudgetData(String budgetNumber, LocalDate issueDate, LocalDate validUntil,
                    String supplierName, String supplierNif, String supplierAddress,
                    String clientName, String clientNif, String clientAddress,
                    String paymentTerms, String notes, List<LineItem> lines)
    {
        this(budgetNumber, issueDate, validUntil, supplierName, supplierNif, supplierAddress,
            clientName, clientNif, clientAddress, paymentTerms, notes, false, "IVA", BigDecimal.ZERO, false, lines);
    }

    public String getBudgetNumber()
    {
        return budgetNumber;
    }

    public LocalDate getIssueDate()
    {
        return issueDate;
    }

    public LocalDate getValidUntil()
    {
        return validUntil;
    }

    public String getSupplierName()
    {
        return supplierName;
    }

    public String getSupplierNif()
    {
        return supplierNif;
    }

    public String getSupplierAddress()
    {
        return supplierAddress;
    }

    public String getClientName()
    {
        return clientName;
    }

    public String getClientNif()
    {
        return clientNif;
    }

    public String getClientAddress()
    {
        return clientAddress;
    }

    public String getPaymentTerms()
    {
        return paymentTerms;
    }

    public String getNotes()
    {
      return notes;
    }

    public boolean isIncludeTotals()
    {
        return includeTotals;
    }

    public String getTaxName()
    {
        return taxName;
    }

    public BigDecimal getTaxPercent()
    {
        return taxPercent != null ? taxPercent : BigDecimal.ZERO;
    }

    public boolean isSplitLines()
    {
        return splitLines;
    }

    public List <LineItem> getLines()
    {
      return lines;
    }

    public BigDecimal getSubtotal()
    {
        BigDecimal subtotal = BigDecimal.ZERO;
        if (lines != null) {
            for (LineItem li : lines) {
                if (li != null) {
                    subtotal = subtotal.add(nz(li.getLineBase()));
                }
            }
        }
        return subtotal;
    }

    public BigDecimal getDiscountTotal()
    {
        BigDecimal discount = BigDecimal.ZERO;
        if (lines != null) {
            for (LineItem li : lines) {
                if (li != null) {
                    discount = discount.add(nz(li.getDiscountAmount()));
                }
            }
        }
        return discount;
    }

    public BigDecimal getGrandTotal()
    {
        return getTaxableBase().add(getTaxAmount());
    }

    public BigDecimal getTaxableBase()
    {
        return getSubtotal().subtract(getDiscountTotal());
    }

    public BigDecimal getTaxAmount()
    {
        BigDecimal percent = getTaxPercent();
        return money(getTaxableBase().multiply(percent).divide(new BigDecimal("100")));
    }

    public boolean hasDiscounts()
    {
        return getDiscountTotal().compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal nz(BigDecimal value)
    {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal money(BigDecimal value)
    {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
