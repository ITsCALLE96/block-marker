package de.itscalle96.blockmarker.kern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Speichert die Einstellungen als lesbare JSON-Datei (config/blockmarker.json).
 * Kaputte Dateien werden nicht überschrieben, sondern als .kaputt gesichert; dann gelten die Vorgaben.
 */
public final class ProfilSpeicher {
	public static final int VERSION = 1;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private ProfilSpeicher() {
	}

	public static String alsJson(Einstellungen e) {
		JsonObject wurzel = new JsonObject();
		wurzel.addProperty("version", VERSION);
		wurzel.addProperty("aktiv", e.aktivName());
		JsonArray profile = new JsonArray();
		for (Profil p : e.profile()) {
			JsonObject po = new JsonObject();
			po.addProperty("name", p.name());
			JsonArray bloecke = new JsonArray();
			for (Eintrag ein : p.eintraege()) {
				JsonObject eo = new JsonObject();
				eo.addProperty("block", ein.block());
				eo.addProperty("farbe", Farben.alsText(ein.farbe()));
				eo.addProperty("leuchtet", ein.leuchtet());
				bloecke.add(eo);
			}
			po.add("bloecke", bloecke);
			profile.add(po);
		}
		wurzel.add("profile", profile);
		return GSON.toJson(wurzel);
	}

	/** Liest JSON; unbekannte/ungültige Einträge werden übersprungen, grob kaputtes JSON wirft eine Ausnahme. */
	public static Einstellungen ausJson(String json) {
		JsonObject wurzel = JsonParser.parseString(json).getAsJsonObject();
		Einstellungen e = new Einstellungen();
		List<String> namen = new ArrayList<>();
		for (JsonElement pe : wurzel.getAsJsonArray("profile")) {
			if (!pe.isJsonObject() || !pe.getAsJsonObject().has("name")) {
				continue;
			}
			JsonObject po = pe.getAsJsonObject();
			Profil p = e.neuesProfil(po.get("name").getAsString());
			namen.add(p.name());
			if (po.has("bloecke") && po.get("bloecke").isJsonArray()) {
				for (JsonElement be : po.getAsJsonArray("bloecke")) {
					if (!be.isJsonObject()) {
						continue;
					}
					JsonObject bo = be.getAsJsonObject();
					if (!bo.has("block")) {
						continue;
					}
					int farbe = bo.has("farbe") ? Farben.lies(bo.get("farbe").getAsString()).orElse(0xFF2BD6) : 0xFF2BD6;
					boolean leuchtet = bo.has("leuchtet") && bo.get("leuchtet").getAsBoolean();
					p.setze(new Eintrag(bo.get("block").getAsString(), farbe, leuchtet));
				}
			}
		}
		if (wurzel.has("aktiv") && wurzel.get("aktiv").isJsonPrimitive()) {
			e.aktiviere(wurzel.get("aktiv").getAsString());
		}
		return e;
	}

	/** Lädt die Datei; fehlt sie, gelten die Vorgaben. Ist sie kaputt, wird sie gesichert und die Vorgaben gelten. */
	public static Einstellungen laden(Path datei) {
		if (!Files.exists(datei)) {
			return Einstellungen.standard();
		}
		try {
			return ausJson(Files.readString(datei, StandardCharsets.UTF_8));
		} catch (IOException | RuntimeException fehler) {
			try {
				Files.copy(datei, datei.resolveSibling(datei.getFileName() + ".kaputt"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignoriert) {
				// Sicherung ist nur ein Extra – die Vorgaben gelten trotzdem
			}
			return Einstellungen.standard();
		}
	}

	/** Schreibt erst in eine Hilfsdatei und ersetzt dann, damit ein Absturz keine halbe Datei hinterlässt. */
	public static void speichern(Path datei, Einstellungen e) throws IOException {
		Files.createDirectories(datei.toAbsolutePath().getParent());
		Path tmp = datei.resolveSibling(datei.getFileName() + ".tmp");
		Files.writeString(tmp, alsJson(e), StandardCharsets.UTF_8);
		try {
			Files.move(tmp, datei, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException atomarGehtNicht) {
			Files.move(tmp, datei, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
