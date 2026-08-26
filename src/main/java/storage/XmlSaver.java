package storage;

import models.BudgetData;
import models.InvoiceData;
import models.LineCategory;
import models.LineItem;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Simple XML persistence for invoices and budgets.
 */
public class XmlSaver {
    private final Path baseDir;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public XmlSaver() {
        this(AppPaths.dataDir());
    }

    public XmlSaver(Path baseDir) {
        this.baseDir = baseDir;
    }

    public Path saveInvoice(InvoiceData data) throws Exception {
        Path dir = baseDir.resolve("factures");
        Files.createDirectories(dir);
        String fileName = "factura-" + safeName(data.getInvoiceNumber()) + ".xml";
        Path target = dir.resolve(fileName);
        return saveInvoice(data, target);
    }

    public Path saveInvoice(InvoiceData data, Path target) throws Exception {
        Document doc = newDocument();
        Element root = doc.createElement("factura");
        doc.appendChild(root);

        addText(doc, root, "tipus", "factura");
        addText(doc, root, "numero", data.getInvoiceNumber());
        addText(doc, root, "data", data.getIssueDate() != null ? DATE_FMT.format(data.getIssueDate()) : "");
        addText(doc, root, "iva_percent", toStr(data.getVatPercent()));
        addText(doc, root, "linies_desglossades", String.valueOf(data.isSplitLines()));

        Element emissor = doc.createElement("emissor");
        addText(doc, emissor, "nom", data.getIssuerName());
        addText(doc, emissor, "nif", data.getIssuerNif());
        addText(doc, emissor, "adreca", data.getIssuerAddress());
        addText(doc, emissor, "compte_emissor", data.getIssuerAccount());
        root.appendChild(emissor);

        Element client = doc.createElement("client");
        addText(doc, client, "nom", data.getCustomerName());
        addText(doc, client, "nif", data.getCustomerNif());
        addText(doc, client, "adreca", data.getCustomerAddress());
        root.appendChild(client);

        appendLines(doc, root, data.getLines());
        write(doc, target);
        return target;
    }

    public Path saveBudget(BudgetData data) throws Exception {
        Path dir = baseDir.resolve("pressupostos");
        Files.createDirectories(dir);
        String fileName = "pressupost-" + safeName(data.getBudgetNumber()) + ".xml";
        Path target = dir.resolve(fileName);
        return saveBudget(data, target);
    }

    public Path saveBudget(BudgetData data, Path target) throws Exception {
        Document doc = newDocument();
        Element root = doc.createElement("pressupost");
        doc.appendChild(root);

        addText(doc, root, "tipus", "pressupost");
        addText(doc, root, "numero", data.getBudgetNumber());
        addText(doc, root, "data", data.getIssueDate() != null ? DATE_FMT.format(data.getIssueDate()) : "");
        addText(doc, root, "valid_fins", data.getValidUntil() != null ? DATE_FMT.format(data.getValidUntil()) : "");
        addText(doc, root, "linies_desglossades", String.valueOf(data.isSplitLines()));
        addText(doc, root, "incloure_totals", String.valueOf(data.isIncludeTotals()));
        addText(doc, root, "nom_impost", data.getTaxName());
        addText(doc, root, "impost_percent", toStr(data.getTaxPercent()));

        Element proveidor = doc.createElement("proveidor");
        addText(doc, proveidor, "nom", data.getSupplierName());
        addText(doc, proveidor, "nif", data.getSupplierNif());
        addText(doc, proveidor, "adreca", data.getSupplierAddress());
        root.appendChild(proveidor);

        Element client = doc.createElement("client");
        addText(doc, client, "nom", data.getClientName());
        addText(doc, client, "nif", data.getClientNif());
        addText(doc, client, "adreca", data.getClientAddress());
        root.appendChild(client);

        addText(doc, root, "pagament", data.getPaymentTerms());
        addText(doc, root, "notes", data.getNotes());

        appendLines(doc, root, data.getLines());
        write(doc, target);
        return target;
    }

    private void appendLines(Document doc, Element root, List<LineItem> lines) {
        Element llistat = doc.createElement("linies");
        if (lines != null) {
            for (LineItem li : lines) {
                Element e = doc.createElement("linia");
                addText(doc, e, "descripcio", li.getDescription());
                addText(doc, e, "quantitat", toStr(li.getQuantity()));
                addText(doc, e, "preu_unitari", toStr(li.getUnitPrice()));
                addText(doc, e, "descompte_percent", toStr(li.getDiscountPercent()));
                addText(doc, e, "categoria", toCategory(li.getCategory()));
                addText(doc, e, "total", toStr(li.getTotal()));
                llistat.appendChild(e);
            }
        }
        root.appendChild(llistat);
    }

    private Document newDocument() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.newDocument();
    }

    public InvoiceData loadInvoice(Path file) throws Exception {
        Document doc = parse(file);
        Element root = doc.getDocumentElement();
        if (!"factura".equalsIgnoreCase(root.getTagName())) {
            throw new IllegalArgumentException("L'arxiu seleccionat no és una factura.");
        }
        String num = text(root, "numero");
        LocalDate data = parseDate(text(root, "data"));
        BigDecimal vatPercent = parseBD(text(root, "iva_percent"));
        if (vatPercent.compareTo(BigDecimal.ZERO) == 0) {
            vatPercent = readLegacyVatPercent(root);
        }
        boolean split = Boolean.parseBoolean(text(root, "linies_desglossades"));

        Element em = child(root, "emissor");
        Element cl = child(root, "client");

        String emNom = text(em, "nom");
        String emNif = text(em, "nif");
        String emAdr = text(em, "adreca");
        String emAccount = text(em, "compte_emissor");

        String clNom = text(cl, "nom");
        String clNif = text(cl, "nif");
        String clAdr = text(cl, "adreca");

        java.util.List<LineItem> lines = readLines(root);
        return new InvoiceData(num, data, emNom, emNif, emAdr, emAccount,
            clNom, clNif, clAdr, vatPercent, split, lines);
    }

    public InvoiceData loadInvoiceByNumber(String invoiceNumber) throws Exception {
        Path dir = baseDir.resolve("factures");
        Path file = dir.resolve("factura-" + safeName(invoiceNumber) + ".xml");
        return loadInvoice(file);
    }

    public BudgetData loadBudget(Path file) throws Exception {
        Document doc = parse(file);
        Element root = doc.getDocumentElement();
        if (!"pressupost".equalsIgnoreCase(root.getTagName())) {
            throw new IllegalArgumentException("L'arxiu seleccionat no és un pressupost.");
        }
        String num = text(root, "numero");
        LocalDate data = parseDate(text(root, "data"));
        LocalDate valid = parseDate(text(root, "valid_fins"));
        boolean split = Boolean.parseBoolean(text(root, "linies_desglossades"));
        boolean includeTotals = Boolean.parseBoolean(text(root, "incloure_totals"));
        String taxName = text(root, "nom_impost");
        if (taxName != null && taxName.isBlank()) {
            taxName = null;
        }
        BigDecimal taxPercent = parseBD(text(root, "impost_percent"));

        Element prov = child(root, "proveidor");
        Element cli = child(root, "client");

        String pvNom = text(prov, "nom");
        String pvNif = text(prov, "nif");
        String pvAdr = text(prov, "adreca");

        String clNom = text(cli, "nom");
        String clNif = text(cli, "nif");
        String clAdr = text(cli, "adreca");

        String pag = text(root, "pagament");
        String notes = text(root, "notes");

        java.util.List<LineItem> lines = readLines(root);
        return new BudgetData(num, data, valid, pvNom, pvNif, pvAdr, clNom, clNif, clAdr,
            pag, notes, includeTotals, taxName, taxPercent, split, lines);
    }

    public BudgetData loadBudgetByNumber(String budgetNumber) throws Exception {
        Path dir = baseDir.resolve("pressupostos");
        Path file = dir.resolve("pressupost-" + safeName(budgetNumber) + ".xml");
        return loadBudget(file);
    }

    private Document parse(Path file) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(file.toFile());
    }

    private Element child(Element parent, String tag) {
        return (Element) parent.getElementsByTagName(tag).item(0);
    }

    private String text(Element parent, String tag) {
        if (parent == null) return "";
        Element n = (Element) parent.getElementsByTagName(tag).item(0);
        return n != null && n.getFirstChild()!=null ? n.getFirstChild().getNodeValue() : "";
    }

    private LocalDate parseDate(String s) {
        try {
            if (s == null || s.isBlank()) return null;
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private java.util.List<LineItem> readLines(Element root) {
        java.util.List<LineItem> list = new java.util.ArrayList<>();
        org.w3c.dom.NodeList nl = root.getElementsByTagName("linia");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            LineItem li = new LineItem();
            li.setDescription(text(e, "descripcio"));
            li.setQuantity(parseBD(text(e, "quantitat")));
            li.setUnitPrice(parseBD(text(e, "preu_unitari")));
            BigDecimal discount = parseBD(text(e, "descompte_percent"));
            if (discount.compareTo(BigDecimal.ZERO) == 0) {
                discount = parseBD(text(e, "irpf_percent"));
            }
            li.setDiscountPercent(discount);
            li.setCategory(parseCategory(text(e, "categoria")));
            list.add(li);
        }
        return list;
    }

    private BigDecimal parseBD(String s) {
        try {
            if (s == null || s.isBlank()) return BigDecimal.ZERO;
            return new BigDecimal(s.trim().replace(',', '.'));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void addText(Document doc, Element parent, String tag, String text) {
        Element child = doc.createElement(tag);
        child.appendChild(doc.createTextNode(text != null ? text : ""));
        parent.appendChild(child);
    }

    private String toStr(BigDecimal bd) {
        return bd != null ? bd.toPlainString() : "";
    }

    private BigDecimal readLegacyVatPercent(Element root) {
        org.w3c.dom.NodeList nl = root.getElementsByTagName("linia");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            BigDecimal vat = parseBD(text(e, "iva_percent"));
            if (vat.compareTo(BigDecimal.ZERO) > 0) {
                return vat;
            }
        }
        return BigDecimal.ZERO;
    }

    private String toCategory(LineCategory category) {
        if (category == null) return "";
        return category.name().toLowerCase(Locale.ROOT);
    }

    private LineCategory parseCategory(String value) {
        if (value == null || value.isBlank()) return LineCategory.MATERIAL;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("servei".equals(normalized)) return LineCategory.SERVEI;
        if ("material".equals(normalized)) return LineCategory.MATERIAL;
        try {
            return LineCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return LineCategory.MATERIAL;
        }
    }

    private void write(Document doc, Path target) throws TransformerException, IOException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        t.transform(new DOMSource(doc), new StreamResult(target.toFile()));
    }

    private String safeName(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "sense-num";
        }
        return s.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
