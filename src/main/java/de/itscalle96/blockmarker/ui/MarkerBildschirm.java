package de.itscalle96.blockmarker.ui;

import de.itscalle96.blockmarker.BlockMarkerClient;
import de.itscalle96.blockmarker.kern.BlockIds;
import de.itscalle96.blockmarker.kern.Einstellungen;
import de.itscalle96.blockmarker.kern.Eintrag;
import de.itscalle96.blockmarker.kern.Farben;
import de.itscalle96.blockmarker.kern.Profil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.IntConsumer;

/**
 * Einstellungen in drei Spalten: Profile · Blöcke im gewählten Profil (mit Farbe/Leuchten) · Blocksuche.
 * Änderungen werden sofort gespeichert; neu geladen wird beim Schließen, wenn sich am aktiven Profil etwas geändert hat.
 */
public final class MarkerBildschirm extends Screen {
	private static final int RAND = 10;
	private static final int OBEN = 34;
	private static final int MAX_TREFFER = 300;

	private final Screen zurueck;
	private final Einstellungen e = BlockMarkerClient.einstellungen();
	private Profil gewaehlt;
	private String gewaehlterBlock;
	private String suche = "";
	private List<Block> alleBloecke;

	private ProfilListe profilListe;
	private BlockListe blockListe;
	private SuchListe suchListe;
	private EditBox namensFeld;
	private EditBox farbFeld;
	private Button aktivKnopf;
	private Button leuchtKnopf;
	private Button entfernenKnopf;
	private Button loeschenKnopf;
	private Button umbenennenKnopf;
	private final List<FarbFeld> farbFelder = new ArrayList<>();

	public MarkerBildschirm(Screen zurueck) {
		super(Component.translatable("blockmarker.titel"));
		this.zurueck = zurueck;
		this.gewaehlt = e.aktivesProfil().orElse(e.profile().isEmpty() ? null : e.profile().getFirst());
	}

	private int spalte() {
		return (width - 4 * RAND) / 3;
	}

	private int x(int nr) {
		return RAND + nr * (spalte() + RAND);
	}

	@Override
	protected void init() {
		if (alleBloecke == null) {
			alleBloecke = BuiltInRegistries.BLOCK.stream()
					.filter(b -> !b.defaultBlockState().isAir())
					.sorted(Comparator.comparing(b -> b.getName().getString().toLowerCase(Locale.ROOT)))
					.toList();
		}
		int w = spalte();
		int listenUnten = height - 110;

		// Spalte 1: Profile
		profilListe = addRenderableWidget(new ProfilListe(w, listenUnten - OBEN, x(0), OBEN));
		namensFeld = addRenderableWidget(new EditBox(font, x(0), height - 104, w, 20, Component.translatable("blockmarker.profilname")));
		namensFeld.setMaxLength(32);
		namensFeld.setHint(Component.translatable("blockmarker.profilname"));
		int halb = (w - 4) / 2;
		addRenderableWidget(Button.builder(Component.translatable("blockmarker.knopf.neu"), b -> neuesProfil())
				.bounds(x(0), height - 80, halb, 20).build());
		umbenennenKnopf = addRenderableWidget(Button.builder(Component.translatable("blockmarker.knopf.umbenennen"), b -> umbenennen())
				.bounds(x(0) + halb + 4, height - 80, halb, 20).build());
		loeschenKnopf = addRenderableWidget(Button.builder(Component.translatable("blockmarker.knopf.loeschen"), b -> loeschen())
				.bounds(x(0), height - 56, halb, 20).build());
		aktivKnopf = addRenderableWidget(Button.builder(Component.empty(), b -> aktivUmschalten())
				.bounds(x(0) + halb + 4, height - 56, halb, 20).build());

		// Spalte 2: Blöcke im Profil + Farbe/Leuchten
		blockListe = addRenderableWidget(new BlockListe(w, listenUnten - OBEN, x(1), OBEN));
		farbFelder.clear();
		int feld = Math.max(10, Math.min(16, (w + 2) / Farben.VORGABEN.size() - 2));
		for (int i = 0; i < Farben.VORGABEN.size(); i++) {
			Farben.Vorgabe v = Farben.VORGABEN.get(i);
			farbFelder.add(addRenderableWidget(new FarbFeld(x(1) + i * (feld + 2), height - 104, feld, v, this::setzeFarbe)));
		}
		farbFeld = addRenderableWidget(new EditBox(font, x(1), height - 80, 64, 20, Component.translatable("blockmarker.farbcode")));
		farbFeld.setMaxLength(7);
		farbFeld.setHint(Component.literal("#RRGGBB"));
		farbFeld.setResponder(text -> Farben.lies(text).ifPresent(this::setzeFarbeAusFeld));
		leuchtKnopf = addRenderableWidget(Button.builder(Component.empty(), b -> leuchtenUmschalten())
				.bounds(x(1) + 68, height - 80, w - 68, 20).build());
		entfernenKnopf = addRenderableWidget(Button.builder(Component.translatable("blockmarker.knopf.entfernen"), b -> blockEntfernen())
				.bounds(x(1), height - 56, w, 20).build());

		// Spalte 3: Suche
		EditBox suchFeld = addRenderableWidget(new EditBox(font, x(2), OBEN, w, 20, Component.translatable("blockmarker.suche")));
		suchFeld.setHint(Component.translatable("blockmarker.suche").withStyle(EditBox.SEARCH_HINT_STYLE));
		suchFeld.setValue(suche);
		suchFeld.setResponder(text -> {
			suche = text;
			aktualisiereSuche();
		});
		suchListe = addRenderableWidget(new SuchListe(w, height - 60 - (OBEN + 24), x(2), OBEN + 24));

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(width / 2 - 100, height - 28, 200, 20).build());

		aktualisiereAlles();
	}

	// ---------- Aktionen ----------

	private void neuesProfil() {
		gewaehlt = e.neuesProfil(namensFeld.getValue().isBlank()
				? Component.translatable("blockmarker.neuesprofil").getString() : namensFeld.getValue());
		gewaehlterBlock = null;
		namensFeld.setValue("");
		BlockMarkerClient.geaendert();
		aktualisiereAlles();
	}

	private void umbenennen() {
		if (gewaehlt != null && e.umbenennen(gewaehlt, namensFeld.getValue())) {
			namensFeld.setValue("");
			BlockMarkerClient.geaendert();
			aktualisiereAlles();
		}
	}

	private void loeschen() {
		if (gewaehlt != null) {
			e.loesche(gewaehlt);
			gewaehlt = e.profile().isEmpty() ? null : e.profile().getFirst();
			gewaehlterBlock = null;
			BlockMarkerClient.geaendert();
			aktualisiereAlles();
		}
	}

	private void aktivUmschalten() {
		if (gewaehlt != null) {
			e.aktiviere(gewaehlt.name().equals(e.aktivName()) ? null : gewaehlt.name());
			BlockMarkerClient.geaendert();
			aktualisiereAlles();
		}
	}

	private void blockHinzufuegen(Block block) {
		if (gewaehlt == null) {
			return;
		}
		String id = BuiltInRegistries.BLOCK.getKey(block).toString();
		if (gewaehlt.enthaelt(id)) {
			gewaehlt.entferne(id);
			if (id.equals(gewaehlterBlock)) {
				gewaehlterBlock = null;
			}
		} else {
			gewaehlt.setze(new Eintrag(id, gewaehlt.standardFarbe(), gewaehlt.standardLeuchten()));
			gewaehlterBlock = id;
		}
		BlockMarkerClient.geaendert();
		aktualisiereAlles();
	}

	private void blockEntfernen() {
		if (gewaehlt != null && gewaehlterBlock != null) {
			gewaehlt.entferne(gewaehlterBlock);
			gewaehlterBlock = null;
			BlockMarkerClient.geaendert();
			aktualisiereAlles();
		}
	}

	private Optional<Eintrag> eintrag() {
		return gewaehlt == null || gewaehlterBlock == null ? Optional.empty() : gewaehlt.finde(gewaehlterBlock);
	}

	private void setzeFarbe(int rgb) {
		eintrag().ifPresent(ein -> {
			gewaehlt.setze(ein.mitFarbe(rgb));
			farbFeld.setValue(Farben.alsText(rgb));
			BlockMarkerClient.geaendert();
			aktualisiereAuswahl();
		});
	}

	private void setzeFarbeAusFeld(int rgb) {
		eintrag().filter(ein -> ein.farbe() != rgb).ifPresent(ein -> {
			gewaehlt.setze(ein.mitFarbe(rgb));
			BlockMarkerClient.geaendert();
			blockListe.aktualisiere();
		});
	}

	private void leuchtenUmschalten() {
		eintrag().ifPresent(ein -> {
			gewaehlt.setze(ein.mitLeuchten(!ein.leuchtet()));
			BlockMarkerClient.geaendert();
			aktualisiereAuswahl();
		});
	}

	// ---------- Anzeige aktualisieren ----------

	private void aktualisiereAlles() {
		profilListe.aktualisiere();
		aktualisiereAuswahl();
		aktualisiereSuche();
	}

	private void aktualisiereAuswahl() {
		boolean hatProfil = gewaehlt != null;
		boolean istAktiv = hatProfil && gewaehlt.name().equals(e.aktivName());
		aktivKnopf.active = hatProfil;
		aktivKnopf.setMessage(Component.translatable(istAktiv ? "blockmarker.knopf.ausschalten" : "blockmarker.knopf.aktivieren"));
		loeschenKnopf.active = hatProfil;
		umbenennenKnopf.active = hatProfil;

		Optional<Eintrag> ein = eintrag();
		boolean hatBlock = ein.isPresent();
		farbFeld.active = hatBlock;
		farbFeld.setEditable(hatBlock);
		leuchtKnopf.active = hatBlock;
		entfernenKnopf.active = hatBlock;
		farbFelder.forEach(f -> {
			f.active = hatBlock;
			f.markiert = ein.map(x -> x.farbe() == f.vorgabe.rgb()).orElse(false);
		});
		leuchtKnopf.setMessage(Component.translatable(ein.map(Eintrag::leuchtet).orElse(false)
				? "blockmarker.knopf.leuchtet.an" : "blockmarker.knopf.leuchtet.aus"));
		leuchtKnopf.setTooltip(Tooltip.create(Component.translatable("blockmarker.knopf.leuchtet.hilfe")));
		String text = ein.map(x -> Farben.alsText(x.farbe())).orElse("");
		if (!Farben.lies(farbFeld.getValue()).equals(ein.map(Eintrag::farbe))) {
			farbFeld.setValue(text);
		}
		blockListe.aktualisiere();
	}

	private void aktualisiereSuche() {
		String s = suche.strip().toLowerCase(Locale.ROOT);
		List<Block> treffer = new ArrayList<>();
		for (Block b : alleBloecke) {
			if (treffer.size() >= MAX_TREFFER) {
				break;
			}
			if (s.isEmpty() || b.getName().getString().toLowerCase(Locale.ROOT).contains(s)
					|| BuiltInRegistries.BLOCK.getKey(b).toString().contains(s)) {
				treffer.add(b);
			}
		}
		suchListe.zeige(treffer);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mausX, int mausY, float delta) {
		super.extractRenderState(g, mausX, mausY, delta);
		g.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
		g.text(font, Component.translatable("blockmarker.spalte.profile"), x(0), OBEN - 11, 0xFFA0A0A0);
		g.text(font, gewaehlt == null ? Component.translatable("blockmarker.spalte.bloecke.leer")
				: Component.translatable("blockmarker.spalte.bloecke", gewaehlt.name()), x(1), OBEN - 11, 0xFFA0A0A0);
		g.text(font, Component.translatable("blockmarker.spalte.suche"), x(2), OBEN - 11, 0xFFA0A0A0);
		Component status = e.aktivName() == null ? Component.translatable("blockmarker.status.aus")
				: Component.translatable("blockmarker.status.aktiv", e.aktivName());
		g.text(font, status, x(2), height - 50, 0xFFE0E0E0);
	}

	@Override
	public void onClose() {
		BlockMarkerClient.bildschirmGeschlossen();
		minecraft.setScreenAndShow(zurueck);
	}

	private static ItemStack icon(Block block) {
		return block.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(block.asItem());
	}

	private static Component blockName(String id) {
		Identifier key = Identifier.tryParse(BlockIds.normalisiere(id));
		return key == null ? Component.literal(id)
				: BuiltInRegistries.BLOCK.getOptional(key).<Component>map(Block::getName).orElse(Component.literal(id));
	}

	private static ItemStack icon(String id) {
		Identifier key = Identifier.tryParse(BlockIds.normalisiere(id));
		return key == null ? ItemStack.EMPTY : BuiltInRegistries.BLOCK.getOptional(key).map(MarkerBildschirm::icon).orElse(ItemStack.EMPTY);
	}

	// ---------- Listen ----------

	private abstract static class Liste<E extends ObjectSelectionList.Entry<E>> extends ObjectSelectionList<E> {
		Liste(int breite, int hoehe, int x, int y, int zeile) {
			super(Minecraft.getInstance(), breite, hoehe, y, zeile);
			setX(x);
		}

		@Override
		public int getRowWidth() {
			return width - 10;
		}

		@Override
		protected int scrollBarX() {
			return getX() + width - 6;
		}
	}

	private final class ProfilListe extends Liste<ProfilListe.Zeile> {
		ProfilListe(int breite, int hoehe, int x, int y) {
			super(breite, hoehe, x, y, 16);
		}

		void aktualisiere() {
			List<Zeile> zeilen = e.profile().stream().map(Zeile::new).toList();
			replaceEntries(zeilen);
			zeilen.stream().filter(z -> z.profil == gewaehlt).findFirst().ifPresent(this::setSelected);
		}

		final class Zeile extends ObjectSelectionList.Entry<Zeile> {
			final Profil profil;

			Zeile(Profil profil) {
				this.profil = profil;
			}

			@Override
			public void extractContent(GuiGraphicsExtractor g, int mausX, int mausY, boolean hover, float delta) {
				boolean aktiv = profil.name().equals(e.aktivName());
				g.text(font, (aktiv ? "● " : "  ") + profil.name(), getContentX() + 2, getContentY() + 3,
						aktiv ? 0xFF55FF55 : 0xFFFFFFFF);
				String anzahl = String.valueOf(profil.eintraege().size());
				g.text(font, anzahl, getContentRight() - font.width(anzahl) - 2, getContentY() + 3, 0xFF808080);
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent ereignis, boolean doppelt) {
				gewaehlt = profil;
				gewaehlterBlock = null;
				ProfilListe.this.setSelected(this);
				if (doppelt) {
					aktivUmschalten();
				} else {
					aktualisiereAuswahl();
				}
				return true;
			}

			@Override
			public Component getNarration() {
				return Component.literal(profil.name());
			}
		}
	}

	private final class BlockListe extends Liste<BlockListe.Zeile> {
		BlockListe(int breite, int hoehe, int x, int y) {
			super(breite, hoehe, x, y, 20);
		}

		void aktualisiere() {
			List<Zeile> zeilen = gewaehlt == null ? List.of() : gewaehlt.eintraege().stream().map(Zeile::new).toList();
			replaceEntries(zeilen);
			zeilen.stream().filter(z -> z.eintrag.block().equals(gewaehlterBlock)).findFirst().ifPresent(this::setSelected);
		}

		final class Zeile extends ObjectSelectionList.Entry<Zeile> {
			final Eintrag eintrag;
			final ItemStack icon;
			final Component name;

			Zeile(Eintrag eintrag) {
				this.eintrag = eintrag;
				this.icon = icon(eintrag.block());
				this.name = blockName(eintrag.block());
			}

			@Override
			public void extractContent(GuiGraphicsExtractor g, int mausX, int mausY, boolean hover, float delta) {
				int x = getContentX();
				int y = getContentY();
				g.item(icon, x, y);
				int rechts = getContentRight();
				g.fill(rechts - 14, y + 2, rechts - 2, y + 14, 0xFF000000);
				g.fill(rechts - 13, y + 3, rechts - 3, y + 13, 0xFF000000 | eintrag.farbe());
				if (eintrag.leuchtet()) {
					g.text(font, "☀", rechts - 26, y + 4, 0xFFFFE600);
				}
				g.text(font, font.plainSubstrByWidth(name.getString(), rechts - x - 50), x + 20, y + 4, 0xFFFFFFFF);
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent ereignis, boolean doppelt) {
				gewaehlterBlock = eintrag.block();
				BlockListe.this.setSelected(this);
				aktualisiereAuswahl();
				return true;
			}

			@Override
			public Component getNarration() {
				return name;
			}
		}
	}

	private final class SuchListe extends Liste<SuchListe.Zeile> {
		SuchListe(int breite, int hoehe, int x, int y) {
			super(breite, hoehe, x, y, 20);
		}

		void zeige(List<Block> bloecke) {
			replaceEntries(bloecke.stream().map(Zeile::new).toList());
			setScrollAmount(0);
		}

		final class Zeile extends ObjectSelectionList.Entry<Zeile> {
			final Block block;
			final String id;
			final ItemStack icon;

			Zeile(Block block) {
				this.block = block;
				this.id = BuiltInRegistries.BLOCK.getKey(block).toString();
				this.icon = icon(block);
			}

			@Override
			public void extractContent(GuiGraphicsExtractor g, int mausX, int mausY, boolean hover, float delta) {
				int x = getContentX();
				int y = getContentY();
				g.item(icon, x, y);
				boolean drin = gewaehlt != null && gewaehlt.enthaelt(id);
				String zeichen = drin ? "✔" : "+";
				int rechts = getContentRight();
				g.text(font, zeichen, rechts - font.width(zeichen) - 2, y + 4, drin ? 0xFF55FF55 : 0xFFA0A0A0);
				g.text(font, font.plainSubstrByWidth(block.getName().getString(), rechts - x - 34), x + 20, y + 4, 0xFFFFFFFF);
				if (hover) {
					g.setTooltipForNextFrame(Component.literal(id).withStyle(net.minecraft.ChatFormatting.GRAY), mausX, mausY);
				}
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent ereignis, boolean doppelt) {
				blockHinzufuegen(block);
				return true;
			}

			@Override
			public Component getNarration() {
				return block.getName();
			}
		}
	}

	/** Klickbares Farbfeld für eine Vorgabe-Farbe. */
	private static final class FarbFeld extends AbstractWidget {
		final Farben.Vorgabe vorgabe;
		final IntConsumer aktion;
		boolean markiert;

		FarbFeld(int x, int y, int groesse, Farben.Vorgabe vorgabe, IntConsumer aktion) {
			super(x, y, groesse, groesse + 4, Component.literal(vorgabe.name()));
			this.vorgabe = vorgabe;
			this.aktion = aktion;
			setTooltip(Tooltip.create(Component.literal(vorgabe.name() + " " + Farben.alsText(vorgabe.rgb()))));
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mausX, int mausY, float delta) {
			int rahmen = markiert ? 0xFFFFFFFF : isHoveredOrFocused() ? 0xFFA0A0A0 : 0xFF000000;
			g.fill(getX(), getY(), getX() + width, getY() + height, rahmen);
			int farbe = (active ? 0xFF000000 : 0x60000000) | vorgabe.rgb();
			g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, farbe);
		}

		@Override
		public void onClick(MouseButtonEvent ereignis, boolean doppelt) {
			aktion.accept(vorgabe.rgb());
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput ausgabe) {
			ausgabe.add(NarratedElementType.TITLE, getMessage());
		}
	}
}
