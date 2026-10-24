package de.itscalle96.blockmarker.kern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prüft den Pack-Generator mit nachgebauten Vanilla-Dateien (Aufbau wie in Minecraft 26.3). */
class PackBauerTest {
	private final Map<String, byte[]> vanilla = new HashMap<>();
	private final PackBauer.Quelle quelle = (ns, pfad) -> Optional.ofNullable(vanilla.get(ns + ":" + pfad));
	/** Tönt nicht wirklich, sondern markiert nur – so bleibt der Test unabhängig vom Bildformat. */
	private final PackBauer.Toener merker = (png, farbe) -> ("getoent " + Farben.alsText(farbe) + " " + new String(png, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);

	PackBauerTest() {
		datei("minecraft:blockstates/ancient_debris.json", """
				{"variants":{"":{"model":"minecraft:block/ancient_debris"}}}""");
		datei("minecraft:models/block/ancient_debris.json", """
				{"parent":"minecraft:block/cube_column","textures":{"end":"minecraft:block/ancient_debris_top","side":"minecraft:block/ancient_debris_side"}}""");
		datei("minecraft:models/block/cube_column.json", """
				{"parent":"block/cube","textures":{"particle":"#side","down":"#end","up":"#end","north":"#side","east":"#side","south":"#side","west":"#side"}}""");
		datei("minecraft:models/block/cube.json", """
				{"parent":"block/block","elements":[{"from":[0,0,0],"to":[16,16,16],"faces":{"down":{"texture":"#down","cullface":"down"},"up":{"texture":"#up","cullface":"up"}}}]}""");
		datei("minecraft:models/block/block.json", """
				{"gui_light":"side"}""");
		datei("minecraft:textures/block/ancient_debris_top.png", "TOP");
		datei("minecraft:textures/block/ancient_debris_side.png", "SIDE");

		datei("minecraft:blockstates/cobblestone.json", """
				{"variants":{"":[{"model":"minecraft:block/cobblestone"},{"model":"minecraft:block/cobblestone_mirrored"}]}}""");
		datei("minecraft:models/block/cobblestone.json", """
				{"parent":"minecraft:block/cube_all","textures":{"all":"minecraft:block/cobblestone"}}""");
		datei("minecraft:models/block/cobblestone_mirrored.json", """
				{"parent":"minecraft:block/cube_mirrored_all","textures":{"all":"minecraft:block/cobblestone"}}""");
		datei("minecraft:models/block/cube_all.json", """
				{"parent":"block/cube","textures":{"particle":"#all","down":"#all","up":"#all"}}""");
		datei("minecraft:models/block/cube_mirrored_all.json", """
				{"parent":"block/cube_mirrored","textures":{"particle":"#all","down":"#all","up":"#all"}}""");
		datei("minecraft:models/block/cube_mirrored.json", """
				{"parent":"block/cube","elements":[{"from":[0,0,0],"to":[16,16,16],"faces":{"up":{"texture":"#up","uv":[16,0,0,16]}}}]}""");
		datei("minecraft:textures/block/cobblestone.png", "COBBLE");

		datei("minecraft:blockstates/sea_lantern.json", """
				{"variants":{"":{"model":"minecraft:block/sea_lantern"}}}""");
		datei("minecraft:models/block/sea_lantern.json", """
				{"parent":"minecraft:block/cube_all","textures":{"all":"minecraft:block/sea_lantern"}}""");
		datei("minecraft:textures/block/sea_lantern.png", "LANTERN");
		datei("minecraft:textures/block/sea_lantern.png.mcmeta", "{\"animation\":{\"frametime\":5}}");

		datei("minecraft:blockstates/redstone_wire.json", """
				{"multipart":[{"apply":{"model":"minecraft:block/cobblestone"}},{"when":{"north":"side"},"apply":[{"model":"minecraft:block/cobblestone","y":90}]}]}""");
	}

	private void datei(String schluessel, String inhalt) {
		vanilla.put(schluessel, inhalt.getBytes(StandardCharsets.UTF_8));
	}

	private static JsonObject json(Map<String, byte[]> dateien, String schluessel) {
		byte[] b = dateien.get(schluessel);
		assertTrue(b != null, "fehlt: " + schluessel + " in " + dateien.keySet());
		return JsonParser.parseString(new String(b, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	@Test
	void blockstateZeigtAufGetoentesModell() {
		Map<String, byte[]> d = PackBauer.baue(List.of(new Eintrag("minecraft:ancient_debris", 0xFF2BD6, false)), quelle, merker);
		String modell = json(d, "minecraft:blockstates/ancient_debris.json")
				.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString();
		assertEquals("blockmarker:block/m/minecraft/ancient_debris/minecraft/block/ancient_debris", modell);

		JsonObject m = json(d, "blockmarker:models/block/m/minecraft/ancient_debris/minecraft/block/ancient_debris.json");
		assertEquals("minecraft:block/ancient_debris", m.get("parent").getAsString(), "Original bleibt Eltern-Modell");
		JsonObject t = m.getAsJsonObject("textures");
		assertEquals("blockmarker:block/m/ff2bd6/minecraft/block/ancient_debris_side", t.get("side").getAsString());
		assertEquals("blockmarker:block/m/ff2bd6/minecraft/block/ancient_debris_top", t.get("end").getAsString());
		assertFalse(t.has("north"), "#Verweise bleiben beim Original und zeigen dann auf die getönten Texturen");
		assertFalse(m.has("elements"), "ohne Leuchten keine eigenen Elemente");

		assertEquals("getoent #FF2BD6 SIDE", new String(d.get("blockmarker:textures/block/m/ff2bd6/minecraft/block/ancient_debris_side.png"), StandardCharsets.UTF_8));
	}

	@Test
	void leuchtenSetztLichtAufAlleElemente() {
		Map<String, byte[]> d = PackBauer.baue(List.of(new Eintrag("minecraft:ancient_debris", 0xFF2BD6, true)), quelle, merker);
		JsonObject m = json(d, "blockmarker:models/block/m/minecraft/ancient_debris/minecraft/block/ancient_debris.json");
		JsonArray el = m.getAsJsonArray("elements");
		assertEquals(1, el.size());
		assertEquals(15, el.get(0).getAsJsonObject().get("light_emission").getAsInt());
		assertTrue(el.get(0).getAsJsonObject().getAsJsonObject("faces").has("up"), "Flächen bleiben erhalten");
		assertFalse(vanilla.get("minecraft:models/block/cube.json").length == 0);
		assertFalse(new String(vanilla.get("minecraft:models/block/cube.json"), StandardCharsets.UTF_8).contains("light_emission"), "Original unverändert");
	}

	@Test
	void mehrereModelleUndGemeinsameTexturNurEinmal() {
		Map<String, byte[]> d = PackBauer.baue(List.of(new Eintrag("minecraft:cobblestone", 0xFF8000, true)), quelle, merker);
		JsonArray varianten = json(d, "minecraft:blockstates/cobblestone.json").getAsJsonObject("variants").getAsJsonArray("");
		assertEquals("blockmarker:block/m/minecraft/cobblestone/minecraft/block/cobblestone", varianten.get(0).getAsJsonObject().get("model").getAsString());
		assertEquals("blockmarker:block/m/minecraft/cobblestone/minecraft/block/cobblestone_mirrored", varianten.get(1).getAsJsonObject().get("model").getAsString());
		JsonObject gespiegelt = json(d, "blockmarker:models/block/m/minecraft/cobblestone/minecraft/block/cobblestone_mirrored.json");
		JsonElement uv = gespiegelt.getAsJsonArray("elements").get(0).getAsJsonObject().getAsJsonObject("faces").getAsJsonObject("up").get("uv");
		assertEquals("[16,0,0,16]", uv.toString(), "nächstgelegene Elemente der Eltern-Kette werden übernommen");
		long texturen = d.keySet().stream().filter(k -> k.endsWith(".png")).count();
		assertEquals(1, texturen);
	}

	@Test
	void andereBloeckeBleibenUnberuehrt() {
		Map<String, byte[]> d = PackBauer.baue(List.of(new Eintrag("minecraft:cobblestone", 0xFF8000, false)), quelle, merker);
		assertFalse(d.containsKey("minecraft:models/block/cobblestone.json"), "Originalmodell wird nicht überschrieben (Treppen usw. nutzen es)");
		assertFalse(d.containsKey("minecraft:textures/block/cobblestone.png"), "Originaltextur wird nicht überschrieben");
		assertTrue(d.keySet().stream().allMatch(k -> k.startsWith("blockmarker:") || k.equals("minecraft:blockstates/cobblestone.json")), d.keySet().toString());
	}

	@Test
	void animierteTexturBehaeltIhreAnimation() {
		Map<String, byte[]> d = PackBauer.baue(List.of(new Eintrag("minecraft:sea_lantern", 0x00F0FF, false)), quelle, merker);
		assertArrayEquals(vanilla.get("minecraft:textures/block/sea_lantern.png.mcmeta"),
				d.get("blockmarker:textures/block/m/00f0ff/minecraft/block/sea_lantern.png.mcmeta"));
	}

	@Test
	void multipartWirdAuchUmgebogen() {
		Map<String, byte[]> d = PackBauer.baue(List.of(new Eintrag("minecraft:redstone_wire", 0xFF1A1A, false)), quelle, merker);
		String text = new String(d.get("minecraft:blockstates/redstone_wire.json"), StandardCharsets.UTF_8);
		assertFalse(text.contains("\"minecraft:block/cobblestone\""), text);
		assertTrue(text.contains("\"y\":90"), "Drehungen bleiben erhalten: " + text);
	}

	@Test
	void unbekannterBlockErzeugtNichts() {
		Map<String, byte[]> d = PackBauer.baue(List.of(new Eintrag("minecraft:air", 0xFF1A1A, true), new Eintrag("mod:kaputt", 1, true)), quelle, merker);
		assertTrue(d.isEmpty());
	}

	@Test
	void zweiFarbenZweiTexturen() {
		Map<String, byte[]> d = PackBauer.baue(List.of(
				new Eintrag("minecraft:cobblestone", 0xFF8000, false),
				new Eintrag("minecraft:sea_lantern", 0x00F0FF, false)), quelle, merker);
		assertTrue(d.containsKey("blockmarker:textures/block/m/ff8000/minecraft/block/cobblestone.png"));
		assertTrue(d.containsKey("blockmarker:textures/block/m/00f0ff/minecraft/block/sea_lantern.png"));
	}

	@Test
	void pngWirdWirklichGetoent() throws IOException {
		BufferedImage bild = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
		bild.setRGB(0, 0, 0xFF808080);
		bild.setRGB(1, 0, 0x00000000);
		ByteArrayOutputStream aus = new ByteArrayOutputStream();
		ImageIO.write(bild, "png", aus);

		byte[] getoent = new PngToener().toene(aus.toByteArray(), 0x00F0FF);
		BufferedImage ergebnis = ImageIO.read(new ByteArrayInputStream(getoent));
		assertEquals(Toenung.toene(0xFF808080, 0x00F0FF), ergebnis.getRGB(0, 0));
		assertEquals(0, ergebnis.getRGB(1, 0) >>> 24, "durchsichtige Pixel bleiben durchsichtig");
		assertNotEquals(0xFF808080, ergebnis.getRGB(0, 0));
	}
}
