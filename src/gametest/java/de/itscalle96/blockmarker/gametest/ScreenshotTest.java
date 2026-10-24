package de.itscalle96.blockmarker.gametest;

import de.itscalle96.blockmarker.BlockMarkerClient;
import de.itscalle96.blockmarker.kern.Einstellungen;
import de.itscalle96.blockmarker.ui.MarkerBildschirm;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Test-Client: baut eine kleine Szene mit Ancient Debris, Diamant-/Tiefenschiefer-Diamanterz, Bruchstein,
 * bemoostem Bruchstein und normalem Stein (zum Vergleich), prüft die Tasten und fotografiert jedes Profil
 * bei Tag und Nacht, ohne und mit Shader. Bilder: build/block-marker-screenshots/.
 */
public final class ScreenshotTest implements FabricClientGameTest {
	private static final Path ZIEL = Path.of(System.getProperty("blockmarker.screenshots", "screenshots-block-marker"));
	private static final List<String> SZENE = List.of(
			"ancient_debris", "diamond_ore", "deepslate_diamond_ore", "cobblestone", "mossy_cobblestone", "stone");

	@Override
	public void runTest(ClientGameTestContext ctx) {
		// Deutsch wie im Pack, damit auch die Suche mit deutschen Blocknamen geprüft wird
		CompletableFuture<Void> sprache = ctx.computeOnClient(mc -> {
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
			BlockMarkerClient.einstellungen().aktiviere(null);
			mc.options.languageCode = "de_de";
			mc.getLanguageManager().setSelected("de_de");
			return BlockMarkerClient.jetztNeuLaden();
		});
		ctx.waitFor(mc -> sprache.isDone() && mc.gui.overlay() == null, 20 * 120);
		try (TestSingleplayerContext welt = ctx.worldBuilder().create()) {
			welt.getConnection().waitForChunksDownload();
			BlockPos spieler = welt.getServer().computeOnServer(server -> welt.getConnection().getServerPlayer().blockPosition());
			int x = spieler.getX(), y = spieler.getY(), z = spieler.getZ();
			welt.getServer().runCommand("weather clear");
			welt.getServer().runCommand(String.format("fill %d %d %d %d %d %d stone", x - 5, y, z + 6, x + 5, y + 3, z + 6));
			for (int i = 0; i < SZENE.size(); i++) {
				welt.getServer().runCommand(String.format("setblock %d %d %d %s", x - 3 + i, y, z + 4, SZENE.get(i)));
				welt.getServer().runCommand(String.format("setblock %d %d %d %s", x - 3 + i, y + 1, z + 5, SZENE.get(i)));
			}
			welt.getServer().runCommand(String.format("tp @p %d.0 %d %d.5 0 20", x, y, z));
			welt.getConnection().waitForChunksRender();

			pruefeTasten(ctx, welt, new BlockPos(x - 3, y, z + 4));

			for (boolean shader : new boolean[] {false, true}) {
				setzeShader(ctx, welt, shader);
				for (String profil : new String[] {null, "Netherite", "Diamanten", "Tempel"}) {
					aktiviere(ctx, welt, profil);
					for (String zeit : new String[] {"noon", "midnight"}) {
						welt.getServer().runCommand("time set " + zeit);
						ctx.waitTicks(shader ? 40 : 5);
						foto(ctx, (profil == null ? "aus" : profil.toLowerCase()) + "_"
								+ (zeit.equals("noon") ? "tag" : "nacht") + "_" + (shader ? "shader" : "ohne-shader"));
					}
				}
			}
			setzeShader(ctx, welt, false);

			// Menü: Profil Diamanten gewählt, Suche nach „diamant“ (deutsche Namen)
			ctx.runOnClient(mc -> BlockMarkerClient.einstellungen().aktiviere("Diamanten"));
			ctx.setScreen(() -> new MarkerBildschirm(null));
			ctx.waitForScreen(MarkerBildschirm.class);
			// Suchfeld hat beim Öffnen den Fokus – einfach lostippen
			ctx.getInput().typeChars("diamant");
			ctx.waitTicks(2);
			int treffer = ctx.computeOnClient(mc -> ((MarkerBildschirm) mc.gui.screen()).anzahlTreffer());
			pruefe(treffer >= 3 && treffer <= 6, "Suche nach „diamant“ findet Diamanterz, Tiefenschiefer-Diamanterz und Diamantblock, Treffer: " + treffer);
			ctx.waitTicks(5);
			foto(ctx, "menue", false);
			ctx.setScreen(() -> null);
		}
	}

	/** Echte Tastendrücke: Profil durchschalten, dann Ancient Debris per Anschauen raus und wieder rein. */
	private static void pruefeTasten(ClientGameTestContext ctx, TestSingleplayerContext welt, BlockPos debris) {
		KeyMapping profil = KeyMapping.get("key.blockmarker.profil");
		KeyMapping block = KeyMapping.get("key.blockmarker.block");
		Einstellungen e = BlockMarkerClient.einstellungen();

		ctx.getInput().pressKey(profil);
		ctx.waitTick();
		pruefe("Netherite".equals(e.aktivName()), "Profil-Taste schaltet auf Netherite, ist: " + e.aktivName());
		warteAufNeuladen(ctx, welt);

		ctx.getInput().lookAt(debris);
		ctx.waitTicks(2);
		ctx.getInput().pressKey(block);
		ctx.waitTick();
		pruefe(!e.aktivesProfil().orElseThrow().enthaelt("minecraft:ancient_debris"), "Block-Taste entfernt Ancient Debris");
		ctx.getInput().pressKey(block);
		ctx.waitTick();
		pruefe(e.aktivesProfil().orElseThrow().enthaelt("minecraft:ancient_debris"), "Block-Taste fügt Ancient Debris wieder hinzu");
		warteAufNeuladen(ctx, welt);

		for (int i = 0; i < 3; i++) {
			ctx.getInput().pressKey(profil);
			ctx.waitTick();
		}
		pruefe(e.aktivName() == null, "nach dem letzten Profil ist der Block-Marker aus, ist: " + e.aktivName());
		warteAufNeuladen(ctx, welt);
		welt.getServer().runCommand("tp @p ~ ~ ~ 0 20");
		ctx.waitTicks(2);
	}

	private static void warteAufNeuladen(ClientGameTestContext ctx, TestSingleplayerContext welt) {
		ctx.waitTicks(25);
		ctx.waitFor(mc -> mc.gui.overlay() == null, 20 * 120);
		welt.getConnection().waitForChunksRender();
	}

	private static void aktiviere(ClientGameTestContext ctx, TestSingleplayerContext welt, String profil) {
		CompletableFuture<Void> fertig = ctx.computeOnClient(mc -> {
			BlockMarkerClient.einstellungen().aktiviere(profil);
			return BlockMarkerClient.jetztNeuLaden();
		});
		ctx.waitFor(mc -> fertig.isDone() && mc.gui.overlay() == null, 20 * 120);
		welt.getConnection().waitForChunksRender();
	}

	private static void setzeShader(ClientGameTestContext ctx, TestSingleplayerContext welt, boolean an) {
		ctx.runOnClient(mc -> IrisApi.getInstance().getConfig().setShadersEnabledAndApply(an));
		ctx.waitTicks(an ? 100 : 20);
		welt.getConnection().waitForChunksRender();
		if (an) {
			pruefe(ctx.computeOnClient(mc -> IrisApi.getInstance().isShaderPackInUse()), "Shader-Pack ist aktiv (shaderpacks/ + config/iris.properties vorhanden?)");
		}
	}

	private static void foto(ClientGameTestContext ctx, String name) {
		foto(ctx, name, true);
	}

	/** Menüs nur in Fenstergröße – vergrößert würde die Oberfläche nicht mitskaliert. */
	private static void foto(ClientGameTestContext ctx, String name, boolean gross) {
		TestScreenshotOptions optionen = TestScreenshotOptions.of(name).disableCounterPrefix();
		Path bild = ctx.takeScreenshot(gross ? optionen.withSize(1280, 720) : optionen);
		try {
			Files.createDirectories(ZIEL);
			Files.copy(bild, ZIEL.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
		} catch (java.io.IOException fehler) {
			throw new java.io.UncheckedIOException(fehler);
		}
	}

	private static void pruefe(boolean bedingung, String text) {
		if (!bedingung) {
			throw new AssertionError(text);
		}
	}
}
