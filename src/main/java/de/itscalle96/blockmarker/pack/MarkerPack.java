package de.itscalle96.blockmarker.pack;

import de.itscalle96.blockmarker.BlockMarkerClient;
import de.itscalle96.blockmarker.kern.Eintrag;
import de.itscalle96.blockmarker.kern.PackBauer;
import de.itscalle96.blockmarker.kern.PngToener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Das Laufzeit-Resourcepack. Es wird bei jedem Neuladen frisch geöffnet, bekommt die darunterliegenden
 * Packs (Vanilla, Mods, Resourcepacks, Server-Pack) und baut daraus beim ersten Zugriff die getönten Dateien.
 */
public final class MarkerPack implements PackResources {
	private final PackLocationInfo ort;
	private final List<Eintrag> eintraege;
	private List<PackResources> unten = List.of();
	private Map<String, byte[]> dateien;

	public MarkerPack(PackLocationInfo ort, List<Eintrag> eintraege) {
		this.ort = ort;
		this.eintraege = List.copyOf(eintraege);
	}

	/** Packs unterhalb dieses Packs, von unten nach oben (so wie Minecraft sie lädt). */
	public void setzeUnten(List<PackResources> packs) {
		this.unten = List.copyOf(packs);
		this.dateien = null;
	}

	private synchronized Map<String, byte[]> dateien() {
		if (dateien == null) {
			long start = System.nanoTime();
			try {
				dateien = PackBauer.baue(eintraege, this::liesUnten, new PngToener());
			} catch (RuntimeException fehler) {
				BlockMarkerClient.LOG.error("Block-Marker: Pack konnte nicht gebaut werden", fehler);
				dateien = Map.of();
			}
			BlockMarkerClient.LOG.info("Block-Marker: {} Blöcke, {} Dateien in {} ms", eintraege.size(), dateien.size(),
					(System.nanoTime() - start) / 1_000_000);
		}
		return dateien;
	}

	private Optional<byte[]> liesUnten(String ns, String pfad) {
		Identifier id = Identifier.tryBuild(ns, pfad);
		if (id == null) {
			return Optional.empty();
		}
		for (int i = unten.size() - 1; i >= 0; i--) {
			IoSupplier<InputStream> quelle = unten.get(i).getResource(PackType.CLIENT_RESOURCES, id);
			if (quelle != null) {
				try (InputStream in = quelle.get()) {
					return Optional.of(in.readAllBytes());
				} catch (IOException fehler) {
					BlockMarkerClient.LOG.warn("Block-Marker: {} nicht lesbar", id, fehler);
				}
			}
		}
		return Optional.empty();
	}

	@Override
	public IoSupplier<InputStream> getRootResource(String... pfad) {
		return null;
	}

	@Override
	public IoSupplier<InputStream> getResource(PackType typ, Identifier id) {
		if (typ != PackType.CLIENT_RESOURCES) {
			return null;
		}
		byte[] inhalt = dateien().get(id.getNamespace() + ":" + id.getPath());
		return inhalt == null ? null : () -> new ByteArrayInputStream(inhalt);
	}

	@Override
	public void listResources(PackType typ, String namensraum, String pfad, ResourceOutput ausgabe) {
		if (typ != PackType.CLIENT_RESOURCES) {
			return;
		}
		String praefix = namensraum + ":" + pfad + "/";
		List<Map.Entry<String, byte[]>> treffer = new ArrayList<>();
		for (Map.Entry<String, byte[]> e : dateien().entrySet()) {
			if (e.getKey().startsWith(praefix)) {
				treffer.add(e);
			}
		}
		for (Map.Entry<String, byte[]> e : treffer) {
			Identifier id = Identifier.tryParse(e.getKey());
			if (id != null) {
				byte[] inhalt = e.getValue();
				ausgabe.accept(id, () -> new ByteArrayInputStream(inhalt));
			}
		}
	}

	@Override
	public Set<String> getNamespaces(PackType typ) {
		Set<String> namen = new TreeSet<>();
		if (typ == PackType.CLIENT_RESOURCES) {
			for (String schluessel : dateien().keySet()) {
				namen.add(schluessel.substring(0, schluessel.indexOf(':')));
			}
		}
		return namen;
	}

	@Override
	public PackLocationInfo location() {
		return ort;
	}

	@Override
	public <T> T getMetadataSection(MetadataSectionType<T> typ) {
		return null;
	}

	@Override
	public void close() {
	}
}
