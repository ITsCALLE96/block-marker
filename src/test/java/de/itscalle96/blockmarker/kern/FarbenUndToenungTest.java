package de.itscalle96.blockmarker.kern;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarbenUndToenungTest {
	@Test
	void farbcodesWerdenGelesen() {
		assertEquals(Optional.of(0xFF2BD6), Farben.lies("#FF2BD6"));
		assertEquals(Optional.of(0xFF2BD6), Farben.lies("ff2bd6"));
		assertEquals(Optional.of(0xFFAA00), Farben.lies(" #fa0 "));
		assertEquals(Optional.empty(), Farben.lies("#12345"));
		assertEquals(Optional.empty(), Farben.lies("#GG0000"));
		assertEquals(Optional.empty(), Farben.lies(null));
	}

	@Test
	void farbcodeHinUndZurueck() {
		for (Farben.Vorgabe v : Farben.VORGABEN) {
			assertEquals(Optional.of(v.rgb()), Farben.lies(Farben.alsText(v.rgb())), v.name());
		}
		assertEquals("#00F0FF", Farben.alsText(0x00F0FF));
	}

	@Test
	void durchsichtigBleibtDurchsichtig() {
		assertEquals(0x00123456, Toenung.toene(0x00123456, 0xFF0000));
	}

	@Test
	void deckkraftBleibtErhalten() {
		assertEquals(0x80, Toenung.toene(0x80808080, 0x00FF00) >>> 24);
	}

	@Test
	void staerkeNullAendertNichts() {
		assertEquals(0xFF5A3C1E, Toenung.toene(0xFF5A3C1E, 0x00F0FF, 0f));
	}

	@Test
	void grauWirdZurZielfarbe() {
		int t = Toenung.toene(0xFF808080, 0xFF0000);
		int r = (t >> 16) & 0xFF, g = (t >> 8) & 0xFF, b = t & 0xFF;
		assertTrue(r > 150, "Rot dominiert: " + Integer.toHexString(t));
		assertTrue(g < 60 && b < 60, "Grün/Blau klein: " + Integer.toHexString(t));
	}

	@Test
	void musterBleibtErkennbar() {
		// Hellerer Ausgangspixel ergibt hellere Tönung – sonst wäre die Textur nur eine Farbfläche
		int farbe = 0x2563FF;
		int vorher = -1;
		for (int grau = 0; grau <= 255; grau += 15) {
			int argb = 0xFF000000 | grau << 16 | grau << 8 | grau;
			int t = Toenung.toene(argb, farbe);
			int summe = ((t >> 16) & 0xFF) + ((t >> 8) & 0xFF) + (t & 0xFF);
			assertTrue(summe >= vorher, "monoton bei Grau " + grau);
			vorher = summe;
		}
		int dunkel = Toenung.toene(0xFF202020, farbe) & 0xFF;
		int hell = Toenung.toene(0xFFE0E0E0, farbe) & 0xFF;
		assertTrue(hell - dunkel > 80, "deutlicher Kontrast zwischen dunkel und hell");
	}

	@Test
	void dunkleErzeWerdenNichtSchwarz() {
		// Ancient Debris ist sehr dunkel – die Tönung soll trotzdem gut sichtbar sein
		int t = Toenung.toene(0xFF3A2A28, 0xFF2BD6);
		assertTrue(((t >> 16) & 0xFF) > 90, Integer.toHexString(t));
	}

	@Test
	void kraeftigeFarbfleckenBehaltenIhreEigeneFarbe() {
		// Grüner Moosfleck bei oranger Markierung: bleibt grünlicher als ein grauer Stein-Pixel gleicher Helligkeit
		int moos = Toenung.toene(0xFF4C8A2A, 0xFF8000);
		int stein = Toenung.toene(0xFF717171, 0xFF8000);
		int gruenMoos = (moos >> 8) & 0xFF, rotMoos = (moos >> 16) & 0xFF;
		int gruenStein = (stein >> 8) & 0xFF, rotStein = (stein >> 16) & 0xFF;
		assertTrue(gruenMoos - rotMoos > gruenStein - rotStein + 40,
				"Moos " + Integer.toHexString(moos) + " vs Stein " + Integer.toHexString(stein));
	}

	@Test
	void helligkeitsSpanneVerstaerktDenKontrast() {
		// Textur mit wenig Kontrast (nur Grautöne 90..130): mit Spanne deutlich unterschiedlicher
		int farbe = 0x00F0FF;
		int ohneDunkel = Toenung.toene(0xFF5A5A5A, farbe) & 0xFF;
		int ohneHell = Toenung.toene(0xFF828282, farbe) & 0xFF;
		float lo = 90 / 255f, hi = 130 / 255f;
		int mitDunkel = Toenung.toene(0xFF5A5A5A, farbe, Toenung.STAERKE, lo, hi) & 0xFF;
		int mitHell = Toenung.toene(0xFF828282, farbe, Toenung.STAERKE, lo, hi) & 0xFF;
		assertTrue(mitHell - mitDunkel > 2 * (ohneHell - ohneDunkel),
				"mit Spanne " + (mitHell - mitDunkel) + ", ohne " + (ohneHell - ohneDunkel));
	}

	@Test
	void einfarbigeTexturBleibtGetoent() {
		int t = Toenung.toene(0xFF808080, 0xFF0000, Toenung.STAERKE, 0.5f, 0.5f);
		assertTrue(((t >> 16) & 0xFF) > 150, Integer.toHexString(t));
	}
}
