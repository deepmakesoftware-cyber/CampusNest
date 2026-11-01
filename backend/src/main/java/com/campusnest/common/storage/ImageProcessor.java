package com.campusnest.common.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;

import com.campusnest.common.error.ApiException;

/**
 * Checks an uploaded photo and produces a clean copy. Security steps, in order:
 *
 * 1. Size limit (5 MB).
 * 2. File type decided by the file's first bytes ("magic number"), NOT by its name or the browser's
 *    Content-Type, which are both easy to fake. Only JPEG and PNG are accepted.
 * 3. Width/height checked BEFORE decoding, so a tiny file claiming to be a 100000x100000 image
 *    (a "decompression bomb") cannot exhaust memory.
 * 4. The image is decoded and re-encoded. The new file contains only pixels: EXIF metadata such as the
 *    GPS location where the photo was taken is dropped, and anything hidden inside the original file
 *    is discarded.
 */
@Component
public class ImageProcessor {

    public static final int MAX_BYTES = 5 * 1024 * 1024;
    static final int MAX_DIMENSION = 6000;

    public record ProcessedImage(byte[] data, String contentType, String extension, int width, int height) {
    }

    public ProcessedImage process(byte[] original) {
        if (original == null || original.length == 0) {
            throw ApiException.badRequest("FILE_MISSING", "Choose a file to upload.");
        }
        if (original.length > MAX_BYTES) {
            throw ApiException.badRequest("IMAGE_TOO_LARGE", "The photo is too large. The maximum size is 5 MB.");
        }
        String format = detectFormat(original);
        if (format == null) {
            throw ApiException.badRequest("UNSUPPORTED_IMAGE_TYPE", "Only JPEG and PNG photos are accepted.");
        }

        BufferedImage image = decode(original, format);
        if (format.equals("jpeg") && image.getType() != BufferedImage.TYPE_INT_RGB) {
            image = toRgb(image); // JPEG cannot store transparency; normalise colour layout
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, format.equals("jpeg") ? "jpg" : "png", out)) {
                throw unreadable();
            }
        } catch (IOException e) {
            throw unreadable();
        }
        return new ProcessedImage(out.toByteArray(), "image/" + format, format.equals("jpeg") ? "jpg" : "png",
                image.getWidth(), image.getHeight());
    }

    /** Returns "jpeg", "png", or null for anything else. */
    static String detectFormat(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        if (b.length >= png.length) {
            for (int i = 0; i < png.length; i++) {
                if (b[i] != png[i]) {
                    return null;
                }
            }
            return "png";
        }
        return null;
    }

    private static BufferedImage decode(byte[] data, String format) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
            if (!readers.hasNext()) {
                throw unreadable();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw ApiException.badRequest("IMAGE_TOO_BIG",
                            "The photo is too big. The maximum is " + MAX_DIMENSION + " x " + MAX_DIMENSION + " pixels.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw unreadable();
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw unreadable();
        }
    }

    private static BufferedImage toRgb(BufferedImage source) {
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        var g = rgb.createGraphics();
        try {
            g.drawImage(source, 0, 0, java.awt.Color.WHITE, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static ApiException unreadable() {
        return ApiException.badRequest("UNREADABLE_IMAGE", "This photo could not be read. Try saving it as a standard JPEG or PNG.");
    }
}
