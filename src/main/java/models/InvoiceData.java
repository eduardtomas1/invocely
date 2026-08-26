package models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 *
 * @author Eduard Tomàs
 */

public class InvoiceData {
    private String invoiceNumber;
    private LocalDate issueDate;
    private String issuerName;
    private String issuerNif;
    private String issuerAddress;
    private String issuerAccount;
    private String customerName;
    private String customerNif;
    private String customerAddress;
    private BigDecimal vatPercent;
    private boolean splitLines;
    private List <LineItem> lines;

    public InvoiceData(String invoiceNumber, LocalDate issueDate,
        String issuerName, String issuerNif, String issuerAddress,
        String issuerAccount,
        String customerName, String customerNif, String customerAddress,
        BigDecimal vatPercent, boolean splitLines,
        List <LineItem> lines)
    {
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
        this.issuerName = issuerName;
        this.issuerNif = issuerNif;
        this.issuerAddress = issuerAddress;
        this.issuerAccount = issuerAccount;
        this.customerName = customerName;
        this.customerNif = customerNif;
        this.customerAddress = customerAddress;
        this.vatPercent = vatPercent != null ? vatPercent : BigDecimal.ZERO;
        this.splitLines = splitLines;
        this.lines = lines;
    }

    public InvoiceData(String invoiceNumber, LocalDate issueDate,
        String issuerName, String issuerNif, String issuerAddress,
        String customerName, String customerNif, String customerAddress,
        List <LineItem> lines)
    {
        this(invoiceNumber, issueDate, issuerName, issuerNif, issuerAddress, "",
            customerName, customerNif, customerAddress, new BigDecimal("21"), false, lines);
    }

    public String getInvoiceNumber()
    {
      return invoiceNumber;
    }

    public LocalDate getIssueDate()
    {
      return issueDate;
    }

    public String getIssuerName()
    {
      return issuerName;
    }

    public String getIssuerNif()
    {
      return issuerNif;
    }

    public String getIssuerAddress()
    {
      return issuerAddress;
    }

    public String getIssuerAccount()
    {
      return issuerAccount;
    }

    public String getCustomerName()
    {
      return customerName;
    }

    public String getCustomerNif()
    {
      return customerNif;
    }

    public String getCustomerAddress()
    {
      return customerAddress;
    }

    public BigDecimal getVatPercent()
    {
      return vatPercent;
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

    public BigDecimal getTaxableBase()
    {
      return getSubtotal().subtract(getDiscountTotal());
    }

    public BigDecimal getVatAmount()
    {
      BigDecimal vat = nz(vatPercent);
      return getTaxableBase().multiply(vat).divide(new BigDecimal("100"));
    }

    public BigDecimal getGrandTotal()
    {
      return getTaxableBase().add(getVatAmount());
    }

    public boolean hasDiscounts()
    {
      return getDiscountTotal().compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal nz(BigDecimal value)
    {
      return value != null ? value : BigDecimal.ZERO;
    }
}
