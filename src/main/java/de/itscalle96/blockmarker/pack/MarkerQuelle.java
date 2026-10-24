package de.itscalle96.blockmarker.pack;

import de.itscalle96.blockmarker.BlockMarkerClient;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlagSet;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Meldet das Block-Marker-Pack bei Minecraft an: immer aktiv, immer ganz oben. */
public final class MarkerQuelle implements RepositorySource {
	public static final String PACK_ID = "blockmarker";

	private static final PackLocationInfo ORT = new PackLocationInfo(PACK_ID,
			Component.translatable("blockmarker.pack.titel"), PackSource.BUILT_IN, Optional.empty());

	@Override
	public void loadPacks(Consumer<Pack> ziel) {
		Pack.ResourcesSupplier lieferant = new Pack.ResourcesSupplier() {
			@Override
			public PackMetadataResources openMetadata(PackLocationInfo ort) {
				return new PackMetadataResources() {
					@Override
					public PackLocationInfo location() {
						return ort;
					}

					@Override
					public IoSupplier<InputStream> getRootResource(String... pfad) {
						return null;
					}

					@Override
					public <T> T getMetadataSection(MetadataSectionType<T> typ) {
						return null;
					}

					@Override
					public void close() {
					}
				};
			}

			@Override
			public Stream<PackResources> openResources(PackLocationInfo ort, Pack.Metadata metadaten) {
				return Stream.of(new MarkerPack(ort, BlockMarkerClient.aktiveEintraege()));
			}
		};
		Pack.Metadata metadaten = new Pack.Metadata(Component.translatable("blockmarker.pack.beschreibung"),
				PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());
		ziel.accept(new Pack(ORT, lieferant, metadaten, new PackSelectionConfig(true, Pack.Position.TOP, true)));
	}
}
