package storage;

import models.BusinessPartner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class StorageIntegrityTest {
    @TempDir Path tempDir;

    @Test
    void corruptAddressBookIsNeverReplacedByAnUpsert() throws Exception {
        Path file = tempDir.resolve("partners.json");
        byte[] corrupt = "[{ definitely not json".getBytes(StandardCharsets.UTF_8);
        Files.write(file, corrupt);
        BusinessPartnerStore store = new BusinessPartnerStore(file);

        BusinessPartner newPartner = new BusinessPartner(null, "New", "NIF", "Address",
                "Account", "new@example.invalid", "123");
        assertThrows(IllegalStateException.class, () -> store.upsert(newPartner));
        assertArrayEquals(corrupt, Files.readAllBytes(file));
    }

    @Test
    void addressBookRoundTripSortsWithoutMutatingCallerList() throws Exception {
        Path file = tempDir.resolve("partners.json");
        BusinessPartner zed = new BusinessPartner("1", "Zed", "Z", "", "", "", "");
        BusinessPartner ana = new BusinessPartner("2", "Ana", "A", "", "", "", "");
        java.util.List<BusinessPartner> input = new java.util.ArrayList<>(Arrays.asList(zed, ana));
        BusinessPartnerStore store = new BusinessPartnerStore(file);

        store.save(input);
        assertEquals("Zed", input.get(0).getName(), "save must not reorder the caller's live list");
        assertEquals("Ana", store.load().get(0).getName());
        if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
        }
    }

    @Test
    void failedAtomicWriteLeavesExistingFileUntouched() throws Exception {
        Path file = tempDir.resolve("draft.xml");
        Files.writeString(file, "original", StandardCharsets.UTF_8);

        assertThrows(Exception.class, () -> SafeFiles.writeAtomically(file, false, output -> {
            output.write("partial replacement".getBytes(StandardCharsets.UTF_8));
            throw new java.io.IOException("simulated failure");
        }));
        assertEquals("original", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void oversizedAddressBookEntryCannotReplaceExistingData() throws Exception {
        Path file = tempDir.resolve("partners.json");
        Files.writeString(file, "[]", StandardCharsets.UTF_8);
        BusinessPartnerStore store = new BusinessPartnerStore(file);
        BusinessPartner oversized = new BusinessPartner("1", "A".repeat(20_001), "", "", "", "", "");

        assertThrows(IllegalArgumentException.class, () -> store.save(Arrays.asList(oversized)));
        assertEquals("[]", Files.readString(file, StandardCharsets.UTF_8));
    }
}
