package de.itscalle96.blockmarker.ui;

import de.itscalle96.blockmarker.kern.BlockSuche;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Liefert alle Blöcke als Suchkandidaten mit Namen in Spielsprache, Englisch und Deutsch.
 * Englisch und Deutsch werden unabhängig von der Spielsprache aus den Sprachdateien gelesen und bis zum
 * nächsten Neuladen der Ressourcen zwischengespeichert.
 */
final class BlockNamen {
	private static ResourceManager geladenFuer;
	private static Language englisch;
	private static Language deutsch;

	private BlockNamen() {
	}

	static List<BlockSuche.Kandidat> alle() {
		Minecraft mc = Minecraft.getInstance();
		ResourceManager rm = mc.getResourceManager();
		if (rm != geladenFuer) {
			englisch = ClientLanguage.loadFrom(rm, List.of("en_us"), false);
			deutsch = ClientLanguage.loadFrom(rm, List.of("de_de"), false);
			geladenFuer = rm;
		}
		Language spiel = Language.getInstance();
		return BuiltInRegistries.BLOCK.stream()
				.filter(b -> !b.defaultBlockState().isAir())
				.map(b -> kandidat(b, spiel))
				.sorted(Comparator.comparing(k -> k.nameSpiel().toLowerCase(Locale.ROOT)))
				.toList();
	}

	private static BlockSuche.Kandidat kandidat(Block b, Language spiel) {
		String schluessel = b.getDescriptionId();
		String id = BuiltInRegistries.BLOCK.getKey(b).toString();
		return new BlockSuche.Kandidat(id, spiel.getOrDefault(schluessel, id),
				englisch.getOrDefault(schluessel, null), deutsch.getOrDefault(schluessel, null));
	}
}
