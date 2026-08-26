package report;

import storage.SafeFiles;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

/** Reads a user-selected logo only after checking its encoded and decoded size. */
public final class SafeImageLoader {
    public static final long MAX_FILE_BYTES = 8L * 1024L * 1024L;
    public static final int MAX_DIMENSION = 4096;
    public static final long MAX_PIXELS = 16_000_000L;

    private SafeImageLoader() { }

    public static BufferedImage read(Path path) throws IOException {
        SafeFiles.requireReadableFile(path, MAX_FILE_BYTES);
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) throw new IOException("The selected file is not a supported image.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("The selected file is not a supported image.");

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS) {
                    throw new IOException("The selected image is too large (maximum 4096 px and 16 megapixels).");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw new IOException("The selected file is not a supported image.");
                return image;
            } finally {
                reader.dispose();
            }
        }
    }
}
