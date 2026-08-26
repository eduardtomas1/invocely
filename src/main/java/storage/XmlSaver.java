package storage;

import models.BudgetData;
import models.InvoiceData;
import models.LineCategory;
import models.LineItem;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
    private static final long MAX_XML_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_LINES = 10_000;
    private static final int MAX_TEXT_LENGTH = 20_000;
    private static final BigDecimal MAX_ABSOLUTE_NUMBER = new BigDecimal("1000000000000000");
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
        ensureStorageDirectory(dir);
        String fileName = "factura-" + safeName(data.getInvoiceNumber()) + ".xml";
        Path target = dir.resolve(fileName);
        return saveInvoice(data, target);
    }

    public Path saveInvoice(InvoiceData data, Path target) throws Exception {
        validateInvoice(data);
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
        ensureStorageDirectory(dir);
        String fileName = "pressupost-" + safeName(data.getBudgetNumber()) + ".xml";
        Path target = dir.resolve(fileName);
        return saveBudget(data, target);
    }

    public Path saveBudget(BudgetData data, Path target) throws Exception {
        validateBudget(data);
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
                if (li == null) continue;
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

    private Document newDocument() throws Exception {
        return SecureXml.newDocument();
    }

    public InvoiceData loadInvoice(Path file) throws Exception {
        Document doc = parse(file);
        Element root = doc.getDocumentElement();
        if (!"factura".equalsIgnoreCase(root.getTagName())) {
            throw new IllegalArgumentException("L'arxiu seleccionat no és una factura.");
        }
        String num = text(root, "numero");
        LocalDate data = parseDate(text(root, "data"));
        BigDecimal vatPercent = parsePercent(text(root, "iva_percent"));
        if (vatPercent.compareTo(BigDecimal.ZERO) == 0) {
            vatPercent = readLegacyVatPercent(root);
        }
        boolean split = parseBoolean(root, "linies_desglossades");

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
        validateDateRange(data, valid);
        boolean split = parseBoolean(root, "linies_desglossades");
        boolean includeTotals = parseBoolean(root, "incloure_totals");
        String taxName = text(root, "nom_impost");
        if (taxName != null && taxName.isBlank()) {
            taxName = null;
        }
        BigDecimal taxPercent = parsePercent(text(root, "impost_percent"));

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
        SafeFiles.requireReadableFile(file, MAX_XML_BYTES);
        return SecureXml.newDocumentBuilder().parse(file.toFile());
    }

    private Element child(Element parent, String tag) {
        if (parent == null) return null;
        org.w3c.dom.Node node = parent.getFirstChild();
        while (node != null) {
            if (node instanceof Element && tag.equals(((Element) node).getTagName())) {
                return (Element) node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    private String text(Element parent, String tag) {
        Element element = child(parent, tag);
        if (element != null) {
            org.w3c.dom.Node node = element.getFirstChild();
            while (node != null) {
                if (node instanceof Element) {
                    throw new IllegalArgumentException(
                            "The XML field '" + tag + "' must contain plain text only.");
                }
                node = node.getNextSibling();
            }
        }
        String value = element != null ? element.getTextContent() : "";
        if (value != null && value.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("The XML field '" + tag + "' is too long.");
        }
        return value != null ? value : "";
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("The XML document contains an invalid date.", e);
        }
    }

    private java.util.List<LineItem> readLines(Element root) {
        java.util.List<LineItem> list = new java.util.ArrayList<>();
        for (Element e : lineElements(root)) {
            LineItem li = new LineItem();
            li.setDescription(text(e, "descripcio"));
            li.setQuantity(parseNumber(text(e, "quantitat")));
            li.setUnitPrice(parseNumber(text(e, "preu_unitari")));
            BigDecimal discount = parsePercent(text(e, "descompte_percent"));
            if (discount.compareTo(BigDecimal.ZERO) == 0) {
                discount = parsePercent(text(e, "irpf_percent"));
            }
            li.setDiscountPercent(discount);
            Element category = child(e, "categoria");
            li.setCategory(category == null ? LineCategory.MATERIAL : parseCategory(text(e, "categoria")));
            list.add(li);
        }
        return list;
    }

    private BigDecimal parseNumber(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        String value = s.trim().replace(',', '.');
        if (value.length() > 64) {
            throw new IllegalArgumentException("The XML document contains a number that is too long.");
        }
        final BigDecimal parsed;
        try {
            parsed = new BigDecimal(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("The XML document contains an invalid number.", e);
        }
        if (parsed.precision() > 24 || parsed.scale() < -6 || parsed.scale() > 8
                || parsed.abs().compareTo(MAX_ABSOLUTE_NUMBER) > 0) {
            throw new IllegalArgumentException("The XML document contains a number outside the supported range.");
        }
        return parsed;
    }

    private BigDecimal parsePercent(String s) {
        BigDecimal parsed = parseNumber(s);
        if (parsed.compareTo(BigDecimal.ZERO) < 0 || parsed.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("The XML document contains a percentage outside 0–100.");
        }
        return parsed;
    }

    private boolean parseBoolean(Element parent, String tag) {
        if (child(parent, tag) == null) return false;
        String value = text(parent, tag).trim();
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IllegalArgumentException("The XML field '" + tag + "' must be true or false.");
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
        for (Element e : lineElements(root)) {
            BigDecimal vat = parsePercent(text(e, "iva_percent"));
            if (vat.compareTo(BigDecimal.ZERO) > 0) {
                return vat;
            }
        }
        return BigDecimal.ZERO;
    }

    private java.util.List<Element> lineElements(Element root) {
        java.util.List<Element> elements = new java.util.ArrayList<>();
        Element lines = child(root, "linies");
        if (lines == null) return elements;
        org.w3c.dom.Node node = lines.getFirstChild();
        while (node != null) {
            if (node instanceof Element && "linia".equals(((Element) node).getTagName())) {
                elements.add((Element) node);
                if (elements.size() > MAX_LINES) {
                    throw new IllegalArgumentException("The XML document contains too many line items.");
                }
            }
            node = node.getNextSibling();
        }
        return elements;
    }

    private void validateInvoice(InvoiceData data) {
        if (data == null) throw new IllegalArgumentException("No invoice data was provided.");
        validateText(data.getInvoiceNumber());
        validateText(data.getIssuerName());
        validateText(data.getIssuerNif());
        validateText(data.getIssuerAddress());
        validateText(data.getIssuerAccount());
        validateText(data.getCustomerName());
        validateText(data.getCustomerNif());
        validateText(data.getCustomerAddress());
        validatePercent(data.getVatPercent());
        validateLines(data.getLines());
    }

    private void validateBudget(BudgetData data) {
        if (data == null) throw new IllegalArgumentException("No quote data was provided.");
        validateText(data.getBudgetNumber());
        validateText(data.getSupplierName());
        validateText(data.getSupplierNif());
        validateText(data.getSupplierAddress());
        validateText(data.getClientName());
        validateText(data.getClientNif());
        validateText(data.getClientAddress());
        validateText(data.getPaymentTerms());
        validateText(data.getNotes());
        validateText(data.getTaxName());
        validatePercent(data.getTaxPercent());
        validateDateRange(data.getIssueDate(), data.getValidUntil());
        validateLines(data.getLines());
    }

    private void validateDateRange(LocalDate issueDate, LocalDate validUntil) {
        if (issueDate != null && validUntil != null && validUntil.isBefore(issueDate)) {
            throw new IllegalArgumentException("The quote validity date cannot be before its issue date.");
        }
    }

    private void validateLines(List<LineItem> lines) {
        if (lines == null) return;
        if (lines.size() > MAX_LINES) {
            throw new IllegalArgumentException("The document contains too many line items.");
        }
        for (LineItem line : lines) {
            if (line == null) continue;
            validateText(line.getDescription());
            validateNumber(line.getQuantity());
            validateNumber(line.getUnitPrice());
            validatePercent(line.getDiscountPercent());
        }
    }

    private void validateText(String value) {
        if (value != null && value.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("A document field is too long.");
        }
    }

    private void validateNumber(BigDecimal value) {
        if (value == null) return;
        if (value.precision() > 24 || value.scale() < -6 || value.scale() > 8
                || value.abs().compareTo(MAX_ABSOLUTE_NUMBER) > 0) {
            throw new IllegalArgumentException("A document number is outside the supported range.");
        }
    }

    private void validatePercent(BigDecimal value) {
        if (value == null) return;
        validateNumber(value);
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("A document percentage is outside 0–100.");
        }
    }

    private void ensureStorageDirectory(Path directory) throws Exception {
        Path normalized = directory.toAbsolutePath().normalize();
        Path appBase = AppPaths.baseDir().toAbsolutePath().normalize();
        if (normalized.startsWith(appBase)) {
            AppPaths.ensurePrivateDirectory(normalized);
        } else {
            SafeFiles.createDirectories(normalized, true);
        }
    }

    private String toCategory(LineCategory category) {
        if (category == null) return "";
        return category.name().toLowerCase(Locale.ROOT);
    }

    private LineCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("The XML document contains an empty line category.");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("servei".equals(normalized)) return LineCategory.SERVEI;
        if ("material".equals(normalized)) return LineCategory.MATERIAL;
        try {
            return LineCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("The XML document contains an unknown line category.", e);
        }
    }

    private void write(Document doc, Path target) throws Exception {
        Transformer t = SecureXml.newTransformerFactory().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        SafeFiles.writeAtomically(target, true,
                output -> t.transform(new DOMSource(doc), new StreamResult(output)));
    }

    private String safeName(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "sense-num";
        }
        return s.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
