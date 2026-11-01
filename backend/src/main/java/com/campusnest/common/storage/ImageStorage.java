package com.campusnest.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Saves listing photos on the local disk (folder set by campusnest.storage.dir, default "uploads").
 * Files get random names (UUIDs), never the name the user uploaded, so a filename like
 * "../../config.properties" can never be used to write or read outside the folder.
 *
 * Kept behind this small class so it could later be swapped for cloud storage without touching services.
 */
@Component
public class ImageStorage {

    private static final Pattern KEY = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png)$");

    private final Path root;

    public ImageStorage(@Value("${campusnest.storage.dir:uploads}") String directory) throws IOException {
        this.root = Path.of(directory, "listing-images").toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /** Stores the bytes and returns the generated storage key, e.g. "3f2a...c1.jpg". */
    public String save(byte[] data, String extension) {
        String key = UUID.randomUUID() + "." + extension;
        try {
            Files.write(resolve(key), data);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store image", e);
        }
        return key;
    }

    public byte[] read(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read image", e);
        }
    }

    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete image", e);
        }
    }

    private Path resolve(String key) {
        if (!KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return root.resolve(key);
    }
}
