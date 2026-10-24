package de.itscalle96.blockmarker.kern;

/**
 * Tönt Texturpixel in eine Zielfarbe. Die Helligkeit des Originals bleibt als Schattierung erhalten, damit das
 * Muster des Blocks erkennbar bleibt. Pixel sind 0xAARRGGBB.
 *
 * <p>Zwei Hilfen für gut lesbare Muster: Die Helligkeit wird auf die Spanne der ganzen Textur gestreckt
 * (wenig Kontrast wird mehr Kontrast), und schon kräftig farbige Pixel – Diamantflecken, Moos – werden schwächer
 * getönt, sodass sie sich vom eingefärbten Grundgestein abheben.
 */
public final class Toenung {
	/** Wie stark die Zielfarbe das Original ersetzt (0 = gar nicht, 1 = ganz). */
	public static final float STAERKE = 0.85f;
	/** Ab dieser Sättigung (0..1) zählt ein Pixel als „schon farbig“ und wird schwächer getönt. */
	private static final float FARBIG_AB = 0.35f;
	/** Wie viel der Tönung ein voll gesättigter Pixel höchstens verliert. */
	private static final float FARBIG_SCHUTZ = 0.7f;

	private Toenung() {
	}

	public static int toene(int argb, int rgb) {
		return toene(argb, rgb, STAERKE);
	}

	public static int toene(int argb, int rgb, float staerke) {
		return toene(argb, rgb, staerke, 0f, 1f);
	}

	/**
	 * @param hellMin dunkelster Helligkeitswert der Textur (0..1)
	 * @param hellMax hellster Helligkeitswert der Textur (0..1)
	 */
	public static int toene(int argb, int rgb, float staerke, float hellMin, float hellMax) {
		int a = argb >>> 24;
		if (a == 0) {
			return argb;
		}
		int r = (argb >> 16) & 0xFF;
		int g = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		float hell = helligkeit(argb);
		float n = hellMax - hellMin > 0.02f ? (hell - hellMin) / (hellMax - hellMin) : hell;
		n = Math.max(0f, Math.min(1f, n));
		// 0.35..1.2: dunkle Stellen bleiben farbig statt schwarz, helle dürfen leicht überstrahlen
		float schatten = 0.35f + 0.85f * n;

		int max = Math.max(r, Math.max(g, b));
		int min = Math.min(r, Math.min(g, b));
		float saettigung = max == 0 ? 0f : (max - min) / (float) max;
		float farbig = Math.max(0f, (saettigung - FARBIG_AB) / (1f - FARBIG_AB));
		float s = staerke * (1f - FARBIG_SCHUTZ * farbig);

		int zr = mische(r, ((rgb >> 16) & 0xFF) * schatten, s);
		int zg = mische(g, ((rgb >> 8) & 0xFF) * schatten, s);
		int zb = mische(b, (rgb & 0xFF) * schatten, s);
		return (a << 24) | (zr << 16) | (zg << 8) | zb;
	}

	/** Wahrgenommene Helligkeit 0..1. */
	public static float helligkeit(int argb) {
		int r = (argb >> 16) & 0xFF;
		int g = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		return (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
	}

	private static int mische(int original, float ziel, float staerke) {
		float wert = original + (Math.min(255f, ziel) - original) * staerke;
		return Math.max(0, Math.min(255, Math.round(wert)));
	}
}
