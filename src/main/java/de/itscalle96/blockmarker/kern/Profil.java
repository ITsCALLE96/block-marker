package de.itscalle96.blockmarker.kern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ein Profil mit seinen markierten Blöcken (Reihenfolge bleibt erhalten). Vorgefertigte Profile tragen einen
 * Vorlagen-Schlüssel und zeigen ihren Namen in der Spielsprache, bis man sie umbenennt.
 */
public final class Profil {
	private String name;
	private final String vorlage;
	private final List<Eintrag> eintraege = new ArrayList<>();

	public Profil(String name) {
		this(name, (String) null);
	}

	private Profil(String name, String vorlage) {
		this.name = name;
		this.vorlage = vorlage;
	}

	public Profil(String name, List<Eintrag> eintraege) {
		this(name);
		eintraege.forEach(this::setze);
	}

	/** Vorgefertigtes Profil, z. B. „diamanten“ → „Diamanten“ (de_de) bzw. „Diamonds“ (en_us). */
	public static Profil vorlage(String schluessel, List<Eintrag> eintraege) {
		Profil p = new Profil(null, schluessel);
		eintraege.forEach(p::setze);
		return p;
	}

	/** Profil aus der Datei: eigener Name (darf null sein) und/oder Vorlagen-Schlüssel. */
	static Profil geladen(String name, String vorlage) {
		return new Profil(name, vorlage);
	}

	/** Angezeigter Name: der eigene, sonst der übersetzte Vorlagen-Name. */
	public String anzeigeName() {
		if (name != null) {
			return name;
		}
		return vorlage != null ? Texte.vorlageName(vorlage) : "?";
	}

	/** Selbst vergebener Name, oder null bei einer nicht umbenannten Vorlage. */
	public String eigenerName() {
		return name;
	}

	public String vorlage() {
		return vorlage;
	}

	void umbenennen(String neu) {
		this.name = neu;
	}

	public List<Eintrag> eintraege() {
		return List.copyOf(eintraege);
	}

	public Optional<Eintrag> finde(String block) {
		String id = BlockIds.normalisiere(block);
		return eintraege.stream().filter(e -> e.block().equals(id)).findFirst();
	}

	public boolean enthaelt(String block) {
		return finde(block).isPresent();
	}

	/** Fügt hinzu oder ersetzt den vorhandenen Eintrag desselben Blocks. */
	public void setze(Eintrag eintrag) {
		for (int i = 0; i < eintraege.size(); i++) {
			if (eintraege.get(i).block().equals(eintrag.block())) {
				eintraege.set(i, eintrag);
				return;
			}
		}
		eintraege.add(eintrag);
	}

	public boolean entferne(String block) {
		String id = BlockIds.normalisiere(block);
		return eintraege.removeIf(e -> e.block().equals(id));
	}

	/** Farbe für neu hinzugefügte Blöcke: die des ersten Eintrags, sonst Magenta. */
	public int standardFarbe() {
		return eintraege.isEmpty() ? 0xFF2BD6 : eintraege.getFirst().farbe();
	}

	/** Leuchten für neu hinzugefügte Blöcke: wie der erste Eintrag, sonst an. */
	public boolean standardLeuchten() {
		return eintraege.isEmpty() || eintraege.getFirst().leuchtet();
	}
}
