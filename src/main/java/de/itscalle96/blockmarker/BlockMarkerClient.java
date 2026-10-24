package de.itscalle96.blockmarker;

import com.mojang.blaze3d.platform.InputConstants;
import de.itscalle96.blockmarker.kern.Einstellungen;
import de.itscalle96.blockmarker.kern.Eintrag;
import de.itscalle96.blockmarker.kern.Profil;
import de.itscalle96.blockmarker.kern.ProfilSpeicher;
import de.itscalle96.blockmarker.kern.Texte;
import de.itscalle96.blockmarker.ui.MarkerBildschirm;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Einstieg der Mod. Hält die Einstellungen, verarbeitet die Tasten und lädt die Resourcepacks neu,
 * sobald sich am aktiven Profil etwas geändert hat (gebündelt, damit schnelles Durchschalten nur einmal lädt).
 */
public final class BlockMarkerClient implements ClientModInitializer {
	public static final String MOD_ID = "blockmarker";
	public static final Logger LOG = LoggerFactory.getLogger("Block-Marker");
	/** Wartezeit nach der letzten Änderung, bevor neu geladen wird (Ticks, 20 = 1 s). */
	private static final int NEULADEN_VERZOEGERUNG = 15;

	private static Einstellungen einstellungen = Einstellungen.standard();
	/** Stand, mit dem das Pack zuletzt gebaut wurde – nur bei echten Unterschieden neu laden. */
	private static List<Eintrag> geladen = List.of();
	private static int neuladenIn = -1;

	private static KeyMapping profilTaste;
	private static KeyMapping blockTaste;
	private static KeyMapping menueTaste;

	@Override
	public void onInitializeClient() {
		// Namen der vorgefertigten Profile in der Spielsprache (de_de/en_us, sonst Englisch)
		Texte.setzeUebersetzer(schluessel -> Language.getInstance().getOrDefault(schluessel));
		einstellungen = ProfilSpeicher.laden(datei());
		geladen = aktiveEintraege();

		KeyMapping.Category kategorie = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "tasten"));
		profilTaste = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.blockmarker.profil",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_COMMA, kategorie));
		blockTaste = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.blockmarker.block",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_PERIOD, kategorie));
		menueTaste = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.blockmarker.menue",
				InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), kategorie));

		ClientTickEvents.END_CLIENT_TICK.register(BlockMarkerClient::tick);
	}

	public static Einstellungen einstellungen() {
		return einstellungen;
	}

	/** Was das Pack gerade zeigen soll: die Blöcke des aktiven Profils, sonst nichts. */
	public static List<Eintrag> aktiveEintraege() {
		return einstellungen.aktivesProfil().map(Profil::eintraege).orElse(List.of());
	}

	private static Path datei() {
		return FabricLoader.getInstance().getConfigDir().resolve("blockmarker.json");
	}

	/** Nach jeder Änderung aufrufen: speichert sofort und lädt (gebündelt) neu, falls nötig. */
	public static void geaendert() {
		try {
			ProfilSpeicher.speichern(datei(), einstellungen);
		} catch (IOException fehler) {
			LOG.error("Block-Marker: Einstellungen nicht gespeichert", fehler);
		}
		// Raus und gleich wieder rein ergibt den alten Stand – dann nicht neu laden
		neuladenIn = aktiveEintraege().equals(geladen) ? -1 : NEULADEN_VERZOEGERUNG;
	}

	/** Der Einstellungs-Bildschirm lädt erst beim Schließen neu – dann aber sofort. */
	public static void bildschirmGeschlossen() {
		if (!aktiveEintraege().equals(geladen)) {
			neuladenIn = 0;
		}
	}

	/** Für den Test-Client: sofort neu laden, ohne auf die Bündelung zu warten. */
	public static java.util.concurrent.CompletableFuture<Void> jetztNeuLaden() {
		neuladenIn = -1;
		geladen = aktiveEintraege();
		return Minecraft.getInstance().reloadResourcePacks();
	}

	private static void tick(Minecraft mc) {
		while (profilTaste.consumeClick()) {
			naechstesProfil(mc);
		}
		while (blockTaste.consumeClick()) {
			angeschautenBlockUmschalten(mc);
		}
		while (menueTaste.consumeClick()) {
			mc.setScreenAndShow(new MarkerBildschirm(mc.gui.screen()));
		}
		if (neuladenIn > 0) {
			neuladenIn--;
		} else if (neuladenIn == 0 && mc.gui.overlay() == null && !(mc.gui.screen() instanceof MarkerBildschirm)) {
			jetztNeuLaden();
		}
	}

	private static void naechstesProfil(Minecraft mc) {
		Optional<Profil> neu = einstellungen.naechstesProfil();
		meldung(mc, neu.map(p -> Component.translatable("blockmarker.meldung.profil", p.anzeigeName()).withStyle(ChatFormatting.AQUA))
				.orElse(Component.translatable("blockmarker.meldung.aus").withStyle(ChatFormatting.GRAY)));
		geaendert();
	}

	/**
	 * Nimmt den Block, auf den das Fadenkreuz zeigt (normale Reichweite, wie beim Abbauen), ins Profil auf
	 * oder entfernt ihn. Nichts hinter Wänden – nur was man ohnehin anklicken könnte.
	 */
	private static void angeschautenBlockUmschalten(Minecraft mc) {
		if (mc.level == null || !(mc.hitResult instanceof BlockHitResult treffer) || treffer.getType() != HitResult.Type.BLOCK) {
			meldung(mc, Component.translatable("blockmarker.meldung.keinblock").withStyle(ChatFormatting.GRAY));
			return;
		}
		BlockPos pos = treffer.getBlockPos();
		Block block = mc.level.getBlockState(pos).getBlock();
		String id = BuiltInRegistries.BLOCK.getKey(block).toString();
		Component name = block.getName();
		switch (einstellungen.umschalten(id)) {
			case KEIN_PROFIL -> meldung(mc, Component.translatable("blockmarker.meldung.keinprofil").withStyle(ChatFormatting.GRAY));
			case HINZUGEFUEGT -> {
				Eintrag e = einstellungen.aktivesProfil().flatMap(p -> p.finde(id)).orElseThrow();
				meldung(mc, Component.translatable("blockmarker.meldung.hinzu", name, einstellungen.aktivName())
						.withColor(e.farbe()));
				geaendert();
			}
			case ENTFERNT -> {
				meldung(mc, Component.translatable("blockmarker.meldung.raus", name, einstellungen.aktivName())
						.withStyle(ChatFormatting.GRAY));
				geaendert();
			}
		}
	}

	private static void meldung(Minecraft mc, Component text) {
		if (mc.player != null) {
			mc.player.sendOverlayMessage(text);
		}
	}

}
