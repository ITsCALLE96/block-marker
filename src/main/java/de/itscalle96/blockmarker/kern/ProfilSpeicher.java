package de.itscalle96.blockmarker.kern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Speichert die Einstellungen als lesbare JSON-Datei (config/blockmarker.json).
 * Vorlagen werden mit ihrem Schlüssel gespeichert ({@code "vorlage": "diamanten"}), damit ihr Name der
 * Spielsprache folgt; ein eigener Name steht unter {@code "name"}. Das aktive Profil ist seine Position.
 * Kaputte Dateien werden nicht überschrieben, sondern als .kaputt gesichert; dann gelten die Vorgaben.
 */
public final class ProfilSpeicher {
	public static final int VERSION = 2;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();

	private ProfilSpeicher() {
	}

	public static String alsJson(Einstellungen e) {
		JsonObject wurzel = new JsonObject();
		wurzel.addProperty("version", VERSION);
		int aktiv = e.aktivesProfil().map(e.profile()::indexOf).orElse(-1);
		wurzel.add("aktiv", aktiv < 0 ? null : new JsonPrimitive(aktiv));
		JsonArray profile = new JsonArray();
		for (Profil p : e.profile()) {
			JsonObject po = new JsonObject();
			if (p.vorlage() != null) {
				po.addProperty("vorlage", p.vorlage());
			}
			if (p.eigenerName() != null) {
				po.addProperty("name", p.eigenerName());
			}
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
		for (JsonElement pe : wurzel.getAsJsonArray("profile")) {
			if (!pe.isJsonObject()) {
				continue;
			}
			JsonObject po = pe.getAsJsonObject();
			String name = text(po, "name");
			String vorlage = text(po, "vorlage");
			if (name == null && vorlage == null) {
				continue;
			}
			Profil p = Profil.geladen(name, vorlage);
			if (po.has("bloecke") && po.get("bloecke").isJsonArray()) {
				for (JsonElement be : po.getAsJsonArray("bloecke")) {
					if (!be.isJsonObject()) {
						continue;
					}
					JsonObject bo = be.getAsJsonObject();
					String block = text(bo, "block");
					if (block == null) {
						continue;
					}
					String farbText = text(bo, "farbe");
					int farbe = farbText == null ? 0xFF2BD6 : Farben.lies(farbText).orElse(0xFF2BD6);
					boolean leuchtet = bo.has("leuchtet") && bo.get("leuchtet").isJsonPrimitive() && bo.get("leuchtet").getAsBoolean();
					p.setze(new Eintrag(block, farbe, leuchtet));
				}
			}
			e.fuegeGeladenesHinzu(p);
		}
		JsonElement aktiv = wurzel.get("aktiv");
		if (aktiv != null && aktiv.isJsonPrimitive()) {
			JsonPrimitive a = aktiv.getAsJsonPrimitive();
			if (a.isNumber() && a.getAsInt() >= 0 && a.getAsInt() < e.profile().size()) {
				e.aktiviere(e.profile().get(a.getAsInt()));
			} else if (a.isString()) {
				e.aktiviere(a.getAsString());
			}
		}
		return e;
	}

	private static String text(JsonObject o, String schluessel) {
		JsonElement el = o.get(schluessel);
		return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
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
