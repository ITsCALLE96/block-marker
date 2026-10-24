package de.itscalle96.blockmarker.kern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Alle Profile und welches gerade aktiv ist (keins = Block-Marker aus). */
public final class Einstellungen {
	public enum Umschaltung { HINZUGEFUEGT, ENTFERNT, KEIN_PROFIL }

	private final List<Profil> profile = new ArrayList<>();
	private String aktiv;

	public static Einstellungen standard() {
		Einstellungen e = new Einstellungen();
		e.profile.add(new Profil("Netherite", List.of(
				new Eintrag("minecraft:ancient_debris", 0xFF2BD6, true))));
		e.profile.add(new Profil("Diamanten", List.of(
				new Eintrag("minecraft:diamond_ore", 0x00F0FF, true),
				new Eintrag("minecraft:deepslate_diamond_ore", 0x00F0FF, true))));
		e.profile.add(new Profil("Tempel", List.of(
				new Eintrag("minecraft:cobblestone", 0xFF8000, false),
				new Eintrag("minecraft:mossy_cobblestone", 0xFF8000, false))));
		return e;
	}

	public List<Profil> profile() {
		return profile;
	}

	public Optional<Profil> profil(String name) {
		return profile.stream().filter(p -> p.name().equals(name)).findFirst();
	}

	public Optional<Profil> aktivesProfil() {
		return aktiv == null ? Optional.empty() : profil(aktiv);
	}

	public String aktivName() {
		return aktivesProfil().map(Profil::name).orElse(null);
	}

	public void aktiviere(String name) {
		this.aktiv = name != null && profil(name).isPresent() ? name : null;
	}

	/** Aus → erstes Profil → … → letztes Profil → aus. Gibt das neue aktive Profil zurück (leer = aus). */
	public Optional<Profil> naechstesProfil() {
		if (profile.isEmpty()) {
			aktiv = null;
			return Optional.empty();
		}
		int i = aktivesProfil().map(profile::indexOf).orElse(-1) + 1;
		aktiv = i < profile.size() ? profile.get(i).name() : null;
		return aktivesProfil();
	}

	/** Block ins aktive Profil aufnehmen bzw. wieder entfernen. */
	public Umschaltung umschalten(String block) {
		Optional<Profil> p = aktivesProfil();
		if (p.isEmpty()) {
			return Umschaltung.KEIN_PROFIL;
		}
		Profil profil = p.get();
		if (profil.entferne(block)) {
			return Umschaltung.ENTFERNT;
		}
		profil.setze(new Eintrag(block, profil.standardFarbe(), profil.standardLeuchten()));
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

	public void loesche(Profil p) {
		profile.remove(p);
		if (p.name().equals(aktiv)) {
			aktiv = null;
		}
	}

	/** Benennt um, wenn der Name frei ist; das aktive Profil bleibt aktiv. */
	public boolean umbenennen(Profil p, String neu) {
		String name = neu == null ? "" : neu.strip();
		if (name.isEmpty() || profil(name).filter(x -> x != p).isPresent()) {
			return false;
		}
		boolean warAktiv = p.name().equals(aktiv);
		p.umbenennen(name);
		if (warAktiv) {
			aktiv = name;
		}
		return true;
	}
}
