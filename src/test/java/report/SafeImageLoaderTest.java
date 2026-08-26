package report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SafeImageLoaderTest {
    @TempDir Path tempDir;

    @Test
    void rejectsExcessiveDimensionsBeforeUsingImageAsALogo() throws Exception {
        Path image = tempDir.resolve("too-wide.png");
        BufferedImage oversized = new BufferedImage(4097, 1, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(oversized, "png", image.toFile());

        assertThrows(java.io.IOException.class, () -> SafeImageLoader.read(image));
    }
}
