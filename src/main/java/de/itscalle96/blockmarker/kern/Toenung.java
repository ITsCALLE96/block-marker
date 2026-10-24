package de.itscalle96.blockmarker.kern;

/**
 * Tönt Texturpixel in eine Zielfarbe. Die Helligkeit des Originals bleibt als Schattierung
 * erhalten, damit das Muster des Blocks erkennbar bleibt. Pixel sind 0xAARRGGBB.
 */
public final class Toenung {
	/** Wie stark die Zielfarbe das Original ersetzt (0 = gar nicht, 1 = ganz). */
	public static final float STAERKE = 0.85f;

	private Toenung() {
	}

	public static int toene(int argb, int rgb) {
		return toene(argb, rgb, STAERKE);
	}

	public static int toene(int argb, int rgb, float staerke) {
		int a = argb >>> 24;
		if (a == 0) {
			return argb;
		}
		int r = (argb >> 16) & 0xFF;
		int g = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		// Wahrgenommene Helligkeit 0..1; leicht angehoben, damit dunkle Erze nicht schwarz werden
		float hell = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
		float schatten = 0.3f + 1.1f * hell;
		int zr = mische(r, ((rgb >> 16) & 0xFF) * schatten, staerke);
		int zg = mische(g, ((rgb >> 8) & 0xFF) * schatten, staerke);
		int zb = mische(b, (rgb & 0xFF) * schatten, staerke);
		return (a << 24) | (zr << 16) | (zg << 8) | zb;
	}

	private static int mische(int original, float ziel, float staerke) {
		float wert = original + (Math.min(255f, ziel) - original) * staerke;
		return Math.max(0, Math.min(255, Math.round(wert)));
	}
}
