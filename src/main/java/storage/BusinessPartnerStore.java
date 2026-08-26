package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import models.BusinessPartner;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BusinessPartnerStore {
    private static final Type LIST_TYPE = new TypeToken<List<BusinessPartner>>() {}.getType();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public List<BusinessPartner> load() {
        Path file = AppPaths.partnersFile();
        if (!Files.exists(file)) return new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<BusinessPartner> partners = gson.fromJson(reader, LIST_TYPE);
            if (partners == null) return new ArrayList<>();
            partners.sort(Comparator.comparing(p -> safe(p.getName())));
            return partners;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void save(List<BusinessPartner> partners) throws Exception {
        if (partners == null) partners = new ArrayList<>();
        partners.sort(Comparator.comparing(p -> safe(p.getName())));
        Path file = AppPaths.partnersFile();
        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(partners, writer);
        }
    }

    public BusinessPartner upsert(BusinessPartner partner) throws Exception {
        if (partner == null) return null;
        List<BusinessPartner> partners = load();
        normalize(partner);
        BusinessPartner existing = findById(partners, partner.getId());
        if (existing == null) {
            existing = findByIdentity(partners, partner.getName(), partner.getNif());
        }
        if (existing == null) {
            if (partner.getId() == null || partner.getId().isBlank()) {
                partner.setId(UUID.randomUUID().toString());
            }
            partners.add(partner);
        } else {
            partner.setId(existing.getId());
            int idx = partners.indexOf(existing);
            partners.set(idx, partner);
        }
        save(partners);
        return partner;
    }

    public void delete(String id) throws Exception {
        if (id == null || id.isBlank()) return;
        List<BusinessPartner> partners = load();
        partners.removeIf(p -> id.equals(p.getId()));
        save(partners);
    }

    private BusinessPartner findById(List<BusinessPartner> partners, String id) {
        if (id == null) return null;
        for (BusinessPartner partner : partners) {
            if (id.equals(partner.getId())) return partner;
        }
        return null;
    }

    private BusinessPartner findByIdentity(List<BusinessPartner> partners, String name, String nif) {
        String keyName = normalizeKey(name);
        String keyNif = normalizeKey(nif);
        if (keyName.isBlank() && keyNif.isBlank()) return null;
        for (BusinessPartner partner : partners) {
            if (normalizeKey(partner.getName()).equals(keyName)
                    && normalizeKey(partner.getNif()).equals(keyNif)) {
                return partner;
            }
        }
        return null;
    }

    private void normalize(BusinessPartner partner) {
        partner.setName(trim(partner.getName()));
        partner.setNif(trim(partner.getNif()));
        partner.setAddress(trim(partner.getAddress()));
        partner.setAccount(trim(partner.getAccount()));
        partner.setEmail(trim(partner.getEmail()));
        partner.setPhone(trim(partner.getPhone()));
    }

    private String normalizeKey(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
