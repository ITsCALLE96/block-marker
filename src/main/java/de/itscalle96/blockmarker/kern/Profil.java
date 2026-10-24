package de.itscalle96.blockmarker.kern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Ein benanntes Profil mit seinen markierten Blöcken (Reihenfolge bleibt erhalten). */
public final class Profil {
	private String name;
	private final List<Eintrag> eintraege = new ArrayList<>();

	public Profil(String name) {
		this.name = name;
	}

	public Profil(String name, List<Eintrag> eintraege) {
		this(name);
		eintraege.forEach(this::setze);
	}

	public String name() {
		return name;
	}

	public void umbenennen(String neu) {
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
