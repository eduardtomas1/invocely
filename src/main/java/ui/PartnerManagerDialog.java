package ui;

import i18n.I18n;
import models.BusinessPartner;
import storage.BusinessPartnerStore;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PartnerManagerDialog extends JDialog {
    private final BusinessPartnerStore store = new BusinessPartnerStore();
    private final DefaultListModel<BusinessPartner> listModel = new DefaultListModel<>();
    private final JList<BusinessPartner> list = new JList<>(listModel);
    private final JTextField tfSearch = new JTextField();

    private final JTextField tfName = new JTextField();
    private final JTextField tfNif = new JTextField();
    private final JTextArea taAddress = createAddressArea();
    private final JTextField tfAccount = new JTextField();
    private final JTextField tfEmail = new JTextField();
    private final JTextField tfPhone = new JTextField();
    private final ThemePalette palette = ThemeManager.palette();

    private List<BusinessPartner> allPartners = new ArrayList<>();
    private BusinessPartner editing;
    private Runnable onChange;

    public PartnerManagerDialog(Frame owner) {
        super(owner, I18n.t("partner.dialog.manage_title"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(14, 14));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        content.setBackground(palette.background());
        content.setOpaque(true);
        setContentPane(content);

        content.add(buildSearchPanel(), BorderLayout.NORTH);
        content.add(buildContent(), BorderLayout.CENTER);
        content.add(buildActions(), BorderLayout.SOUTH);

        loadPartners();
        setSize(860, 520);
        setLocationRelativeTo(owner);
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        JLabel label = new JLabel(I18n.t("partner.search.label"));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setForeground(palette.muted());
        panel.add(label, BorderLayout.WEST);
        panel.add(tfSearch, BorderLayout.CENTER);
        panel.setOpaque(false);
        tfSearch.putClientProperty("JTextField.placeholderText", I18n.t("partner.search.placeholder"));
        tfSearch.putClientProperty("JTextField.showClearButton", true);
        tfSearch.getDocument().addDocumentListener(new SimpleDocumentListener(this::applyFilter));
        return panel;
    }

    private JComponent buildContent() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.35);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setOpaque(false);
        split.setLeftComponent(buildListPanel());
        split.setRightComponent(buildFormPanel());
        return split;
    }

    private JComponent buildListPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(palette.card());
        panel.setBorder(createCardBorder(null));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new PartnerCellRenderer());
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectPartner(list.getSelectedValue());
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(palette.card());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(createCardBorder(I18n.t("partner.details")));
        form.setBackground(palette.card());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        addRow(form, gbc, 0, I18n.t("partner.field.name"), tfName);
        addRow(form, gbc, 1, I18n.t("partner.field.nif"), tfNif);
        addRow(form, gbc, 2, I18n.t("partner.field.address"), wrapArea(taAddress));
        addRow(form, gbc, 3, I18n.t("partner.field.account"), tfAccount);
        addRow(form, gbc, 4, I18n.t("partner.field.email"), tfEmail);
        addRow(form, gbc, 5, I18n.t("partner.field.phone"), tfPhone);

        return form;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        left.setOpaque(false);
        right.setOpaque(false);
        JButton btnNew = new JButton(I18n.t("partner.button.new"));
        JButton btnSave = new JButton(I18n.t("common.save"));
        JButton btnDelete = new JButton(I18n.t("common.delete"));
        JButton btnClose = new JButton(I18n.t("common.close"));

        btnNew.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> savePartner());
        btnDelete.addActionListener(e -> deletePartner());
        btnClose.addActionListener(e -> dispose());

        left.add(btnNew);
        left.add(btnSave);
        left.add(btnDelete);
        right.add(btnClose);
        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        getRootPane().setDefaultButton(btnSave);
        return panel;
    }

    private void loadPartners() {
        allPartners = store.load();
        applyFilter();
    }

    private void applyFilter() {
        String query = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase(Locale.ROOT);
        listModel.clear();
        for (BusinessPartner partner : allPartners) {
            if (query.isEmpty() || matches(partner, query)) {
                listModel.addElement(partner);
            }
        }
        if (!listModel.isEmpty()) {
            list.setSelectedIndex(0);
        } else {
            selectPartner(null);
        }
    }

    private boolean matches(BusinessPartner partner, String query) {
        String name = partner.getName() == null ? "" : partner.getName().toLowerCase(Locale.ROOT);
        String nif = partner.getNif() == null ? "" : partner.getNif().toLowerCase(Locale.ROOT);
        return name.contains(query) || nif.contains(query);
    }

    private void selectPartner(BusinessPartner partner) {
        editing = partner;
        if (partner == null) {
            clearFormFields();
            return;
        }
        tfName.setText(value(partner.getName()));
        tfNif.setText(value(partner.getNif()));
        taAddress.setText(value(partner.getAddress()));
        tfAccount.setText(value(partner.getAccount()));
        tfEmail.setText(value(partner.getEmail()));
        tfPhone.setText(value(partner.getPhone()));
    }

    private void clearForm() {
        list.clearSelection();
        editing = null;
        clearFormFields();
    }

    private void clearFormFields() {
        tfName.setText("");
        tfNif.setText("");
        taAddress.setText("");
        tfAccount.setText("");
        tfEmail.setText("");
        tfPhone.setText("");
    }

    private void savePartner() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, I18n.t("partner.msg.missing_name"), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        BusinessPartner partner = new BusinessPartner();
        if (editing != null) {
            partner.setId(editing.getId());
        }
        partner.setName(name);
        partner.setNif(tfNif.getText());
        partner.setAddress(taAddress.getText());
        partner.setAccount(tfAccount.getText());
        partner.setEmail(tfEmail.getText());
        partner.setPhone(tfPhone.getText());
        try {
            BusinessPartner saved = store.upsert(partner);
            loadPartners();
            selectById(saved.getId());
            if (onChange != null) onChange.run();
            JOptionPane.showMessageDialog(this, I18n.t("partner.msg.saved"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deletePartner() {
        if (editing == null || editing.getId() == null) return;
        int res = JOptionPane.showConfirmDialog(this,
                I18n.t("partner.msg.confirm_delete"),
                I18n.t("dialog.confirm"),
                JOptionPane.YES_NO_OPTION);
        if (res != JOptionPane.YES_OPTION) return;
        try {
            store.delete(editing.getId());
            loadPartners();
            if (onChange != null) onChange.run();
            JOptionPane.showMessageDialog(this, I18n.t("partner.msg.deleted"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectById(String id) {
        for (int i = 0; i < listModel.size(); i++) {
            BusinessPartner partner = listModel.getElementAt(i);
            if (partner != null && id != null && id.equals(partner.getId())) {
                list.setSelectedIndex(i);
                break;
            }
        }
    }

    private void addRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        lbl.setForeground(palette.muted());
        form.add(lbl, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(field, gbc);
    }

    private JScrollPane wrapArea(JTextArea area) {
        JScrollPane scroll = new JScrollPane(area);
        Border border = UIManager.getBorder("TextField.border");
        if (border != null) {
            scroll.setBorder(border);
        }
        scroll.getViewport().setBackground(palette.card());
        scroll.setBackground(palette.card());
        return scroll;
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

    private String value(String value) {
        return value == null ? "" : value;
    }

    private Border createCardBorder(String title) {
        Border line = BorderFactory.createLineBorder(palette.border());
        Border inner = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        if (title == null || title.isBlank()) {
            return BorderFactory.createCompoundBorder(line, inner);
        }
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(line, title, TitledBorder.LEADING, TitledBorder.TOP, null, palette.muted()),
                inner
        );
    }

}
