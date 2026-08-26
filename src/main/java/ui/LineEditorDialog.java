package ui;

import i18n.I18n;
import table.LineTableModel;

import javax.swing.*;
import java.awt.*;

public class LineEditorDialog extends JDialog {
  private final ItemTablePanel panel;

  public LineEditorDialog(Frame owner, LineTableModel model, boolean splitLines) {
    super(owner, I18n.t("table.edit_lines"), true);
    setLayout(new BorderLayout(12, 12));
    getContentPane().setBackground(ThemeManager.palette().background());

    panel = new ItemTablePanel(model);
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    panel.setSplitLines(splitLines);

    add(panel, BorderLayout.CENTER);
    add(buildActions(), BorderLayout.SOUTH);
    setMinimumSize(new Dimension(900, 560));
    pack();
  }

  public boolean getSplitLines() {
    return panel.isSplitLines();
  }

  private JPanel buildActions() {
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
    actions.setBackground(ThemeManager.palette().background());
    JButton close = new JButton(I18n.t("common.close"));
    close.addActionListener(e -> dispose());
    actions.add(close);
    return actions;
  }
}
