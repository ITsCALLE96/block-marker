Highlight the blocks you are looking for: Block-Marker tints chosen blocks in a bright color and can make them glow, even in the dark. The block's pattern stays visible, so it still looks like itself, just easier to spot.

![Profiles off, Netherite, Diamonds and Temple, by day and night, without and with Complementary shader](https://raw.githubusercontent.com/ITsCALLE96/block-marker/main/docs/screenshots.jpg)

**Not an X-ray mod.** Block-Marker only changes how a block *looks*. Nothing becomes transparent, nothing shows through walls, there is no ore finder. A block you cannot see stays invisible. There is no server part.

## Features

- **Profiles** you switch with one key. After the last profile, Block-Marker turns off.
- **Three ready-made profiles:** Netherite (Ancient Debris), Diamonds (diamond ore and deepslate diamond ore), Temple (cobblestone, mossy cobblestone).
- **Add blocks** by looking at a block and pressing a key (press again to remove it), or with the search in the menu, which shows a preview icon.
- **Colors:** bright presets or your own color code (`#RRGGBB`).
- **"Glows" switch** per block: the block is fully bright at night and in caves.
- **Search** finds blocks by their name in the game language, their English or German name, or the block ID ("Ancient", "Antiker", "cobble", `minecraft:cobblestone`). Case does not matter, parts of words are enough.
- **Languages:** English and German. Any other game language shows English.

## Keys

| Key (default) | Action |
|---|---|
| , (comma) | next profile, after the last one off |
| . (period) | add the block you are looking at to the active profile, press again to remove it (normal reach only) |
| unbound | open the Block-Marker menu |

The menu is also available via **Mod Menu → Block-Marker → Settings**. Settings are stored in `config/blockmarker.json`.

## How it works

Block-Marker registers an invisible resource pack that always stays on top. For each block of the active profile it overrides only that block's blockstate and points to copies of its models with **tinted copies of the textures** from the packs below. Your own resource packs and server packs keep working. "Glows" sets `light_emission: 15` on the model elements. After a change the game reloads its resource packs (like F3+T). When you switch quickly, it reloads only once.

Limits: stairs, slabs and similar blocks with the same texture stay unchanged. Blocks without a regular model (chests, signs, …) cannot be tinted.

## Compatibility

Requires Fabric API. Mod Menu is optional. Tested with Sodium 0.9.2 and Iris 1.11.7, with and without the Complementary Reimagined shader.

Source code and issues: [GitHub](https://github.com/ITsCALLE96/block-marker) · License: MIT

---

## Deutsch

Block-Marker färbt gewählte Blöcke kräftig ein und lässt sie auf Wunsch leuchten, auch im Dunkeln. Das Muster des Blocks bleibt erkennbar.

**Kein X-Ray:** Die Mod ändert nur, wie ein Block aussieht. Nichts wird durchsichtig, nichts leuchtet durch Wände, es gibt keinen Erzsucher. Einen Block, den man nicht sieht, sieht man auch mit Block-Marker nicht. Einen Server-Teil gibt es nicht.

- **Profile**, per Taste durchschalten, nach dem letzten wieder aus. Vorgefertigt: Netherite, Diamanten, Tempel.
- **Blöcke hinzufügen:** Block anschauen und Taste drücken (noch einmal drücken nimmt ihn heraus) oder im Menü suchen, mit Vorschau-Icon.
- **Farben:** knallige Vorgaben oder eigener Farbcode, dazu pro Block der Schalter „Leuchtet“.
- **Suche** über den Namen in der Spielsprache, den englischen oder deutschen Namen oder die Block-ID.
- **Tasten:** Komma = nächstes Profil, Punkt = angeschauten Block aufnehmen oder entfernen, Menü-Taste frei belegbar (oder Mod Menu → Block-Marker → Einstellungen).
- **Sprachen:** Deutsch und Englisch.

Benötigt Fabric API, Mod Menu ist optional. Getestet mit Sodium und Iris, mit und ohne Shader.
