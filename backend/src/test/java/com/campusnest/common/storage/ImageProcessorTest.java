package com.campusnest.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.campusnest.common.error.ApiException;

/** Unit tests for upload validation and metadata stripping. */
class ImageProcessorTest {

    private final ImageProcessor processor = new ImageProcessor();

    static byte[] image(String format, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    /** Inserts a fake EXIF block (as cameras and phones do, often including GPS position) after the JPEG header. */
    static byte[] withExif(byte[] jpeg) {
        byte[] payload = "Exif\0\0GPS:19.0222,72.8562".getBytes(StandardCharsets.ISO_8859_1);
        int length = payload.length + 2;
        byte[] result = new byte[jpeg.length + 4 + payload.length];
        result[0] = jpeg[0];
        result[1] = jpeg[1];                       // FF D8 (start of image)
        result[2] = (byte) 0xFF;
        result[3] = (byte) 0xE1;                   // APP1 marker = EXIF
        result[4] = (byte) (length >> 8);
        result[5] = (byte) length;
        System.arraycopy(payload, 0, result, 6, payload.length);
        System.arraycopy(jpeg, 2, result, 6 + payload.length, jpeg.length - 2);
        return result;
    }

    @Test
    void acceptsPngAndReportsSize() throws IOException {
        var result = processor.process(image("png", 40, 30));
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.extension()).isEqualTo("png");
        assertThat(result.width()).isEqualTo(40);
        assertThat(result.height()).isEqualTo(30);
    }

    @Test
    void stripsExifMetadataFromJpeg() throws IOException {
        byte[] original = withExif(image("jpg", 40, 30));
        assertThat(new String(original, StandardCharsets.ISO_8859_1)).contains("GPS:19.0222");

        var result = processor.process(original);

        assertThat(result.contentType()).isEqualTo("image/jpeg");
        String cleaned = new String(result.data(), StandardCharsets.ISO_8859_1);
        assertThat(cleaned).doesNotContain("Exif").doesNotContain("GPS:19.0222");
    }

    @Test
    void rejectsFileThatIsNotAnImageEvenIfNamedLikeOne() {
        byte[] script = "<script>alert('x')</script>".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> processor.process(script))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo("UNSUPPORTED_IMAGE_TYPE");
    }

    @Test
    void rejectsCorruptImageWithValidHeader() {
        byte[] fake = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3, 4, 5};
        assertThatThrownBy(() -> processor.process(fake))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo("UNREADABLE_IMAGE");
    }

    @Test
    void rejectsFilesOverFiveMegabytes() {
        byte[] huge = new byte[ImageProcessor.MAX_BYTES + 1];
        assertThatThrownBy(() -> processor.process(huge))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo("IMAGE_TOO_LARGE");
    }

    @Test
    void rejectsImagesWiderThanLimitBeforeDecoding() throws IOException {
        byte[] wide = image("png", ImageProcessor.MAX_DIMENSION + 1, 1);
        assertThatThrownBy(() -> processor.process(wide))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode())
                .isEqualTo("IMAGE_TOO_BIG");
    }
}
