package de.itscalle96.blockmarker.ui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Öffnet den Block-Marker-Bildschirm über „Mods → Block-Marker → Einstellungen“. */
public final class ModMenuEinstieg implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return MarkerBildschirm::new;
	}
}
