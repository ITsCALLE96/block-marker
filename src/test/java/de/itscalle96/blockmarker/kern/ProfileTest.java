package de.itscalle96.blockmarker.kern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileTest {
	private static final Map<String, String> DEUTSCH = Map.of(
			"blockmarker.vorlage.netherite", "Netherite",
			"blockmarker.vorlage.diamanten", "Diamanten",
			"blockmarker.vorlage.tempel", "Tempel");

	private static void deutsch() {
		Texte.setzeUebersetzer(s -> DEUTSCH.getOrDefault(s, s));
	}

	@AfterEach
	void zurueckAufEnglisch() {
		Texte.setzeUebersetzer(s -> Map.of(
				"blockmarker.vorlage.netherite", "Netherite",
				"blockmarker.vorlage.diamanten", "Diamonds",
				"blockmarker.vorlage.tempel", "Temple").getOrDefault(s, s));
	}

	private static List<String> namen(Einstellungen e) {
		return e.profile().stream().map(Profil::anzeigeName).toList();
	}

	@Test
	void vorgabenEnthaltenDieDreiProfile() {
		Einstellungen e = Einstellungen.standard();
		assertEquals(List.of("Netherite", "Diamonds", "Temple"), namen(e));
		assertTrue(e.vorlage("netherite").orElseThrow().enthaelt("minecraft:ancient_debris"));
		assertTrue(e.vorlage("diamanten").orElseThrow().enthaelt("deepslate_diamond_ore"));
		assertTrue(e.vorlage("tempel").orElseThrow().enthaelt("minecraft:mossy_cobblestone"));
		assertNull(e.aktivName(), "nach dem ersten Start ist der Block-Marker aus");
	}

	@Test
	void vorlagenNamenFolgenDerSpielsprache() {
		Einstellungen e = Einstellungen.standard();
		e.aktiviere(e.vorlage("diamanten").orElseThrow());
		assertEquals("Diamonds", e.aktivName());
		deutsch();
		assertEquals(List.of("Netherite", "Diamanten", "Tempel"), namen(e));
		assertEquals("Diamanten", e.aktivName(), "aktives Profil bleibt beim Sprachwechsel aktiv");
		assertEquals("Diamanten", e.profil("Diamanten").orElseThrow().anzeigeName());
	}

	@Test
	void umbenannteVorlageBehaeltIhrenNamenInJederSprache() {
		Einstellungen e = Einstellungen.standard();
		Profil tempel = e.vorlage("tempel").orElseThrow();
		assertTrue(e.umbenennen(tempel, "Dschungeltempel"));
		deutsch();
		assertEquals("Dschungeltempel", tempel.anzeigeName());
	}

	@Test
	void tasteSchaltetReihumUndWiederAus() {
		Einstellungen e = Einstellungen.standard();
		assertEquals("Netherite", e.naechstesProfil().orElseThrow().anzeigeName());
		assertEquals("Diamonds", e.naechstesProfil().orElseThrow().anzeigeName());
		assertEquals("Temple", e.naechstesProfil().orElseThrow().anzeigeName());
		assertTrue(e.naechstesProfil().isEmpty());
		assertEquals("Netherite", e.naechstesProfil().orElseThrow().anzeigeName());
	}

	@Test
	void blockAnschauenUndTasteFuegtHinzuUndNochmalEntfernt() {
		Einstellungen e = Einstellungen.standard();
		assertEquals(Einstellungen.Umschaltung.KEIN_PROFIL, e.umschalten("minecraft:gold_ore"));
		e.aktiviere("Diamonds");
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
		assertEquals("Temple (2)", e.neuesProfil("Temple").anzeigeName());
		assertEquals("Profil", e.neuesProfil(" ").anzeigeName());
	}

	@Test
	void umbenennenUndLoeschenHaltenAktivStimmig() {
		Einstellungen e = Einstellungen.standard();
		Profil tempel = e.vorlage("tempel").orElseThrow();
		e.aktiviere(tempel);
		assertFalse(e.umbenennen(tempel, "Netherite"), "Name schon vergeben");
		assertTrue(e.umbenennen(tempel, "Dschungeltempel"));
		assertEquals("Dschungeltempel", e.aktivName());
		e.loesche(tempel);
		assertNull(e.aktivName());
	}

	@Test
	void speichernUndLadenErgibtDasGleiche(@TempDir Path ordner) throws IOException {
		Einstellungen e = Einstellungen.standard();
		e.aktiviere(e.vorlage("diamanten").orElseThrow());
		e.umbenennen(e.vorlage("tempel").orElseThrow(), "Dschungeltempel");
		Profil eigenes = e.neuesProfil("Mein Lager");
		eigenes.setze(new Eintrag("minecraft:barrel", 0x7CFF00, false));
		eigenes.setze(new Eintrag("somemod:crate", 0xA040FF, true));
		Path datei = ordner.resolve("config/blockmarker.json");

		ProfilSpeicher.speichern(datei, e);
		Einstellungen geladen = ProfilSpeicher.laden(datei);

		assertEquals(ProfilSpeicher.alsJson(e), ProfilSpeicher.alsJson(geladen));
		assertEquals("diamanten", geladen.aktivesProfil().orElseThrow().vorlage());
		assertEquals("Dschungeltempel", geladen.vorlage("tempel").orElseThrow().anzeigeName());
		Eintrag kiste = geladen.profil("Mein Lager").orElseThrow().finde("somemod:crate").orElseThrow();
		assertEquals(0xA040FF, kiste.farbe());
		assertTrue(kiste.leuchtet());
		assertFalse(Files.exists(ordner.resolve("config/blockmarker.json.tmp")));

		deutsch();
		assertEquals("Diamanten", ProfilSpeicher.laden(datei).aktivName(), "gespeichert auf Englisch, geladen auf Deutsch");
	}

	@Test
	void dateiIstLesbarUndSpeichertVorlagenSprachneutral() {
		String json = ProfilSpeicher.alsJson(Einstellungen.standard());
		assertTrue(json.contains("\"farbe\": \"#FF2BD6\""), json);
		assertTrue(json.contains("\"leuchtet\": true"), json);
		assertTrue(json.contains("\"vorlage\": \"diamanten\""), json);
		assertFalse(json.contains("Diamonds"), "kein übersetzter Name in der Datei: " + json);
		assertTrue(json.contains("\"aktiv\": null"), json);
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
				{"version":2,"aktiv":7,"profile":[
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

	@Test
	void alteDateiMitAktivAlsNameWirdGelesen() {
		String json = """
				{"version":1,"aktiv":"Mein Lager","profile":[{"name":"Mein Lager","bloecke":[]}]}""";
		assertEquals("Mein Lager", ProfilSpeicher.ausJson(json).aktivName());
	}
}
