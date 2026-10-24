package de.itscalle96.blockmarker.kern;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Tönt PNG-Dateien Pixel für Pixel mit {@link Toenung}. Animierte Texturen (Bildstreifen) funktionieren genauso. */
public final class PngToener implements PackBauer.Toener {
	@Override
	public byte[] toene(byte[] png, int farbe) {
		try {
			BufferedImage bild = ImageIO.read(new ByteArrayInputStream(png));
			if (bild == null) {
				return png;
			}
			BufferedImage aus = new BufferedImage(bild.getWidth(), bild.getHeight(), BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < bild.getHeight(); y++) {
				for (int x = 0; x < bild.getWidth(); x++) {
					aus.setRGB(x, y, Toenung.toene(bild.getRGB(x, y), farbe));
				}
			}
			ByteArrayOutputStream puffer = new ByteArrayOutputStream();
			ImageIO.write(aus, "png", puffer);
			return puffer.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
