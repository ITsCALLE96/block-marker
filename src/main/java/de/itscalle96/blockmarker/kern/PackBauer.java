package de.itscalle96.blockmarker.kern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Baut den Inhalt des Laufzeit-Resourcepacks für die markierten Blöcke.
 *
 * <p>Pro Block wird nur dessen {@code blockstates}-Datei überschrieben. Sie zeigt auf eigene Modell-Kopien
 * ({@code blockmarker:block/m/…}), die das Originalmodell als Eltern behalten, aber alle Texturen durch
 * getönte Kopien ersetzen. „Leuchtet“ übernimmt zusätzlich die Elemente des Originals mit
 * {@code light_emission: 15}. Andere Blöcke mit denselben Texturen (Treppen, Stufen …) bleiben unverändert.
 * Es wird nichts unsichtbar oder durchsichtig gemacht.
 */
public final class PackBauer {
	public static final String NS = "blockmarker";
	private static final Gson GSON = new Gson();
	private static final int MAX_ELTERN = 32;

	/** Liest Dateien aus den Resourcepacks unterhalb des Block-Marker-Packs. Pfad relativ zu {@code assets/<ns>/}. */
	@FunctionalInterface
	public interface Quelle {
		Optional<byte[]> lies(String namensraum, String pfad);
	}

	/** Tönt ein PNG und gibt das neue PNG zurück. */
	@FunctionalInterface
	public interface Toener {
		byte[] toene(byte[] png, int farbe);
	}

	private final Quelle quelle;
	private final Toener toener;
	/** Schlüssel „namensraum:pfad“ (Pfad relativ zu assets/namensraum/), Wert = Dateiinhalt. */
	private final Map<String, byte[]> dateien = new LinkedHashMap<>();
	private final Map<String, JsonObject> modellCache = new HashMap<>();

	public PackBauer(Quelle quelle, Toener toener) {
		this.quelle = quelle;
		this.toener = toener;
	}

	public static Map<String, byte[]> baue(Iterable<Eintrag> eintraege, Quelle quelle, Toener toener) {
		PackBauer bauer = new PackBauer(quelle, toener);
		for (Eintrag e : eintraege) {
			bauer.fuegeHinzu(e);
		}
		return bauer.dateien;
	}

	/** Gibt false zurück, wenn der Block keine Blockstate-Datei hat (z. B. Luft) – dann bleibt er unverändert. */
	public boolean fuegeHinzu(Eintrag e) {
		String ns = BlockIds.namensraum(e.block());
		String pfad = BlockIds.pfad(e.block());
		Optional<JsonObject> zustand = json(ns, "blockstates/" + pfad + ".json");
		if (zustand.isEmpty()) {
			return false;
		}
		JsonObject neu = zustand.get().deepCopy();
		Map<String, String> umbenannt = new HashMap<>();
		ersetzeModelle(neu, e, umbenannt);
		if (umbenannt.values().stream().allMatch(v -> v == null)) {
			return false;
		}
		dateien.put(ns + ":blockstates/" + pfad + ".json", GSON.toJson(neu).getBytes(StandardCharsets.UTF_8));
		return true;
	}

	public Map<String, byte[]> dateien() {
		return dateien;
	}

	private void ersetzeModelle(JsonElement knoten, Eintrag e, Map<String, String> umbenannt) {
		if (knoten.isJsonArray()) {
			for (JsonElement kind : knoten.getAsJsonArray()) {
				ersetzeModelle(kind, e, umbenannt);
			}
		} else if (knoten.isJsonObject()) {
			JsonObject o = knoten.getAsJsonObject();
			for (Map.Entry<String, JsonElement> eintrag : o.entrySet()) {
				JsonElement wert = eintrag.getValue();
				if (eintrag.getKey().equals("model") && wert.isJsonPrimitive()) {
					String alt = BlockIds.normalisiere(wert.getAsString());
					if (!umbenannt.containsKey(alt)) {
						umbenannt.put(alt, baueModell(alt, e));
					}
					String neu = umbenannt.get(alt);
					if (neu != null) {
						eintrag.setValue(new JsonPrimitive(neu));
					}
				} else {
					ersetzeModelle(wert, e, umbenannt);
				}
			}
		}
	}

	/** Legt die Modell-Kopie an und gibt ihre ID zurück, oder null, wenn das Modell nicht lesbar ist. */
	private String baueModell(String modell, Eintrag e) {
		JsonObject texturen = new JsonObject();
		JsonArray elemente = null;
		String aktuell = modell;
		for (int tiefe = 0; aktuell != null && tiefe < MAX_ELTERN; tiefe++) {
			Optional<JsonObject> m = modellJson(aktuell);
			if (m.isEmpty()) {
				if (tiefe == 0) {
					return null;
				}
				break;
			}
			JsonObject o = m.get();
			if (o.has("textures") && o.get("textures").isJsonObject()) {
				for (Map.Entry<String, JsonElement> t : o.getAsJsonObject("textures").entrySet()) {
					if (!texturen.has(t.getKey())) {
						texturen.add(t.getKey(), t.getValue());
					}
				}
			}
			if (elemente == null && o.has("elements") && o.get("elements").isJsonArray()) {
				elemente = o.getAsJsonArray("elements");
			}
			aktuell = o.has("parent") ? BlockIds.normalisiere(o.get("parent").getAsString()) : null;
		}

		JsonObject neu = new JsonObject();
		neu.addProperty("parent", modell);
		JsonObject neueTexturen = new JsonObject();
		for (Map.Entry<String, JsonElement> t : texturen.entrySet()) {
			JsonElement getoent = toeneTexturWert(t.getValue(), e.farbe());
			if (getoent != null) {
				neueTexturen.add(t.getKey(), getoent);
			}
		}
		neu.add("textures", neueTexturen);
		if (e.leuchtet() && elemente != null) {
			JsonArray hell = elemente.deepCopy();
			for (JsonElement el : hell) {
				if (el.isJsonObject()) {
					el.getAsJsonObject().addProperty("light_emission", 15);
				}
			}
			neu.add("elements", hell);
		}
		String id = NS + ":block/m/" + BlockIds.namensraum(e.block()) + "/" + BlockIds.pfad(e.block())
				+ "/" + BlockIds.namensraum(modell) + "/" + BlockIds.pfad(modell);
		dateien.put(NS + ":models/" + BlockIds.pfad(id) + ".json", GSON.toJson(neu).getBytes(StandardCharsets.UTF_8));
		return id;
	}

	/** „#verweis“ bleibt, eine echte Textur wird getönt kopiert. Auch die Objektform {"sprite": …} wird unterstützt. */
	private JsonElement toeneTexturWert(JsonElement wert, int farbe) {
		if (wert.isJsonPrimitive()) {
			String s = wert.getAsString();
			if (s.startsWith("#")) {
				return null;
			}
			String neu = toeneTextur(s, farbe);
			return neu == null ? null : new JsonPrimitive(neu);
		}
		if (wert.isJsonObject() && wert.getAsJsonObject().has("sprite")) {
			JsonObject o = wert.getAsJsonObject().deepCopy();
			String s = o.get("sprite").getAsString();
			if (s.startsWith("#")) {
				return null;
			}
			String neu = toeneTextur(s, farbe);
			if (neu == null) {
				return null;
			}
			o.addProperty("sprite", neu);
			return o;
		}
		return null;
	}

	private String toeneTextur(String textur, int farbe) {
		String ns = BlockIds.namensraum(textur);
		String pfad = BlockIds.pfad(textur);
		String id = NS + ":block/m/" + String.format(Locale.ROOT, "%06x", farbe) + "/" + ns + "/" + pfad;
		String ziel = NS + ":textures/" + BlockIds.pfad(id) + ".png";
		if (dateien.containsKey(ziel)) {
			return id;
		}
		Optional<byte[]> png = quelle.lies(ns, "textures/" + pfad + ".png");
		if (png.isEmpty()) {
			return null;
		}
		dateien.put(ziel, toener.toene(png.get(), farbe));
		quelle.lies(ns, "textures/" + pfad + ".png.mcmeta").ifPresent(meta -> dateien.put(ziel + ".mcmeta", meta));
		return id;
	}

	private Optional<JsonObject> modellJson(String modell) {
		if (modellCache.containsKey(modell)) {
			return Optional.ofNullable(modellCache.get(modell));
		}
		Optional<JsonObject> o = json(BlockIds.namensraum(modell), "models/" + BlockIds.pfad(modell) + ".json");
		modellCache.put(modell, o.orElse(null));
		return o;
	}

	private Optional<JsonObject> json(String ns, String pfad) {
		return quelle.lies(ns, pfad).flatMap(bytes -> {
			try {
				JsonElement el = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
				return el.isJsonObject() ? Optional.of(el.getAsJsonObject()) : Optional.empty();
			} catch (RuntimeException kaputt) {
				return Optional.empty();
			}
		});
	}
}
