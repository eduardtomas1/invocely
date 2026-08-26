package app.invocely;

import i18n.I18n;
import models.BudgetData;
import models.DocumentType;
import models.InvoiceData;
import report.ReportGenerator;
import storage.AppPreferences;
import storage.XmlSaver;
import storage.DefaultsManager;
import ui.AppIcons;
import ui.BudgetPanel;
import ui.DefaultsDialog;
import ui.DocTypeDialog;
import ui.InvoicePanel;
import ui.PartnerManagerDialog;
import ui.ThemeManager;
import ui.ThemePalette;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 *
 * @author Eduard Tomas
 */

public class InvoicelyApp extends JFrame {

    private final CardLayout card = new CardLayout();
    private final JPanel cardPanel = new JPanel(card);
    private InvoicePanel invoicePanel;
    private BudgetPanel  budgetPanel;
    private DocumentType activeType;
    private final DefaultsManager defaults = new DefaultsManager();
    private JToolBar toolbar;
    private JButton btnSaveXml;

    public InvoicelyApp() {
        super(I18n.t("app.title"));
        ThemePalette palette = ThemeManager.palette();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1024, 720));
        setLocationRelativeTo(null);
        applyAppIcon();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(palette.background());

        createMenuBar();
        toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
        chooseDocType();
    }

    private void importInvoice() {
        Path selected = pickOpenPath(I18n.t("menu.file.import_invoice"), "xml", AppPreferences.KEY_IMPORT_DIR);
        if (selected == null) return;
        try {
            XmlSaver saver = new XmlSaver();
            InvoiceData data = saver.loadInvoice(selected);
            switchDoc(DocumentType.INVOICE);
            invoicePanel.fillFromData(data);
            JOptionPane.showMessageDialog(this, I18n.t("msg.invoice_loaded"));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importBudget() {
        Path selected = pickOpenPath(I18n.t("menu.file.import_budget"), "xml", AppPreferences.KEY_IMPORT_DIR);
        if (selected == null) return;
        try {
            XmlSaver saver = new XmlSaver();
            BudgetData data = saver.loadBudget(selected);
            switchDoc(DocumentType.BUDGET);
            budgetPanel.fillFromData(data);
            JOptionPane.showMessageDialog(this, I18n.t("msg.budget_loaded"));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu file = new JMenu(I18n.t("menu.file"));
        JMenuItem exportPdf  = new JMenuItem(I18n.t("menu.file.export_pdf"));
        JMenuItem exportXlsx = new JMenuItem(I18n.t("menu.file.export_xlsx"));
        JMenuItem exportCsv  = new JMenuItem(I18n.t("menu.file.export_csv"));
        JMenuItem saveXml    = new JMenuItem(I18n.t("menu.file.save_xml"));
        JMenuItem importInvXml = new JMenuItem(I18n.t("menu.file.import_invoice"));
        JMenuItem importBudXml = new JMenuItem(I18n.t("menu.file.import_budget"));
        file.add(exportPdf);
        file.add(exportXlsx);
        file.add(exportCsv);
        file.add(saveXml);
        file.addSeparator();
        file.add(importInvXml);
        file.add(importBudXml);
        file.addSeparator();
        mb.add(file);

        JMenu partners = new JMenu(I18n.t("menu.partners"));
        JMenuItem managePartners = new JMenuItem(I18n.t("menu.partners.manage"));
        partners.add(managePartners);
        mb.add(partners);

        JMenu defaults = new JMenu(I18n.t("menu.defaults"));
        JMenuItem saveDefaults = new JMenuItem(I18n.t("menu.file.save_defaults"));
        JMenuItem loadDefaults = new JMenuItem(I18n.t("menu.file.load_defaults"));
        JMenuItem editDefaults = new JMenuItem(I18n.t("menu.file.edit_defaults"));
        JMenuItem setReportLogo = new JMenuItem(I18n.t("menu.defaults.set_logo"));
        JMenuItem clearReportLogo = new JMenuItem(I18n.t("menu.defaults.clear_logo"));
        defaults.add(saveDefaults);
        defaults.add(loadDefaults);
        defaults.add(editDefaults);
        defaults.addSeparator();
        defaults.add(setReportLogo);
        defaults.add(clearReportLogo);
        mb.add(defaults);

        JMenu view = new JMenu(I18n.t("menu.view"));
        JMenuItem switchToInv = new JMenuItem(I18n.t("menu.view.invoice"));
        JMenuItem switchToBud = new JMenuItem(I18n.t("menu.view.budget"));
        view.add(switchToInv);
        view.add(switchToBud);
        view.addSeparator();
        JMenu langMenu = new JMenu(I18n.t("menu.view.language"));
        ButtonGroup langGroup = new ButtonGroup();
        JRadioButtonMenuItem langEn = new JRadioButtonMenuItem(I18n.t("menu.view.language.en"));
        JRadioButtonMenuItem langCa = new JRadioButtonMenuItem(I18n.t("menu.view.language.ca"));
        JRadioButtonMenuItem langEs = new JRadioButtonMenuItem(I18n.t("menu.view.language.es"));
        langGroup.add(langEn);
        langGroup.add(langCa);
        langGroup.add(langEs);
        String currentLanguage = I18n.getLocale().getLanguage();
        if ("es".equalsIgnoreCase(currentLanguage)) {
            langEs.setSelected(true);
        } else if ("ca".equalsIgnoreCase(currentLanguage)) {
            langCa.setSelected(true);
        } else {
            langEn.setSelected(true);
        }
        langMenu.add(langEn);
        langMenu.add(langCa);
        langMenu.add(langEs);
        view.add(langMenu);
        mb.add(view);

        setJMenuBar(mb);

        exportPdf.addActionListener(e -> doExport("pdf"));
        exportXlsx.addActionListener(e -> doExport("xlsx"));
        exportCsv.addActionListener(e -> doExport("csv"));
        saveXml.addActionListener(e -> doSaveXml());
        saveDefaults.addActionListener(e -> saveDefaults());
        loadDefaults.addActionListener(e -> loadDefaults());
        editDefaults.addActionListener(e -> editDefaults());
        setReportLogo.addActionListener(e -> configureReportLogo());
        clearReportLogo.addActionListener(e -> clearReportLogo());
        importInvXml.addActionListener(e -> importInvoice());
        importBudXml.addActionListener(e -> importBudget());
        managePartners.addActionListener(e -> openPartnerManager());

        switchToInv.addActionListener(e -> switchDoc(DocumentType.INVOICE));
        switchToBud.addActionListener(e -> switchDoc(DocumentType.BUDGET));
        langEn.addActionListener(e -> applyLanguage("en-US"));
        langCa.addActionListener(e -> applyLanguage("ca-ES"));
        langEs.addActionListener(e -> applyLanguage("es-ES"));
    }

    private void chooseDocType() {
        DocTypeDialog dlg = new DocTypeDialog(this);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);

        DocumentType dt = dlg.getChosen();
        if (dt == null) {
            System.exit(0);
        }
        initPanels(dt);
    }

    private void initPanels(DocumentType dt) {
        if (dt == DocumentType.INVOICE) {
            invoicePanel = new InvoicePanel();
            cardPanel.add(wrapWithMargin(invoicePanel), "INV");
            tryApplyDefaults(DocumentType.INVOICE);
        } else {
            budgetPanel = new BudgetPanel();
            cardPanel.add(wrapWithMargin(budgetPanel), "BUD");
            tryApplyDefaults(DocumentType.BUDGET);
        }
        cardPanel.setOpaque(true);
        cardPanel.setBackground(ThemeManager.palette().background());
        switchDoc(dt);
    }

    private void switchDoc(DocumentType dt) {
        if (dt == DocumentType.INVOICE) {
            if (invoicePanel == null) {
                invoicePanel = new InvoicePanel();
                cardPanel.add(wrapWithMargin(invoicePanel), "INV");
                tryApplyDefaults(DocumentType.INVOICE);
            }
            card.show(cardPanel, "INV");
        } else {
            if (budgetPanel == null) {
                budgetPanel = new BudgetPanel();
                cardPanel.add(wrapWithMargin(budgetPanel), "BUD");
                tryApplyDefaults(DocumentType.BUDGET);
            }
            card.show(cardPanel, "BUD");
        }
        activeType = dt;
    }

    private DocumentType currentType() {
        return (invoicePanel != null && invoicePanel.isShowing())
                ? DocumentType.INVOICE
                : DocumentType.BUDGET;
    }

    private void doExport(String type) {
        try {
            ReportGenerator gen = new ReportGenerator();
            if (currentType() == DocumentType.INVOICE) {
                InvoiceData inv = invoicePanel.collect();
                String baseName = buildBaseName("factura", inv.getInvoiceNumber());
                Path target = pickSavePath(I18n.t("dialog.export_title", type.toUpperCase()), baseName, type, AppPreferences.KEY_EXPORT_DIR);
                if (target == null) return;
                gen.exportInvoice(inv, type, target);
                JOptionPane.showMessageDialog(this, I18n.t("msg.export_ok", target.toString()));
            } else {
                BudgetData bud = budgetPanel.collect();
                String baseName = buildBaseName("pressupost", bud.getBudgetNumber());
                Path target = pickSavePath(I18n.t("dialog.export_title", type.toUpperCase()), baseName, type, AppPreferences.KEY_EXPORT_DIR);
                if (target == null) return;
                gen.exportBudget(bud, type, target);
                JOptionPane.showMessageDialog(this, I18n.t("msg.export_ok", target.toString()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doSaveXml() {
        try {
            XmlSaver saver = new XmlSaver();
            if (currentType() == DocumentType.INVOICE) {
                InvoiceData inv = invoicePanel.collect();
                String baseName = buildBaseName("factura", inv.getInvoiceNumber());
                Path target = pickSavePath(I18n.t("dialog.save_invoice_xml"), baseName, "xml", AppPreferences.KEY_XML_DIR);
                if (target == null) return;
                saver.saveInvoice(inv, target);
                JOptionPane.showMessageDialog(this, I18n.t("msg.invoice_saved_xml", target.toString()));
            } else {
                BudgetData bud = budgetPanel.collect();
                String baseName = buildBaseName("pressupost", bud.getBudgetNumber());
                Path target = pickSavePath(I18n.t("dialog.save_budget_xml"), baseName, "xml", AppPreferences.KEY_XML_DIR);
                if (target == null) return;
                saver.saveBudget(bud, target);
                JOptionPane.showMessageDialog(this, I18n.t("msg.budget_saved_xml", target.toString()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveDefaults() {
        try {
            if (currentType() == DocumentType.INVOICE) {
                defaults.saveInvoiceDefaults(invoicePanel.collect());
                JOptionPane.showMessageDialog(this, I18n.t("msg.invoice_defaults_saved"));
            } else {
                defaults.saveBudgetDefaults(budgetPanel.collect());
                JOptionPane.showMessageDialog(this, I18n.t("msg.budget_defaults_saved"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDefaults() {
        try {
            if (currentType() == DocumentType.INVOICE) {
                InvoiceData data = defaults.loadInvoiceDefaults();
                if (data != null) {
                    invoicePanel.fillFromData(data);
                } else {
                    JOptionPane.showMessageDialog(this, I18n.t("msg.no_invoice_defaults"));
                }
            } else {
                BudgetData data = defaults.loadBudgetDefaults();
                if (data != null) {
                    budgetPanel.fillFromData(data);
                } else {
                    JOptionPane.showMessageDialog(this, I18n.t("msg.no_budget_defaults"));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editDefaults() {
        DefaultsDialog dialog = new DefaultsDialog(this, currentType(), defaults);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            JOptionPane.showMessageDialog(this, I18n.t("msg.defaults_updated"));
        }
    }

    private void openPartnerManager() {
        PartnerManagerDialog dialog = new PartnerManagerDialog(this);
        dialog.setVisible(true);
    }

    private void configureReportLogo() {
        JFileChooser fc = new JFileChooser(AppPreferences.getLastDirectory(AppPreferences.KEY_REPORT_LOGO_DIR).toFile());
        fc.setDialogTitle(I18n.t("dialog.select_report_logo"));
        fc.setFileFilter(new FileNameExtensionFilter("PNG/JPG", "png", "jpg", "jpeg"));
        int res = fc.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        if (file == null) return;
        Path path = file.toPath().toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            JOptionPane.showMessageDialog(this, I18n.t("msg.report_logo_missing"), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        try (InputStream in = Files.newInputStream(path)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                JOptionPane.showMessageDialog(this, I18n.t("msg.report_logo_invalid"), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            AppPreferences.setReportLogoPath(path);
            AppPreferences.setLastDirectory(AppPreferences.KEY_REPORT_LOGO_DIR, path.getParent());
            JOptionPane.showMessageDialog(this, I18n.t("msg.report_logo_saved"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), I18n.t("dialog.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearReportLogo() {
        AppPreferences.clearReportLogoPath();
        JOptionPane.showMessageDialog(this, I18n.t("msg.report_logo_cleared"));
    }


    private void tryApplyDefaults(DocumentType type) {
        try {
            if (type == DocumentType.INVOICE) {
                InvoiceData data = defaults.loadInvoiceDefaults();
                if (data != null && invoicePanel != null) {
                    invoicePanel.fillFromData(data);
                }
            } else {
                BudgetData data = defaults.loadBudgetDefaults();
                if (data != null && budgetPanel != null) {
                    budgetPanel.fillFromData(data);
                }
            }
        } catch (Exception ignored) { }
    }

    private void applyAppIcon() {
        try (InputStream in = getClass().getResourceAsStream("/icon/logo.png")) {
            if (in == null) return;
            BufferedImage raw = ImageIO.read(in);
            if (raw == null) return;
            Image windowIcon = scaleIcon(cropTransparent(raw), 128);
            setIconImage(windowIcon);
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(raw);
                }
            }
        } catch (IOException ignored) { }
    }

    private BufferedImage cropTransparent(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (source.getRGB(x, y) >> 24) & 0xff;
                if (alpha == 0) continue;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }

        if (maxX < minX || maxY < minY) {
            return source;
        }

        BufferedImage cropped = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = cropped.createGraphics();
        g2.drawImage(source, 0, 0, cropped.getWidth(), cropped.getHeight(), minX, minY, maxX + 1, maxY + 1, null);
        g2.dispose();
        return cropped;
    }

    private Image scaleIcon(BufferedImage source, int size) {
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(source, 0, 0, size, size, null);
        g2.dispose();
        return scaled;
    }

    public static void main(String[] args) {
        // Must be set before AWT initializes so macOS uses the product name in its menu bar.
        System.setProperty("apple.awt.application.name", "Invoicely");
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("flatlaf.useWindowDecorations", "true");
        SwingUtilities.invokeLater(() -> {
            I18n.init();
            ThemeManager.bootstrap();
            new InvoicelyApp().setVisible(true);
        });
    }

    private JPanel wrapWithMargin(JComponent content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        wrapper.setOpaque(false);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private void applyLanguage(String tag) {
        if (tag == null || tag.isBlank()) return;
        if (tag.equalsIgnoreCase(I18n.getLocale().toLanguageTag())) return;
        AppPreferences.setLanguageTag(tag);
        I18n.setLocale(Locale.forLanguageTag(tag));
        setTitle(I18n.t("app.title"));
        createMenuBar();
        rebuildToolbar();
        rebuildPanelsForLanguage();
        refreshTheme();
    }

    private void rebuildToolbar() {
        if (toolbar != null) {
            remove(toolbar);
        }
        toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);
    }

    private void rebuildPanelsForLanguage() {
        InvoiceData invoiceData = invoicePanel != null ? invoicePanel.collect() : null;
        BudgetData budgetData = budgetPanel != null ? budgetPanel.collect() : null;
        cardPanel.removeAll();
        invoicePanel = null;
        budgetPanel = null;
        if (invoiceData != null) {
            invoicePanel = new InvoicePanel();
            cardPanel.add(wrapWithMargin(invoicePanel), "INV");
            invoicePanel.fillFromData(invoiceData);
        }
        if (budgetData != null) {
            budgetPanel = new BudgetPanel();
            cardPanel.add(wrapWithMargin(budgetPanel), "BUD");
            budgetPanel.fillFromData(budgetData);
        }
        if (activeType != null) {
            card.show(cardPanel, activeType == DocumentType.INVOICE ? "INV" : "BUD");
        }
    }

    private void refreshTheme() {
        ThemeManager.apply(this);
        ThemePalette palette = ThemeManager.palette();
        if (invoicePanel != null) invoicePanel.refreshTheme();
        if (budgetPanel != null) budgetPanel.refreshTheme();
        cardPanel.setBackground(palette.background());
        getContentPane().setBackground(palette.background());
        if (toolbar != null) {
            toolbar.setBackground(palette.background());
        }
        revalidate();
        repaint();
    }

    private JToolBar createToolbar() {
        ThemePalette palette = ThemeManager.palette();
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        tb.setOpaque(true);
        tb.setBackground(palette.background());
        int iconSize = 32;
        Insets buttonInsets = new Insets(5, 12, 5, 12);
        int iconTextGap = 8;

        JButton btnExportPdf = new JButton(I18n.t("toolbar.export_pdf"), AppIcons.pdfIcon(iconSize));
        btnExportPdf.setMargin(buttonInsets);
        btnExportPdf.setIconTextGap(iconTextGap);
        btnExportPdf.addActionListener(e -> doExport("pdf"));
        tb.add(btnExportPdf);

        tb.addSeparator(new Dimension(12, 0));

        JButton btnExportExcel = new JButton(I18n.t("toolbar.export_excel"), AppIcons.excelIcon(iconSize));
        btnExportExcel.setMargin(buttonInsets);
        btnExportExcel.setIconTextGap(iconTextGap);
        btnExportExcel.addActionListener(e -> doExport("xlsx"));
        tb.add(btnExportExcel);

        tb.addSeparator(new Dimension(12, 0));

        btnSaveXml = new JButton(I18n.t("toolbar.save_draft"), AppIcons.draftIcon(iconSize));
        btnSaveXml.setMargin(buttonInsets);
        btnSaveXml.setIconTextGap(iconTextGap);
        btnSaveXml.addActionListener(e -> doSaveXml());
        tb.add(btnSaveXml);

        return tb;
    }

    private Path pickOpenPath(String title, String extension, String prefsKey) {
        JFileChooser fc = new JFileChooser(AppPreferences.getLastDirectory(prefsKey).toFile());
        fc.setDialogTitle(title);
        if (extension != null && !extension.isBlank()) {
            fc.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase(), extension));
        }
        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f != null) {
                Path path = f.toPath();
                AppPreferences.setLastDirectory(prefsKey, path.getParent());
                return path;
            }
        }
        return null;
    }

    private Path pickSavePath(String title, String baseName, String extension, String prefsKey) {
        JFileChooser fc = new JFileChooser(AppPreferences.getLastDirectory(prefsKey).toFile());
        fc.setDialogTitle(title);
        if (extension != null && !extension.isBlank()) {
            fc.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase(), extension));
        }
        if (baseName != null && !baseName.isBlank()) {
            fc.setSelectedFile(new File(baseName + "." + extension));
        }
        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f != null) {
                Path path = ensureExtension(f.toPath(), extension);
                if (Files.exists(path)) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        I18n.t("dialog.overwrite_message"),
                        I18n.t("dialog.overwrite_title"),
                        JOptionPane.YES_NO_OPTION);
                    if (choice != JOptionPane.YES_OPTION) {
                        return null;
                    }
                }
                AppPreferences.setLastDirectory(prefsKey, path.getParent());
                return path;
            }
        }
        return null;
    }

    private Path ensureExtension(Path path, String extension) {
        if (path == null) return null;
        if (extension == null || extension.isBlank()) return path;
        String fileName = path.getFileName().toString();
        String ext = "." + extension.toLowerCase();
        if (fileName.toLowerCase().endsWith(ext)) {
            return path;
        }
        return path.resolveSibling(fileName + ext);
    }

    private String buildBaseName(String prefix, String number) {
        String safe = safeName(number);
        if ("sense-num".equals(safe)) return prefix;
        return prefix + "-" + safe;
    }

    private String safeName(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "sense-num";
        }
        return s.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
