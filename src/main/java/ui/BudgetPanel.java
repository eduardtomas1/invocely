package ui;

import i18n.I18n;
import models.BusinessPartner;
import models.BudgetData;
import models.LineItem;
import storage.BusinessPartnerStore;
import table.LineTableModel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import validation.DocumentValidator;

/**
 *
 * @author Eduard Tomas
 */

public class BudgetPanel extends JPanel {

  private final JTextField tfNumber = new JTextField();
  private final JTextField tfDate   = new JTextField();
  private final JTextField tfValid  = new JTextField();

  private final JTextField tfSuppName = new JTextField();
  private final JTextField tfSuppNif  = new JTextField();
  private final JTextArea tfSuppAddr = createAddressArea();

  private final JTextField tfClientName = new JTextField();
  private final JTextField tfClientNif  = new JTextField();
  private final JTextArea tfClientAddr = createAddressArea();

  private final JTextField tfPayment   = new JTextField();
  private final JTextArea  taNotes     = new JTextArea(4,20);
  private final JCheckBox cbIncludeTotals = new JCheckBox();
  private final JTextField tfTaxName = new JTextField("IVA");
  private final JTextField tfTaxPercent = new JTextField("21");

  private final ItemTablePanel itemsPanel = new ItemTablePanel();
  private final BusinessPartnerStore partnerStore = new BusinessPartnerStore();
  private final List<JComponent> sections = new ArrayList<>();
  private final List<JTextComponent> allFields = new ArrayList<>();
  private JScrollPane notesScroll;
  private DocumentState cleanState;

  public BudgetPanel() {
    setLayout(new BorderLayout(12,12));
    setOpaque(true);
    setBackground(panelBgColor());

    taNotes.setLineWrap(true);
    taNotes.setWrapStyleWord(true);

    // placeholders
    applyPlaceholder(tfNumber, I18n.t("placeholder.budget_number"));
    applyPlaceholder(tfDate, I18n.t("placeholder.date"));
    applyPlaceholder(tfValid, I18n.t("placeholder.date"));
    applyPlaceholder(tfSuppName, I18n.t("placeholder.supplier_name"));
    applyPlaceholder(tfSuppNif, I18n.t("placeholder.supplier_nif"));
    applyPlaceholder(tfSuppAddr, I18n.t("placeholder.supplier_address"));
    applyPlaceholder(tfClientName, I18n.t("placeholder.client_name"));
    applyPlaceholder(tfClientNif, I18n.t("placeholder.client_nif"));
    applyPlaceholder(tfClientAddr, I18n.t("placeholder.client_address"));
    applyPlaceholder(tfPayment, I18n.t("placeholder.payment_terms"));
    applyPlaceholder(taNotes, I18n.t("placeholder.notes"));
    applyPlaceholder(tfTaxName, I18n.t("placeholder.tax_name"));
    applyPlaceholder(tfTaxPercent, I18n.t("placeholder.tax_percent"));
    allFields.add(tfNumber);
    allFields.add(tfDate);
    allFields.add(tfValid);
    allFields.add(tfSuppName);
    allFields.add(tfSuppNif);
    allFields.add(tfSuppAddr);
    allFields.add(tfClientName);
    allFields.add(tfClientNif);
    allFields.add(tfClientAddr);
    allFields.add(tfPayment);
    allFields.add(taNotes);
    allFields.add(tfTaxName);
    allFields.add(tfTaxPercent);

    prefillDate(tfDate);

    JPanel north = new JPanel(new GridLayout(1,3,12,12));
    north.setOpaque(true);
    north.setBackground(panelBgColor());
    north.add(createFormSection(I18n.t("section.budget"),
        new String[] { I18n.t("label.budget_number"), I18n.t("label.issue_date"), I18n.t("label.valid_until_required") },
        new JComponent[] { tfNumber, tfDate, tfValid }));
    north.add(createPartnerSection(I18n.t("section.supplier"),
        new String[] { I18n.t("label.supplier_name"), I18n.t("label.supplier_nif"),
            I18n.t("label.supplier_address") },
        new JComponent[] { tfSuppName, tfSuppNif, tfSuppAddr },
        this::openSupplierSelector, this::saveSupplierPartner));
    north.add(createPartnerSection(I18n.t("section.client"),
        new String[] { I18n.t("label.client_name"), I18n.t("label.client_nif"),
            I18n.t("label.client_address") },
        new JComponent[] { tfClientName, tfClientNif, tfClientAddr },
        this::openClientSelector, this::saveClientPartner));

    add(north, BorderLayout.NORTH);

    itemsPanel.setBorder(sectionBorder(I18n.t("section.items")));
    add(itemsPanel, BorderLayout.CENTER);

    JPanel south = new JPanel(new BorderLayout(0, 0));
    south.setOpaque(true);
    south.setBackground(panelBgColor());
    south.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
    south.add(createFormSection(I18n.t("section.conditions"),
        new String[] { I18n.t("label.payment_terms"), I18n.t("label.notes"),
            I18n.t("label.include_totals"), I18n.t("label.tax_name"), I18n.t("label.tax_percent") },
        new JComponent[] { tfPayment, createNotesScroll(), cbIncludeTotals, tfTaxName, tfTaxPercent }),
        BorderLayout.CENTER);
    add(south, BorderLayout.SOUTH);
    markClean();
  }

  public BudgetData collect() {
    itemsPanel.requireCommittedEdits();
    LineTableModel m = itemsPanel.getModel();
    List<LineItem> lines = copyLines(m.getItems());
    LocalDate issueDate = InputParser.requiredDate(cleanText(tfDate));
    LocalDate validUntil = InputParser.requiredDate(cleanText(tfValid));
    InputParser.validDateRange(issueDate, validUntil);
    BudgetData data = new BudgetData(
        cleanText(tfNumber),
        issueDate,
        validUntil,
        cleanText(tfSuppName), cleanText(tfSuppNif), cleanText(tfSuppAddr),
        cleanText(tfClientName), cleanText(tfClientNif), cleanText(tfClientAddr),
        cleanText(tfPayment), cleanText(taNotes),
        cbIncludeTotals.isSelected(),
        cleanText(tfTaxName),
        InputParser.percent(cleanText(tfTaxPercent)),
        itemsPanel.isSplitLines(),
        lines
    );
    DocumentValidator.validateBudget(data);
    return data;
  }

  public void requireCommittedTableEdits() {
    itemsPanel.requireCommittedEdits();
  }

  public void fillFromData(BudgetData data) {
    itemsPanel.cancelPendingEdits();
    setField(tfNumber, data.getBudgetNumber());
    setField(tfDate, data.getIssueDate() != null ? formatDate(data.getIssueDate()) : "");
    setField(tfValid, data.getValidUntil() != null ? formatDate(data.getValidUntil()) : "");
    setField(tfSuppName, data.getSupplierName());
    setField(tfSuppNif, data.getSupplierNif());
    setField(tfSuppAddr, data.getSupplierAddress());
    setField(tfClientName, data.getClientName());
    setField(tfClientNif, data.getClientNif());
    setField(tfClientAddr, data.getClientAddress());
    setField(tfPayment, data.getPaymentTerms());
    setField(taNotes, data.getNotes());
    cbIncludeTotals.setSelected(data.isIncludeTotals());
    setField(tfTaxName, data.getTaxName());
    setField(tfTaxPercent, formatPercent(data.getTaxPercent()));
    itemsPanel.setItems(data.getLines());
    itemsPanel.setSplitLines(data.isSplitLines());
    markClean();
  }

  public void applyDefaults(BudgetData data) {
    if (data == null) return;
    if ((data.getIssueDate() == null) != (data.getValidUntil() == null)) {
      throw new IllegalArgumentException(I18n.t("validation.default_dates_pair"));
    }
    setField(tfNumber, data.getBudgetNumber());
    if (data.getIssueDate() != null) setField(tfDate, formatDate(data.getIssueDate()));
    if (data.getValidUntil() != null) setField(tfValid, formatDate(data.getValidUntil()));
    setField(tfSuppName, data.getSupplierName());
    setField(tfSuppNif, data.getSupplierNif());
    setField(tfSuppAddr, data.getSupplierAddress());
    setField(tfClientName, data.getClientName());
    setField(tfClientNif, data.getClientNif());
    setField(tfClientAddr, data.getClientAddress());
    setField(tfPayment, data.getPaymentTerms());
    setField(taNotes, data.getNotes());
    cbIncludeTotals.setSelected(data.isIncludeTotals());
    setField(tfTaxName, data.getTaxName());
    setField(tfTaxPercent, formatPercent(data.getTaxPercent()));
    itemsPanel.setSplitLines(data.isSplitLines());
  }

  public DraftState snapshotDraft() {
    return new DraftState(captureState(), cleanState);
  }

  public void restoreDraft(DraftState state) {
    if (state == null) return;
    applyState(state.current);
    cleanState = state.clean != null ? state.clean.copy() : captureState();
  }

  public boolean isDirty() {
    return itemsPanel.hasPendingChanges() || !captureState().equals(cleanState);
  }

  public void markClean() {
    cleanState = captureState();
  }

  public void markCleanIfUnchanged(DraftState savedDraft) {
    if (savedDraft != null && captureState().equals(savedDraft.current)) {
      cleanState = savedDraft.current.copy();
    }
  }

  public static final class DraftState {
    private final DocumentState current;
    private final DocumentState clean;

    private DraftState(DocumentState current, DocumentState clean) {
      this.current = current.copy();
      this.clean = clean != null ? clean.copy() : null;
    }
  }

  private DocumentState captureState() {
    return new DocumentState(new String[] {
        cleanText(tfNumber), cleanText(tfDate), cleanText(tfValid), cleanText(tfSuppName),
        cleanText(tfSuppNif), cleanText(tfSuppAddr), cleanText(tfClientName), cleanText(tfClientNif),
        cleanText(tfClientAddr), cleanText(tfPayment), cleanText(taNotes), cleanText(tfTaxName),
        cleanText(tfTaxPercent)
    }, cbIncludeTotals.isSelected(), itemsPanel.isSplitLines(),
        copyLines(itemsPanel.getModel().getItems()));
  }

  private void applyState(DocumentState state) {
    if (state == null) return;
    setField(tfNumber, state.fields[0]);
    setField(tfDate, state.fields[1]);
    setField(tfValid, state.fields[2]);
    setField(tfSuppName, state.fields[3]);
    setField(tfSuppNif, state.fields[4]);
    setField(tfSuppAddr, state.fields[5]);
    setField(tfClientName, state.fields[6]);
    setField(tfClientNif, state.fields[7]);
    setField(tfClientAddr, state.fields[8]);
    setField(tfPayment, state.fields[9]);
    setField(taNotes, state.fields[10]);
    setField(tfTaxName, state.fields[11]);
    setField(tfTaxPercent, state.fields[12]);
    cbIncludeTotals.setSelected(state.includeTotals);
    itemsPanel.setItems(copyLines(state.lines));
    itemsPanel.setSplitLines(state.splitLines);
  }

  private static final class DocumentState {
    private final String[] fields;
    private final boolean includeTotals;
    private final boolean splitLines;
    private final List<LineItem> lines;

    private DocumentState(String[] fields, boolean includeTotals, boolean splitLines, List<LineItem> lines) {
      this.fields = fields.clone();
      this.includeTotals = includeTotals;
      this.splitLines = splitLines;
      this.lines = lines;
    }

    private DocumentState copy() {
      List<LineItem> copiedLines = new ArrayList<>();
      for (LineItem line : lines) {
        copiedLines.add(new LineItem(line.getDescription(), line.getQuantity(), line.getUnitPrice(),
            line.getDiscountPercent(), line.getCategory()));
      }
      return new DocumentState(fields, includeTotals, splitLines, copiedLines);
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) return true;
      if (!(other instanceof DocumentState)) return false;
      DocumentState that = (DocumentState) other;
      if (includeTotals != that.includeTotals || splitLines != that.splitLines
          || !Arrays.equals(fields, that.fields) || lines.size() != that.lines.size()) return false;
      for (int i = 0; i < lines.size(); i++) {
        LineItem left = lines.get(i);
        LineItem right = that.lines.get(i);
        if (!Objects.equals(left.getDescription(), right.getDescription())
            || !Objects.equals(left.getQuantity(), right.getQuantity())
            || !Objects.equals(left.getUnitPrice(), right.getUnitPrice())
            || !Objects.equals(left.getDiscountPercent(), right.getDiscountPercent())
            || left.getCategory() != right.getCategory()) return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int result = Arrays.hashCode(fields);
      result = 31 * result + Boolean.hashCode(includeTotals);
      return 31 * result + Boolean.hashCode(splitLines);
    }
  }

  private String formatDate(LocalDate date) {
    return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
  }

  private void prefillDate(JTextField field) {
    if (field == null) return;
    Boolean active = (Boolean) field.getClientProperty("placeholder.active");
    String current = field.getText();
    if (Boolean.TRUE.equals(active) || current == null || current.isBlank()) {
      field.setText(formatDate(LocalDate.now()));
      field.putClientProperty("placeholder.active", Boolean.FALSE);
      syncFieldColor(field);
    }
  }

  private String formatPercent(BigDecimal value) {
    return value != null ? value.toPlainString() : "";
  }

  private List<LineItem> copyLines(List<LineItem> source) {
    List<LineItem> copy = new java.util.ArrayList<>();
    if (source == null) return copy;
    for (LineItem line : source) {
      if (line == null) continue;
      copy.add(new LineItem(line.getDescription(), line.getQuantity(), line.getUnitPrice(),
          line.getDiscountPercent(), line.getCategory()));
    }
    return copy;
  }

  private JPanel createFormSection(String title, String[] labels, JComponent[] fields) {
    JPanel section = new JPanel(new GridBagLayout());
    section.setOpaque(true);
    section.setBackground(sectionBgColor());
    section.setBorder(sectionBorder(title));
    section.putClientProperty("section.title", title);
    sections.add(section);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(4, 6, 4, 6);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 1;

    for (int i = 0; i < labels.length; i++) {
      gbc.gridx = 0;
      gbc.gridy = i;
      gbc.weightx = 0;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weighty = 0;
      JLabel label = createLabel(labels[i]);
      AccessibilitySupport.bindLabel(label, fields[i]);
      section.add(label, gbc);

      gbc.gridx = 1;
      gbc.weightx = 1;
      boolean isArea = fields[i] instanceof JTextArea;
      gbc.fill = isArea ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
      gbc.weighty = isArea ? 1 : 0;
      section.add(fields[i], gbc);
    }
    return section;
  }

  private JPanel createPartnerSection(String title, String[] labels, JComponent[] fields,
                                      Runnable onSelect, Runnable onSave) {
    JPanel section = new JPanel(new GridBagLayout());
    section.setOpaque(true);
    section.setBackground(sectionBgColor());
    section.setBorder(sectionBorder(title));
    section.putClientProperty("section.title", title);
    sections.add(section);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(4, 6, 4, 6);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 1;

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0;
    section.add(createLabel(I18n.t("partner.label")), gbc);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    actions.setOpaque(false);
    JButton selectBtn = new JButton(I18n.t("partner.select"));
    selectBtn.setFont(selectBtn.getFont().deriveFont(11f));
    selectBtn.setMargin(new Insets(2, 8, 2, 8));
    selectBtn.addActionListener(e -> onSelect.run());
    JButton saveBtn = new JButton(I18n.t("partner.save"));
    saveBtn.setFont(saveBtn.getFont().deriveFont(11f));
    saveBtn.setMargin(new Insets(2, 8, 2, 8));
    saveBtn.addActionListener(e -> onSave.run());
    actions.add(selectBtn);
    actions.add(saveBtn);

    gbc.gridx = 1;
    gbc.weightx = 1;
    section.add(actions, gbc);

    for (int i = 0; i < labels.length; i++) {
      gbc.gridx = 0;
      gbc.gridy = i + 1;
      gbc.weightx = 0;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weighty = 0;
      JLabel label = createLabel(labels[i]);
      AccessibilitySupport.bindLabel(label, fields[i]);
      section.add(label, gbc);

      gbc.gridx = 1;
      gbc.weightx = 1;
      boolean isArea = fields[i] instanceof JTextArea;
      gbc.fill = isArea ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
      gbc.weighty = isArea ? 1 : 0;
      section.add(fields[i], gbc);
    }
    return section;
  }

  private Border sectionBorder(String title) {
    return BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(sectionBorderColor()),
            title,
            TitledBorder.LEADING,
            TitledBorder.TOP,
            null,
            ThemeManager.palette().text()
        ),
        BorderFactory.createEmptyBorder(6, 6, 10, 6)
    );
  }

  private JLabel createLabel(String text) {
    JLabel l = new JLabel(text);
    l.setFont(l.getFont().deriveFont(Font.BOLD));
    return l;
  }

  private JScrollPane createNotesScroll() {
    notesScroll = new JScrollPane(taNotes);
    notesScroll.setBorder(BorderFactory.createLineBorder(sectionBorderColor()));
    notesScroll.getViewport().setBackground(sectionBgColor());
    return notesScroll;
  }

  private void setField(JTextComponent comp, String value) {
    String ph = (String) comp.getClientProperty("placeholder.text");
    if (value == null) value = "";
    comp.setText(value);
    comp.putClientProperty("placeholder.active", Boolean.FALSE);
    if (value.isEmpty() && ph != null) {
      comp.setText(ph);
      comp.putClientProperty("placeholder.active", Boolean.TRUE);
    }
    syncFieldColor(comp);
  }

  public void refreshTheme() {
    ThemePalette palette = ThemeManager.palette();
    Color bg = sectionBgColor();
    setBackground(panelBgColor());
    refreshFieldColors();
    taNotes.setBackground(sectionBgColor());
    if (notesScroll != null) {
      notesScroll.setBorder(BorderFactory.createLineBorder(sectionBorderColor()));
      notesScroll.getViewport().setBackground(sectionBgColor());
    }
    for (JComponent section : sections) {
      section.setBackground(bg);
      Object title = section.getClientProperty("section.title");
      if (title instanceof String) {
        section.setBorder(sectionBorder((String) title));
      }
      applyLabelColor(section, palette.text());
    }
    itemsPanel.setBorder(sectionBorder(I18n.t("section.items")));
    itemsPanel.refreshTheme();
    revalidate();
    repaint();
  }

  private void openSupplierSelector() {
    openPartnerSelector(this::loadSupplierPartner);
  }

  private void openClientSelector() {
    openPartnerSelector(this::loadClientPartner);
  }

  private void openPartnerSelector(Consumer<BusinessPartner> onSelected) {
    Window window = SwingUtilities.getWindowAncestor(this);
    Frame frame = window instanceof Frame ? (Frame) window : null;
    try {
      PartnerSelectorDialog dialog = new PartnerSelectorDialog(frame);
      dialog.setOnSelected(onSelected);
      dialog.setVisible(true);
    } catch (RuntimeException ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
    }
  }

  private void loadSupplierPartner(BusinessPartner partner) {
    if (partner == null) return;
    setField(tfSuppName, partner.getName());
    setField(tfSuppNif, partner.getNif());
    setField(tfSuppAddr, partner.getAddress());
  }

  private void loadClientPartner(BusinessPartner partner) {
    if (partner == null) return;
    setField(tfClientName, partner.getName());
    setField(tfClientNif, partner.getNif());
    setField(tfClientAddr, partner.getAddress());
  }

  private void saveSupplierPartner() {
    savePartnerFromFields(cleanText(tfSuppName), cleanText(tfSuppNif),
        cleanText(tfSuppAddr), null);
  }

  private void saveClientPartner() {
    savePartnerFromFields(cleanText(tfClientName), cleanText(tfClientNif),
        cleanText(tfClientAddr), null);
  }

  private void savePartnerFromFields(String name, String nif, String address, String account) {
    if (name == null || name.trim().isEmpty()) {
      JOptionPane.showMessageDialog(this, I18n.t("partner.msg.missing_name"),
          I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
      return;
    }
    BusinessPartner partner = new BusinessPartner();
    partner.setName(name);
    partner.setNif(nif);
    partner.setAddress(address);
    partner.setAccount(account);
    try {
      partnerStore.upsert(partner);
      JOptionPane.showMessageDialog(this, I18n.t("partner.msg.saved"));
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, ex.getMessage(),
          I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
    }
  }

  private void applyLabelColor(Container container, Color color) {
    for (Component c : container.getComponents()) {
      if (c instanceof JLabel) {
        ((JLabel) c).setForeground(color);
      }
    }
  }

  private Color sectionBorderColor() {
    return ThemeManager.palette().border();
  }

  private Color sectionBgColor() {
    return ThemeManager.palette().card();
  }

  private Color panelBgColor() {
    return ThemeManager.palette().background();
  }

  private void applyPlaceholder(JTextComponent comp, String placeholder) {
    comp.putClientProperty("placeholder.text", placeholder);
    AccessibilitySupport.describe(comp, I18n.t("accessibility.example", placeholder.replace('\n', ' ')));
    if (comp.getText().isEmpty()) {
      comp.setText(placeholder);
      comp.putClientProperty("placeholder.active", Boolean.TRUE);
    } else {
      comp.putClientProperty("placeholder.active", Boolean.FALSE);
    }
    syncFieldColor(comp);
    comp.addFocusListener(new java.awt.event.FocusAdapter() {
      @Override public void focusGained(java.awt.event.FocusEvent e) {
        Boolean active = (Boolean) comp.getClientProperty("placeholder.active");
        String ph = (String) comp.getClientProperty("placeholder.text");
        if (Boolean.TRUE.equals(active) && ph != null && comp.getText().equals(ph)) {
          comp.setText("");
          comp.putClientProperty("placeholder.active", Boolean.FALSE);
        }
        syncFieldColor(comp);
      }
      @Override public void focusLost(java.awt.event.FocusEvent e) {
        if (comp.getText().isEmpty()) {
          comp.setText(placeholder);
          comp.putClientProperty("placeholder.active", Boolean.TRUE);
        }
        syncFieldColor(comp);
      }
    });
  }

  private String cleanText(JTextComponent comp) {
    String ph = (String) comp.getClientProperty("placeholder.text");
    String txt = comp.getText();
    Boolean active = (Boolean) comp.getClientProperty("placeholder.active");
    if (Boolean.TRUE.equals(active) && ph != null && ph.equals(txt)) return "";
    return txt;
  }

  private void refreshFieldColors() {
    for (JTextComponent comp : allFields) {
      syncFieldColor(comp);
    }
  }

  private void syncFieldColor(JTextComponent comp) {
    ThemePalette palette = ThemeManager.palette();
    boolean active = Boolean.TRUE.equals(comp.getClientProperty("placeholder.active"));
    comp.setForeground(active ? palette.placeholder() : palette.text());
    comp.setCaretColor(palette.text());
    comp.setBackground(palette.card());
  }

  private JTextArea createAddressArea() {
    JTextArea area = new JTextArea(3, 20);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    Border border = UIManager.getBorder("TextField.border");
    if (border != null) {
      area.setBorder(border);
    }
    return area;
  }
}
