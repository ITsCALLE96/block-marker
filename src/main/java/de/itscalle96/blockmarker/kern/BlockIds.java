package de.itscalle96.blockmarker.kern;

import java.util.Locale;

/** Kleine Helfer rund um Ressourcen-IDs ohne Minecraft-Abhängigkeit. */
public final class BlockIds {
	private BlockIds() {
	}

	/** „stone“ → „minecraft:stone“, Großbuchstaben → klein. */
	public static String normalisiere(String id) {
		String s = id.strip().toLowerCase(Locale.ROOT);
		return s.contains(":") ? s : "minecraft:" + s;
	}

	public static String namensraum(String id) {
		String n = normalisiere(id);
		return n.substring(0, n.indexOf(':'));
	}

	public static String pfad(String id) {
		String n = normalisiere(id);
		return n.substring(n.indexOf(':') + 1);
	}
}
