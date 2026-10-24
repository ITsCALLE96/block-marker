package de.itscalle96.blockmarker.kern;

/** Ein markierter Block im Profil: Block-ID („minecraft:ancient_debris“), Farbe 0xRRGGBB, leuchtet ja/nein. */
public record Eintrag(String block, int farbe, boolean leuchtet) {
	public Eintrag {
		block = BlockIds.normalisiere(block);
		farbe &= 0xFFFFFF;
	}

	public Eintrag mitFarbe(int neueFarbe) {
		return new Eintrag(block, neueFarbe, leuchtet);
	}

	public Eintrag mitLeuchten(boolean neu) {
		return new Eintrag(block, farbe, neu);
	}
}
