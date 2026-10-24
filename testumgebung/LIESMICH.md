# Testumgebung für `./gradlew runClientGameTest`

Wird vor jedem Testlauf in den Test-Ordner kopiert.

- `config/iris.properties` wählt den Shader (Complementary Reimagined, wie im Weihnachts-Modpack), Shader starten aus.
- `shaderpacks/` ist nicht im Git (Lizenz/Größe). Einmal herunterladen:
  `curl -L -o testumgebung/shaderpacks/ComplementaryReimagined_r5.9.3.zip https://cdn.modrinth.com/data/HVnmMxH1/versions/Bqen1mJX/ComplementaryReimagined_r5.9.3.zip`
