package models;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author Eduard Tomàs
 */

public class LineItem {
    private static final int MONEY_SCALE = 2;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private LineCategory category;

    public LineItem()
    {
        this("", BigDecimal.ONE, BigDecimal.ZERO,
                BigDecimal.ZERO, LineCategory.MATERIAL);
    }

    public LineItem(String description, BigDecimal quantity, BigDecimal unitPrice,
      BigDecimal discountPercent, LineCategory category)
    {
      this.description = description;
      this.quantity = quantity;
      this.unitPrice = unitPrice;
      this.discountPercent = discountPercent;
      this.category = category != null ? category : LineCategory.MATERIAL;
    }

    public String getDescription()
    {
      return description;
    }

    public void setDescription(String description)
    {
      this.description = description;
    }

    public BigDecimal getQuantity()
    {
      return quantity;
    }

    public void setQuantity(BigDecimal quantity)
    {
      this.quantity = quantity;
    }

    public BigDecimal getUnitPrice()
    {
      return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice)
    {
      this.unitPrice = unitPrice;
    }

    public BigDecimal getDiscountPercent()
    {
      return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent)
    {
      this.discountPercent = discountPercent;
    }

    public LineCategory getCategory()
    {
      return category;
    }

    public void setCategory(LineCategory category)
    {
      this.category = category != null ? category : LineCategory.MATERIAL;
    }

    public boolean isTitleRow()
    {
      if (description == null || description.trim().isEmpty()) return false;
      return isZero(quantity) && isZero(unitPrice) && isZero(discountPercent);
    }

    public BigDecimal getLineBase()
    {
      return money(nz(unitPrice).multiply(nz(quantity)));
    }

    public BigDecimal getDiscountAmount()
    {
      return money(getLineBase().multiply(normalizePercent(discountPercent))
              .divide(new BigDecimal("100")));
    }

    public BigDecimal getTotal()
    {
        BigDecimal net = getLineBase();
        return net.subtract(getDiscountAmount());
    }

    private BigDecimal normalizePercent(BigDecimal value)
    {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal money(BigDecimal value)
    {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value)
    {
        return value != null ? value : BigDecimal.ZERO;
    }

    private boolean isZero(BigDecimal value)
    {
      return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }
}
