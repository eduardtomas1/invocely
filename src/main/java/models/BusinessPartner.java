package models;

public class BusinessPartner {
    private String id;
    private String name;
    private String nif;
    private String address;
    private String account;
    private String email;
    private String phone;

    public BusinessPartner() { }

    public BusinessPartner(String id, String name, String nif, String address,
                           String account, String email, String phone) {
        this.id = id;
        this.name = name;
        this.nif = nif;
        this.address = address;
        this.account = account;
        this.email = email;
        this.phone = phone;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String displayName() {
        String base = name != null ? name.trim() : "";
        String idPart = nif != null && !nif.trim().isEmpty() ? " (" + nif.trim() + ")" : "";
        return base + idPart;
    }

    @Override
    public String toString() {
        return displayName();
    }
}
