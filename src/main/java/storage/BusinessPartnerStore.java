package storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import i18n.I18n;
import models.BusinessPartner;

import java.io.Reader;
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
    private static final long MAX_JSON_BYTES = 2L * 1024L * 1024L;
    private static final int MAX_PARTNERS = 10_000;
    private static final int MAX_FIELD_LENGTH = 20_000;
    private static final Type LIST_TYPE = new TypeToken<List<BusinessPartner>>() {}.getType();
    private final Gson gson = new GsonBuilder().disableJdkUnsafe().setPrettyPrinting().create();
    private final Path storageFile;

    public BusinessPartnerStore() {
        this(AppPaths.partnersFile());
    }

    public BusinessPartnerStore(Path storageFile) {
        this.storageFile = storageFile;
    }

    public List<BusinessPartner> load() {
        Path file = storageFile;
        if (!Files.exists(file)) return new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            SafeFiles.requireReadableFile(file, MAX_JSON_BYTES);
            List<BusinessPartner> partners = gson.fromJson(reader, LIST_TYPE);
            if (partners == null) return new ArrayList<>();
            validatePartners(partners);
            partners.sort(Comparator.comparing(p -> safe(p.getName())));
            return partners;
        } catch (Exception e) {
            throw new IllegalStateException(I18n.t("storage.partners_read_error"), e);
        }
    }

    public void save(List<BusinessPartner> partners) throws Exception {
        List<BusinessPartner> sorted = partners == null
                ? new ArrayList<>() : new ArrayList<>(partners);
        validatePartners(sorted);
        sorted.sort(Comparator.comparing(p -> safe(p.getName())));
        Path file = storageFile;
        Path appBase = AppPaths.baseDir().toAbsolutePath().normalize();
        Path parent = file.toAbsolutePath().normalize().getParent();
        if (parent != null && parent.startsWith(appBase)) {
            AppPaths.ensurePrivateDirectory(parent);
        } else if (parent != null) {
            SafeFiles.createDirectories(parent, false);
        }
        byte[] json = gson.toJson(sorted).getBytes(StandardCharsets.UTF_8);
        if (json.length > MAX_JSON_BYTES) {
            throw new IllegalArgumentException(I18n.t("storage.partners_too_large"));
        }
        SafeFiles.writeAtomically(file, true, output -> output.write(json));
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

    private void validatePartners(List<BusinessPartner> partners) {
        if (partners.size() > MAX_PARTNERS) {
            throw new IllegalArgumentException(I18n.t("storage.partners_too_large"));
        }
        long characters = 0;
        for (BusinessPartner partner : partners) {
            if (partner == null) continue;
            String[] fields = {partner.getId(), partner.getName(), partner.getNif(), partner.getAddress(),
                    partner.getAccount(), partner.getEmail(), partner.getPhone()};
            for (String field : fields) {
                int length = field == null ? 0 : field.length();
                if (length > MAX_FIELD_LENGTH) {
                    throw new IllegalArgumentException(I18n.t("storage.partner_field_too_long"));
                }
                characters += length;
                if (characters > MAX_JSON_BYTES) {
                    throw new IllegalArgumentException(I18n.t("storage.partners_too_large"));
                }
            }
        }
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
