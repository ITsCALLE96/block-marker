package de.itscalle96.blockmarker.mixin;

import de.itscalle96.blockmarker.pack.MarkerPack;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** Gibt dem Block-Marker-Pack beim Öffnen die Packs darunter, damit es die Original-Dateien lesen kann. */
@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {
	@Inject(method = "openAllSelected", at = @At("RETURN"))
	private void blockmarker$untenSetzen(CallbackInfoReturnable<List<PackResources>> rueckgabe) {
		List<PackResources> packs = rueckgabe.getReturnValue();
		for (int i = 0; i < packs.size(); i++) {
			if (packs.get(i) instanceof MarkerPack marker) {
				marker.setzeUnten(new ArrayList<>(packs.subList(0, i)));
			}
		}
	}
}
