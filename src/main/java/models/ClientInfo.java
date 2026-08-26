package models;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a client that can be loaded into invoices and budgets.
 */
public class ClientInfo {
    private final String id;
    private String name;
    private String nif;
    private String address;

    public ClientInfo(String name, String nif, String address) {
        this.id = UUID.randomUUID().toString();
        this.name = name != null ? name : "";
        this.nif = nif != null ? nif : "";
        this.address = address != null ? address : "";
    }

    public ClientInfo(String id, String name, String nif, String address) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name != null ? name : "";
        this.nif = nif != null ? nif : "";
        this.address = address != null ? address : "";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif != null ? nif : "";
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address != null ? address : "";
    }

    /**
     * Returns a display string for the client (used in search/selection).
     */
    public String getDisplayName() {
        if (name.isEmpty() && nif.isEmpty()) {
            return "(Sense nom)";
        }
        if (nif.isEmpty()) {
            return name;
        }
        return name + " - " + nif;
    }

    /**
     * Checks if this client matches a search query (case-insensitive).
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String lower = query.toLowerCase().trim();
        return name.toLowerCase().contains(lower)
                || nif.toLowerCase().contains(lower)
                || address.toLowerCase().contains(lower);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientInfo that = (ClientInfo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
