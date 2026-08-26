package storage;

import models.ClientInfo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Handles persistence of client data to XML file.
 */
public class ClientStorage {
    private static final String CLIENTS_FILE = "clients.xml";
    private final Path storageFile;

    public ClientStorage() {
        this(AppPaths.dataDir().resolve(CLIENTS_FILE));
    }

    public ClientStorage(Path storageFile) {
        this.storageFile = storageFile;
    }

    /**
     * Loads all clients from the storage file.
     */
    public List<ClientInfo> loadClients() {
        List<ClientInfo> clients = new ArrayList<>();
        if (!Files.exists(storageFile)) {
            return clients;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(storageFile.toFile());
            Element root = doc.getDocumentElement();
            NodeList clientNodes = root.getElementsByTagName("client");
            for (int i = 0; i < clientNodes.getLength(); i++) {
                Element clientEl = (Element) clientNodes.item(i);
                String id = text(clientEl, "id");
                String name = text(clientEl, "nom");
                String nif = text(clientEl, "nif");
                String address = text(clientEl, "adreca");
                clients.add(new ClientInfo(id, name, nif, address));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clients;
    }

    /**
     * Saves all clients to the storage file.
     */
    public void saveClients(List<ClientInfo> clients) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElement("clients");
        doc.appendChild(root);

        for (ClientInfo client : clients) {
            Element clientEl = doc.createElement("client");
            addText(doc, clientEl, "id", client.getId());
            addText(doc, clientEl, "nom", client.getName());
            addText(doc, clientEl, "nif", client.getNif());
            addText(doc, clientEl, "adreca", client.getAddress());
            root.appendChild(clientEl);
        }

        write(doc);
    }

    /**
     * Adds a new client to the storage.
     */
    public void addClient(ClientInfo client) throws Exception {
        List<ClientInfo> clients = loadClients();
        clients.add(client);
        saveClients(clients);
    }

    /**
     * Updates an existing client in the storage.
     */
    public void updateClient(ClientInfo client) throws Exception {
        List<ClientInfo> clients = loadClients();
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).getId().equals(client.getId())) {
                clients.set(i, client);
                break;
            }
        }
        saveClients(clients);
    }

    /**
     * Removes a client from the storage by ID.
     */
    public void removeClient(String clientId) throws Exception {
        List<ClientInfo> clients = loadClients();
        clients.removeIf(c -> c.getId().equals(clientId));
        saveClients(clients);
    }

    private String text(Element parent, String tag) {
        if (parent == null) return "";
        Element n = (Element) parent.getElementsByTagName(tag).item(0);
        return n != null && n.getFirstChild() != null ? n.getFirstChild().getNodeValue() : "";
    }

    private void addText(Document doc, Element parent, String tag, String text) {
        Element child = doc.createElement(tag);
        child.appendChild(doc.createTextNode(text != null ? text : ""));
        parent.appendChild(child);
    }

    private void write(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        Path parent = storageFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        t.transform(new DOMSource(doc), new StreamResult(storageFile.toFile()));
    }
}
