package de.itscalle96.blockmarker.mixin;

import de.itscalle96.blockmarker.pack.MarkerQuelle;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Arrays;

/** Hängt das Block-Marker-Pack an die Resourcepack-Liste des Clients (nicht an Datapacks). */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/packs/repository/PackRepository;<init>([Lnet/minecraft/server/packs/repository/RepositorySource;)V"))
	private RepositorySource[] blockmarker$packAnmelden(RepositorySource[] quellen) {
		RepositorySource[] neu = Arrays.copyOf(quellen, quellen.length + 1);
		neu[quellen.length] = new MarkerQuelle();
		return neu;
	}
}
