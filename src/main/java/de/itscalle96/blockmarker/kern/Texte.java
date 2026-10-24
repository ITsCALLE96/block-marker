package de.itscalle96.blockmarker.kern;

import java.util.Map;
import java.util.function.Function;

/**
 * Übersetzungen für Texte, die im Kern entstehen (Namen der vorgefertigten Profile). Im Spiel setzt die Mod den
 * Übersetzer auf die Sprachdateien (de_de/en_us); ohne Spiel – etwa in Tests – gilt Englisch.
 */
public final class Texte {
	private static final Map<String, String> ENGLISCH = Map.of(
			"blockmarker.vorlage.netherite", "Netherite",
			"blockmarker.vorlage.diamanten", "Diamonds",
			"blockmarker.vorlage.tempel", "Temple");

	private static volatile Function<String, String> uebersetzer = schluessel -> ENGLISCH.getOrDefault(schluessel, schluessel);

	private Texte() {
	}

	public static void setzeUebersetzer(Function<String, String> neu) {
		uebersetzer = neu;
	}

	public static String text(String schluessel) {
		return uebersetzer.apply(schluessel);
	}

	public static String vorlageName(String vorlage) {
		return text("blockmarker.vorlage." + vorlage);
	}
}
