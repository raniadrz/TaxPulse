package gr.taxpulse.document.storage;

import gr.taxpulse.config.TaxPulseProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** Stores documents under {@code taxpulse.storage.root-path}, guarding against path traversal. */
@Slf4j
@Component
public class LocalFileSystemStorage implements DocumentStorage {

    private final Path root;

    public LocalFileSystemStorage(TaxPulseProperties properties) {
        this.root = Path.of(properties.storage().rootPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage root " + root, e);
        }
        log.info("Document storage root: {}", root);
    }

    @Override
    public long store(String key, InputStream content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            // Write to a temp file first, then move atomically: no half-written files on failure.
            Path tmp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
            try {
                long bytes = Files.copy(content, tmp, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
                return bytes;
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store " + key, e);
        }
    }

    @Override
    public Resource load(String key) {
        return new PathResource(resolve(key));
    }

    @Override
    public Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete stored file {}: {}", key, e.getMessage());
        }
    }
}
