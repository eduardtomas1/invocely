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
import java.util.function.Consumer;

public class PartnerSelectorDialog extends JDialog {
    private final BusinessPartnerStore store = new BusinessPartnerStore();
    private final DefaultListModel<BusinessPartner> listModel = new DefaultListModel<>();
    private final JList<BusinessPartner> list = new JList<>(listModel);
    private final JTextField tfSearch = new JTextField();
    private final ThemePalette palette = ThemeManager.palette();

    private final JLabel lblName = new JLabel();
    private final JLabel lblNif = new JLabel();
    private final JTextArea taAddress = new JTextArea();

    private List<BusinessPartner> allPartners = new ArrayList<>();
    private Consumer<BusinessPartner> onSelected;

    public PartnerSelectorDialog(Frame owner) {
        super(owner, I18n.t("partner.dialog.select_title"), true);
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
        setSize(720, 420);
        setLocationRelativeTo(owner);
    }

    public void setOnSelected(Consumer<BusinessPartner> onSelected) {
        this.onSelected = onSelected;
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
        split.setResizeWeight(0.5);
        split.setDividerSize(8);
        split.setBorder(null);
        split.setOpaque(false);
        split.setLeftComponent(buildListPanel());
        split.setRightComponent(buildPreviewPanel());
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
                updatePreview(list.getSelectedValue());
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(palette.card());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildPreviewPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(createCardBorder(I18n.t("partner.details")));
        panel.setBackground(palette.card());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        taAddress.setLineWrap(true);
        taAddress.setWrapStyleWord(true);
        taAddress.setEditable(false);
        taAddress.setOpaque(false);
        taAddress.setForeground(palette.text());
        lblName.setFont(lblName.getFont().deriveFont(Font.BOLD));
        lblNif.setForeground(palette.text());

        addRow(panel, gbc, 0, I18n.t("partner.field.name"), lblName);
        addRow(panel, gbc, 1, I18n.t("partner.field.nif"), lblNif);
        addRow(panel, gbc, 2, I18n.t("partner.field.address"), taAddress);
        return panel;
    }

    private JPanel buildActions() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        left.setOpaque(false);
        right.setOpaque(false);
        JButton btnManage = new JButton(I18n.t("common.manage"));
        JButton btnUse = new JButton(I18n.t("partner.button.use"));
        JButton btnCancel = new JButton(I18n.t("common.cancel"));

        btnManage.addActionListener(e -> openManager());
        btnUse.addActionListener(e -> useSelection());
        btnCancel.addActionListener(e -> dispose());

        left.add(btnManage);
        right.add(btnCancel);
        right.add(btnUse);
        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        getRootPane().setDefaultButton(btnUse);
        return panel;
    }

    private void openManager() {
        try {
            Window owner = getOwner();
            Frame frame = owner instanceof Frame ? (Frame) owner : null;
            PartnerManagerDialog dialog = new PartnerManagerDialog(frame);
            dialog.setOnChange(this::loadPartners);
            dialog.setVisible(true);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
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
            updatePreview(null);
        }
    }

    private boolean matches(BusinessPartner partner, String query) {
        String name = partner.getName() == null ? "" : partner.getName().toLowerCase(Locale.ROOT);
        String nif = partner.getNif() == null ? "" : partner.getNif().toLowerCase(Locale.ROOT);
        return name.contains(query) || nif.contains(query);
    }

    private void updatePreview(BusinessPartner partner) {
        if (partner == null) {
            lblName.setText("-");
            lblNif.setText("-");
            taAddress.setText("-");
            return;
        }
        lblName.setText(value(partner.getName()));
        lblNif.setText(value(partner.getNif()));
        taAddress.setText(value(partner.getAddress()));
    }

    private void useSelection() {
        BusinessPartner selected = list.getSelectedValue();
        if (selected == null) return;
        if (onSelected != null) onSelected.accept(selected);
        dispose();
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        lbl.setForeground(palette.muted());
        panel.add(lbl, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(value, gbc);
    }

    private String value(String value) {
        if (value == null) return "-";
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "-" : value;
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
