package ui;

import i18n.I18n;
import models.BudgetData;
import models.DocumentType;
import models.InvoiceData;
import models.LineItem;
import storage.DefaultsManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.math.BigDecimal;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

public class DefaultsDialog extends JDialog {
    private boolean saved = false;

    public DefaultsDialog(Frame owner, DocumentType type, DefaultsManager defaults) throws IOException {
        super(owner, I18n.t("defaults.title"), true);
        setLayout(new BorderLayout(12, 12));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(ThemeManager.palette().background());

        JComponent form;
        if (type == DocumentType.INVOICE) {
            InvoiceDefaultsForm panel = new InvoiceDefaultsForm();
            InvoiceData data = defaults.loadInvoiceDefaults();
            if (data != null) {
                panel.setData(data);
            }
            form = panel;
            add(buildActions(() -> {
                try {
                    defaults.saveInvoiceDefaults(panel.toData());
                    saved = true;
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
                }
            }), BorderLayout.SOUTH);
        } else {
            BudgetDefaultsForm panel = new BudgetDefaultsForm();
            BudgetData data = defaults.loadBudgetDefaults();
            if (data != null) {
                panel.setData(data);
            }
            form = panel;
            add(buildActions(() -> {
                try {
                    defaults.saveBudgetDefaults(panel.toData());
                    saved = true;
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
                }
            }), BorderLayout.SOUTH);
        }

        add(form, BorderLayout.CENTER);
        pack();
        setMinimumSize(new Dimension(760, 420));
    }

    public boolean isSaved() {
        return saved;
    }

    private JPanel buildActions(Runnable onSave) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actions.setBackground(ThemeManager.palette().background());
        JButton cancel = new JButton(I18n.t("common.cancel"));
        JButton save = new JButton(I18n.t("common.save"));
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> onSave.run());
        actions.add(cancel);
        actions.add(save);
        return actions;
    }

    private static class InvoiceDefaultsForm extends JPanel {
        private final JTextField tfNumber = new JTextField();
        private final JTextField tfDate = new JTextField();
        private final JTextField tfIssuerName = new JTextField();
        private final JTextField tfIssuerNif = new JTextField();
        private final JTextArea tfIssuerAddr = createAddressArea();
        private final JTextField tfIssuerAccount = new JTextField();
        private final JTextField tfCustName = new JTextField();
        private final JTextField tfCustNif = new JTextField();
        private final JTextArea tfCustAddr = createAddressArea();
        private final JTextField tfVatPercent = new JTextField();
        private final JCheckBox cbSplit = new JCheckBox(I18n.t("defaults.checkbox.split_lines"));

        InvoiceDefaultsForm() {
            super(new GridLayout(1, 3, 12, 12));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            setBackground(ThemeManager.palette().background());
            add(createSection(I18n.t("section.invoice"),
                new String[] { I18n.t("label.invoice_number"), I18n.t("label.issue_date"),
                    I18n.t("label.vat_percent"), I18n.t("defaults.checkbox.split_lines") },
                new JComponent[] { tfNumber, tfDate, tfVatPercent, cbSplit }));
            add(createSection(I18n.t("section.issuer"),
                new String[] { I18n.t("label.issuer_name"), I18n.t("label.issuer_nif"),
                    I18n.t("label.issuer_address"), I18n.t("label.issuer_account") },
                new JComponent[] { tfIssuerName, tfIssuerNif, tfIssuerAddr, tfIssuerAccount }));
            add(createSection(I18n.t("section.customer"),
                new String[] { I18n.t("label.customer_name"), I18n.t("label.customer_nif"),
                    I18n.t("label.customer_address") },
                new JComponent[] { tfCustName, tfCustNif, tfCustAddr }));
        }

        void setData(InvoiceData data) {
            setField(tfNumber, data.getInvoiceNumber());
            setField(tfDate, data.getIssueDate() != null ? formatDate(data.getIssueDate()) : "");
            setField(tfIssuerName, data.getIssuerName());
            setField(tfIssuerNif, data.getIssuerNif());
            setField(tfIssuerAddr, data.getIssuerAddress());
            setField(tfIssuerAccount, data.getIssuerAccount());
            setField(tfCustName, data.getCustomerName());
            setField(tfCustNif, data.getCustomerNif());
            setField(tfCustAddr, data.getCustomerAddress());
            setField(tfVatPercent, data.getVatPercent() != null ? data.getVatPercent().toPlainString() : "");
            cbSplit.setSelected(data.isSplitLines());
        }

        InvoiceData toData() {
            return new InvoiceData(
                tfNumber.getText().trim(),
                InputParser.optionalDate(tfDate.getText()),
                tfIssuerName.getText().trim(),
                tfIssuerNif.getText().trim(),
                tfIssuerAddr.getText().trim(),
                tfIssuerAccount.getText().trim(),
                tfCustName.getText().trim(),
                tfCustNif.getText().trim(),
                tfCustAddr.getText().trim(),
                InputParser.percent(tfVatPercent.getText()),
                cbSplit.isSelected(),
                new ArrayList<LineItem>()
            );
        }
    }

    private static class BudgetDefaultsForm extends JPanel {
        private final JTextField tfNumber = new JTextField();
        private final JTextField tfDate = new JTextField();
        private final JTextField tfValid = new JTextField();
        private final JTextField tfSuppName = new JTextField();
        private final JTextField tfSuppNif = new JTextField();
        private final JTextArea tfSuppAddr = createAddressArea();
        private final JTextField tfClientName = new JTextField();
        private final JTextField tfClientNif = new JTextField();
        private final JTextArea tfClientAddr = createAddressArea();
        private final JTextField tfPayment = new JTextField();
        private final JTextArea taNotes = new JTextArea(4, 20);
        private final JCheckBox cbIncludeTotals = new JCheckBox();
        private final JTextField tfTaxName = new JTextField("IVA");
        private final JTextField tfTaxPercent = new JTextField("21");
        private final JCheckBox cbSplit = new JCheckBox(I18n.t("defaults.checkbox.split_lines"));

        BudgetDefaultsForm() {
            super(new GridLayout(1, 3, 12, 12));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            setBackground(ThemeManager.palette().background());
            taNotes.setLineWrap(true);
            taNotes.setWrapStyleWord(true);

            add(createSection(I18n.t("section.budget"),
                new String[] { I18n.t("label.budget_number"), I18n.t("label.issue_date"),
                    I18n.t("label.valid_until"), I18n.t("defaults.checkbox.split_lines") },
                new JComponent[] { tfNumber, tfDate, tfValid, cbSplit }));
            add(createSection(I18n.t("section.supplier"),
                new String[] { I18n.t("label.supplier_name"), I18n.t("label.supplier_nif"),
                    I18n.t("label.supplier_address") },
                new JComponent[] { tfSuppName, tfSuppNif, tfSuppAddr }));
            add(createSection(I18n.t("section.client"),
                new String[] { I18n.t("label.client_name"), I18n.t("label.client_nif"),
                    I18n.t("label.client_address"), I18n.t("label.payment_terms"), I18n.t("label.notes"),
                    I18n.t("label.include_totals"), I18n.t("label.tax_name"), I18n.t("label.tax_percent") },
                new JComponent[] { tfClientName, tfClientNif, tfClientAddr, tfPayment, wrapNotes(taNotes),
                    cbIncludeTotals, tfTaxName, tfTaxPercent }));
        }

        void setData(BudgetData data) {
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
            taNotes.setText(data.getNotes() != null ? data.getNotes() : "");
            cbIncludeTotals.setSelected(data.isIncludeTotals());
            setField(tfTaxName, data.getTaxName());
            setField(tfTaxPercent, data.getTaxPercent() != null ? data.getTaxPercent().toPlainString() : "");
            cbSplit.setSelected(data.isSplitLines());
        }

        BudgetData toData() {
            LocalDate issueDate = InputParser.optionalDate(tfDate.getText());
            LocalDate validUntil = InputParser.optionalDate(tfValid.getText());
            if ((issueDate == null) != (validUntil == null)) {
                throw new IllegalArgumentException(I18n.t("validation.default_dates_pair"));
            }
            if (issueDate != null && validUntil != null) {
                InputParser.validDateRange(issueDate, validUntil);
            }
            return new BudgetData(
                tfNumber.getText().trim(),
                issueDate,
                validUntil,
                tfSuppName.getText().trim(),
                tfSuppNif.getText().trim(),
                tfSuppAddr.getText().trim(),
                tfClientName.getText().trim(),
                tfClientNif.getText().trim(),
                tfClientAddr.getText().trim(),
                tfPayment.getText().trim(),
                taNotes.getText().trim(),
                cbIncludeTotals.isSelected(),
                tfTaxName.getText().trim(),
                InputParser.percent(tfTaxPercent.getText()),
                cbSplit.isSelected(),
                new ArrayList<LineItem>()
            );
        }
    }

    private static JPanel createSection(String title, String[] labels, JComponent[] fields) {
        ThemePalette palette = ThemeManager.palette();
        JPanel section = new JPanel(new GridBagLayout());
        Border line = BorderFactory.createLineBorder(palette.border());
        Border inner = BorderFactory.createEmptyBorder(6, 6, 8, 6);
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(line, title, TitledBorder.LEADING, TitledBorder.TOP, null, palette.muted()),
            inner
        ));
        section.setBackground(palette.card());
        section.setOpaque(true);

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
            JLabel label = new JLabel(labels[i]);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            label.setForeground(palette.text());
            section.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            boolean isArea = fields[i] instanceof JTextArea || fields[i] instanceof JScrollPane;
            gbc.fill = isArea ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
            gbc.weighty = isArea ? 1 : 0;
            section.add(fields[i], gbc);
        }
        return section;
    }

    private static JTextArea createAddressArea() {
        JTextArea area = new JTextArea(3, 20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        Border border = UIManager.getBorder("TextField.border");
        if (border != null) {
            area.setBorder(border);
        }
        return area;
    }

    private static JScrollPane wrapNotes(JTextArea notes) {
        JScrollPane scroll = new JScrollPane(notes);
        scroll.setBorder(UIManager.getBorder("TextField.border"));
        return scroll;
    }

    private static void setField(JTextComponent comp, String value) {
        comp.setText(value != null ? value : "");
    }

    private static String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

}
