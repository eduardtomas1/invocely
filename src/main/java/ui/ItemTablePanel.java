package ui;

import i18n.I18n;
import models.LineCategory;
import models.LineItem;
import table.LineTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Eduard Tomas
 */

public class ItemTablePanel extends JPanel {
  private static final int BASE_ROW_HEIGHT = 26;
  private static final int MAX_ROW_HEIGHT = BASE_ROW_HEIGHT * 3;

  private final LineTableModel model;
  private final List<JTable> tables = new ArrayList<>();
  private final List<JScrollPane> scrolls = new ArrayList<>();
  private final List<JLabel> labels = new ArrayList<>();
  private Color oddRowColor;
  private Color evenRowColor;
  private Color selectedRowColor;
  private boolean splitLines = false;

  private final CardLayout viewLayout = new CardLayout();
  private final JPanel viewPanel = new JPanel(viewLayout);
  private final JToggleButton splitToggle;

  public ItemTablePanel() {
    this(new LineTableModel());
  }

  public ItemTablePanel(LineTableModel model) {
    super(new BorderLayout(0, 8));
    setOpaque(false);

    this.model = model;

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    header.setOpaque(false);
    JLabel title = new JLabel(I18n.t("table.lines"));
    title.setFont(title.getFont().deriveFont(Font.BOLD));
    labels.add(title);
    splitToggle = new JToggleButton(I18n.t("table.split"));
    splitToggle.setToolTipText(I18n.t("table.split.tooltip"));
    JButton edit = new JButton(I18n.t("table.edit_lines"));
    edit.addActionListener(e -> openEditorDialog());
    header.add(title);
    header.add(splitToggle);
    header.add(edit);
    add(header, BorderLayout.NORTH);

    viewPanel.setOpaque(false);
    viewPanel.add(buildSingleView(), "single");
    viewPanel.add(buildSplitView(), "split");
    add(viewPanel, BorderLayout.CENTER);

    splitToggle.addActionListener(e -> applySplitState(splitToggle.isSelected()));
    applySplitState(false);
  }

  public LineTableModel getModel() { return model; }

  public void setItems(List<LineItem> items) {
    model.setItems(items);
  }

  public boolean isSplitLines() {
    return splitLines;
  }

  public void setSplitLines(boolean split) {
    splitToggle.setSelected(split);
    applySplitState(split);
  }

  private JPanel buildSingleView() {
    JTable table = createTable(null);
    JScrollPane scroll = wrapTable(table);

    JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
    btns.setOpaque(false);
    JButton add = new JButton(I18n.t("table.add_line"));
    JButton del = new JButton(I18n.t("table.delete_line"));
    JButton up = new JButton(I18n.t("table.move_up"));
    JButton down = new JButton(I18n.t("table.move_down"));
    del.setEnabled(false);
    up.setEnabled(false);
    down.setEnabled(false);
    btns.add(add);
    btns.add(del);
    btns.add(up);
    btns.add(down);

    add.addActionListener(e -> model.addEmpty(LineCategory.MATERIAL));
    del.addActionListener(e -> {
      int row = table.getSelectedRow();
      if (row >= 0) {
        model.remove(table.convertRowIndexToModel(row));
      }
    });
    up.addActionListener(e -> moveRow(table, -1, null));
    down.addActionListener(e -> moveRow(table, 1, null));
    table.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        boolean hasSelection = table.getSelectedRow() >= 0;
        del.setEnabled(hasSelection);
        up.setEnabled(hasSelection);
        down.setEnabled(hasSelection);
      }
    });

    JPanel panel = new JPanel(new BorderLayout(0, 8));
    panel.setOpaque(false);
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(btns, BorderLayout.SOUTH);
    return panel;
  }

  private JPanel buildSplitView() {
    JPanel splitPanel = new JPanel(new GridLayout(2, 1, 0, 12));
    splitPanel.setOpaque(false);

    splitPanel.add(buildCategoryPanel(I18n.t("table.materials"), LineCategory.MATERIAL));
    splitPanel.add(buildCategoryPanel(I18n.t("table.services"), LineCategory.SERVEI));

    return splitPanel;
  }

  private JPanel buildCategoryPanel(String title, LineCategory category) {
    JTable table = createTable(category);
    JScrollPane scroll = wrapTable(table);

    JLabel header = new JLabel(title);
    header.setFont(header.getFont().deriveFont(Font.BOLD));
    header.setForeground(ThemeManager.palette().text());
    labels.add(header);

    JButton add = new JButton(I18n.t("table.add"));
    JButton del = new JButton(I18n.t("table.delete"));
    JButton up = new JButton(I18n.t("table.move_up"));
    JButton down = new JButton(I18n.t("table.move_down"));
    del.setEnabled(false);
    up.setEnabled(false);
    down.setEnabled(false);

    add.addActionListener(e -> model.addEmpty(category));
    del.addActionListener(e -> {
      int row = table.getSelectedRow();
      if (row >= 0) {
        model.remove(table.convertRowIndexToModel(row));
      }
    });
    up.addActionListener(e -> moveRow(table, -1, category));
    down.addActionListener(e -> moveRow(table, 1, category));
    table.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        boolean hasSelection = table.getSelectedRow() >= 0;
        del.setEnabled(hasSelection);
        up.setEnabled(hasSelection);
        down.setEnabled(hasSelection);
      }
    });

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    actions.setOpaque(false);
    actions.add(add);
    actions.add(del);
    actions.add(up);
    actions.add(down);

    JPanel top = new JPanel(new BorderLayout());
    top.setOpaque(false);
    top.add(header, BorderLayout.WEST);
    top.add(actions, BorderLayout.EAST);

    JPanel panel = new JPanel(new BorderLayout(0, 8));
    panel.setOpaque(false);
    panel.add(top, BorderLayout.NORTH);
    panel.add(scroll, BorderLayout.CENTER);
    return panel;
  }

  private JTable createTable(LineCategory filter) {
    oddRowColor = oddRow();
    evenRowColor = evenRow();
    selectedRowColor = selectedRow();
    JTable t = new JTable(model) {
      @Override
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        LineItem item = itemAtViewRow(this, row);
        boolean isTitle = item != null && item.isTitleRow();
        if (c instanceof JTextArea) {
          JTextArea area = (JTextArea) c;
          area.setSize(getColumnModel().getColumn(column).getWidth(), Short.MAX_VALUE);
          int prefHeight = Math.min(MAX_ROW_HEIGHT,
              Math.max(BASE_ROW_HEIGHT, area.getPreferredSize().height));
          if (getRowHeight(row) != prefHeight) {
            setRowHeight(row, prefHeight);
          }
        }
        if (!isRowSelected(row)) {
          c.setBackground(isTitle ? titleRowBackground() : (row % 2 == 0 ? evenRowColor : oddRowColor));
          c.setForeground(tableForeground());
        } else {
          c.setBackground(selectedRowColor);
          c.setForeground(selectionForeground());
        }
        if (c.getFont() != null) {
          c.setFont(c.getFont().deriveFont(isTitle ? Font.BOLD : Font.PLAIN));
        }
        return c;
      }
    };
    t.setRowHeight(BASE_ROW_HEIGHT);
    t.setIntercellSpacing(new Dimension(0, 1));
    t.setShowGrid(false);
    t.setFillsViewportHeight(true);
    t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    t.putClientProperty("terminateEditOnFocusLost", true);
    t.setSurrendersFocusOnKeystroke(true);
    t.setBackground(cardBackground());
    t.setForeground(tableForeground());
    t.setSelectionBackground(selectedRowColor);
    t.setSelectionForeground(selectionForeground());

    TableRowSorter<LineTableModel> sorter = new TableRowSorter<>(model);
    if (filter != null) {
      sorter.setRowFilter(new LineCategoryFilter(filter));
    }
    t.setRowSorter(sorter);

    t.getTableHeader().setReorderingAllowed(false);
    DefaultTableCellRenderer header = (DefaultTableCellRenderer) t.getTableHeader().getDefaultRenderer();
    header.setHorizontalAlignment(SwingConstants.LEFT);
    header.setBackground(headerBackground());
    header.setForeground(headerForeground());

    int[] widths = {300, 90, 110, 100, 110};
    for (int i = 0; i < widths.length && i < t.getColumnModel().getColumnCount(); i++) {
      t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    DescriptionCellRenderer descRenderer = new DescriptionCellRenderer();
    t.getColumnModel().getColumn(0).setCellRenderer(descRenderer);
    t.getColumnModel().getColumn(0).setCellEditor(new DescriptionCellEditor());
    t.getColumnModel().getColumn(1).setCellEditor(new NumberCellEditor());
    t.getColumnModel().getColumn(2).setCellEditor(new NumberCellEditor());
    t.getColumnModel().getColumn(3).setCellEditor(new NumberCellEditor());
    applyEditorTheme(t);

    tables.add(t);
    return t;
  }

  private JScrollPane wrapTable(JTable table) {
    JScrollPane scroll = new JScrollPane(table);
    scroll.getViewport().setBackground(cardBackground());
    scroll.setBackground(cardBackground());
    scroll.setBorder(BorderFactory.createLineBorder(borderColor()));
    scrolls.add(scroll);
    return scroll;
  }

  private void applySplitState(boolean split) {
    splitLines = split;
    viewLayout.show(viewPanel, split ? "split" : "single");
    revalidate();
    repaint();
  }

  private void moveRow(JTable table, int direction, LineCategory category) {
    int row = table.getSelectedRow();
    if (row < 0) return;
    int modelIndex = table.convertRowIndexToModel(row);
    if (model.moveRow(modelIndex, direction, category)) {
      int nextViewRow = row + (direction < 0 ? -1 : 1);
      if (nextViewRow >= 0 && nextViewRow < table.getRowCount()) {
        table.getSelectionModel().setSelectionInterval(nextViewRow, nextViewRow);
      }
    }
  }

  private LineItem itemAtViewRow(JTable table, int viewRow) {
    if (viewRow < 0) return null;
    int modelRow = table.convertRowIndexToModel(viewRow);
    return model.getItemAt(modelRow);
  }

  private void openEditorDialog() {
    Window window = SwingUtilities.getWindowAncestor(this);
    Frame frame = window instanceof Frame ? (Frame) window : null;
    LineEditorDialog dialog = new LineEditorDialog(frame, model, isSplitLines());
    dialog.setLocationRelativeTo(this);
    dialog.setVisible(true);
    setSplitLines(dialog.getSplitLines());
  }

  public void refreshTheme() {
    model.refreshColumns();
    oddRowColor = oddRow();
    evenRowColor = evenRow();
    selectedRowColor = selectedRow();
    setOpaque(false);
    Color labelColor = ThemeManager.palette().text();
    for (JLabel label : labels) {
      label.setForeground(labelColor);
    }
    for (JTable table : tables) {
      table.setBackground(cardBackground());
      table.setSelectionBackground(selectedRowColor);
      table.setSelectionForeground(selectionForeground());
      table.setForeground(tableForeground());
      table.setGridColor(borderColor());
      table.getTableHeader().setBackground(headerBackground());
      table.getTableHeader().setForeground(headerForeground());
      applyEditorTheme(table);
    }
    for (JScrollPane scroll : scrolls) {
      scroll.getViewport().setBackground(cardBackground());
      scroll.setBackground(cardBackground());
      scroll.setBorder(BorderFactory.createLineBorder(borderColor()));
    }
    repaint();
  }

  private Color oddRow() {
    Color c = UIManager.getColor("App.table.odd");
    if (c != null) return c;
    return new Color(248, 250, 252);
  }

  private Color evenRow() {
    Color c = UIManager.getColor("App.table.even");
    if (c != null) return c;
    return cardBackground();
  }

  private Color selectedRow() {
    Color c = UIManager.getColor("App.table.selection");
    if (c != null) return c;
    return new Color(220, 235, 252);
  }

  private Color titleRowBackground() {
    return blend(cardBackground(), selectedRowColor, 0.20f);
  }

  private Color borderColor() {
    Color c = UIManager.getColor("Component.borderColor");
    return c != null ? c : new Color(214, 220, 228);
  }

  private Color headerBackground() {
    Color c = UIManager.getColor("TableHeader.background");
    if (c != null) return c;
    return cardBackground().darker();
  }

  private Color headerForeground() {
    Color c = UIManager.getColor("TableHeader.foreground");
    if (c != null) return c;
    return new Color(32, 35, 39);
  }

  private Color tableForeground() {
    Color c = UIManager.getColor("Table.foreground");
    if (c != null) return c;
    return new Color(32, 35, 39);
  }

  private Color selectionForeground() {
    Color c = UIManager.getColor("Table.selectionForeground");
    if (c != null) return c;
    return new Color(32, 35, 39);
  }

  private Color cardBackground() {
    Color c = UIManager.getColor("App.cardColor");
    if (c != null) return c;
    Color p = UIManager.getColor("Panel.background");
    return p != null ? p : Color.WHITE;
  }

  private Color blend(Color base, Color accent, float ratio) {
    float inv = 1f - ratio;
    return new Color(
        Math.round(base.getRed() * inv + accent.getRed() * ratio),
        Math.round(base.getGreen() * inv + accent.getGreen() * ratio),
        Math.round(base.getBlue() * inv + accent.getBlue() * ratio)
    );
  }

  private void applyEditorTheme(JTable table) {
    if (table == null) return;
    for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
      TableCellEditor editor = table.getColumnModel().getColumn(i).getCellEditor();
      if (editor instanceof DescriptionCellEditor) {
        ((DescriptionCellEditor) editor).applyTheme(cardBackground(), tableForeground(), selectionForeground(), selectedRow(), borderColor());
      } else if (editor instanceof NumberCellEditor) {
        ((NumberCellEditor) editor).applyTheme(cardBackground(), tableForeground(), selectionForeground(), selectedRow());
      }
    }
  }

  private static class DescriptionCellRenderer extends JTextArea implements TableCellRenderer {
    DescriptionCellRenderer() {
      setLineWrap(true);
      setWrapStyleWord(true);
      setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      setText(value != null ? value.toString() : "");
      setFont(table.getFont());
      return this;
    }
  }

  private static class DescriptionCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JTextArea area = new JTextArea();
    private final JScrollPane scroll;

    DescriptionCellEditor() {
      area.setLineWrap(true);
      area.setWrapStyleWord(true);
      scroll = new JScrollPane(area);
      scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      scroll.setBorder(null);
    }

    void applyTheme(Color background, Color foreground, Color selectedForeground, Color selectedBackground, Color border) {
      area.setBackground(background);
      area.setForeground(foreground);
      area.setCaretColor(foreground);
      area.setSelectionColor(selectedBackground);
      area.setSelectedTextColor(selectedForeground);
      scroll.getViewport().setBackground(background);
      scroll.setBackground(background);
      scroll.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, border));
    }

    @Override
    public Object getCellEditorValue() {
      return area.getText();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                 int row, int column) {
      area.setText(value != null ? value.toString() : "");
      area.setFont(table.getFont());
      area.setBackground(table.getBackground());
      area.setForeground(table.getForeground());
      area.setCaretColor(table.getForeground());
      area.setSelectionColor(table.getSelectionBackground());
      area.setSelectedTextColor(table.getSelectionForeground());
      scroll.getViewport().setBackground(table.getBackground());
      scroll.setBackground(table.getBackground());
      return scroll;
    }
  }

  private static class NumberCellEditor extends DefaultCellEditor {
    private final JTextField field;

    NumberCellEditor() {
      super(new JTextField());
      field = (JTextField) getComponent();
    }

    void applyTheme(Color background, Color foreground, Color selectedForeground, Color selectedBackground) {
      field.setBackground(background);
      field.setForeground(foreground);
      field.setCaretColor(foreground);
      field.setSelectionColor(selectedBackground);
      field.setSelectedTextColor(selectedForeground);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                 int row, int column) {
      String text = value != null ? value.toString() : "";
      field.setText(text);
      field.selectAll();
      field.setBackground(table.getBackground());
      field.setForeground(table.getForeground());
      field.setCaretColor(table.getForeground());
      field.setSelectionColor(table.getSelectionBackground());
      field.setSelectedTextColor(table.getSelectionForeground());
      return field;
    }
  }

  private static class LineCategoryFilter extends RowFilter<LineTableModel, Integer> {
    private final LineCategory category;

    LineCategoryFilter(LineCategory category) {
      this.category = category;
    }

    @Override
    public boolean include(Entry<? extends LineTableModel, ? extends Integer> entry) {
      LineTableModel model = entry.getModel();
      Integer index = entry.getIdentifier();
      if (index == null) return false;
      LineItem item = model.getItemAt(index);
      return item != null && category == item.getCategory();
    }
  }
}
