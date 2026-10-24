package de.itscalle96.blockmarker.kern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileTest {
	@Test
	void vorgabenEnthaltenDieDreiProfile() {
		Einstellungen e = Einstellungen.standard();
		assertEquals(List.of("Netherite", "Diamanten", "Tempel"), e.profile().stream().map(Profil::name).toList());
		assertTrue(e.profil("Netherite").orElseThrow().enthaelt("minecraft:ancient_debris"));
		assertTrue(e.profil("Diamanten").orElseThrow().enthaelt("deepslate_diamond_ore"));
		assertTrue(e.profil("Tempel").orElseThrow().enthaelt("minecraft:mossy_cobblestone"));
		assertNull(e.aktivName(), "nach dem ersten Start ist der Block-Marker aus");
	}

	@Test
	void tasteSchaltetReihumUndWiederAus() {
		Einstellungen e = Einstellungen.standard();
		assertEquals("Netherite", e.naechstesProfil().orElseThrow().name());
		assertEquals("Diamanten", e.naechstesProfil().orElseThrow().name());
		assertEquals("Tempel", e.naechstesProfil().orElseThrow().name());
		assertTrue(e.naechstesProfil().isEmpty());
		assertEquals("Netherite", e.naechstesProfil().orElseThrow().name());
	}

	@Test
	void blockAnschauenUndTasteFuegtHinzuUndNochmalEntfernt() {
		Einstellungen e = Einstellungen.standard();
		assertEquals(Einstellungen.Umschaltung.KEIN_PROFIL, e.umschalten("minecraft:gold_ore"));
		e.aktiviere("Diamanten");
		assertEquals(Einstellungen.Umschaltung.HINZUGEFUEGT, e.umschalten("minecraft:gold_ore"));
		Eintrag neu = e.aktivesProfil().orElseThrow().finde("minecraft:gold_ore").orElseThrow();
		assertEquals(0x00F0FF, neu.farbe(), "übernimmt die Farbe des Profils");
		assertTrue(neu.leuchtet());
		assertEquals(Einstellungen.Umschaltung.ENTFERNT, e.umschalten("gold_ore"));
		assertFalse(e.aktivesProfil().orElseThrow().enthaelt("minecraft:gold_ore"));
	}

	@Test
	void doppelteNamenWerdenDurchnummeriert() {
		Einstellungen e = Einstellungen.standard();
		assertEquals("Tempel (2)", e.neuesProfil("Tempel").name());
		assertEquals("Profil", e.neuesProfil(" ").name());
	}

	@Test
	void umbenennenUndLoeschenHaltenAktivStimmig() {
		Einstellungen e = Einstellungen.standard();
		e.aktiviere("Tempel");
		Profil tempel = e.aktivesProfil().orElseThrow();
		assertFalse(e.umbenennen(tempel, "Netherite"), "Name schon vergeben");
		assertTrue(e.umbenennen(tempel, "Dschungeltempel"));
		assertEquals("Dschungeltempel", e.aktivName());
		e.loesche(tempel);
		assertNull(e.aktivName());
	}

	@Test
	void speichernUndLadenErgibtDasGleiche(@TempDir Path ordner) throws IOException {
		Einstellungen e = Einstellungen.standard();
		e.aktiviere("Diamanten");
		Profil eigenes = e.neuesProfil("Mein Lager");
		eigenes.setze(new Eintrag("minecraft:barrel", 0x7CFF00, false));
		eigenes.setze(new Eintrag("somemod:crate", 0xA040FF, true));
		Path datei = ordner.resolve("config/blockmarker.json");

		ProfilSpeicher.speichern(datei, e);
		Einstellungen geladen = ProfilSpeicher.laden(datei);

		assertEquals(ProfilSpeicher.alsJson(e), ProfilSpeicher.alsJson(geladen));
		assertEquals("Diamanten", geladen.aktivName());
		Eintrag kiste = geladen.profil("Mein Lager").orElseThrow().finde("somemod:crate").orElseThrow();
		assertEquals(0xA040FF, kiste.farbe());
		assertTrue(kiste.leuchtet());
		assertFalse(Files.exists(ordner.resolve("config/blockmarker.json.tmp")));
	}

	@Test
	void dateiIstLesbarUndDeutsch() {
		String json = ProfilSpeicher.alsJson(Einstellungen.standard());
		assertTrue(json.contains("\"farbe\": \"#FF2BD6\""), json);
		assertTrue(json.contains("\"leuchtet\": true"), json);
	}

	@Test
	void fehlendeDateiErgibtVorgaben(@TempDir Path ordner) {
		Einstellungen e = ProfilSpeicher.laden(ordner.resolve("gibtsnicht.json"));
		assertEquals(3, e.profile().size());
	}

	@Test
	void kaputteDateiWirdGesichertUndVorgabenGelten(@TempDir Path ordner) throws IOException {
		Path datei = ordner.resolve("blockmarker.json");
		Files.writeString(datei, "{ das ist kein json", StandardCharsets.UTF_8);
		Einstellungen e = ProfilSpeicher.laden(datei);
		assertEquals(3, e.profile().size());
		assertTrue(Files.exists(ordner.resolve("blockmarker.json.kaputt")));
	}

	@Test
	void ungueltigeEintraegeWerdenUebersprungen() {
		String json = """
				{"version":1,"aktiv":"Weg","profile":[
				  {"name":"A","bloecke":[{"block":"Stone","farbe":"#zzzzzz"},{"farbe":"#FF0000"},42]},
				  {"ohneName":true}
				]}""";
		Einstellungen e = ProfilSpeicher.ausJson(json);
		assertEquals(1, e.profile().size());
		Eintrag stein = e.profil("A").orElseThrow().finde("minecraft:stone").orElseThrow();
		assertEquals(0xFF2BD6, stein.farbe(), "ungültiger Farbcode fällt auf Magenta zurück");
		assertFalse(stein.leuchtet());
		assertNull(e.aktivName(), "unbekanntes aktives Profil wird ignoriert");
	}
}
