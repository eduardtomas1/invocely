package table;

import i18n.I18n;
import models.LineCategory;
import models.LineItem;
import validation.DocumentValidator;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Eduard Tomas
 */

public class LineTableModel extends AbstractTableModel {

  private final String[] colKeys = {
      "table.col.description",
      "table.col.quantity",
      "table.col.unit_price",
      "table.col.discount_percent",
      "table.col.total"
  };
  private final Class<?>[] types = {
      String.class, BigDecimal.class, BigDecimal.class, BigDecimal.class,
      BigDecimal.class
  };
  private final List<LineItem> data = new ArrayList<>();

  @Override public int getRowCount()
  {
    return data.size();
  }

  @Override public int getColumnCount()
  {
    return colKeys.length;
  }

  @Override public String getColumnName(int column)
  {
    return I18n.t(colKeys[column]);
  }

  @Override public Class < ? > getColumnClass(int columnIndex)
  {
    return types[columnIndex];
  }

  @Override public boolean isCellEditable(int rowIndex, int columnIndex)
  {
    return columnIndex < 4;
  }

  @Override
  public Object getValueAt(int row, int col)
  {
    LineItem li = data.get(row);
    if (li == null) return null;
    switch (col) {
      case 0: return li.getDescription();
      case 1: return li.getQuantity();
      case 2: return li.getUnitPrice();
      case 3: return li.getDiscountPercent();
      case 4: return li.getTotal();
      default: return null;
    }
  }

  @Override
  public void setValueAt(Object value, int row, int col)
  {
    LineItem li = data.get(row);
    if (li == null) return;
    switch (col) {
      case 0: li.setDescription(value != null ? String.valueOf(value) : null); break;
      case 1: li.setQuantity(toBD(value, false)); break;
      case 2: li.setUnitPrice(toBD(value, false)); break;
      case 3: li.setDiscountPercent(toBD(value, true)); break;
      default: return;
    }
    fireTableRowsUpdated(row, row);
  }

  private BigDecimal toBD(Object o, boolean percent)
  {
    if (o == null || String.valueOf(o).trim().isEmpty()) return null;
    final BigDecimal parsed;
    try {
      parsed = o instanceof BigDecimal ? (BigDecimal) o
          : new BigDecimal(String.valueOf(o).trim().replace(',', '.'));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(I18n.t("validation.invalid_number"), ex);
    }
    if (percent) DocumentValidator.validatePercent(parsed);
    else DocumentValidator.validateNumber(parsed);
    return parsed;
  }

  public void addEmpty()
  {
    addEmpty(LineCategory.MATERIAL);
  }

  public void addEmpty(LineCategory category)
  {
    LineItem item = new LineItem();
    item.setCategory(category);
    data.add(item);
    fireTableDataChanged();
  }

  public void remove(int index)
  {
    if (index >= 0 && index < data.size()) {
      data.remove(index);
      fireTableDataChanged();
    }
  }

  public boolean moveRow(int modelIndex, int direction, LineCategory category)
  {
    if (modelIndex < 0 || modelIndex >= data.size()) return false;
    int target = findNeighborIndex(modelIndex, direction, category);
    if (target < 0 || target >= data.size() || target == modelIndex) return false;
    LineItem tmp = data.get(modelIndex);
    data.set(modelIndex, data.get(target));
    data.set(target, tmp);
    fireTableDataChanged();
    return true;
  }

  private int findNeighborIndex(int modelIndex, int direction, LineCategory category)
  {
    int step = direction < 0 ? -1 : 1;
    int i = modelIndex + step;
    if (category == null) {
      return (i >= 0 && i < data.size()) ? i : -1;
    }
    while (i >= 0 && i < data.size()) {
      LineItem li = data.get(i);
      if (li != null && li.getCategory() == category) {
        return i;
      }
      i += step;
    }
    return -1;
  }

  public List<LineItem> getItems()
  {
    return new ArrayList<>(data);
  }

  public void setItems(List<LineItem> items)
  {
    data.clear();
    if (items != null) {
      data.addAll(items);
    }
    fireTableDataChanged();
  }

  public void refreshColumns()
  {
    fireTableStructureChanged();
  }

  public LineItem getItemAt(int row)
  {
    if (row < 0 || row >= data.size()) return null;
    return data.get(row);
  }
}
