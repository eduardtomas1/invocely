package ui;

import i18n.I18n;
import table.LineTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LineEditorDialog extends JDialog {
  private final ItemTablePanel panel;
  private final boolean initialSplitLines;

  public LineEditorDialog(Frame owner, LineTableModel model, boolean splitLines) {
    super(owner, I18n.t("table.edit_lines"), true);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent event) {
        attemptClose();
      }
    });
    setLayout(new BorderLayout(12, 12));
    getContentPane().setBackground(ThemeManager.palette().background());

    panel = new ItemTablePanel(model);
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    panel.setSplitLines(splitLines);
    initialSplitLines = splitLines;

    add(panel, BorderLayout.CENTER);
    add(buildActions(), BorderLayout.SOUTH);
    setMinimumSize(new Dimension(900, 560));
    pack();
  }

  public boolean getSplitLines() {
    return panel.isSplitLines();
  }

  public boolean hasPendingChanges() {
    return hasPendingChanges(panel.hasPendingChanges(), panel.isSplitLines(), initialSplitLines);
  }

  static boolean hasPendingChanges(boolean tablePending, boolean currentSplitLines,
                                   boolean initialSplitLines) {
    return tablePending || currentSplitLines != initialSplitLines;
  }

  private JPanel buildActions() {
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
    actions.setBackground(ThemeManager.palette().background());
    JButton close = new JButton(I18n.t("common.close"));
    close.addActionListener(e -> attemptClose());
    actions.add(close);
    return actions;
  }

  private void attemptClose() {
    try {
      panel.requireCommittedEdits();
      dispose();
    } catch (ItemTablePanel.PendingEditException ignored) {
      // The active editor already displays the localized validation message.
    }
  }
}
