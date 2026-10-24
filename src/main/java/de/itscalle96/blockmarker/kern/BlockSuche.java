package de.itscalle96.blockmarker.kern;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Blocksuche für das Menü. Ein Block passt, wenn der Suchbegriff (ohne Groß-/Kleinschreibung, auch als Teilwort)
 * im Namen der Spielsprache, im englischen oder deutschen Namen oder in der Block-ID vorkommt. So finden
 * englisch und deutsch spielende Mitspieler dieselben Blöcke, egal welche Sprache eingestellt ist.
 */
public final class BlockSuche {
	private BlockSuche() {
	}

	/** Ein Block mit ID („minecraft:cobblestone“) und seinen Namen; fehlende Namen dürfen null sein. */
	public record Kandidat(String id, String nameSpiel, String nameEnglisch, String nameDeutsch) {
	}

	public static boolean passt(Kandidat k, String begriff) {
		String b = normalisiere(begriff);
		return b.isEmpty()
				|| enthaelt(k.id(), b)
				|| enthaelt(k.nameSpiel(), b)
				|| enthaelt(k.nameEnglisch(), b)
				|| enthaelt(k.nameDeutsch(), b);
	}

	public static List<Kandidat> filtere(List<Kandidat> alle, String begriff, int max) {
		List<Kandidat> treffer = new ArrayList<>();
		for (Kandidat k : alle) {
			if (treffer.size() >= max) {
				break;
			}
			if (passt(k, begriff)) {
				treffer.add(k);
			}
		}
		return treffer;
	}

	private static boolean enthaelt(String text, String begriff) {
		return text != null && normalisiere(text).contains(begriff);
	}

	private static String normalisiere(String s) {
		return s == null ? "" : s.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}
}
