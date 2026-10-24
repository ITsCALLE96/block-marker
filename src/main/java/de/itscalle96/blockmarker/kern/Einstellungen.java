package de.itscalle96.blockmarker.kern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Alle Profile und welches gerade aktiv ist (keins = Block-Marker aus). */
public final class Einstellungen {
	public enum Umschaltung { HINZUGEFUEGT, ENTFERNT, KEIN_PROFIL }

	private final List<Profil> profile = new ArrayList<>();
	private Profil aktiv;

	public static Einstellungen standard() {
		Einstellungen e = new Einstellungen();
		e.profile.add(Profil.vorlage("netherite", List.of(
				new Eintrag("minecraft:ancient_debris", 0xFF2BD6, true))));
		e.profile.add(Profil.vorlage("diamanten", List.of(
				new Eintrag("minecraft:diamond_ore", 0x00F0FF, true),
				new Eintrag("minecraft:deepslate_diamond_ore", 0x00F0FF, true))));
		e.profile.add(Profil.vorlage("tempel", List.of(
				new Eintrag("minecraft:cobblestone", 0xFF8000, false),
				new Eintrag("minecraft:mossy_cobblestone", 0xFF8000, false))));
		return e;
	}

	public List<Profil> profile() {
		return profile;
	}

	/** Sucht nach dem angezeigten Namen (in der aktuellen Sprache). */
	public Optional<Profil> profil(String anzeigeName) {
		return profile.stream().filter(p -> p.anzeigeName().equals(anzeigeName)).findFirst();
	}

	/** Sucht eine Vorlage über ihren Schlüssel, unabhängig von Sprache und Umbenennung. */
	public Optional<Profil> vorlage(String schluessel) {
		return profile.stream().filter(p -> schluessel.equals(p.vorlage())).findFirst();
	}

	public Optional<Profil> aktivesProfil() {
		return Optional.ofNullable(aktiv);
	}

	public String aktivName() {
		return aktiv == null ? null : aktiv.anzeigeName();
	}

	/** null schaltet aus. Unbekannte Profile werden ignoriert (= aus). */
	public void aktiviere(Profil p) {
		this.aktiv = p != null && profile.contains(p) ? p : null;
	}

	/** Aktiviert über den angezeigten Namen; null oder unbekannt = aus. */
	public void aktiviere(String anzeigeName) {
		aktiviere(anzeigeName == null ? null : profil(anzeigeName).orElse(null));
	}

	/** Aus → erstes Profil → … → letztes Profil → aus. Gibt das neue aktive Profil zurück (leer = aus). */
	public Optional<Profil> naechstesProfil() {
		int i = aktiv == null ? 0 : profile.indexOf(aktiv) + 1;
		aktiv = i < profile.size() ? profile.get(i) : null;
		return aktivesProfil();
	}

	/** Block ins aktive Profil aufnehmen bzw. wieder entfernen. */
	public Umschaltung umschalten(String block) {
		if (aktiv == null) {
			return Umschaltung.KEIN_PROFIL;
		}
		if (aktiv.entferne(block)) {
			return Umschaltung.ENTFERNT;
		}
		aktiv.setze(new Eintrag(block, aktiv.standardFarbe(), aktiv.standardLeuchten()));
		return Umschaltung.HINZUGEFUEGT;
	}

	/** Fügt ein neues Profil hinzu; bei vergebenem Namen wird „ (2)“ usw. angehängt. Gibt das Profil zurück. */
	public Profil neuesProfil(String wunschname) {
		String basis = wunschname == null || wunschname.isBlank() ? "Profil" : wunschname.strip();
		String name = basis;
		for (int n = 2; profil(name).isPresent(); n++) {
			name = basis + " (" + n + ")";
		}
		Profil p = new Profil(name);
		profile.add(p);
		return p;
	}

	/** Für das Laden aus der Datei: Profil unverändert anhängen. */
	void fuegeGeladenesHinzu(Profil p) {
		profile.add(p);
	}

	public void loesche(Profil p) {
		profile.remove(p);
		if (p == aktiv) {
			aktiv = null;
		}
	}

	/** Benennt um, wenn der Name frei ist; das aktive Profil bleibt aktiv. */
	public boolean umbenennen(Profil p, String neu) {
		String name = neu == null ? "" : neu.strip();
		if (name.isEmpty() || profil(name).filter(x -> x != p).isPresent()) {
			return false;
		}
		p.umbenennen(name);
		return true;
	}
}
