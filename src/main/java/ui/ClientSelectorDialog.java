package ui;

import models.ClientInfo;
import storage.ClientStorage;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A beautiful dialog for selecting and managing clients.
 * Features a searchable list with quick filtering and CRUD operations.
 */
public class ClientSelectorDialog extends JDialog {
    private final ClientStorage storage;
    private final JTextField searchField;
    private final JList<ClientInfo> clientList;
    private final DefaultListModel<ClientInfo> listModel;
    private final JLabel previewName;
    private final JLabel previewNif;
    private final JTextArea previewAddress;
    private final JButton selectBtn;
    private final JButton editBtn;
    private final JButton deleteBtn;

    private List<ClientInfo> allClients;
    private ClientInfo selectedClient;
    private Consumer<ClientInfo> onClientSelected;

    public ClientSelectorDialog(Frame owner) {
        super(owner, "Seleccionar Client", true);
        this.storage = new ClientStorage();
        this.allClients = storage.loadClients();
        this.listModel = new DefaultListModel<>();

        setLayout(new BorderLayout(12, 12));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(ThemeManager.palette().background());

        // Top panel with search field
        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        topPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("Cercar:");
        searchLabel.setFont(searchLabel.getFont().deriveFont(Font.BOLD));
        searchLabel.setForeground(ThemeManager.palette().muted());
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Escriu per filtrar clients...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterClients(); }
            @Override public void removeUpdate(DocumentEvent e) { filterClients(); }
            @Override public void changedUpdate(DocumentEvent e) { filterClients(); }
        });

        topPanel.add(searchLabel, BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Center panel with list and preview
        JPanel centerPanel = new JPanel(new BorderLayout(12, 0));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        centerPanel.setOpaque(false);

        // Client list with custom renderer
        clientList = new JList<>(listModel);
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientList.setCellRenderer(new ClientListCellRenderer());
        clientList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePreview();
                updateButtonStates();
            }
        });
        clientList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && clientList.getSelectedValue() != null) {
                    selectAndClose();
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(clientList);
        listScroll.setPreferredSize(new Dimension(280, 300));
        listScroll.setBorder(createThemedBorder("Clients"));
        listScroll.getViewport().setBackground(ThemeManager.palette().card());
        listScroll.setBackground(ThemeManager.palette().card());

        // Preview panel
        JPanel previewPanel = new JPanel(new GridBagLayout());
        previewPanel.setBorder(createThemedBorder("Detalls del client"));
        previewPanel.setOpaque(true);
        previewPanel.setBackground(ThemeManager.palette().card());
        previewPanel.setPreferredSize(new Dimension(260, 300));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.gridx = 0; gbc.gridy = 0;
        previewPanel.add(createBoldLabel("Nom:"), gbc);

        gbc.gridy = 1;
        previewName = new JLabel("-");
        previewName.setForeground(ThemeManager.palette().text());
        previewPanel.add(previewName, gbc);

        gbc.gridy = 2;
        previewPanel.add(createBoldLabel("NIF/DNI:"), gbc);

        gbc.gridy = 3;
        previewNif = new JLabel("-");
        previewNif.setForeground(ThemeManager.palette().text());
        previewPanel.add(previewNif, gbc);

        gbc.gridy = 4;
        previewPanel.add(createBoldLabel("Adreca:"), gbc);

        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        previewAddress = new JTextArea(4, 20);
        previewAddress.setEditable(false);
        previewAddress.setLineWrap(true);
        previewAddress.setWrapStyleWord(true);
        previewAddress.setOpaque(false);
        previewAddress.setFont(previewName.getFont());
        previewAddress.setForeground(ThemeManager.palette().text());
        previewAddress.setText("-");
        previewPanel.add(previewAddress, gbc);

        centerPanel.add(listScroll, BorderLayout.CENTER);
        centerPanel.add(previewPanel, BorderLayout.EAST);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel with action buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        bottomPanel.setOpaque(false);

        // Left side: CRUD buttons
        JPanel crudPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        crudPanel.setOpaque(false);

        JButton addBtn = new JButton("Afegir");
        addBtn.addActionListener(e -> addNewClient());

        editBtn = new JButton("Editar");
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> editSelectedClient());

        deleteBtn = new JButton("Eliminar");
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> deleteSelectedClient());

        crudPanel.add(addBtn);
        crudPanel.add(editBtn);
        crudPanel.add(deleteBtn);

        // Right side: Cancel and Select buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        selectBtn = new JButton("Seleccionar");
        selectBtn.setEnabled(false);
        selectBtn.addActionListener(e -> selectAndClose());

        actionPanel.add(cancelBtn);
        actionPanel.add(selectBtn);

        bottomPanel.add(crudPanel, BorderLayout.WEST);
        bottomPanel.add(actionPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Initialize the list
        refreshList();

        // Key bindings
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && !listModel.isEmpty()) {
                    clientList.requestFocusInWindow();
                    if (clientList.getSelectedIndex() < 0) {
                        clientList.setSelectedIndex(0);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER && clientList.getSelectedValue() != null) {
                    selectAndClose();
                }
            }
        });

        clientList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && clientList.getSelectedValue() != null) {
                    selectAndClose();
                }
            }
        });

        // Dialog setup
        pack();
        setMinimumSize(new Dimension(600, 450));
        setLocationRelativeTo(owner);

        // Focus search field on open
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                searchField.requestFocusInWindow();
            }
        });
    }

    /**
     * Sets a callback to be invoked when a client is selected.
     */
    public void setOnClientSelected(Consumer<ClientInfo> callback) {
        this.onClientSelected = callback;
    }

    /**
     * Returns the selected client, or null if none was selected.
     */
    public ClientInfo getSelectedClient() {
        return selectedClient;
    }

    private void filterClients() {
        String query = searchField.getText();
        listModel.clear();
        for (ClientInfo client : allClients) {
            if (client.matchesSearch(query)) {
                listModel.addElement(client);
            }
        }
        if (!listModel.isEmpty()) {
            clientList.setSelectedIndex(0);
        }
        updatePreview();
        updateButtonStates();
    }

    private void refreshList() {
        allClients = storage.loadClients();
        filterClients();
    }

    private void updatePreview() {
        ClientInfo selected = clientList.getSelectedValue();
        if (selected != null) {
            previewName.setText(selected.getName().isEmpty() ? "-" : selected.getName());
            previewNif.setText(selected.getNif().isEmpty() ? "-" : selected.getNif());
            previewAddress.setText(selected.getAddress().isEmpty() ? "-" : selected.getAddress());
        } else {
            previewName.setText("-");
            previewNif.setText("-");
            previewAddress.setText("-");
        }
    }

    private void updateButtonStates() {
        boolean hasSelection = clientList.getSelectedValue() != null;
        selectBtn.setEnabled(hasSelection);
        editBtn.setEnabled(hasSelection);
        deleteBtn.setEnabled(hasSelection);
    }

    private void selectAndClose() {
        selectedClient = clientList.getSelectedValue();
        if (selectedClient != null && onClientSelected != null) {
            onClientSelected.accept(selectedClient);
        }
        dispose();
    }

    private void addNewClient() {
        ClientEditorDialog editor = new ClientEditorDialog((Frame) getOwner(), null);
        editor.setVisible(true);
        if (editor.isSaved()) {
            try {
                storage.addClient(editor.getClient());
                refreshList();
                // Select the newly added client
                for (int i = 0; i < listModel.size(); i++) {
                    if (listModel.get(i).getId().equals(editor.getClient().getId())) {
                        clientList.setSelectedIndex(i);
                        clientList.ensureIndexIsVisible(i);
                        break;
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error guardant el client: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editSelectedClient() {
        ClientInfo selected = clientList.getSelectedValue();
        if (selected == null) return;

        ClientEditorDialog editor = new ClientEditorDialog((Frame) getOwner(), selected);
        editor.setVisible(true);
        if (editor.isSaved()) {
            try {
                storage.updateClient(editor.getClient());
                refreshList();
                // Reselect the edited client
                for (int i = 0; i < listModel.size(); i++) {
                    if (listModel.get(i).getId().equals(editor.getClient().getId())) {
                        clientList.setSelectedIndex(i);
                        clientList.ensureIndexIsVisible(i);
                        break;
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error actualitzant el client: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelectedClient() {
        ClientInfo selected = clientList.getSelectedValue();
        if (selected == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Segur que vols eliminar el client \"" + selected.getDisplayName() + "\"?",
                "Confirmar eliminacio",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                storage.removeClient(selected.getId());
                refreshList();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error eliminant el client: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JLabel createBoldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setForeground(ThemeManager.palette().muted());
        return label;
    }

    private Border createThemedBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(ThemeManager.palette().border()),
                        title,
                        TitledBorder.LEADING,
                        TitledBorder.TOP,
                        null,
                        ThemeManager.palette().text()
                ),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        );
    }

    /**
     * Custom cell renderer for the client list.
     */
    private static class ClientListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ClientInfo) {
                ClientInfo client = (ClientInfo) value;
                ThemePalette palette = ThemeManager.palette();
                String secondaryColor = toHex(isSelected ? palette.text() : palette.muted());
                String html = String.format(
                        "<html><b>%s</b><br><span style='color:%s;font-size:90%%;'>%s</span></html>",
                        escapeHtml(client.getName().isEmpty() ? "(Sense nom)" : client.getName()),
                        secondaryColor,
                        escapeHtml(client.getNif().isEmpty() ? "-" : client.getNif())
                );
                setText(html);
                setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                setForeground(palette.text());
            }

            if (isSelected) {
                setBackground(ThemeManager.palette().tableSelected());
            } else {
                setBackground(index % 2 == 0 ? ThemeManager.palette().tableEven() : ThemeManager.palette().tableOdd());
            }

            return this;
        }

        private String escapeHtml(String text) {
            return text.replace("&", "&amp;")
                       .replace("<", "&lt;")
                       .replace(">", "&gt;");
        }

        private String toHex(Color color) {
            return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        }
    }

    /**
     * Dialog for creating/editing a client.
     */
    private static class ClientEditorDialog extends JDialog {
        private final JTextField tfName;
        private final JTextField tfNif;
        private final JTextArea taAddress;
        private ClientInfo client;
        private boolean saved = false;

        ClientEditorDialog(Frame owner, ClientInfo existing) {
            super(owner, existing == null ? "Nou Client" : "Editar Client", true);
            this.client = existing;

            setLayout(new BorderLayout(12, 12));
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
            formPanel.setBackground(ThemeManager.palette().card());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 8, 6, 8);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Name
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.weightx = 0;
            formPanel.add(createBoldLabel("Nom/Rao social:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            tfName = new JTextField(25);
            formPanel.add(tfName, gbc);

            // NIF
            gbc.gridx = 0; gbc.gridy = 1;
            gbc.weightx = 0;
            formPanel.add(createBoldLabel("NIF/DNI:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            tfNif = new JTextField(25);
            formPanel.add(tfNif, gbc);

            // Address
            gbc.gridx = 0; gbc.gridy = 2;
            gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            formPanel.add(createBoldLabel("Adreca fiscal:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            taAddress = new JTextArea(4, 25);
            taAddress.setLineWrap(true);
            taAddress.setWrapStyleWord(true);
            Border border = UIManager.getBorder("TextField.border");
            if (border != null) {
                taAddress.setBorder(border);
            }
            formPanel.add(taAddress, gbc);

            add(formPanel, BorderLayout.CENTER);

            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            buttonPanel.setBackground(ThemeManager.palette().background());

            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(e -> dispose());

            JButton saveBtn = new JButton("Guardar");
            saveBtn.addActionListener(e -> saveAndClose());

            buttonPanel.add(cancelBtn);
            buttonPanel.add(saveBtn);
            add(buttonPanel, BorderLayout.SOUTH);

            // Populate fields if editing
            if (existing != null) {
                tfName.setText(existing.getName());
                tfNif.setText(existing.getNif());
                taAddress.setText(existing.getAddress());
            }

            pack();
            setMinimumSize(new Dimension(420, 320));
            setLocationRelativeTo(owner);

            // Focus name field
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    tfName.requestFocusInWindow();
                }
            });
        }

        private void saveAndClose() {
            String name = tfName.getText().trim();
            String nif = tfNif.getText().trim();
            String address = taAddress.getText().trim();

            if (name.isEmpty() && nif.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Si us plau, introdueix almenys el nom o el NIF del client.",
                        "Dades incompletes", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (client == null) {
                client = new ClientInfo(name, nif, address);
            } else {
                client.setName(name);
                client.setNif(nif);
                client.setAddress(address);
            }
            saved = true;
            dispose();
        }

        boolean isSaved() {
            return saved;
        }

        ClientInfo getClient() {
            return client;
        }

        private static JLabel createBoldLabel(String text) {
            JLabel label = new JLabel(text);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            return label;
        }
    }
}
