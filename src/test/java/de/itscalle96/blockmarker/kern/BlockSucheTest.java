package de.itscalle96.blockmarker.kern;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Blocksuche in beiden Sprachen: Spielsprache, Englisch, Deutsch und Block-ID. */
class BlockSucheTest {
	private static final BlockSuche.Kandidat DEBRIS_DE = new BlockSuche.Kandidat("minecraft:ancient_debris",
			"Antiker Schrott", "Ancient Debris", "Antiker Schrott");
	private static final BlockSuche.Kandidat COBBLE_DE = new BlockSuche.Kandidat("minecraft:cobblestone",
			"Bruchstein", "Cobblestone", "Bruchstein");
	private static final BlockSuche.Kandidat DIAMANT_DE = new BlockSuche.Kandidat("minecraft:diamond_ore",
			"Diamanterz", "Diamond Ore", "Diamanterz");
	private static final List<BlockSuche.Kandidat> DEUTSCHES_SPIEL = List.of(DEBRIS_DE, COBBLE_DE, DIAMANT_DE);

	/** Gleiche Blöcke, Spielsprache Englisch. */
	private static final List<BlockSuche.Kandidat> ENGLISCHES_SPIEL = DEUTSCHES_SPIEL.stream()
			.map(k -> new BlockSuche.Kandidat(k.id(), k.nameEnglisch(), k.nameEnglisch(), k.nameDeutsch()))
			.toList();

	private static List<String> ids(List<BlockSuche.Kandidat> liste, String begriff) {
		return BlockSuche.filtere(liste, begriff, 100).stream().map(BlockSuche.Kandidat::id).toList();
	}

	@Test
	void deutschesSpielFindetDeutschEnglischUndId() {
		assertEquals(List.of("minecraft:ancient_debris"), ids(DEUTSCHES_SPIEL, "Antiker"));
		assertEquals(List.of("minecraft:ancient_debris"), ids(DEUTSCHES_SPIEL, "Ancient"));
		assertEquals(List.of("minecraft:cobblestone"), ids(DEUTSCHES_SPIEL, "cobble"));
		assertEquals(List.of("minecraft:cobblestone"), ids(DEUTSCHES_SPIEL, "Bruchst"));
		assertEquals(List.of("minecraft:diamond_ore"), ids(DEUTSCHES_SPIEL, "diamond_ore"));
	}

	@Test
	void englischesSpielFindetEnglischDeutschUndId() {
		assertEquals(List.of("minecraft:ancient_debris"), ids(ENGLISCHES_SPIEL, "Ancient"));
		assertEquals(List.of("minecraft:ancient_debris"), ids(ENGLISCHES_SPIEL, "Antiker"));
		assertEquals(List.of("minecraft:cobblestone"), ids(ENGLISCHES_SPIEL, "cobble"));
		assertEquals(List.of("minecraft:diamond_ore"), ids(ENGLISCHES_SPIEL, "Diamanterz"));
	}

	@Test
	void vollstaendigeIdMitNamensraum() {
		assertEquals(List.of("minecraft:cobblestone"), ids(DEUTSCHES_SPIEL, "minecraft:cobblestone"));
		assertEquals(List.of("minecraft:ancient_debris"), ids(ENGLISCHES_SPIEL, "ancient_debris"));
	}

	@Test
	void grossKleinschreibungUndLeerzeichenEgal() {
		assertEquals(List.of("minecraft:ancient_debris"), ids(DEUTSCHES_SPIEL, "  ANCIENT debris "));
		assertEquals(List.of("minecraft:ancient_debris"), ids(ENGLISCHES_SPIEL, "antiker SCHROTT"));
	}

	@Test
	void teilwoerterImWort() {
		assertTrue(BlockSuche.passt(DIAMANT_DE, "manter"));
		assertTrue(BlockSuche.passt(DEBRIS_DE, "ebri"));
		assertFalse(BlockSuche.passt(DEBRIS_DE, "netherite"));
	}

	@Test
	void leereSucheZeigtAllesBisZurGrenze() {
		assertEquals(3, BlockSuche.filtere(DEUTSCHES_SPIEL, "", 100).size());
		assertEquals(2, BlockSuche.filtere(DEUTSCHES_SPIEL, " ", 2).size());
	}

	@Test
	void fehlendeNamenStoerenNicht() {
		BlockSuche.Kandidat ohne = new BlockSuche.Kandidat("somemod:crate", null, null, null);
		assertTrue(BlockSuche.passt(ohne, "crate"));
		assertFalse(BlockSuche.passt(ohne, "kiste"));
	}
}
