package ui;

import i18n.I18n;
import models.BusinessPartner;
import models.InvoiceData;
import models.LineItem;
import storage.BusinessPartnerStore;
import table.LineTableModel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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

public class InvoicePanel extends JPanel {

    private final DecimalFormat amountFormat = createAmountFormat();

    private final JTextField tfNumber = new JTextField();
    private final JTextField tfDate   = new JTextField();
    private final JTextField tfIssuerName = new JTextField();
    private final JTextField tfIssuerNif  = new JTextField();
    private final JTextArea tfIssuerAddr = createAddressArea();
    private final JTextField tfIssuerAccount = new JTextField();
    private final JTextField tfCustName = new JTextField();
    private final JTextField tfCustNif  = new JTextField();
    private final JTextArea tfCustAddr = createAddressArea();
    private final JTextField tfVatPercent = new JTextField("21");
    private final ItemTablePanel itemsPanel = new ItemTablePanel();
    private final BusinessPartnerStore partnerStore = new BusinessPartnerStore();
    private final JLabel lblSubtotal = createAmountLabel();
    private final JLabel lblTotalDiscount = createAmountLabel();
    private final JLabel lblTotalVat = createAmountLabel();
    private final JLabel lblGrandTotal = createAmountLabel();
    private final List<JComponent> sections = new ArrayList<>();
    private final List<JPanel> metricPanels = new ArrayList<>();
    private final List<JLabel> metricTitleLabels = new ArrayList<>();
    private final List<JTextComponent> allFields = new ArrayList<>();
    private DocumentState cleanState;

    public InvoicePanel() {
      setLayout(new BorderLayout(12,12));
      setOpaque(true);
      setBackground(panelBgColor());

      // placeholders for quick guidance
      applyPlaceholder(tfNumber, I18n.t("placeholder.invoice_number"));
      applyPlaceholder(tfDate, I18n.t("placeholder.date"));
      applyPlaceholder(tfIssuerName, I18n.t("placeholder.issuer_name"));
      applyPlaceholder(tfIssuerNif, I18n.t("placeholder.issuer_nif"));
      applyPlaceholder(tfIssuerAddr, I18n.t("placeholder.issuer_address"));
      applyPlaceholder(tfIssuerAccount, I18n.t("placeholder.issuer_account"));
      applyPlaceholder(tfCustName, I18n.t("placeholder.customer_name"));
      applyPlaceholder(tfCustNif, I18n.t("placeholder.customer_nif"));
      applyPlaceholder(tfCustAddr, I18n.t("placeholder.customer_address"));
      allFields.add(tfNumber);
      allFields.add(tfDate);
      allFields.add(tfIssuerName);
      allFields.add(tfIssuerNif);
      allFields.add(tfIssuerAddr);
      allFields.add(tfIssuerAccount);
      allFields.add(tfCustName);
      allFields.add(tfCustNif);
      allFields.add(tfCustAddr);
      allFields.add(tfVatPercent);

      prefillDate(tfDate);

      JPanel north = new JPanel(new GridLayout(1,3,12,12));
      north.setOpaque(true);
      north.setBackground(panelBgColor());
      north.add(createFormSection(I18n.t("section.invoice"),
              new String[] { I18n.t("label.invoice_number"), I18n.t("label.issue_date") },
              new JComponent[] { tfNumber, tfDate }));
      north.add(createFormSection(I18n.t("section.issuer"),
              new String[] { I18n.t("label.issuer_name"), I18n.t("label.issuer_nif"),
                  I18n.t("label.issuer_address"), I18n.t("label.issuer_account") },
              new JComponent[] { tfIssuerName, tfIssuerNif, tfIssuerAddr, tfIssuerAccount }));
      north.add(createPartnerSection(I18n.t("section.customer"),
              new String[] { I18n.t("label.customer_name"), I18n.t("label.customer_nif"),
                  I18n.t("label.customer_address") },
              new JComponent[] { tfCustName, tfCustNif, tfCustAddr },
              this::openCustomerSelector, this::saveCustomerPartner));

      add(north, BorderLayout.NORTH);

      itemsPanel.setBorder(sectionBorder(I18n.t("section.items")));
      add(itemsPanel, BorderLayout.CENTER);

      itemsPanel.getModel().addTableModelListener(e -> updateTotals());
      registerTotalsListener(tfVatPercent);
      updateTotals();

      add(buildTotalsBand(), BorderLayout.SOUTH);
      markClean();
    }

    public InvoiceData collect() {
      itemsPanel.requireCommittedEdits();
      LineTableModel m = itemsPanel.getModel();
      List<LineItem> lines = copyLines(m.getItems());
      InvoiceData data = new InvoiceData(
          cleanText(tfNumber),
          InputParser.requiredDate(cleanText(tfDate)),
          cleanText(tfIssuerName), cleanText(tfIssuerNif), cleanText(tfIssuerAddr),
          cleanText(tfIssuerAccount),
          cleanText(tfCustName), cleanText(tfCustNif), cleanText(tfCustAddr),
          InputParser.percent(cleanText(tfVatPercent)),
          itemsPanel.isSplitLines(),
          lines
      );
      DocumentValidator.validateInvoice(data);
      return data;
    }

    public void requireCommittedTableEdits() {
      itemsPanel.requireCommittedEdits();
    }

    public void fillFromData(InvoiceData data) {
      itemsPanel.cancelPendingEdits();
      setField(tfNumber, data.getInvoiceNumber());
      setField(tfDate, data.getIssueDate() != null ? formatDate(data.getIssueDate()) : "");
      setField(tfIssuerName, data.getIssuerName());
      setField(tfIssuerNif, data.getIssuerNif());
      setField(tfIssuerAddr, data.getIssuerAddress());
      setField(tfIssuerAccount, data.getIssuerAccount());
      setField(tfCustName, data.getCustomerName());
      setField(tfCustNif, data.getCustomerNif());
      setField(tfCustAddr, data.getCustomerAddress());
      setField(tfVatPercent, formatPercent(data.getVatPercent()));
      itemsPanel.setItems(data.getLines());
      itemsPanel.setSplitLines(data.isSplitLines());
      updateTotals();
      markClean();
    }

    public void applyDefaults(InvoiceData data) {
      if (data == null) return;
      setField(tfNumber, data.getInvoiceNumber());
      if (data.getIssueDate() != null) setField(tfDate, formatDate(data.getIssueDate()));
      setField(tfIssuerName, data.getIssuerName());
      setField(tfIssuerNif, data.getIssuerNif());
      setField(tfIssuerAddr, data.getIssuerAddress());
      setField(tfIssuerAccount, data.getIssuerAccount());
      setField(tfCustName, data.getCustomerName());
      setField(tfCustNif, data.getCustomerNif());
      setField(tfCustAddr, data.getCustomerAddress());
      setField(tfVatPercent, formatPercent(data.getVatPercent()));
      itemsPanel.setSplitLines(data.isSplitLines());
      updateTotals();
    }

    public DraftState snapshotDraft() {
      return new DraftState(captureState(), cleanState);
    }

    public void restoreDraft(DraftState state) {
      if (state == null) return;
      applyState(state.current);
      cleanState = state.clean != null ? state.clean.copy() : captureState();
      updateTotals();
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
          cleanText(tfNumber), cleanText(tfDate), cleanText(tfIssuerName), cleanText(tfIssuerNif),
          cleanText(tfIssuerAddr), cleanText(tfIssuerAccount), cleanText(tfCustName),
          cleanText(tfCustNif), cleanText(tfCustAddr), cleanText(tfVatPercent)
      }, itemsPanel.isSplitLines(), copyLines(itemsPanel.getModel().getItems()));
    }

    private void applyState(DocumentState state) {
      if (state == null) return;
      setField(tfNumber, state.fields[0]);
      setField(tfDate, state.fields[1]);
      setField(tfIssuerName, state.fields[2]);
      setField(tfIssuerNif, state.fields[3]);
      setField(tfIssuerAddr, state.fields[4]);
      setField(tfIssuerAccount, state.fields[5]);
      setField(tfCustName, state.fields[6]);
      setField(tfCustNif, state.fields[7]);
      setField(tfCustAddr, state.fields[8]);
      setField(tfVatPercent, state.fields[9]);
      itemsPanel.setItems(copyLines(state.lines));
      itemsPanel.setSplitLines(state.splitLines);
    }

    private static final class DocumentState {
      private final String[] fields;
      private final boolean splitLines;
      private final List<LineItem> lines;

      private DocumentState(String[] fields, boolean splitLines, List<LineItem> lines) {
        this.fields = fields.clone();
        this.splitLines = splitLines;
        this.lines = lines;
      }

      private DocumentState copy() {
        List<LineItem> copiedLines = new ArrayList<>();
        for (LineItem line : lines) {
          copiedLines.add(new LineItem(line.getDescription(), line.getQuantity(), line.getUnitPrice(),
              line.getDiscountPercent(), line.getCategory()));
        }
        return new DocumentState(fields, splitLines, copiedLines);
      }

      @Override
      public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DocumentState)) return false;
        DocumentState that = (DocumentState) other;
        if (splitLines != that.splitLines || !Arrays.equals(fields, that.fields)
            || lines.size() != that.lines.size()) return false;
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
        return 31 * Arrays.hashCode(fields) + Boolean.hashCode(splitLines);
      }
    }

    private String formatDate(LocalDate date) {
      if (date == null) return "";
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

    private void updateTotals() {
      BigDecimal subtotal = BigDecimal.ZERO;
      BigDecimal discountTotal = BigDecimal.ZERO;
      for (LineItem li : itemsPanel.getModel().getItems()) {
        if (li == null) continue;
        BigDecimal base = nz(li.getLineBase());
        subtotal = subtotal.add(base);
        discountTotal = discountTotal.add(nz(li.getDiscountAmount()));
      }
      BigDecimal taxable = subtotal.subtract(discountTotal);
      BigDecimal vatPercent = previewPercent(tfVatPercent.getText());
      BigDecimal vatTotal = taxable.multiply(vatPercent).divide(BigDecimal.valueOf(100))
          .setScale(2, RoundingMode.HALF_UP);
      BigDecimal grand = taxable.add(vatTotal);
      lblSubtotal.setText(amountFormat.format(subtotal));
      lblTotalDiscount.setText(amountFormat.format(discountTotal));
      lblTotalVat.setText(amountFormat.format(vatTotal));
      lblGrandTotal.setText(amountFormat.format(grand));
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
      gbc.weighty = 0;

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
      gbc.weighty = 0;

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

    private JLabel createAmountLabel() {
      JLabel l = new JLabel("0.00");
      l.setFont(l.getFont().deriveFont(Font.BOLD, 16f));
      l.setHorizontalAlignment(SwingConstants.RIGHT);
      l.setForeground(ThemeManager.palette().text());
      return l;
    }

    private JPanel buildTotalsBand() {
      JPanel totals = new JPanel(new GridLayout(1, 4, 12, 12));
      totals.setOpaque(true);
      totals.setBackground(panelBgColor());
      totals.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

      totals.add(metric(I18n.t("metric.subtotal"), lblSubtotal));
      totals.add(metric(I18n.t("metric.discount"), lblTotalDiscount));
      totals.add(metricWithInput(I18n.t("metric.vat"), tfVatPercent, lblTotalVat));
      totals.add(metric(I18n.t("metric.total"), lblGrandTotal));
      return totals;
    }

    private JPanel metric(String title, JLabel value) {
      JPanel p = metricBase();
      JLabel titleLbl = new JLabel(title);
      titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD));
      titleLbl.setForeground(mutedTextColor());
      p.add(titleLbl, BorderLayout.NORTH);
      p.add(value, BorderLayout.SOUTH);
      metricPanels.add(p);
      metricTitleLabels.add(titleLbl);
      return p;
    }

    private JPanel metricWithInput(String title, JTextField input, JLabel value) {
      JPanel p = metricBase();
      JPanel header = new JPanel(new BorderLayout(6, 0));
      header.setOpaque(false);
      JLabel titleLbl = new JLabel(title);
      titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD));
      titleLbl.setForeground(mutedTextColor());
      AccessibilitySupport.bindLabel(titleLbl, input);
      input.setColumns(4);
      input.setHorizontalAlignment(SwingConstants.RIGHT);
      header.add(titleLbl, BorderLayout.WEST);
      header.add(input, BorderLayout.EAST);
      p.add(header, BorderLayout.NORTH);
      p.add(value, BorderLayout.SOUTH);
      metricPanels.add(p);
      metricTitleLabels.add(titleLbl);
      return p;
    }

    private JPanel metricBase() {
      JPanel p = new JPanel(new BorderLayout());
      p.setOpaque(true);
      p.setBackground(sectionBgColor());
      p.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createLineBorder(sectionBorderColor()),
          BorderFactory.createEmptyBorder(8, 10, 8, 10)
      ));
      return p;
    }

    public void refreshTheme() {
      ThemePalette palette = ThemeManager.palette();
      Color bg = sectionBgColor();
      setBackground(panelBgColor());
      refreshFieldColors();
      lblSubtotal.setForeground(palette.text());
      lblTotalDiscount.setForeground(palette.text());
      lblTotalVat.setForeground(palette.text());
      lblGrandTotal.setForeground(palette.text());
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
      for (JPanel p : metricPanels) {
        p.setBackground(bg);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(sectionBorderColor()),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
      }
      for (JLabel metricTitle : metricTitleLabels) {
        metricTitle.setForeground(mutedTextColor());
      }
      revalidate();
      repaint();
    }

    private void openCustomerSelector() {
      openPartnerSelector(this::loadCustomerPartner);
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

    private void loadCustomerPartner(BusinessPartner partner) {
      if (partner == null) return;
      setField(tfCustName, partner.getName());
      setField(tfCustNif, partner.getNif());
      setField(tfCustAddr, partner.getAddress());
    }

    private void saveCustomerPartner() {
      savePartnerFromFields(cleanText(tfCustName), cleanText(tfCustNif),
          cleanText(tfCustAddr), null);
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

    private Color mutedTextColor() {
      Color c = UIManager.getColor("Label.disabledForeground");
      if (c != null) return c;
      return ThemeManager.palette().muted();
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

    private BigDecimal previewPercent(String value) {
      try {
        return InputParser.percent(value);
      } catch (Exception e) {
        return BigDecimal.ZERO;
      }
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

    private String formatPercent(BigDecimal value) {
      if (value == null) return "";
      return value.stripTrailingZeros().toPlainString();
    }

    private BigDecimal nz(BigDecimal value) {
      return value != null ? value : BigDecimal.ZERO;
    }

    private static DecimalFormat createAmountFormat() {
      DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance(I18n.getLocale());
      format.applyPattern("#,##0.00");
      format.setRoundingMode(RoundingMode.HALF_UP);
      return format;
    }

    private void registerTotalsListener(JTextComponent comp) {
      comp.getDocument().addDocumentListener(new DocumentListener() {
        @Override public void insertUpdate(DocumentEvent e) { updateTotals(); }
        @Override public void removeUpdate(DocumentEvent e) { updateTotals(); }
        @Override public void changedUpdate(DocumentEvent e) { updateTotals(); }
      });
    }
}
