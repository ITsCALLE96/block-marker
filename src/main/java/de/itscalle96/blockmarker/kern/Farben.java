package de.itscalle96.blockmarker.kern;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Farbcodes lesen/schreiben und die knalligen Vorgaben. Farben sind 0xRRGGBB. */
public final class Farben {
	private Farben() {
	}

	public record Vorgabe(String name, int rgb) {
	}

	public static final List<Vorgabe> VORGABEN = List.of(
			new Vorgabe("Rot", 0xFF1A1A),
			new Vorgabe("Orange", 0xFF8000),
			new Vorgabe("Gelb", 0xFFE600),
			new Vorgabe("Hellgrün", 0x7CFF00),
			new Vorgabe("Grün", 0x00E040),
			new Vorgabe("Türkis", 0x00F0FF),
			new Vorgabe("Blau", 0x2563FF),
			new Vorgabe("Lila", 0xA040FF),
			new Vorgabe("Magenta", 0xFF2BD6),
			new Vorgabe("Pink", 0xFF6FAE),
			new Vorgabe("Weiß", 0xFFFFFF));

	/** Liest „#RRGGBB“, „RRGGBB“, „#RGB“ oder „RGB“ (Groß/klein egal). */
	public static Optional<Integer> lies(String text) {
		if (text == null) {
			return Optional.empty();
		}
		String s = text.strip();
		if (s.startsWith("#")) {
			s = s.substring(1);
		}
		if (s.length() == 3) {
			s = "" + s.charAt(0) + s.charAt(0) + s.charAt(1) + s.charAt(1) + s.charAt(2) + s.charAt(2);
		}
		if (s.length() != 6) {
			return Optional.empty();
		}
		for (int i = 0; i < 6; i++) {
			if (Character.digit(s.charAt(i), 16) < 0) {
				return Optional.empty();
			}
		}
		return Optional.of(Integer.parseInt(s, 16));
	}

	public static String alsText(int rgb) {
		return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
	}
}
