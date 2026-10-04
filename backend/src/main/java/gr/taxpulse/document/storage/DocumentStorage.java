package gr.taxpulse.document.storage;

import java.io.InputStream;
import java.nio.file.Path;
import org.springframework.core.io.Resource;

/**
 * Binary storage abstraction. The default implementation uses the local file system (on-premise,
 * GDPR friendly); an S3/MinIO adapter can be swapped in without touching the services.
 */
public interface DocumentStorage {

    /** Stores the stream under {@code key} and returns the number of bytes written. */
    long store(String key, InputStream content);

    Resource load(String key);

    /** Local path for libraries that need random file access (e.g. PDFBox). */
    Path resolve(String key);

    void delete(String key);
}
